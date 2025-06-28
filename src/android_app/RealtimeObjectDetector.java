package com.example.objectdetector;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
// TensorBuffer is not explicitly used here as TensorImage.getBuffer() provides the ByteBuffer directly
// import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
// import java.io.InputStreamReader; // Not used if using FileUtil.loadLabels
// import java.io.BufferedReader; // Not used if using FileUtil.loadLabels
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RealtimeObjectDetector {

    private static final String TAG = "RealtimeObjectDetector";
    // Model from: https://tfhub.dev/tensorflow/lite-model/ssd_mobilenet_v2_fpnlite_320x320_1/1/metadata/1?lite-format=tflite
    private static final String MODEL_FILE_NAME = "ssd_mobilenet_v2_fpnlite_320x320_coco.tflite";
    private static final String LABEL_FILE_NAME = "labelmap.txt";
    private static final int MODEL_INPUT_WIDTH = 320;
    private static final int MODEL_INPUT_HEIGHT = 320;

    private Interpreter tflite;
    private List<String> labels;
    // private final Context context; // Context not strictly needed if AssetManager is passed or not used beyond init

    private final ImageProcessor imageProcessor;

    // Normalization parameters for MobileNet SSD models (inputs in [-1,1])
    private static final float NORMALIZE_MEAN = 127.5f;
    private static final float NORMALIZE_STD = 127.5f;

    private static final float CONFIDENCE_THRESHOLD = 0.5f; // Minimum score for valid detection
    // Max number of detections the model is configured to output.
    // For ssd_mobilenet_v2_fpnlite_320x320, this is typically 100, but we can choose to process fewer.
    // Let's use a smaller number for this example to simplify output tensor declaration slightly,
    // but ensure it's less than or equal to the model's actual max output.
    // The model actually outputs up to 100 detections. We should use that.
    private static final int MAX_DETECTIONS_FROM_MODEL = 100;


    public RealtimeObjectDetector(Context context) throws IOException {
        // this.context = context; // Store if needed for other purposes
        try {
            this.tflite = new Interpreter(loadModelFile(context.getAssets(), MODEL_FILE_NAME));
            this.labels = FileUtil.loadLabels(context.getAssets(), LABEL_FILE_NAME);
            Log.d(TAG, "TensorFlow Lite model and labels loaded successfully. Labels count: " + labels.size());
        } catch (IOException e) {
            Log.e(TAG, "Error loading TFLite model or labels", e);
            throw e;
        }

        imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(MODEL_INPUT_HEIGHT, MODEL_INPUT_WIDTH, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(NORMALIZE_MEAN, NORMALIZE_STD))
                .build();
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        try (AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
             FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())) {
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    public List<DetectionResult> detect(Bitmap bitmap) {
        if (tflite == null) {
            Log.e(TAG, "TFLite model not initialized.");
            return new ArrayList<>();
        }
        if (bitmap == null) {
            Log.e(TAG, "Input bitmap is null.");
            return new ArrayList<>();
        }

        long startTime = System.currentTimeMillis();

        TensorImage tensorImage = TensorImage.fromBitmap(bitmap);
        tensorImage = imageProcessor.process(tensorImage);

        Object[] inputs = {tensorImage.getBuffer()};

        // Output map based on TF Hub documentation for ssd_mobilenet_v2_fpnlite_320x320_1
        // Output tensor names (metadata):
        // 'detection_boxes': (1, MAX_DETECTIONS_FROM_MODEL, 4) -> Type: float32
        // 'detection_classes': (1, MAX_DETECTIONS_FROM_MODEL) -> Type: float32 (indices)
        // 'detection_scores': (1, MAX_DETECTIONS_FROM_MODEL) -> Type: float32
        // 'num_detections': (1) -> Type: float32 (count)
        // Order in the output map matters and should correspond to the model's output tensor indices.
        // If using model metadata or signature runner, this can be more robust.
        // For Interpreter.runForMultipleInputsOutputs, order is by tensor index.
        // Assuming standard output order for SSD models:
        // Index 0: detection_boxes
        // Index 1: detection_classes
        // Index 2: detection_scores
        // Index 3: num_detections

        Map<Integer, Object> outputs = new HashMap<>();
        float[][][] detectionBoxes = new float[1][MAX_DETECTIONS_FROM_MODEL][4]; // ymin, xmin, ymax, xmax
        float[][] detectionClasses = new float[1][MAX_DETECTIONS_FROM_MODEL]; // Class indices
        float[][] detectionScores = new float[1][MAX_DETECTIONS_FROM_MODEL];  // Confidence scores
        float[] numDetections = new float[1]; // Number of actual detections

        outputs.put(0, detectionBoxes);    // Corresponds to 'detection_boxes'
        outputs.put(1, detectionClasses);  // Corresponds to 'detection_classes'
        outputs.put(2, detectionScores);   // Corresponds to 'detection_scores'
        outputs.put(3, numDetections);     // Corresponds to 'num_detections'

        try {
            tflite.runForMultipleInputsOutputs(inputs, outputs);
        } catch (Exception e) {
            Log.e(TAG, "Error during model inference", e);
            return new ArrayList<>();
        }

        List<DetectionResult> detectionResults = new ArrayList<>();
        int effectiveNumDetections = (int) numDetections[0];

        for (int i = 0; i < effectiveNumDetections; ++i) {
            if (i >= MAX_DETECTIONS_FROM_MODEL) break; // Safety break, though numDetections should be <= MAX_DETECTIONS_FROM_MODEL

            float score = detectionScores[0][i];
            if (score >= CONFIDENCE_THRESHOLD) {
                int classId = (int) detectionClasses[0][i];

                if (classId < 0 || classId >= labels.size()) {
                    Log.w(TAG, "Detected classId " + classId + " is out of bounds for labels list size " + labels.size() + ". Label: " + (labels.size() > classId && classId >=0 ? labels.get(classId) : "N/A"));
                    // Some models might output class_id + 1 if labelmap doesn't have background.
                    // COCO labels are typically 0-indexed for objects.
                    // If classId is, for example, 90, and labelmap has 90 entries (0-89), this is an issue.
                    // The labelmap.txt I created has 90 lines, so valid indices are 0-89.
                    // The model ssd_mobilenet_v2_fpnlite_320x320 also outputs 0-89 for COCO.
                    continue;
                }
                String label = labels.get(classId);

                // Bounding box: [ymin, xmin, ymax, xmax] in normalized coordinates (0.0 to 1.0)
                // We convert to actual pixel values of the *original* bitmap for RectF.
                // BoundingBoxView will then scale this to its own dimensions.
                float ymin = detectionBoxes[0][i][0] * bitmap.getHeight();
                float xmin = detectionBoxes[0][i][1] * bitmap.getWidth();
                float ymax = detectionBoxes[0][i][2] * bitmap.getHeight();
                float xmax = detectionBoxes[0][i][3] * bitmap.getWidth();

                RectF boundingBox = new RectF(xmin, ymin, xmax, ymax); // left, top, right, bottom
                detectionResults.add(new DetectionResult(boundingBox, label, score));
            }
        }

        long endTime = System.currentTimeMillis();
        Log.i(TAG, "Detection time: " + (endTime - startTime) + " ms for " + detectionResults.size() + " objects (out of " + effectiveNumDetections + " raw detections).");

        return detectionResults;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
            Log.d(TAG, "TFLite model closed.");
        }
    }
}
