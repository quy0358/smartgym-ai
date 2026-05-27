package ntu.quy65132908.smartgym_ai.ui.pose;

final class PoseSignalSmoother {
    private final float alpha;
    private final float activeThreshold;
    private final float inactiveThreshold;
    private boolean initialized;
    private boolean active;
    private float score;

    PoseSignalSmoother(float alpha, float activeThreshold, float inactiveThreshold) {
        this.alpha = clamp(alpha, 0.05f, 1f);
        this.activeThreshold = clamp(activeThreshold, 0f, 1f);
        this.inactiveThreshold = clamp(inactiveThreshold, 0f, this.activeThreshold);
    }

    boolean update(boolean rawActive) {
        float input = rawActive ? 1f : 0f;
        if (!initialized) {
            score = input;
            initialized = true;
        } else {
            score = alpha * input + (1f - alpha) * score;
        }
        if (!active && score >= activeThreshold) {
            active = true;
        } else if (active && score <= inactiveThreshold) {
            active = false;
        }
        return active;
    }

    void reset() {
        initialized = false;
        active = false;
        score = 0f;
    }

    float getScore() {
        return score;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
