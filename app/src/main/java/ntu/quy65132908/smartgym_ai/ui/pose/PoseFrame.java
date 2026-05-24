package ntu.quy65132908.smartgym_ai.ui.pose;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class PoseFrame {
    private static final float DEFAULT_VISIBILITY = 0.55f;

    private final Map<BodyLandmark, PosePoint> points;
    private final int imageWidth;
    private final int imageHeight;
    private final boolean frontCamera;

    public PoseFrame(Map<BodyLandmark, PosePoint> points, int imageWidth, int imageHeight, boolean frontCamera) {
        this.points = new EnumMap<>(BodyLandmark.class);
        if (points != null) {
            this.points.putAll(points);
        }
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.frontCamera = frontCamera;
    }

    public static PoseFrame empty() {
        return new PoseFrame(Collections.emptyMap(), 0, 0, true);
    }

    @Nullable
    public PosePoint get(BodyLandmark landmark) {
        return points.get(landmark);
    }

    public Map<BodyLandmark, PosePoint> getPoints() {
        return Collections.unmodifiableMap(points);
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public boolean isFrontCamera() {
        return frontCamera;
    }

    public boolean has(BodyLandmark landmark) {
        PosePoint point = get(landmark);
        return point != null && point.isVisible(DEFAULT_VISIBILITY);
    }

    public float visibilityScore() {
        if (points.isEmpty()) {
            return 0f;
        }
        float total = 0f;
        for (PosePoint point : points.values()) {
            total += clamp(point.getLikelihood());
        }
        return total / points.size();
    }

    private float clamp(float value) {
        if (value < 0f) return 0f;
        if (value > 1f) return 1f;
        return value;
    }
}
