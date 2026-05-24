package ntu.quy65132908.smartgym_ai.data.model;

public class NutritionGoal {
    private String id;
    private int calories;
    private int proteinGrams;
    private int carbsGrams;
    private int fatGrams;
    private int waterMl;
    private String goalType;

    public NutritionGoal() {}

    public NutritionGoal(int calories, int proteinGrams, int carbsGrams, int fatGrams, int waterMl) {
        this.calories = calories;
        this.proteinGrams = proteinGrams;
        this.carbsGrams = carbsGrams;
        this.fatGrams = fatGrams;
        this.waterMl = waterMl;
    }

    public String getId() { return id; }
    public int getCalories() { return calories; }
    public int getProteinGrams() { return proteinGrams; }
    public int getCarbsGrams() { return carbsGrams; }
    public int getFatGrams() { return fatGrams; }
    public int getWaterMl() { return waterMl; }
    public String getGoalType() { return goalType; }

    public void setId(String id) { this.id = id; }
    public void setCalories(int calories) { this.calories = calories; }
    public void setProteinGrams(int proteinGrams) { this.proteinGrams = proteinGrams; }
    public void setCarbsGrams(int carbsGrams) { this.carbsGrams = carbsGrams; }
    public void setFatGrams(int fatGrams) { this.fatGrams = fatGrams; }
    public void setWaterMl(int waterMl) { this.waterMl = waterMl; }
    public void setGoalType(String goalType) { this.goalType = goalType; }
}
