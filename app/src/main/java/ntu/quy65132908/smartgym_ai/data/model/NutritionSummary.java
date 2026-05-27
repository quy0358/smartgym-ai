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
    public int getCaloriesOverTarget() {
        return goal != null ? Math.max(0, caloriesConsumed - goal.getCalories()) : 0;
    }

    public int getCaloriesPercent() {
        return percent(caloriesConsumed, goal != null ? goal.getCalories() : 0);
    }

    public int getProteinPercent() {
        return percent(proteinConsumed, goal != null ? goal.getProteinGrams() : 0);
    }

    public int getCarbsPercent() {
        return percent(carbsConsumed, goal != null ? goal.getCarbsGrams() : 0);
    }

    public int getFatPercent() {
        return percent(fatConsumed, goal != null ? goal.getFatGrams() : 0);
    }

    private int percent(int consumed, int target) {
        if (target <= 0) {
            return 0;
        }
        return Math.min(100, Math.round((consumed * 100f) / target));
    }
}
