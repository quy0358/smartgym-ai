package ntu.quy65132908.smartgym_ai.data.model;

import java.util.HashMap;
import java.util.Map;

public class WorkoutSession {
    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_POSE = "POSE";

    private String id;
    private String workoutId;
    private long completedAt;
    private int durationMinutes;
    private String intensity;
    private String source;

    public WorkoutSession() {}

    public static WorkoutSession fromWorkout(String workoutId, Workout workout, long completedAt, String source) {
        WorkoutSession session = new WorkoutSession();
        session.setId(workoutId);
        session.setWorkoutId(workoutId);
        session.setCompletedAt(completedAt);
        session.setDurationMinutes(workout != null ? workout.getDurationMinutes() : 0);
        session.setIntensity(workout != null ? workout.getIntensity() : null);
        session.setSource(source);
        return session;
    }

    public String getId() { return id; }
    public String getWorkoutId() { return workoutId; }
    public long getCompletedAt() { return completedAt; }
    public int getDurationMinutes() { return Math.max(0, durationMinutes); }
    public String getIntensity() { return intensity; }
    public String getSource() { return source != null ? source : SOURCE_MANUAL; }

    public void setId(String id) { this.id = id; }
    public void setWorkoutId(String workoutId) { this.workoutId = workoutId; }
    public void setCompletedAt(long completedAt) { this.completedAt = Math.max(0L, completedAt); }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = Math.max(0, durationMinutes); }
    public void setIntensity(String intensity) { this.intensity = intensity; }
    public void setSource(String source) {
        this.source = SOURCE_POSE.equals(source) ? SOURCE_POSE : SOURCE_MANUAL;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (workoutId != null) {
            map.put("workoutId", workoutId);
        }
        map.put("completedAt", completedAt);
        map.put("durationMinutes", getDurationMinutes());
        if (intensity != null) {
            map.put("intensity", intensity);
        }
        map.put("source", getSource());
        return map;
    }
}
