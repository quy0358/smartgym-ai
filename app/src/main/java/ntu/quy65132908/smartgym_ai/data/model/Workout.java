package ntu.quy65132908.smartgym_ai.data.model;

import com.google.firebase.firestore.PropertyName;

import java.util.List;
import java.util.Locale;

public class Workout {
    public static final String DAY_TYPE_TRAINING = "TRAINING";
    public static final String DAY_TYPE_RECOVERY = "RECOVERY";
    public static final String DAY_TYPE_REST = "REST";

    private String id;
    private String title;
    private String subtitle;
    private String intensity;
    private int durationMinutes;
    private List<Exercise> exercises;
    private boolean isCompleted;
    private boolean isCustom;
    private int dayOfWeek; // 1=Thứ 2...7=Chủ nhật
    private int exerciseCount; // Số bài tập đã phi chuẩn hóa để hiển thị trên Dashboard
    private String dayType = DAY_TYPE_TRAINING;

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

    @PropertyName("isCompleted")
    public boolean isCompleted() { return isCompleted; }

    @PropertyName("isCustom")
    public boolean isCustom() { return isCustom; }

    public String getDayType() { return normalizeDayType(dayType); }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public void setIntensity(String intensity) { this.intensity = intensity; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setExercises(List<Exercise> exercises) { this.exercises = exercises; }

    @PropertyName("isCompleted")
    public void setCompleted(boolean completed) { isCompleted = completed; }

    @PropertyName("isCustom")
    public void setCustom(boolean custom) { isCustom = custom; }

    public void setDayType(String dayType) { this.dayType = normalizeDayType(dayType); }

    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public int getExerciseCount() { return exerciseCount; }
    public void setExerciseCount(int exerciseCount) { this.exerciseCount = exerciseCount; }

    public boolean isTrainingDay() {
        return DAY_TYPE_TRAINING.equals(getDayType());
    }

    public boolean isRecoveryDay() {
        return DAY_TYPE_RECOVERY.equals(getDayType());
    }

    public boolean isRestDay() {
        return DAY_TYPE_REST.equals(getDayType());
    }

    public static String normalizeDayType(String rawDayType) {
        if (rawDayType == null) {
            return DAY_TYPE_TRAINING;
        }
        String normalized = rawDayType.trim().toUpperCase(Locale.ROOT);
        switch (normalized) {
            case DAY_TYPE_REST:
                return DAY_TYPE_REST;
            case DAY_TYPE_RECOVERY:
                return DAY_TYPE_RECOVERY;
            case DAY_TYPE_TRAINING:
            default:
                return DAY_TYPE_TRAINING;
        }
    }
}
