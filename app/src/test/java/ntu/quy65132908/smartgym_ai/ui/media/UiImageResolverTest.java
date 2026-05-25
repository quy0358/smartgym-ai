package ntu.quy65132908.smartgym_ai.ui.media;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Challenge;
import ntu.quy65132908.smartgym_ai.data.model.Workout;

public class UiImageResolverTest {

    @Test
    public void exerciseImageFor_returnsSpecificWorkoutArtwork() {
        assertEquals(R.drawable.img_exercise_push_up, UiImageResolver.exerciseImageFor("push_up", "Chống đẩy", "Ngực"));
        assertEquals(R.drawable.img_muscle_upper_legs, UiImageResolver.exerciseImageFor("squat", "Squat", "Chân"));
        assertEquals(R.drawable.img_muscle_waist, UiImageResolver.exerciseImageFor("plank", "Plank", "Core"));
    }

    @Test
    public void mealImageFor_mapsMealTypesToFoodArtwork() {
        assertEquals(R.drawable.img_meal_breakfast, UiImageResolver.mealImageFor("Sáng", "Yogurt"));
        assertEquals(R.drawable.img_meal_lunch, UiImageResolver.mealImageFor("Trưa", "Cơm gà"));
        assertEquals(R.drawable.img_meal_dinner, UiImageResolver.mealImageFor("Tối", "Salad"));
    }

    @Test
    public void heroImages_areDistinctAcrossMainSections() {
        assertEquals(R.drawable.img_hero_workout, UiImageResolver.heroImageFor(UiImageResolver.Section.WORKOUT));
        assertEquals(R.drawable.img_hero_nutrition, UiImageResolver.heroImageFor(UiImageResolver.Section.NUTRITION));
        assertNotEquals(
                UiImageResolver.heroImageFor(UiImageResolver.Section.WORKOUT),
                UiImageResolver.heroImageFor(UiImageResolver.Section.NUTRITION));
    }

    @Test
    public void workoutImageFor_usesDistinctArtworkForWeeklyPlanCategories() {
        Workout chest = workout("Thứ 2 - Tập nặng cơ ngực", Workout.DAY_TYPE_TRAINING);
        Workout back = workout("Thứ 3 - Tập nhẹ cơ lưng", Workout.DAY_TYPE_TRAINING);
        Workout legs = workout("Thứ 5 - Tập nặng cơ chân", Workout.DAY_TYPE_TRAINING);
        Workout arms = workout("Thứ 6 - Tập nhẹ cơ tay", Workout.DAY_TYPE_TRAINING);
        Workout core = workout("Chủ nhật - Tập nặng cơ bụng", Workout.DAY_TYPE_TRAINING);
        Workout recovery = workout("Thứ 4 - Rest day/ Recover", Workout.DAY_TYPE_RECOVERY);
        Workout custom = workout("Bài tập tùy chỉnh", Workout.DAY_TYPE_TRAINING);

        assertEquals(R.drawable.img_muscle_chest, UiImageResolver.workoutImageFor(chest));
        assertEquals(R.drawable.img_muscle_back, UiImageResolver.workoutImageFor(back));
        assertEquals(R.drawable.img_muscle_upper_legs, UiImageResolver.workoutImageFor(legs));
        assertEquals(R.drawable.img_muscle_upper_arms, UiImageResolver.workoutImageFor(arms));
        assertEquals(R.drawable.img_muscle_waist, UiImageResolver.workoutImageFor(core));
        assertEquals(R.drawable.img_recovery, UiImageResolver.workoutImageFor(recovery));
        assertEquals(R.drawable.img_workout_plan, UiImageResolver.workoutImageFor(custom));

        assertAllDistinct(
                UiImageResolver.workoutImageFor(chest),
                UiImageResolver.workoutImageFor(back),
                UiImageResolver.workoutImageFor(legs),
                UiImageResolver.workoutImageFor(arms),
                UiImageResolver.workoutImageFor(core));
    }

    @Test
    public void challengeImageFor_usesDistinctArtworkForDefaultChallenges() {
        Challenge move7 = new Challenge("move_7", "7 ngày vận động", "", 7, 20);
        Challenge strength14 = new Challenge("strength_14", "14 ngày khỏe hơn", "", 14, 25);
        Challenge habit30 = new Challenge("habit_30", "30 ngày bền bỉ", "", 30, 20);

        assertEquals(R.drawable.img_exercise_cardio, UiImageResolver.challengeImageFor(move7));
        assertEquals(R.drawable.img_hero_workout, UiImageResolver.challengeImageFor(strength14));
        assertEquals(R.drawable.img_hero_wellness, UiImageResolver.challengeImageFor(habit30));
        assertAllDistinct(
                UiImageResolver.challengeImageFor(move7),
                UiImageResolver.challengeImageFor(strength14),
                UiImageResolver.challengeImageFor(habit30));
    }

    private static Workout workout(String title, String dayType) {
        Workout workout = new Workout();
        workout.setTitle(title);
        workout.setDayType(dayType);
        return workout;
    }

    private static void assertAllDistinct(int... drawables) {
        for (int i = 0; i < drawables.length; i++) {
            for (int j = i + 1; j < drawables.length; j++) {
                assertNotEquals("Expected different drawable resources", drawables[i], drawables[j]);
            }
        }
    }
}
