package ntu.quy65132908.smartgym_ai.data.model;

import com.google.firebase.firestore.PropertyName;

public class Exercise {
    private String id;
    private String name;
    private int sets;
    private int reps;
    private Float weight;
    private boolean isCompleted;
    private String notes;
    private String poseTypeKey;
    private String primaryMuscle;
    private String catalogItemId;
    private int durationSeconds;
    private int orderIndex;

    public Exercise() {}

    public Exercise(String id, String name, int sets, int reps, Float weight, boolean isCompleted) {
        this.id = id;
        this.name = name;
        this.sets = sets;
        this.reps = reps;
        this.weight = weight;
        this.isCompleted = isCompleted;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getSets() { return sets; }
    public int getReps() { return reps; }
    public Float getWeight() { return weight; }

    @PropertyName("isCompleted")
    public boolean isCompleted() { return isCompleted; }

    public String getNotes() { return notes; }
    public String getPoseTypeKey() { return poseTypeKey; }
    public String getPrimaryMuscle() { return primaryMuscle; }
    public String getCatalogItemId() { return catalogItemId; }
    public int getDurationSeconds() { return Math.max(0, durationSeconds); }
    public int getOrderIndex() { return Math.max(0, orderIndex); }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setSets(int sets) { this.sets = sets; }
    public void setReps(int reps) { this.reps = reps; }
    public void setWeight(Float weight) { this.weight = weight; }

    @PropertyName("isCompleted")
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public void setNotes(String notes) { this.notes = notes; }
    public void setPoseTypeKey(String poseTypeKey) { this.poseTypeKey = poseTypeKey; }
    public void setPrimaryMuscle(String primaryMuscle) { this.primaryMuscle = primaryMuscle; }
    public void setCatalogItemId(String catalogItemId) { this.catalogItemId = catalogItemId; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = Math.max(0, durationSeconds); }
    public void setOrderIndex(int orderIndex) { this.orderIndex = Math.max(0, orderIndex); }
}
