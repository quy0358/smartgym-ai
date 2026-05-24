package ntu.quy65132908.smartgym_ai.data.model;

public class ExerciseCatalogFilters {
    private String primaryMuscle;
    private String equipment;
    private String difficulty;
    private boolean poseSupportedOnly;

    public static ExerciseCatalogFilters all() {
        return new ExerciseCatalogFilters();
    }

    public String getPrimaryMuscle() {
        return primaryMuscle;
    }

    public ExerciseCatalogFilters setPrimaryMuscle(String primaryMuscle) {
        this.primaryMuscle = primaryMuscle;
        return this;
    }

    public String getEquipment() {
        return equipment;
    }

    public ExerciseCatalogFilters setEquipment(String equipment) {
        this.equipment = equipment;
        return this;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public ExerciseCatalogFilters setDifficulty(String difficulty) {
        this.difficulty = difficulty;
        return this;
    }

    public boolean isPoseSupportedOnly() {
        return poseSupportedOnly;
    }

    public ExerciseCatalogFilters setPoseSupportedOnly(boolean poseSupportedOnly) {
        this.poseSupportedOnly = poseSupportedOnly;
        return this;
    }
}
