package ntu.quy65132908.smartgym_ai.data.model;

import java.util.ArrayList;
import java.util.List;

public class CustomWorkoutTemplate {
    private String id;
    private String title;
    private String goal;
    private int durationMinutes;
    private List<Exercise> exercises = new ArrayList<>();
    private long createdAt;
    private boolean injuryAware;

    public CustomWorkoutTemplate() {}

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getGoal() { return goal; }
    public int getDurationMinutes() { return durationMinutes; }
    public List<Exercise> getExercises() { return exercises; }
    public long getCreatedAt() { return createdAt; }
    public boolean isInjuryAware() { return injuryAware; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setGoal(String goal) { this.goal = goal; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setExercises(List<Exercise> exercises) { this.exercises = exercises != null ? exercises : new ArrayList<>(); }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setInjuryAware(boolean injuryAware) { this.injuryAware = injuryAware; }
}
