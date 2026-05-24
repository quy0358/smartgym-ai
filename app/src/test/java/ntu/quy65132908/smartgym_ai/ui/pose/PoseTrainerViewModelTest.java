package ntu.quy65132908.smartgym_ai.ui.pose;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import org.junit.Rule;
import org.junit.Test;

public class PoseTrainerViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void init_unknownExerciseTypeRequestsManualSelection() {
        SavedStateHandle handle = new SavedStateHandle();
        handle.set("exerciseType", "unknown");
        handle.set("selectionRequired", true);

        PoseTrainerViewModel viewModel = new PoseTrainerViewModel(handle);

        PoseTrainerUiState state = viewModel.getUiState().getValue();
        assertEquals(ExerciseType.PUSH_UP, state.getExerciseType());
        assertTrue(state.getFeedback().contains("Chọn bài"));
    }

    @Test
    public void selectExerciseType_resetsMetricsAndUpdatesType() {
        SavedStateHandle handle = new SavedStateHandle();
        handle.set("exerciseType", "push_up");
        PoseTrainerViewModel viewModel = new PoseTrainerViewModel(handle);
        viewModel.onPoseFrame(PoseTestFactory.pushUpDown());
        viewModel.onPoseFrame(PoseTestFactory.pushUpDown());
        viewModel.onPoseFrame(PoseTestFactory.pushUpTop());
        viewModel.onPoseFrame(PoseTestFactory.pushUpTop());

        viewModel.selectExerciseType(ExerciseType.PLANK);

        PoseTrainerUiState state = viewModel.getUiState().getValue();
        assertEquals(ExerciseType.PLANK, state.getExerciseType());
        assertEquals(0, state.getReps());
        assertEquals(0, state.getHoldSeconds());
    }
}
