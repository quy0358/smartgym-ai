package ntu.quy65132908.smartgym_ai.ui.pose;

public class PoseTrainerUiState {
    private final ExerciseType exerciseType;
    private final String feedback;
    private final int reps;
    private final int holdSeconds;
    private final int qualityPercent;
    private final boolean cameraReady;
    private final boolean permissionDenied;
    private final boolean loading;

    public PoseTrainerUiState(ExerciseType exerciseType,
                              String feedback,
                              int reps,
                              int qualityPercent,
                              boolean cameraReady,
                              boolean permissionDenied,
                              boolean loading) {
        this(exerciseType, feedback, reps, 0, qualityPercent, cameraReady, permissionDenied, loading);
    }

    public PoseTrainerUiState(ExerciseType exerciseType,
                              String feedback,
                              int reps,
                              int holdSeconds,
                              int qualityPercent,
                              boolean cameraReady,
                              boolean permissionDenied,
                              boolean loading) {
        this.exerciseType = exerciseType != null ? exerciseType : ExerciseType.PUSH_UP;
        this.feedback = feedback != null ? feedback : "";
        this.reps = Math.max(0, reps);
        this.holdSeconds = Math.max(0, holdSeconds);
        this.qualityPercent = Math.max(0, Math.min(100, qualityPercent));
        this.cameraReady = cameraReady;
        this.permissionDenied = permissionDenied;
        this.loading = loading;
    }

    public ExerciseType getExerciseType() {
        return exerciseType;
    }

    public String getFeedback() {
        return feedback;
    }

    public int getReps() {
        return reps;
    }

    public int getHoldSeconds() {
        return holdSeconds;
    }

    public int getQualityPercent() {
        return qualityPercent;
    }

    public boolean isCameraReady() {
        return cameraReady;
    }

    public boolean isPermissionDenied() {
        return permissionDenied;
    }

    public boolean isLoading() {
        return loading;
    }
}
