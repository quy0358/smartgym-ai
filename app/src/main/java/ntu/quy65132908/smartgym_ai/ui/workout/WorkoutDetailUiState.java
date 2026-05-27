package ntu.quy65132908.smartgym_ai.ui.workout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ntu.quy65132908.smartgym_ai.data.model.Exercise;
import ntu.quy65132908.smartgym_ai.data.model.Workout;

/**
 * Trạng thái UI bất biến cho màn hình chi tiết bài tập.
 * Nguồn dữ liệu duy nhất: Fragment quan sát một LiveData kiểu này.
 */
public class WorkoutDetailUiState {
    private final List<Exercise> exercises;
    private final int progressPercent;
    private final boolean isLoading;
    private final String errorMessage;
    private final String subtitle;
    private final String dayType;

    private WorkoutDetailUiState(List<Exercise> exercises, int progressPercent,
                                 boolean isLoading, String errorMessage, String subtitle, String dayType) {
        this.exercises = exercises;
        this.progressPercent = progressPercent;
        this.isLoading = isLoading;
        this.errorMessage = errorMessage;
        this.subtitle = subtitle != null ? subtitle : "";
        this.dayType = Workout.normalizeDayType(dayType);
    }

    public static WorkoutDetailUiState loading(String subtitle) {
        return loading(subtitle, Workout.DAY_TYPE_TRAINING);
    }

    public static WorkoutDetailUiState loading(String subtitle, String dayType) {
        return new WorkoutDetailUiState(null, 0, true, null, subtitle, dayType);
    }

    public static WorkoutDetailUiState success(List<Exercise> exercises, String subtitle) {
        return success(exercises, subtitle, Workout.DAY_TYPE_TRAINING);
    }

    public static WorkoutDetailUiState success(List<Exercise> exercises, String subtitle, String dayType) {
        if (exercises == null) exercises = Collections.emptyList();
        List<Exercise> exerciseCopies = copyExercises(exercises);

        int completed = 0;
        for (Exercise ex : exerciseCopies) {
            if (ex.isCompleted()) completed++;
        }
        int percent = exerciseCopies.isEmpty() ? 0 : (completed * 100) / exerciseCopies.size();

        return new WorkoutDetailUiState(
                Collections.unmodifiableList(exerciseCopies),
                percent,
                false,
                null,
                subtitle,
                dayType);
    }

    public static WorkoutDetailUiState rest(String subtitle) {
        return new WorkoutDetailUiState(
                Collections.emptyList(),
                0,
                false,
                null,
                subtitle,
                Workout.DAY_TYPE_REST);
    }

    public static WorkoutDetailUiState error(String message, String subtitle) {
        return new WorkoutDetailUiState(null, 0, false, message, subtitle, Workout.DAY_TYPE_TRAINING);
    }

    public static WorkoutDetailUiState error(String message, String subtitle, String dayType) {
        return new WorkoutDetailUiState(null, 0, false, message, subtitle, dayType);
    }

    public List<Exercise> getExercises() { return exercises; }
    public int getProgressPercent() { return progressPercent; }
    public boolean isLoading() { return isLoading; }
    public String getErrorMessage() { return errorMessage; }
    public String getSubtitle() { return subtitle; }
    public String getDayType() { return dayType; }

    public boolean isEmpty() {
        return exercises != null && exercises.isEmpty();
    }

    public boolean hasExercises() {
        return exercises != null && !exercises.isEmpty();
    }

    public boolean isRestDay() {
        return Workout.DAY_TYPE_REST.equals(dayType);
    }

    public boolean isRecoveryDay() {
        return Workout.DAY_TYPE_RECOVERY.equals(dayType);
    }

    public boolean shouldShowProgress() {
        return !isRestDay();
    }

    public boolean shouldShowPoseAction() {
        return !isRestDay() && hasExercises();
    }

    private static List<Exercise> copyExercises(List<Exercise> exercises) {
        List<Exercise> copies = new ArrayList<>();
        for (Exercise exercise : exercises) {
            if (exercise == null) {
                continue;
            }
            copies.add(copyExercise(exercise));
        }
        return copies;
    }

    private static Exercise copyExercise(Exercise exercise) {
        Exercise copy = new Exercise(
                exercise.getId(),
                exercise.getName(),
                exercise.getSets(),
                exercise.getReps(),
                exercise.getWeight(),
                exercise.isCompleted());
        copy.setNotes(exercise.getNotes());
        copy.setPoseTypeKey(exercise.getPoseTypeKey());
        copy.setPrimaryMuscle(exercise.getPrimaryMuscle());
        copy.setDurationSeconds(exercise.getDurationSeconds());
        copy.setOrderIndex(exercise.getOrderIndex());
        return copy;
    }
}
