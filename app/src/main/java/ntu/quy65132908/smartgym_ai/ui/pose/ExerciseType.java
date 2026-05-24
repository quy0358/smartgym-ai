package ntu.quy65132908.smartgym_ai.ui.pose;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public enum ExerciseType {
    PUSH_UP("push_up", "Chống đẩy"),
    SQUAT("squat", "Squat"),
    PLANK("plank", "Plank");

    private final String key;
    private final String displayName;

    ExerciseType(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean usesDurationMetric() {
        return this == PLANK;
    }

    @Nullable
    public static ExerciseType matchKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT)
                .replace("-", "_")
                .replace(" ", "_");
        for (ExerciseType type : values()) {
            if (type.key.equals(normalized) || type.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return type;
            }
        }
        return null;
    }

    @NonNull
    public static ExerciseType fromKey(String key) {
        ExerciseType matched = matchKey(key);
        return matched != null ? matched : PUSH_UP;
    }
}
