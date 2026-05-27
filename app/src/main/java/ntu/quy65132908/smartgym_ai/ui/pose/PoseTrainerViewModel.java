package ntu.quy65132908.smartgym_ai.ui.pose;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.model.WorkoutSession;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ChallengeRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

@HiltViewModel
public class PoseTrainerViewModel extends ViewModel {
    private static final String SELECTION_REQUIRED_MESSAGE = "Ch\u1ecdn b\u00e0i t\u1eadp AI Pose \u0111\u1ec3 b\u1eaft \u0111\u1ea7u.";
    private static final String CAMERA_PERMISSION_MESSAGE = "Kh\u00f4ng th\u1ec3 m\u1edf camera khi ch\u01b0a c\u00f3 quy\u1ec1n.";
    private static final String COMPLETION_READY_MESSAGE = "\u0110\u00e3 \u0111\u1ea1t m\u1ee5c ti\u00eau. X\u00e1c nh\u1eadn \u0111\u1ec3 c\u1eadp nh\u1eadt b\u00e0i t\u1eadp.";
    private static final String COMPLETION_SAVED_MESSAGE = "\u0110\u00e3 \u0111\u00e1nh d\u1ea5u ho\u00e0n th\u00e0nh b\u00e0i t\u1eadp.";
    private static final String COMPLETION_SAVE_ERROR = "Kh\u00f4ng th\u1ec3 c\u1eadp nh\u1eadt b\u00e0i t\u1eadp. Th\u1eed l\u1ea1i.";

    private final WorkoutRepository workoutRepository;
    private final ChallengeRepository challengeRepository;
    private final AuthRepository authRepository;
    private final FormFeedbackEngine feedbackEngine;
    private final MutableLiveData<PoseTrainerUiState> uiState = new MutableLiveData<>();
    private final SingleLiveEvent<String> message = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> completionSaved = new SingleLiveEvent<>();

    private final String workoutId;
    private final String exerciseId;
    private final boolean exerciseSelectionLocked;
    private final int targetReps;
    private final int targetSeconds;
    private ExerciseType exerciseType;
    private boolean completionRecorded;

    @Inject
    public PoseTrainerViewModel(WorkoutRepository workoutRepository,
                                ChallengeRepository challengeRepository,
                                AuthRepository authRepository,
                                SavedStateHandle savedStateHandle) {
        this.workoutRepository = workoutRepository;
        this.challengeRepository = challengeRepository;
        this.authRepository = authRepository;
        this.workoutId = stringArg(savedStateHandle, "workoutId");
        this.exerciseId = stringArg(savedStateHandle, "exerciseId");
        Boolean selectionRequired = savedStateHandle.get("selectionRequired");
        Boolean lockExerciseSelection = savedStateHandle.get("lockExerciseSelection");
        Integer targetRepsArg = savedStateHandle.get("targetReps");
        Integer targetSecondsArg = savedStateHandle.get("targetSeconds");

        this.exerciseType = ExerciseType.fromKey(stringArg(savedStateHandle, "exerciseType"));
        this.exerciseSelectionLocked = Boolean.TRUE.equals(lockExerciseSelection);
        this.targetReps = targetRepsArg != null ? Math.max(0, targetRepsArg) : 0;
        this.targetSeconds = targetSecondsArg != null ? Math.max(0, targetSecondsArg) : 0;
        this.feedbackEngine = new FormFeedbackEngine();
        this.feedbackEngine.setExerciseType(exerciseType);
        emitState(
                Boolean.TRUE.equals(selectionRequired) ? SELECTION_REQUIRED_MESSAGE : feedbackEngine.getReadyMessage(),
                0,
                0,
                0,
                false,
                false,
                false,
                false,
                false);
    }

    public PoseTrainerViewModel(SavedStateHandle savedStateHandle) {
        this(null, null, null, savedStateHandle);
    }

    public LiveData<PoseTrainerUiState> getUiState() {
        return uiState;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public LiveData<Boolean> getCompletionSaved() {
        return completionSaved;
    }

    public ExerciseType getExerciseType() {
        return exerciseType;
    }

    public void selectExerciseType(ExerciseType selectedType) {
        if (exerciseSelectionLocked || selectedType == null) {
            return;
        }
        exerciseType = selectedType;
        feedbackEngine.setExerciseType(selectedType);
        PoseTrainerUiState current = currentState();
        emitState(feedbackEngine.getReadyMessage(), 0, 0, 0,
                current.isCameraReady(), false, current.isLoading(), false, false);
    }

    public void setLoading(boolean loading) {
        PoseTrainerUiState current = currentState();
        emitState(current.getFeedback(), current.getReps(), current.getHoldSeconds(), current.getQualityPercent(),
                current.isCameraReady(), current.isPermissionDenied(), loading,
                current.isCompletionReady(), current.isCompletionSaving());
    }

    public void setCameraReady(boolean ready) {
        PoseTrainerUiState current = currentState();
        emitState(current.getFeedback(), current.getReps(), current.getHoldSeconds(), current.getQualityPercent(),
                ready, false, false, current.isCompletionReady(), current.isCompletionSaving());
    }

    public void setPermissionDenied() {
        PoseTrainerUiState current = currentState();
        emitState(CAMERA_PERMISSION_MESSAGE, current.getReps(), current.getHoldSeconds(), current.getQualityPercent(),
                false, true, false, current.isCompletionReady(), false);
    }

    public void onPoseFrame(PoseFrame frame) {
        PoseFeedback feedback = feedbackEngine.evaluate(frame);
        boolean completionReady = isCompletionTargetReached(feedback);
        String feedbackMessage = completionReady ? COMPLETION_READY_MESSAGE : feedback.getMessage();
        uiState.postValue(buildState(feedbackMessage, feedback.getReps(), feedback.getHoldSeconds(),
                feedback.getQualityPercent(), true, false, false, completionReady, false));
    }

    public void confirmExerciseCompletion() {
        PoseTrainerUiState current = currentState();
        if (completionRecorded || current.isCompletionSaving()) {
            return;
        }
        if (!current.isCompletionReady()) {
            return;
        }
        if (!isLinkedWorkoutExercise()) {
            message.setValue(COMPLETION_SAVE_ERROR);
            return;
        }
        if (workoutRepository == null || authRepository == null) {
            completionRecorded = true;
            emitCompletionSavedState(current);
            return;
        }

        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            message.setValue(COMPLETION_SAVE_ERROR);
            return;
        }

        emitState(current.getFeedback(), current.getReps(), current.getHoldSeconds(), current.getQualityPercent(),
                current.isCameraReady(), current.isPermissionDenied(), false, current.isCompletionReady(), true);
        workoutRepository.markExerciseCompleteAndSyncWorkout(
                user.getUid(),
                workoutId,
                exerciseId,
                true,
                WorkoutSession.SOURCE_POSE,
                new WorkoutRepository.CompletionCallback() {
                    @Override
                    public void onSuccess(boolean workoutCompleted) {
                        completionRecorded = true;
                        emitCompletionSavedState(currentState());
                        if (workoutCompleted && challengeRepository != null) {
                            challengeRepository.recordWorkoutCompletion(user.getUid(), new ChallengeRepository.SimpleCallback() {
                                @Override
                                public void onSuccess() {
                                    // Challenge progress is secondary to exercise completion.
                                }

                                @Override
                                public void onError(Exception e) {
                                    // Do not roll back the completed exercise if challenge sync fails.
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        PoseTrainerUiState latest = currentState();
                        emitState(latest.getFeedback(), latest.getReps(), latest.getHoldSeconds(),
                                latest.getQualityPercent(), latest.isCameraReady(), latest.isPermissionDenied(),
                                false, latest.isCompletionReady(), false);
                        message.postValue(COMPLETION_SAVE_ERROR);
                    }
                });
    }

    private boolean isCompletionTargetReached(PoseFeedback feedback) {
        if (completionRecorded || !isLinkedWorkoutExercise() || feedback == null) {
            return false;
        }
        if (targetSeconds > 0 && exerciseType.usesDurationMetric()) {
            return feedback.getHoldSeconds() >= targetSeconds;
        }
        return targetReps > 0 && feedback.getReps() >= targetReps;
    }

    private void emitCompletionSavedState(PoseTrainerUiState current) {
        emitState(COMPLETION_SAVED_MESSAGE, current.getReps(), current.getHoldSeconds(), current.getQualityPercent(),
                current.isCameraReady(), current.isPermissionDenied(), false, false, false);
        message.postValue(COMPLETION_SAVED_MESSAGE);
        completionSaved.postValue(true);
    }

    private void emitState(String feedback,
                           int reps,
                           int holdSeconds,
                           int qualityPercent,
                           boolean cameraReady,
                           boolean permissionDenied,
                           boolean loading,
                           boolean completionReady,
                           boolean completionSaving) {
        uiState.setValue(buildState(feedback, reps, holdSeconds, qualityPercent, cameraReady, permissionDenied,
                loading, completionReady, completionSaving));
    }

    private PoseTrainerUiState buildState(String feedback,
                                          int reps,
                                          int holdSeconds,
                                          int qualityPercent,
                                          boolean cameraReady,
                                          boolean permissionDenied,
                                          boolean loading,
                                          boolean completionReady,
                                          boolean completionSaving) {
        return new PoseTrainerUiState(exerciseType, feedback, reps, holdSeconds, qualityPercent,
                cameraReady, permissionDenied, loading, exerciseSelectionLocked, completionReady,
                completionSaving, completionRecorded, targetReps, targetSeconds);
    }

    private PoseTrainerUiState currentState() {
        PoseTrainerUiState current = uiState.getValue();
        if (current != null) {
            return current;
        }
        return buildState(feedbackEngine.getReadyMessage(), 0, 0, 0,
                false, false, false, false, false);
    }

    private boolean isLinkedWorkoutExercise() {
        return !workoutId.isEmpty() && !exerciseId.isEmpty();
    }

    private static String stringArg(SavedStateHandle savedStateHandle, String key) {
        String value = savedStateHandle.get(key);
        return value != null ? value : "";
    }
}
