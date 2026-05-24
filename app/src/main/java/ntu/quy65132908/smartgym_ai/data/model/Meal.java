package ntu.quy65132908.smartgym_ai.data.model;

public class Meal {
    private String id;
    private String mealType;
    private String name;
    private int calories;
    private int proteinGrams;
    private int carbsGrams;
    private int fatGrams;
    private String notes;

    public Meal() {}

    public String getId() { return id; }
    public String getMealType() { return mealType; }
    public String getName() { return name; }
    public int getCalories() { return calories; }
    public int getProteinGrams() { return proteinGrams; }
    public int getCarbsGrams() { return carbsGrams; }
    public int getFatGrams() { return fatGrams; }
    public String getNotes() { return notes; }

    public void setId(String id) { this.id = id; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public void setName(String name) { this.name = name; }
    public void setCalories(int calories) { this.calories = calories; }
    public void setProteinGrams(int proteinGrams) { this.proteinGrams = proteinGrams; }
    public void setCarbsGrams(int carbsGrams) { this.carbsGrams = carbsGrams; }
    public void setFatGrams(int fatGrams) { this.fatGrams = fatGrams; }
    public void setNotes(String notes) { this.notes = notes; }
}
