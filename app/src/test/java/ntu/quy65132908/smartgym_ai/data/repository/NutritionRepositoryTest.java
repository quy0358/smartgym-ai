package ntu.quy65132908.smartgym_ai.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ntu.quy65132908.smartgym_ai.data.model.FoodLogEntry;
import ntu.quy65132908.smartgym_ai.data.model.NutritionGoal;
import ntu.quy65132908.smartgym_ai.data.model.NutritionSummary;

public class NutritionRepositoryTest {

    @Test
    public void calculateTodaySummary_sumsLoggedFoodAndRemainingTargets() {
        NutritionGoal goal = new NutritionGoal(2200, 140, 260, 70, 2500);
        FoodLogEntry breakfast = new FoodLogEntry("f1", "Yến mạch", "Sáng", 420, 22, 58, 10, 1716400000000L);
        FoodLogEntry lunch = new FoodLogEntry("f2", "Ức gà cơm gạo lứt", "Trưa", 610, 48, 72, 14, 1716420000000L);

        NutritionSummary summary = NutritionRepository.calculateTodaySummary(
                goal,
                Arrays.asList(breakfast, lunch)
        );

        assertEquals(1030, summary.getCaloriesConsumed());
        assertEquals(1170, summary.getCaloriesRemaining());
        assertEquals(70, summary.getProteinConsumed());
        assertEquals(130, summary.getCarbsConsumed());
        assertEquals(24, summary.getFatConsumed());
        assertEquals(47, summary.getCaloriesPercent());
    }

    @Test
    public void defaultGoalForUserWeight_usesSafeVietnameseFitnessTargets() {
        NutritionGoal goal = NutritionRepository.defaultGoalForWeight(70f, "giảm mỡ");

        assertTrue(goal.getCalories() >= 1600);
        assertEquals(126, goal.getProteinGrams());
        assertEquals("giảm mỡ", goal.getGoalType());
        assertEquals(2500, goal.getWaterMl());
    }

    @Test
    public void calculateTodaySummary_emptyLogsUsesGoalAndZeroProgress() {
        NutritionGoal goal = new NutritionGoal(2100, 126, 240, 70, 2500);

        NutritionSummary summary = NutritionRepository.calculateTodaySummary(goal, Collections.emptyList());

        assertEquals(0, summary.getCaloriesConsumed());
        assertEquals(2100, summary.getCaloriesRemaining());
        assertEquals(0, summary.getProteinConsumed());
        assertEquals(0, summary.getCarbsConsumed());
        assertEquals(0, summary.getFatConsumed());
        assertEquals(0, summary.getCaloriesPercent());
    }

    @Test
    public void calculateTodaySummary_clampsNegativeEntriesAndCapsPercent() {
        NutritionGoal goal = new NutritionGoal(1000, 100, 120, 45, 2500);
        FoodLogEntry negative = new FoodLogEntry("bad", "Bad", "Sáng", -100, -5, -7, -1, 1L);
        FoodLogEntry high = new FoodLogEntry("high", "High", "Trưa", 1500, 40, 70, 20, 2L);

        NutritionSummary summary = NutritionRepository.calculateTodaySummary(goal, Arrays.asList(negative, high));

        assertEquals(1500, summary.getCaloriesConsumed());
        assertEquals(0, summary.getCaloriesRemaining());
        assertEquals(40, summary.getProteinConsumed());
        assertEquals(70, summary.getCarbsConsumed());
        assertEquals(20, summary.getFatConsumed());
        assertEquals(100, summary.getCaloriesPercent());
        assertEquals(500, summary.getCaloriesOverTarget());
        assertEquals(40, summary.getProteinPercent());
        assertEquals(58, summary.getCarbsPercent());
        assertEquals(44, summary.getFatPercent());
    }

    @Test
    public void defaultGoalForUserWeight_adjustsCaloriesForGainMaintainAndLoss() {
        NutritionGoal loss = NutritionRepository.defaultGoalForWeight(70f, "giảm mỡ");
        NutritionGoal maintain = NutritionRepository.defaultGoalForWeight(70f, "duy trì");
        NutritionGoal gain = NutritionRepository.defaultGoalForWeight(70f, "tăng cơ");

        assertTrue(loss.getCalories() < maintain.getCalories());
        assertTrue(gain.getCalories() > maintain.getCalories());
        assertEquals("duy trì", maintain.getGoalType());
        assertEquals("tăng cơ", gain.getGoalType());
    }

    @Test
    public void sanitizeFoodLogForSave_trimsDefaultsAndClampsUnsafeValues() {
        FoodLogEntry entry = new FoodLogEntry(
                null,
                "  Bữa sáng nhiều năng lượng  ",
                "  ",
                -10,
                1500,
                30,
                -2,
                0L
        );

        NutritionRepository.sanitizeFoodLogForSave(entry);

        assertEquals("Bữa sáng nhiều năng lượng", entry.getName());
        assertEquals("Bữa ăn", entry.getMealType());
        assertEquals(0, entry.getCalories());
        assertEquals(1000, entry.getProteinGrams());
        assertEquals(30, entry.getCarbsGrams());
        assertEquals(0, entry.getFatGrams());
        assertTrue(entry.getEatenAt() > 0L);
    }

    @Test
    public void sanitizeFoodLogForSave_preservesAiMetadataAndUsesSharedLimits() {
        FoodLogEntry entry = new FoodLogEntry(
                null,
                "  Chicken rice  ",
                " Lunch ",
                9000,
                1500,
                1600,
                1700,
                1L
        );
        entry.setServingText("  1 bowl  ");
        entry.setCategory("protein");
        entry.setSource(FoodLogEntry.SOURCE_AI);
        entry.setPlanImportKey("  2026-05-26|PLAN|Lunch|Chicken rice  ");
        entry.setAiConfidence(1.5f);
        entry.setNotes("  Review before saving  ");

        NutritionRepository.sanitizeFoodLogForSave(entry);

        assertEquals("Chicken rice", entry.getName());
        assertEquals("Lunch", entry.getMealType());
        assertEquals("1 bowl", entry.getServingText());
        assertEquals("protein", entry.getCategory());
        assertEquals(FoodLogEntry.SOURCE_AI, entry.getSource());
        assertEquals("2026-05-26|PLAN|Lunch|Chicken rice", entry.getPlanImportKey());
        assertEquals(1f, entry.getAiConfidence(), 0.001f);
        assertEquals("Review before saving", entry.getNotes());
        assertEquals(5000, entry.getCalories());
        assertEquals(1000, entry.getProteinGrams());
        assertEquals(1000, entry.getCarbsGrams());
        assertEquals(1000, entry.getFatGrams());
        assertTrue(entry.getUpdatedAt() > 0L);
    }

    @Test
    public void sanitizeFoodLogForSave_blankNameThrowsHelpfulError() {
        try {
            NutritionRepository.sanitizeFoodLogForSave(
                    new FoodLogEntry(null, " ", "Sáng", 100, 10, 20, 3, 1L));
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("food name"));
            return;
        }

        throw new AssertionError("Expected validation error");
    }

    @Test
    public void sanitizeMealPlanForSave_requiresDaysAndDefaultsTitle() {
        ntu.quy65132908.smartgym_ai.data.model.MealPlan plan =
                new ntu.quy65132908.smartgym_ai.data.model.MealPlan();
        plan.setTitle(" ");
        ntu.quy65132908.smartgym_ai.data.model.MealPlanDay day =
                new ntu.quy65132908.smartgym_ai.data.model.MealPlanDay();
        plan.setDays(Collections.singletonList(day));

        NutritionRepository.sanitizeMealPlanForSave(plan);

        assertEquals("Kế hoạch ăn", plan.getTitle());
        assertTrue(plan.getCreatedAt() > 0L);
    }

    @Test
    public void saveFoodLogs_rejectsOversizedBatchBeforeFirestoreWrite() {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        NutritionRepository repository = new NutritionRepository(firestore);
        List<FoodLogEntry> entries = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            entries.add(new FoodLogEntry(null, "Food " + i, "Lunch", 100, 10, 10, 3, 1L));
        }
        RecordingFoodLogsSaveCallback callback = new RecordingFoodLogsSaveCallback();

        repository.saveFoodLogs("uid-1", "2026-05-27", entries, callback);

        assertTrue(callback.error instanceof IllegalArgumentException);
        assertTrue(callback.error.getMessage().contains("too many"));
        verify(firestore, never()).collection(any());
    }

    @Test
    public void saveFoodLogs_rejectsMissingInputBeforeFirestoreWrite() {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        NutritionRepository repository = new NutritionRepository(firestore);
        RecordingFoodLogsSaveCallback callback = new RecordingFoodLogsSaveCallback();

        repository.saveFoodLogs(" ", "2026-05-27", Collections.singletonList(
                new FoodLogEntry(null, "Rice", "Lunch", 100, 10, 10, 3, 1L)
        ), callback);

        assertTrue(callback.error instanceof IllegalArgumentException);
        verify(firestore, never()).collection(any());

        callback = new RecordingFoodLogsSaveCallback();
        repository.saveFoodLogs("uid-1", "2026-05-27", Collections.emptyList(), callback);

        assertTrue(callback.error instanceof IllegalArgumentException);
        verify(firestore, never()).collection(any());
    }

    private static final class RecordingFoodLogsSaveCallback implements NutritionRepository.FoodLogsSaveCallback {
        Exception error;

        @Override
        public void onSuccess(List<FoodLogEntry> savedEntries) {
        }

        @Override
        public void onError(Exception e) {
            error = e;
        }
    }
}
