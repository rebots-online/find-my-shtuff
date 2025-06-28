package com.example.objectdetector; // Assuming a base package name

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;

import java.util.LinkedList;
import java.util.List;

public class BoundingBoxView extends View {

    private List<DetectionResult> results;
    private final Paint boxPaint;
    private final Paint textPaint;

    // These scale factors are crucial for correctly mapping model coordinates (often normalized or relative to a specific model input size)
    // to the view's coordinates. They should be updated based on the actual preview size and model input size.
    private float scaleFactorX = 1.0f;
    private float scaleFactorY = 1.0f;
    private int previewWidth = 1; // Actual width of the camera preview (or model input)
    private int previewHeight = 1; // Actual height of the camera preview (or model input)


    public BoundingBoxView(Context context, AttributeSet attrs) {
        super(context, attrs);
        results = new LinkedList<>();

        boxPaint = new Paint();
        // Using a color from colors.xml would be better, e.g., ContextCompat.getColor(context, R.color.boundingBoxColor)
        // For now, using a default color.
        boxPaint.setColor(Color.GREEN); // Example color
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(8.0f); // Example stroke width

        textPaint = new Paint();
        textPaint.setColor(Color.GREEN); // Example text color
        textPaint.setTextSize(40.0f); // Example text size
        textPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * Sets the detection results to be drawn on the view.
     * @param detectionResults List of DetectionResult objects.
     */
    public void setResults(List<DetectionResult> detectionResults, int imageWidth, int imageHeight) {
        this.results = detectionResults != null ? detectionResults : new LinkedList<>();
        this.previewWidth = imageWidth;
        this.previewHeight = imageHeight;
        invalidate(); // Request a redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (results == null || results.isEmpty()) {
            return;
        }

        // Calculate scale factors. This assumes that the coordinates from DetectionResult
        // are relative to previewWidth and previewHeight. If they are normalized (0.0-1.0),
        // then previewWidth and previewHeight here should be the view's width and height.
        // This logic might need adjustment based on where the coordinate scaling happens.
        // For now, assuming results are in coordinates relative to the preview (image) dimensions.
        scaleFactorX = (float) getWidth() / previewWidth;
        scaleFactorY = (float) getHeight() / previewHeight;


        for (DetectionResult result : results) {
            RectF boundingBox = result.getBoundingBox();

            // Scale the bounding box coordinates
            // Important: The origin of the bounding box (e.g. top-left, center) and the coordinate system
            // (e.g. y-axis pointing down or up) must be consistent with what the model provides.
            // This example assumes top-left origin and y-axis pointing down.
            float left = boundingBox.left * scaleFactorX;
            float top = boundingBox.top * scaleFactorY;
            float right = boundingBox.right * scaleFactorX;
            float bottom = boundingBox.bottom * scaleFactorY;

            // Draw bounding box
            canvas.drawRect(left, top, right, bottom, boxPaint);

            // Draw label and score
            String label = result.getLabel() + " (" + String.format("%.2f", result.getConfidence()) + ")";
            // Adjust text position as needed
            canvas.drawText(label, left, top - 10, textPaint);
        }
    }

    // It would be good practice to define R.color.boundingBoxColor etc. in res/values/colors.xml
    // and load them in the constructor, e.g.:
    // boxPaint.setColor(ContextCompat.getColor(getContext(), R.color.your_box_color));
    // textPaint.setColor(ContextCompat.getColor(getContext(), R.color.your_text_color));
}
