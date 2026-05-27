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
        assertEquals(R.drawable.img_exercise_push_up, UiImageResolver.exerciseImageFor("push_up", "Chong day", "Nguc"));
        assertEquals(R.drawable.img_exercise_squat, UiImageResolver.exerciseImageFor("squat", "Squat", "Chan"));
        assertEquals(R.drawable.img_exercise_plank, UiImageResolver.exerciseImageFor("plank", "Plank", "Core"));
        assertEquals(R.drawable.img_exercise_crunch, UiImageResolver.exerciseImageFor("", "Gap bung", "Core"));
        assertNotEquals(
                UiImageResolver.exerciseImageFor("plank", "Plank", "Core"),
                UiImageResolver.exerciseImageFor("", "Gap bung", "Core"));
    }

    @Test
    public void mealImageFor_mapsMealTypesToFoodArtwork() {
        assertEquals(R.drawable.img_meal_breakfast, UiImageResolver.mealImageFor("Sang", "Yogurt"));
        assertEquals(R.drawable.img_meal_lunch, UiImageResolver.mealImageFor("Trua", "Com ga"));
        assertEquals(R.drawable.img_meal_dinner, UiImageResolver.mealImageFor("Toi", "Salad"));
    }

    @Test
    public void mealIconFor_usesCategoryIconsInsteadOfRepeatedMealPhotos() {
        assertEquals(R.drawable.ic_nutrition_protein, UiImageResolver.mealIconFor("protein", "Sang"));
        assertEquals(R.drawable.ic_nutrition_carb, UiImageResolver.mealIconFor("carb", "Trua"));
        assertEquals(R.drawable.ic_nutrition_veg, UiImageResolver.mealIconFor("veg", "Toi"));
        assertEquals(R.drawable.ic_nutrition_snack, UiImageResolver.mealIconFor("snack", "Phu"));

        assertNotEquals(R.drawable.img_meal_breakfast, UiImageResolver.mealIconFor("protein", "Sang"));
        assertNotEquals(R.drawable.img_meal_lunch, UiImageResolver.mealIconFor("carb", "Trua"));
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
        Workout chest = workout("Thu 2 - Tap nang co nguc", Workout.DAY_TYPE_TRAINING);
        Workout back = workout("Thu 3 - Tap nhe co lung", Workout.DAY_TYPE_TRAINING);
        Workout legs = workout("Thu 5 - Tap nang co chan", Workout.DAY_TYPE_TRAINING);
        Workout arms = workout("Thu 6 - Tap nhe co tay", Workout.DAY_TYPE_TRAINING);
        Workout core = workout("Chu nhat - Tap nang co bung", Workout.DAY_TYPE_TRAINING);
        Workout recovery = workout("Thu 4 - Rest day/ Recover", Workout.DAY_TYPE_RECOVERY);
        Workout custom = workout("Bai tap tuy chinh", Workout.DAY_TYPE_TRAINING);

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
        Challenge move7 = new Challenge("move_7", "7 ngay van dong", "", 7, 20);
        Challenge strength14 = new Challenge("strength_14", "14 ngay khoe hon", "", 14, 25);
        Challenge habit30 = new Challenge("habit_30", "30 ngay ben bi", "", 30, 20);

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
