package ntu.quy65132908.smartgym_ai.ui.media;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import java.text.Normalizer;
import java.util.Locale;

import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Challenge;
import ntu.quy65132908.smartgym_ai.data.model.Workout;

public final class UiImageResolver {
    public enum Section {
        DASHBOARD,
        WORKOUT,
        NUTRITION,
        PROGRESS,
        COMMUNITY,
        WELLNESS,
        PROFILE,
        ONBOARDING
    }

    private UiImageResolver() {}

    @DrawableRes
    public static int heroImageFor(Section section) {
        if (section == null) {
            return R.drawable.img_hero_workout;
        }
        switch (section) {
            case NUTRITION:
                return R.drawable.img_hero_nutrition;
            case PROGRESS:
                return R.drawable.img_hero_progress;
            case COMMUNITY:
                return R.drawable.img_hero_community;
            case WELLNESS:
                return R.drawable.img_hero_wellness;
            case PROFILE:
                return R.drawable.img_hero_profile;
            case ONBOARDING:
                return R.drawable.img_hero_onboarding;
            case DASHBOARD:
            case WORKOUT:
            default:
                return R.drawable.img_hero_workout;
        }
    }

    @DrawableRes
    public static int workoutImageFor(@Nullable Workout workout) {
        if (workout != null) {
            if (workout.isRecoveryDay()) {
                return R.drawable.img_recovery;
            }
            if (workout.isRestDay()) {
                return R.drawable.img_hero_wellness;
            }
            String key = normalize(workout.getTitle() + " " + workout.getSubtitle() + " " + workout.getIntensity());
            if (key.contains("tuy chinh") || key.contains("custom")) {
                return R.drawable.img_workout_plan;
            }
            if (key.contains("nguc") || key.contains("chest") || key.contains("push")) {
                return R.drawable.img_muscle_chest;
            }
            if (key.contains("lung") || key.contains("back") || key.contains("row") || key.contains("pull")) {
                return R.drawable.img_muscle_back;
            }
            if (key.contains("chan") || key.contains("leg") || key.contains("squat") || key.contains("lunge")) {
                return R.drawable.img_muscle_upper_legs;
            }
            if (key.contains("tay") || key.contains("arm") || key.contains("bicep") || key.contains("tricep")) {
                return R.drawable.img_muscle_upper_arms;
            }
            if (key.contains("bung") || key.contains("core") || key.contains("abs") || key.contains("plank")) {
                return R.drawable.img_muscle_waist;
            }
            if (key.contains("cardio") || key.contains("run") || key.contains("hiit")) {
                return R.drawable.img_exercise_cardio;
            }
        }
        return R.drawable.img_workout_program;
    }

    @DrawableRes
    public static int challengeImageFor(@Nullable Challenge challenge) {
        String key = "";
        if (challenge != null) {
            key = normalize(challenge.getId() + " " + challenge.getTitle() + " " + challenge.getDescription());
        }
        if (key.contains("move_7") || key.contains("van dong") || key.contains("move")) {
            return R.drawable.img_exercise_cardio;
        }
        if (key.contains("strength_14") || key.contains("suc manh") || key.contains("khoe") || key.contains("strength")) {
            return R.drawable.img_hero_workout;
        }
        if (key.contains("habit_30") || key.contains("ben bi") || key.contains("habit")) {
            return R.drawable.img_hero_wellness;
        }
        return R.drawable.img_recovery;
    }

    @DrawableRes
    public static int exerciseImageFor(@Nullable String id, @Nullable String name, @Nullable String primaryMuscle) {
        String key = normalize(id + " " + name + " " + primaryMuscle);
        if (key.contains("push_up") || key.contains("pushup") || key.contains("chong day") || key.contains("nguc")) {
            return R.drawable.img_exercise_push_up;
        }
        if (key.contains("lung") || key.contains("back") || key.contains("row") || key.contains("pull")) {
            return R.drawable.img_muscle_back;
        }
        if (key.contains("deadlift") || key.contains("hinge")) {
            return R.drawable.img_exercise_deadlift;
        }
        if (key.contains("cardio") || key.contains("run") || key.contains("burpee")) {
            return R.drawable.img_exercise_cardio;
        }
        if (key.contains("squat") || key.contains("lunge") || key.contains("chan") || key.contains("upper legs")) {
            return R.drawable.img_muscle_upper_legs;
        }
        if (key.contains("plank") || key.contains("waist") || key.contains("core") || key.contains("bung")) {
            return R.drawable.img_muscle_waist;
        }
        if (key.contains("vai") || key.contains("shoulder") || key.contains("overhead")) {
            return R.drawable.img_muscle_shoulders;
        }
        if (key.contains("arm") || key.contains("tay") || key.contains("bicep") || key.contains("tricep")) {
            return R.drawable.img_muscle_upper_arms;
        }
        if (key.contains("calf") || key.contains("lower legs")) {
            return R.drawable.img_muscle_lower_legs;
        }
        return R.drawable.img_muscle_chest;
    }

    @DrawableRes
    public static int mealImageFor(@Nullable String mealType, @Nullable String mealName) {
        String key = normalize(mealType + " " + mealName);
        if (key.contains("sang") || key.contains("breakfast")) {
            return R.drawable.img_meal_breakfast;
        }
        if (key.contains("trua") || key.contains("lunch")) {
            return R.drawable.img_meal_lunch;
        }
        if (key.contains("toi") || key.contains("dinner")) {
            return R.drawable.img_meal_dinner;
        }
        return R.drawable.img_meal_default;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return decomposed
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
