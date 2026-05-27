package ntu.quy65132908.smartgym_ai.data.model;

public class FoodNutritionEstimate {
    public static final String CATEGORY_PROTEIN = "protein";
    public static final String CATEGORY_CARB = "carb";
    public static final String CATEGORY_VEG = "veg";
    public static final String CATEGORY_SNACK = "snack";
    public static final String CATEGORY_MIXED = "mixed";

    private String name;
    private String servingText;
    private String mealType;
    private String category;
    private int calories;
    private int proteinGrams;
    private int carbsGrams;
    private int fatGrams;
    private float confidence;
    private String notes;

    public FoodNutritionEstimate() {}

    public FoodNutritionEstimate(String name,
                                 String servingText,
                                 String mealType,
                                 String category,
                                 int calories,
                                 int proteinGrams,
                                 int carbsGrams,
                                 int fatGrams,
                                 float confidence,
                                 String notes) {
        this.name = name;
        this.servingText = servingText;
        this.mealType = mealType;
        this.category = category;
        this.calories = calories;
        this.proteinGrams = proteinGrams;
        this.carbsGrams = carbsGrams;
        this.fatGrams = fatGrams;
        this.confidence = confidence;
        this.notes = notes;
    }

    public String getName() { return name; }
    public String getServingText() { return servingText; }
    public String getMealType() { return mealType; }
    public String getCategory() { return category; }
    public int getCalories() { return calories; }
    public int getProteinGrams() { return proteinGrams; }
    public int getCarbsGrams() { return carbsGrams; }
    public int getFatGrams() { return fatGrams; }
    public float getConfidence() { return confidence; }
    public String getNotes() { return notes; }

    public void setName(String name) { this.name = name; }
    public void setServingText(String servingText) { this.servingText = servingText; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public void setCategory(String category) { this.category = category; }
    public void setCalories(int calories) { this.calories = calories; }
    public void setProteinGrams(int proteinGrams) { this.proteinGrams = proteinGrams; }
    public void setCarbsGrams(int carbsGrams) { this.carbsGrams = carbsGrams; }
    public void setFatGrams(int fatGrams) { this.fatGrams = fatGrams; }
    public void setConfidence(float confidence) { this.confidence = confidence; }
    public void setNotes(String notes) { this.notes = notes; }
}
