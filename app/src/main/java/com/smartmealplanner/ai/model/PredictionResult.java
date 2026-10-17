package com.smartmealplanner.ai.model;

/**
 * Encapsulates a prediction from on-device LiteRT/TensorFlow Lite inference.
 */
public class PredictionResult {
    private final String label;
    private final String displayName;
    private final float confidence; // [0.0, 1.0]

    public PredictionResult(String label, String displayName, float confidence) {
        this.label = label;
        this.displayName = displayName;
        this.confidence = confidence;
    }

    public String getLabel() {
        return label;
    }

    public String getDisplayName() {
        return displayName;
    }

    public float getConfidence() {
        return confidence;
    }

    public int getConfidencePercentage() {
        return Math.round(confidence * 100.0f);
    }

    public boolean isHighConfidence() {
        return confidence >= 0.65f;
    }

    @Override
    public String toString() {
        return displayName + " (" + getConfidencePercentage() + "%)";
    }
}
