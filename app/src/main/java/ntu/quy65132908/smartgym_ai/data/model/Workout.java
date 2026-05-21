package ntu.quy65132908.smartgym_ai.data.model;

import java.util.List;

public class Workout {
    private String id;
    private String title;
    private String subtitle;
    private String intensity;
    private int durationMinutes;
    private List<Exercise> exercises;
    private boolean isCompleted;
    private int dayOfWeek; // 1=Monday...7=Sunday

    public Workout() {}

    public Workout(String id, String title, String subtitle, String intensity, int durationMinutes) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.intensity = intensity;
        this.durationMinutes = durationMinutes;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getIntensity() { return intensity; }
    public int getDurationMinutes() { return durationMinutes; }
    public List<Exercise> getExercises() { return exercises; }
    public boolean isCompleted() { return isCompleted; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public void setIntensity(String intensity) { this.intensity = intensity; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setExercises(List<Exercise> exercises) { this.exercises = exercises; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
}
