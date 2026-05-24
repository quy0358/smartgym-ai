package ntu.quy65132908.smartgym_ai.ui.pose;

public class PoseFeedback {
    private final String message;
    private final int reps;
    private final int holdSeconds;
    private final int qualityPercent;
    private final boolean personDetected;

    public PoseFeedback(String message, int reps, int qualityPercent, boolean personDetected) {
        this(message, reps, qualityPercent, personDetected, 0);
    }

    public PoseFeedback(String message, int reps, int qualityPercent, boolean personDetected, int holdSeconds) {
        this.message = message;
        this.reps = Math.max(0, reps);
        this.holdSeconds = Math.max(0, holdSeconds);
        this.qualityPercent = clamp(qualityPercent);
        this.personDetected = personDetected;
    }

    public String getMessage() {
        return message;
    }

    public int getReps() {
        return reps;
    }

    public int getHoldSeconds() {
        return holdSeconds;
    }

    public int getQualityPercent() {
        return qualityPercent;
    }

    public boolean isPersonDetected() {
        return personDetected;
    }

    private int clamp(int value) {
        if (value < 0) return 0;
        if (value > 100) return 100;
        return value;
    }
}
