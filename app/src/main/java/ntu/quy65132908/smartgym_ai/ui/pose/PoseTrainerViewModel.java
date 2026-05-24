package ntu.quy65132908.smartgym_ai.ui.pose;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class PoseTrainerViewModel extends ViewModel {
    private static final String SELECTION_REQUIRED_MESSAGE = "Chọn bài tập AI Pose để bắt đầu.";
    private static final String CAMERA_PERMISSION_MESSAGE = "Không thể mở camera khi chưa có quyền.";

    private final FormFeedbackEngine feedbackEngine;
    private final MutableLiveData<PoseTrainerUiState> uiState = new MutableLiveData<>();
    private ExerciseType exerciseType;

    @Inject
    public PoseTrainerViewModel(SavedStateHandle savedStateHandle) {
        String exerciseTypeArg = savedStateHandle.get("exerciseType");
        Boolean selectionRequired = savedStateHandle.get("selectionRequired");
        this.exerciseType = ExerciseType.fromKey(exerciseTypeArg);
        this.feedbackEngine = new FormFeedbackEngine();
        this.feedbackEngine.setExerciseType(exerciseType);
        uiState.setValue(new PoseTrainerUiState(
                exerciseType,
                Boolean.TRUE.equals(selectionRequired) ? SELECTION_REQUIRED_MESSAGE : feedbackEngine.getReadyMessage(),
                0,
                0,
                0,
                false,
                false,
                false));
    }

    public LiveData<PoseTrainerUiState> getUiState() {
        return uiState;
    }

    public ExerciseType getExerciseType() {
        return exerciseType;
    }

    public void selectExerciseType(ExerciseType selectedType) {
        if (selectedType == null) {
            return;
        }
        exerciseType = selectedType;
        feedbackEngine.setExerciseType(selectedType);
        PoseTrainerUiState current = currentState();
        uiState.setValue(new PoseTrainerUiState(
                exerciseType,
                feedbackEngine.getReadyMessage(),
                0,
                0,
                0,
                current.isCameraReady(),
                false,
                current.isLoading()));
    }

    public void setLoading(boolean loading) {
        PoseTrainerUiState current = currentState();
        uiState.setValue(new PoseTrainerUiState(
                exerciseType,
                current.getFeedback(),
                current.getReps(),
                current.getHoldSeconds(),
                current.getQualityPercent(),
                current.isCameraReady(),
                current.isPermissionDenied(),
                loading));
    }

    public void setCameraReady(boolean ready) {
        PoseTrainerUiState current = currentState();
        uiState.setValue(new PoseTrainerUiState(
                exerciseType,
                current.getFeedback(),
                current.getReps(),
                current.getHoldSeconds(),
                current.getQualityPercent(),
                ready,
                false,
                false));
    }

    public void setPermissionDenied() {
        PoseTrainerUiState current = currentState();
        uiState.setValue(new PoseTrainerUiState(
                exerciseType,
                CAMERA_PERMISSION_MESSAGE,
                current.getReps(),
                current.getHoldSeconds(),
                current.getQualityPercent(),
                false,
                true,
                false));
    }

    public void onPoseFrame(PoseFrame frame) {
        PoseFeedback feedback = feedbackEngine.evaluate(frame);
        uiState.postValue(new PoseTrainerUiState(
                exerciseType,
                feedback.getMessage(),
                feedback.getReps(),
                feedback.getHoldSeconds(),
                feedback.getQualityPercent(),
                true,
                false,
                false));
    }

    private PoseTrainerUiState currentState() {
        PoseTrainerUiState current = uiState.getValue();
        if (current != null) {
            return current;
        }
        return new PoseTrainerUiState(
                exerciseType,
                feedbackEngine.getReadyMessage(),
                0,
                0,
                0,
                false,
                false,
                false);
    }
}
