package ntu.quy65132908.smartgym_ai.ui.pose;

import androidx.annotation.Nullable;

import java.text.Normalizer;
import java.util.Locale;

import ntu.quy65132908.smartgym_ai.data.model.Exercise;

public final class PoseExerciseResolver {
    private PoseExerciseResolver() {
    }

    @Nullable
    public static ExerciseType resolve(@Nullable Exercise exercise) {
        if (exercise == null) {
            return null;
        }
        return resolve(exercise.getPoseTypeKey(), exercise.getName());
    }

    @Nullable
    public static ExerciseType resolve(@Nullable String poseTypeKey, @Nullable String exerciseName) {
        ExerciseType byKey = ExerciseType.matchKey(poseTypeKey);
        if (byKey != null) {
            return byKey;
        }

        String normalized = normalize(exerciseName);
        if (normalized.isEmpty()) {
            return null;
        }
        if (containsAny(normalized, "push up", "pushup", "chong day", "hit dat")) {
            return ExerciseType.PUSH_UP;
        }
        if (containsAny(normalized, "squat", "ngoi xom", "ganh", "ha mong")) {
            return ExerciseType.SQUAT;
        }
        if (normalized.contains("plank")) {
            return ExerciseType.PLANK;
        }
        return null;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    static String normalize(@Nullable String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("\\p{M}", "");
        return decomposed.toLowerCase(Locale.ROOT)
                .replace("-", " ")
                .replace("_", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
