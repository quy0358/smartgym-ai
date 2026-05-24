package ntu.quy65132908.smartgym_ai.ui.pose;

public class PosePoint {
    private final float x;
    private final float y;
    private final float likelihood;

    public PosePoint(float x, float y, float likelihood) {
        this.x = x;
        this.y = y;
        this.likelihood = likelihood;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getLikelihood() {
        return likelihood;
    }

    public boolean isVisible(float threshold) {
        return likelihood >= threshold;
    }
}
