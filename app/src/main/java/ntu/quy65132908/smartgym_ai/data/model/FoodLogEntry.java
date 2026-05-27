package ntu.quy65132908.smartgym_ai.data.model;

public class FoodLogEntry {
    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_AI = "AI";
    public static final String SOURCE_PLAN = "PLAN";

    private String id;
    private String name;
    private String mealType;
    private String servingText;
    private String category;
    private String source;
    private String planImportKey;
    private Float aiConfidence;
    private String notes;
    private int calories;
    private int proteinGrams;
    private int carbsGrams;
    private int fatGrams;
    private long eatenAt;
    private long updatedAt;

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
    public String getServingText() { return servingText; }
    public String getCategory() { return category; }
    public String getSource() { return source; }
    public String getPlanImportKey() { return planImportKey; }
    public Float getAiConfidence() { return aiConfidence; }
    public String getNotes() { return notes; }
    public int getCalories() { return calories; }
    public int getProteinGrams() { return proteinGrams; }
    public int getCarbsGrams() { return carbsGrams; }
    public int getFatGrams() { return fatGrams; }
    public long getEatenAt() { return eatenAt; }
    public long getUpdatedAt() { return updatedAt; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public void setServingText(String servingText) { this.servingText = servingText; }
    public void setCategory(String category) { this.category = category; }
    public void setSource(String source) { this.source = source; }
    public void setPlanImportKey(String planImportKey) { this.planImportKey = planImportKey; }
    public void setAiConfidence(Float aiConfidence) { this.aiConfidence = aiConfidence; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setCalories(int calories) { this.calories = calories; }
    public void setProteinGrams(int proteinGrams) { this.proteinGrams = proteinGrams; }
    public void setCarbsGrams(int carbsGrams) { this.carbsGrams = carbsGrams; }
    public void setFatGrams(int fatGrams) { this.fatGrams = fatGrams; }
    public void setEatenAt(long eatenAt) { this.eatenAt = eatenAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
