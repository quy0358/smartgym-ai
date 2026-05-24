package ntu.quy65132908.smartgym_ai.ui.pose;

public class PoseRepCounter {
    private enum Phase {
        TOP,
        DOWN
    }

    private final int stableFrameThreshold;
    private Phase phase = Phase.TOP;
    private int stableDownFrames;
    private int stableTopFrames;
    private int reps;

    public PoseRepCounter(int stableFrameThreshold) {
        this.stableFrameThreshold = Math.max(1, stableFrameThreshold);
    }

    public int update(boolean downPosition) {
        if (downPosition) {
            stableDownFrames++;
            stableTopFrames = 0;
            if (stableDownFrames >= stableFrameThreshold) {
                phase = Phase.DOWN;
            }
            return reps;
        }

        stableTopFrames++;
        stableDownFrames = 0;
        if (phase == Phase.DOWN && stableTopFrames >= stableFrameThreshold) {
            reps++;
            phase = Phase.TOP;
        }
        return reps;
    }

    public int getReps() {
        return reps;
    }

    public void reset() {
        phase = Phase.TOP;
        stableDownFrames = 0;
        stableTopFrames = 0;
        reps = 0;
    }
}
