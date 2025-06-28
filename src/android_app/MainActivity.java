package com.example.objectdetector;

import android.Manifest;
import android.content.pm.PackageManager;
// import android.graphics.Bitmap; // Not directly used here
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.List;

// It's good practice to ensure R class is imported correctly.
// If your package is 'com.example.objectdetector', then this import is implicit
// or can be 'import com.example.objectdetector.R;'

public class MainActivity extends AppCompatActivity implements CameraProcessor.DetectionListener {

    private static final String TAG = "MainActivity";
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    private PreviewView previewView;
    private BoundingBoxView boundingBoxView;

    private RealtimeObjectDetector realtimeObjectDetector;
    private CameraProcessor cameraProcessor;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean allGranted = true;
                for (Boolean granted : permissions.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    startDetection();
                } else {
                    Toast.makeText(this, "Camera permission is required to use this app.", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Make sure R.layout.activity_main is correctly referenced.
        // If the package name declared in AndroidManifest.xml (`com.example.objectdetector`)
        // matches the package name in build.gradle's `namespace` (or `applicationId` if namespace isn't set),
        // then `R` will be generated in `com.example.objectdetector.R`.
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.preview_view);
        boundingBoxView = findViewById(R.id.bounding_box_view);

        if (allPermissionsGranted()) {
            startDetection();
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startDetection() {
        try {
            // Initialize the detector first
            realtimeObjectDetector = new RealtimeObjectDetector(this);
            Log.d(TAG, "RealtimeObjectDetector initialized.");
        } catch (IOException e) {
            Log.e(TAG, "Error initializing RealtimeObjectDetector", e);
            Toast.makeText(this, "Failed to load model: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Then initialize the camera processor which uses the detector
        cameraProcessor = new CameraProcessor(this, this, previewView, realtimeObjectDetector, this);
        cameraProcessor.startCamera();
        Log.d(TAG, "CameraProcessor started.");
    }

    @Override
    public void onDetections(@NonNull List<DetectionResult> detectionResults, int imageWidth, int imageHeight) {
        // This callback is from CameraProcessor and is already on the main thread.
        if (boundingBoxView != null) {
            boundingBoxView.setResults(detectionResults, imageWidth, imageHeight);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // It's important to release resources in the correct order or handle nulls
        if (cameraProcessor != null) {
            cameraProcessor.stopCamera();
            Log.d(TAG, "CameraProcessor stopped in onDestroy.");
        }
        if (realtimeObjectDetector != null) {
            realtimeObjectDetector.close(); // Close the TFLite interpreter
            Log.d(TAG, "RealtimeObjectDetector closed in onDestroy.");
        }
    }
}
