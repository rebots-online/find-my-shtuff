package com.example.objectdetector; // Assuming a base package name

import android.graphics.RectF;

/**
 * Data class to hold detection results for a single object.
 */
public class DetectionResult {
    private final RectF boundingBox;
    private final String label;
    private final float confidence;

    /**
     * Constructs a DetectionResult.
     * @param boundingBox The bounding box of the detected object.
     * @param label The label of the detected object.
     * @param confidence The confidence score of the detection.
     */
    public DetectionResult(RectF boundingBox, String label, float confidence) {
        this.boundingBox = boundingBox;
        this.label = label;
        this.confidence = confidence;
    }

    public RectF getBoundingBox() {
        return boundingBox;
    }

    public String getLabel() {
        return label;
    }

    public float getConfidence() {
        return confidence;
    }

    @Override
    public String toString() {
        return "DetectionResult{" +
                "boundingBox=" + boundingBox +
                ", label='" + label + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}
