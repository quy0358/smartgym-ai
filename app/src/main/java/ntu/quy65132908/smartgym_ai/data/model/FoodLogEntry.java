package ntu.quy65132908.smartgym_ai.data.model;

public class FoodLogEntry {
    private String id;
    private String name;
    private String mealType;
    private int calories;
    private int proteinGrams;
    private int carbsGrams;
    private int fatGrams;
    private long eatenAt;

    public FoodLogEntry() {}

    public FoodLogEntry(String id,
                        String name,
                        String mealType,
                        int calories,
                        int proteinGrams,
                        int carbsGrams,
                        int fatGrams,
                        long eatenAt) {
        this.id = id;
        this.name = name;
        this.mealType = mealType;
        this.calories = calories;
        this.proteinGrams = proteinGrams;
        this.carbsGrams = carbsGrams;
        this.fatGrams = fatGrams;
        this.eatenAt = eatenAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getMealType() { return mealType; }
    public int getCalories() { return calories; }
    public int getProteinGrams() { return proteinGrams; }
    public int getCarbsGrams() { return carbsGrams; }
    public int getFatGrams() { return fatGrams; }
    public long getEatenAt() { return eatenAt; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public void setCalories(int calories) { this.calories = calories; }
    public void setProteinGrams(int proteinGrams) { this.proteinGrams = proteinGrams; }
    public void setCarbsGrams(int carbsGrams) { this.carbsGrams = carbsGrams; }
    public void setFatGrams(int fatGrams) { this.fatGrams = fatGrams; }
    public void setEatenAt(long eatenAt) { this.eatenAt = eatenAt; }
}
