package ntu.quy65132908.smartgym_ai.data.model;

public class NutritionSummary {
    private final NutritionGoal goal;
    private final int caloriesConsumed;
    private final int proteinConsumed;
    private final int carbsConsumed;
    private final int fatConsumed;

    public NutritionSummary(NutritionGoal goal, int caloriesConsumed, int proteinConsumed, int carbsConsumed, int fatConsumed) {
        this.goal = goal;
        this.caloriesConsumed = caloriesConsumed;
        this.proteinConsumed = proteinConsumed;
        this.carbsConsumed = carbsConsumed;
        this.fatConsumed = fatConsumed;
    }

    public NutritionGoal getGoal() { return goal; }
    public int getCaloriesConsumed() { return caloriesConsumed; }
    public int getProteinConsumed() { return proteinConsumed; }
    public int getCarbsConsumed() { return carbsConsumed; }
    public int getFatConsumed() { return fatConsumed; }
    public int getCaloriesRemaining() { return Math.max(0, goal.getCalories() - caloriesConsumed); }

    public int getCaloriesPercent() {
        if (goal == null || goal.getCalories() <= 0) {
            return 0;
        }
        return Math.min(100, Math.round((caloriesConsumed * 100f) / goal.getCalories()));
    }
}
