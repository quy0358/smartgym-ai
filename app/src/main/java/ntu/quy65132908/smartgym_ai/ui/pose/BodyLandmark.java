package ntu.quy65132908.smartgym_ai.ui.pose;

import androidx.annotation.NonNull;

public enum BodyLandmark {
    LEFT_SHOULDER,
    RIGHT_SHOULDER,
    LEFT_ELBOW,
    RIGHT_ELBOW,
    LEFT_WRIST,
    RIGHT_WRIST,
    LEFT_HIP,
    RIGHT_HIP,
    LEFT_KNEE,
    RIGHT_KNEE,
    LEFT_ANKLE,
    RIGHT_ANKLE;

    @NonNull
    public BodyLandmark opposite() {
        switch (this) {
            case LEFT_SHOULDER: return RIGHT_SHOULDER;
            case RIGHT_SHOULDER: return LEFT_SHOULDER;
            case LEFT_ELBOW: return RIGHT_ELBOW;
            case RIGHT_ELBOW: return LEFT_ELBOW;
            case LEFT_WRIST: return RIGHT_WRIST;
            case RIGHT_WRIST: return LEFT_WRIST;
            case LEFT_HIP: return RIGHT_HIP;
            case RIGHT_HIP: return LEFT_HIP;
            case LEFT_KNEE: return RIGHT_KNEE;
            case RIGHT_KNEE: return LEFT_KNEE;
            case LEFT_ANKLE: return RIGHT_ANKLE;
            case RIGHT_ANKLE: return LEFT_ANKLE;
            default: return this;
        }
    }
}
