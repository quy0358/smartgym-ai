package ntu.quy65132908.smartgym_ai.data.model;

public class ExerciseCatalogItem {
    private String id;
    private String name;
    private String primaryMuscle;
    private String equipment;
    private String difficulty;
    private int defaultSets;
    private int defaultReps;
    private int restSeconds;
    private String poseTypeKey;
    private String safetyNote;

    public ExerciseCatalogItem() {}

    public ExerciseCatalogItem(String id,
                               String name,
                               String primaryMuscle,
                               String equipment,
                               String difficulty,
                               int defaultSets,
                               int defaultReps,
                               int restSeconds,
                               String poseTypeKey,
                               String safetyNote) {
        this.id = id;
        this.name = name;
        this.primaryMuscle = primaryMuscle;
        this.equipment = equipment;
        this.difficulty = difficulty;
        this.defaultSets = defaultSets;
        this.defaultReps = defaultReps;
        this.restSeconds = restSeconds;
        this.poseTypeKey = poseTypeKey;
        this.safetyNote = safetyNote;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPrimaryMuscle() { return primaryMuscle; }
    public String getEquipment() { return equipment; }
    public String getDifficulty() { return difficulty; }
    public int getDefaultSets() { return defaultSets; }
    public int getDefaultReps() { return defaultReps; }
    public int getRestSeconds() { return restSeconds; }
    public String getPoseTypeKey() { return poseTypeKey; }
    public String getSafetyNote() { return safetyNote; }
    public boolean isPoseSupported() { return poseTypeKey != null && !poseTypeKey.trim().isEmpty(); }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPrimaryMuscle(String primaryMuscle) { this.primaryMuscle = primaryMuscle; }
    public void setEquipment(String equipment) { this.equipment = equipment; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setDefaultSets(int defaultSets) { this.defaultSets = defaultSets; }
    public void setDefaultReps(int defaultReps) { this.defaultReps = defaultReps; }
    public void setRestSeconds(int restSeconds) { this.restSeconds = restSeconds; }
    public void setPoseTypeKey(String poseTypeKey) { this.poseTypeKey = poseTypeKey; }
    public void setSafetyNote(String safetyNote) { this.safetyNote = safetyNote; }

    public Exercise toExercise() {
        boolean timedExercise = "plank".equalsIgnoreCase(poseTypeKey);
        Exercise exercise = new Exercise(null, name, defaultSets, timedExercise ? 0 : defaultReps, null, false);
        exercise.setPoseTypeKey(poseTypeKey);
        exercise.setPrimaryMuscle(primaryMuscle);
        exercise.setCatalogItemId(id);
        if (timedExercise) {
            exercise.setDurationSeconds(defaultReps);
        }
        String note = safetyNote != null ? safetyNote : "";
        if (restSeconds > 0) {
            note = note.isEmpty() ? "Nghỉ " + restSeconds + " giây" : "Nghỉ " + restSeconds + " giây. " + note;
        }
        exercise.setNotes(note);
        return exercise;
    }
}
