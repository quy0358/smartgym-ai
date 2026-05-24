package ntu.quy65132908.smartgym_ai.ui.nutrition;

public class NutritionFormErrors {
    private final String foodNameError;
    private final String caloriesError;
    private final String macroError;

    public NutritionFormErrors(String foodNameError, String caloriesError, String macroError) {
        this.foodNameError = foodNameError;
        this.caloriesError = caloriesError;
        this.macroError = macroError;
    }

    public String getFoodNameError() { return foodNameError; }
    public String getCaloriesError() { return caloriesError; }
    public String getMacroError() { return macroError; }

    public boolean hasErrors() {
        return foodNameError != null || caloriesError != null || macroError != null;
    }

    public static NutritionFormErrors none() {
        return new NutritionFormErrors(null, null, null);
    }
}
