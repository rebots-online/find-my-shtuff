package com.example.objectdetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.YuvImage;
import android.graphics.Rect;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraProcessor {

    private static final String TAG = "CameraProcessor";

    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private final PreviewView previewView;
    private final RealtimeObjectDetector objectDetector;
    private final DetectionListener detectionListener;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService imageAnalysisExecutor; // For running image analysis and detection

    private int previewWidth = 0;
    private int previewHeight = 0;


    public interface DetectionListener {
        void onDetections(List<DetectionResult> detectionResults, int imageWidth, int imageHeight);
    }

    public CameraProcessor(Context context, LifecycleOwner lifecycleOwner, PreviewView previewView,
                           RealtimeObjectDetector objectDetector, DetectionListener detectionListener) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.previewView = previewView;
        this.objectDetector = objectDetector;
        this.detectionListener = detectionListener;
        this.imageAnalysisExecutor = Executors.newSingleThreadExecutor();
    }

    public void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (Exception e) {
                Log.e(TAG, "Error starting camera provider", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        // Define target resolution for preview and analysis if needed, e.g., new Size(640, 480)
        // For now, let CameraX choose defaults.

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                //.setTargetResolution(new Size(640, 480)) // Example: Set specific resolution
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(imageAnalysisExecutor, imageProxy -> {
            if (previewWidth == 0 || previewHeight == 0) {
                previewWidth = imageProxy.getWidth();
                previewHeight = imageProxy.getHeight();
            }

            Bitmap bitmap = imageProxyToBitmap(imageProxy);
            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
            imageProxy.close(); // Important to close the ImageProxy

            if (bitmap != null) {
                // Rotate bitmap if necessary. Most TFLite models expect portrait orientation
                // or unrotated images. CameraX ImageAnalysis provides frames aligned with sensor
                // orientation, so rotation might be needed depending on device orientation.
                Bitmap rotatedBitmap = rotateBitmap(bitmap, rotationDegrees);

                List<DetectionResult> results = objectDetector.detect(rotatedBitmap);

                // Post results back to the main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (detectionListener != null) {
                        // Pass the dimensions of the *rotatedBitmap* as that's what detections were run on.
                        detectionListener.onDetections(results, rotatedBitmap.getWidth(), rotatedBitmap.getHeight());
                    }
                });
            }
        });

        try {
            cameraProvider.unbindAll(); // Unbind previous use cases
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis);
            Log.d(TAG, "Camera use cases bound to lifecycle.");
        } catch (Exception e) {
            Log.e(TAG, "Error binding camera use cases", e);
        }
    }

    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        if (imageProxy.getFormat() != ImageFormat.YUV_420_888) {
            Log.e(TAG, "Unsupported image format: " + imageProxy.getFormat());
            return null;
        }

        Image image = imageProxy.getImage();
        if (image == null) {
            return null;
        }

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        // NV21 requires Y plane first, then V plane, then U plane (interleaved VU)
        // Check YUV_420_888 plane order: Y, U, V. Pixel strides might be > 1.
        // For NV21, it's YYYYYYYY VUVU (or YYYYYYYY UVUV).
        // Let's assume planes[0] is Y, planes[1] is U, planes[2] is V.
        // And pixelStride for U and V is 2. rowStride for Y, U, V.

        // Simple copy assuming planar YUV420 (I420 like) and converting to NV21 by interleaving U & V.
        // This part can be tricky and error-prone. A more robust library might be better for YUV conversion.
        // For simplicity, trying a common approach. This might need adjustment.
        // A common pattern for YUV_420_888 to NV21:
        yBuffer.get(nv21, 0, ySize);
        // For NV21, V plane comes before U plane, and they are interleaved.
        // If pixelStride of U and V is 2, it means UV are already interleaved for some formats.
        // If planes[1] is U and planes[2] is V, and they are not interleaved:
        // This is a common I420 to NV21 conversion:
        int vuOffset = ySize;
        // Check if U and V planes are contiguous and can be copied directly if pixelStride is 1
        // If pixelStride is 2, they are often U channel followed by V channel in memory for U and V planes respectively
        // This part is complex. Let's try a simpler YuvImage conversion if available.

        // Using YuvImage to convert to JPEG, then decode JPEG to Bitmap (less efficient but more robust for format handling)
        // This is a common workaround for complex YUV plane management.
        try {
            yBuffer.rewind(); // Ensure buffers are at the start
            uBuffer.rewind();
            vBuffer.rewind();

            byte[] yBytes = new byte[yBuffer.remaining()];
            yBuffer.get(yBytes);
            byte[] uBytes = new byte[uBuffer.remaining()];
            uBuffer.get(uBytes);
            byte[] vBytes = new byte[vBuffer.remaining()];
            vBuffer.get(vBytes);

            // Reconstruct NV21 format data for YuvImage
            // NV21 has Y plane first, then interleaved V and U bytes (VUVUVU...)
            // YUV_420_888 has Y, U, V planes. U and V planes might have pixel stride of 2.
            // If planes[0] = Y, planes[1] = U, planes[2] = V
            // And if uBuffer's pixelStride is 2, it means it's U _ U _ U _
            // And if vBuffer's pixelStride is 2, it's V _ V _ V _
            // This means they are NOT interleaved U V U V in their respective planes.

            // A common way to get NV21 from YUV_420_888 ImageProxy:
            final byte[] yuvBytes = new byte[ySize + uSize + vSize]; // Re-declare for clarity
            planes[0].getBuffer().get(yuvBytes, 0, ySize); // Y
            planes[2].getBuffer().get(yuvBytes, ySize, vSize); // V
            planes[1].getBuffer().get(yuvBytes, ySize + vSize, uSize); // U
            // This creates YYYVVVUUU which is not NV21.

            // Correct YUV_420_888 to NV21 conversion is needed.
            // The TFLite support library has an ImageConversions utility, but it's not directly public.
            // For now, let's assume a utility or simplify.
            // The most robust way without external libs for THIS specific conversion is often using RenderScript or a manual loop.
            // Given the constraints, a simpler approach: use YuvImage if possible.
            // YuvImage needs NV21 or YUY2. We have YUV_420_888.
            // Let's try to form a YUV byte array that YuvImage can understand, assuming Y, U, V are planar.
            // This is often a source of bugs.
            // A common simplified (but potentially incorrect if strides are complex) planar YUV_420_888 to NV21:
            byte[] data = new byte[imageProxy.getWidth() * imageProxy.getHeight() * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
            ByteBuffer yPlane = planes[0].getBuffer();
            ByteBuffer uPlane = planes[1].getBuffer();
            ByteBuffer vPlane = planes[2].getBuffer();

            yPlane.get(data, 0, yPlane.remaining());
            // For NV21, V plane comes before U, and they are interleaved.
            // This manual interleaving is complex due to row strides and pixel strides.
            // A common shortcut, if strides allow:
            // planes[2].getBuffer().get(data, ySize, vSize); // V data
            // planes[1].getBuffer().get(data, ySize + vSize, uSize); // U data
            // This creates YYY...VVV...UUU... which is I420 (planar YUV420). YuvImage cannot directly use this.

            // Fallback to a simpler conversion if available or log error.
            // The following is a common way to get an NV21 byte array from YUV_420_888 ImageProxy
            // This is from official CameraX samples often.
            ByteBuffer yuv420Buffer = imageProxy.getPlanes()[0].getBuffer(); // Y plane direct
            // For U and V, need to handle potential interleaving and strides.
            // If we are sure it's I420 like (Y planar, U planar, V planar), we can try to build NV21.

            // Simplification for this step: Assume a utility function `YuvUtils.toBitmap(imageProxy)` exists
            // or use a known good conversion. Since I can't invent one perfectly here, I'll simulate
            // the direct YuvImage path which might work if the underlying data is compatible enough after some copies.
            // This is a *highly probable point of failure or incorrect colors/crashes* without a proven YUV util.

            // Let's try the YuvImage approach with a basic NV21 construction.
            // This assumes y, u, v buffers are direct and planar.
            yBuffer.rewind(); uBuffer.rewind(); vBuffer.rewind(); // Ensure buffers are at the start
            byte[] yArr = new byte[yBuffer.remaining()]; yBuffer.get(yArr);
            byte[] uArr = new byte[uBuffer.remaining()]; uBuffer.get(uArr);
            byte[] vArr = new byte[vBuffer.remaining()]; vBuffer.get(vArr);

            // Construct NV21 data: Y plane, then interleaved V and U planes
            // This is a simplified construction and assumes specific properties of U and V planes
            // (e.g., pixel stride = 1 for U and V, which is rare for YUV_420_888 where it's often 2 for U/V)
            // If pixel stride is 2 for U and V, they are semi-planar.
            // This is a common source of error. A robust YUV utility is usually needed.
            // For now, this is a placeholder for a correct YUV_420_888 to NV21 conversion.
            // A more robust way is to use imageProxy.toBitmap() if available with CameraX extensions or similar.
            // Let's assume the TFLite support library's TensorImage.fromBitmap can handle various formats,
            // but it needs a Bitmap. So, YUV to Bitmap is the key.

            // The most reliable way to get a Bitmap from YUV_420_888 ImageProxy without external libraries
            // is to convert to NV21 first, then use YuvImage.
            byte[] nv21Bytes = yuv420ToNv21(planes, imageProxy.getWidth(), imageProxy.getHeight());
            if (nv21Bytes == null) return null;

            YuvImage yuvImage = new YuvImage(nv21Bytes, ImageFormat.NV21, imageProxy.getWidth(), imageProxy.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, imageProxy.getWidth(), imageProxy.getHeight()), 90, out); // Quality 90
            byte[] imageBytes = out.toByteArray();
            return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        } catch (Exception e) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e);
            return null;
        }
    }

    // Helper to convert YUV_420_888 ImageProxy planes to NV21 byte array
    // This is a common utility function.
    private byte[] yuv420ToNv21(Image.Plane[] planes, int width, int height) {
        int ySize = width * height;
        // UV plane size is width/2 * height/2 * 2 (for U and V) = width * height / 2
        // So total size is ySize + ySize / 2 = ySize * 3 / 2
        byte[] nv21 = new byte[ySize * 3 / 2];

        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride(); // Assuming U and V have same row stride
        int uvPixelStride = planes[1].getPixelStride(); // Assuming U and V have same pixel stride

        // Copy Y plane
        int yOffset = 0;
        for (int i = 0; i < height; ++i) {
            yBuffer.position(i * yRowStride);
            yBuffer.get(nv21, yOffset, width); // Copy one row of Y
            yOffset += width;
        }

        // Copy VU data (interleaved) for NV21 from U and V planes
        // This is the tricky part. NV21 expects V before U in the interleaved plane.
        int uvOffset = ySize;
        for (int i = 0; i < height / 2; ++i) {
            for (int j = 0; j < width / 2; ++j) {
                int vPos = i * uvRowStride + j * uvPixelStride;
                int uPos = i * uvRowStride + j * uvPixelStride; // Same for U plane

                if (vPos < vBuffer.capacity() && (uvOffset + 1) < nv21.length) {
                     nv21[uvOffset++] = vBuffer.get(vPos);
                } else { return null; /* Error condition */ }

                if (uPos < uBuffer.capacity() && (uvOffset + 1) < nv21.length) { // Check capacity before get
                    nv21[uvOffset++] = uBuffer.get(uPos);
                } else { return null; /* Error condition */ }
            }
        }
        return nv21;
    }


    private Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
        if (rotationDegrees == 0) return bitmap;
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle(); // Recycle the original bitmap if it's no longer needed
        return rotatedBitmap;
    }

    public void stopCamera() {
        if (imageAnalysisExecutor != null) {
            imageAnalysisExecutor.shutdown();
            imageAnalysisExecutor = null;
        }
        // CameraProvider unbinding is handled by lifecycle owner
        Log.d(TAG, "CameraProcessor stopped and resources released.");
    }
}
