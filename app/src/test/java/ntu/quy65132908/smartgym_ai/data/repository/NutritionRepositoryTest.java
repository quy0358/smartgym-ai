package ntu.quy65132908.smartgym_ai.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

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
}
