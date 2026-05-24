package ntu.quy65132908.smartgym_ai.ui.nutrition;

import java.util.ArrayList;
import java.util.List;

import ntu.quy65132908.smartgym_ai.data.model.FoodLogEntry;
import ntu.quy65132908.smartgym_ai.data.model.MealPlan;
import ntu.quy65132908.smartgym_ai.data.model.NutritionSummary;

public class NutritionUiState {
    private final NutritionSummary summary;
    private final List<FoodLogEntry> foodLogs;
    private final MealPlan mealPlan;
    private final String mealPlanPreview;
    private final boolean loading;
    private final boolean savingFood;
    private final boolean loggedOut;
    private final boolean foodLogsLoading;
    private final String emptyFoodLogsText;

    public NutritionUiState(NutritionSummary summary,
                            List<FoodLogEntry> foodLogs,
                            MealPlan mealPlan,
                            String mealPlanPreview,
                            boolean loading,
                            boolean savingFood,
                            boolean loggedOut,
                            boolean foodLogsLoading,
                            String emptyFoodLogsText) {
        this.summary = summary;
        this.foodLogs = foodLogs != null ? new ArrayList<>(foodLogs) : new ArrayList<>();
        this.mealPlan = mealPlan;
        this.mealPlanPreview = mealPlanPreview;
        this.loading = loading;
        this.savingFood = savingFood;
        this.loggedOut = loggedOut;
        this.foodLogsLoading = foodLogsLoading;
        this.emptyFoodLogsText = emptyFoodLogsText;
    }

    public NutritionSummary getSummary() { return summary; }
    public List<FoodLogEntry> getFoodLogs() { return new ArrayList<>(foodLogs); }
    public MealPlan getMealPlan() { return mealPlan; }
    public String getMealPlanPreview() { return mealPlanPreview; }
    public boolean isLoading() { return loading; }
    public boolean isSavingFood() { return savingFood; }
    public boolean isLoggedOut() { return loggedOut; }
    public boolean isFoodLogsLoading() { return foodLogsLoading; }
    public String getEmptyFoodLogsText() { return emptyFoodLogsText; }
}
