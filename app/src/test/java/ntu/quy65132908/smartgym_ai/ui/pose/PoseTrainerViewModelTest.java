package ntu.quy65132908.smartgym_ai.ui.pose;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import com.google.firebase.auth.FirebaseUser;

import org.junit.Rule;
import org.junit.Test;

import ntu.quy65132908.smartgym_ai.data.model.WorkoutSession;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ChallengeRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;

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
        assertTrue(state.getFeedback().contains("AI Pose"));
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

    @Test
    public void linkedWorkout_marksCompletionReadyOnlyAfterTargetReached() {
        SavedStateHandle handle = new SavedStateHandle();
        handle.set("exerciseType", "push_up");
        handle.set("workoutId", "w1");
        handle.set("exerciseId", "e1");
        handle.set("exerciseName", "Chong day");
        handle.set("targetReps", 1);
        handle.set("lockExerciseSelection", true);
        PoseTrainerViewModel viewModel = new PoseTrainerViewModel(handle);

        viewModel.selectExerciseType(ExerciseType.SQUAT);
        viewModel.onPoseFrame(PoseTestFactory.pushUpDown());
        viewModel.onPoseFrame(PoseTestFactory.pushUpDown());
        viewModel.onPoseFrame(PoseTestFactory.pushUpTop());
        viewModel.onPoseFrame(PoseTestFactory.pushUpTop());

        PoseTrainerUiState state = viewModel.getUiState().getValue();
        assertEquals(ExerciseType.PUSH_UP, state.getExerciseType());
        assertEquals(1, state.getReps());
        assertTrue(state.isExerciseSelectionLocked());
        assertTrue(state.isCompletionReady());
        assertFalse(state.isCompletionRecorded());
        assertEquals(1, state.getTargetReps());
    }

    @Test
    public void confirmExerciseCompletion_updatesWorkoutAndChallengeWhenWorkoutComplete() {
        WorkoutRepository workoutRepository = mock(WorkoutRepository.class);
        ChallengeRepository challengeRepository = mock(ChallengeRepository.class);
        AuthRepository authRepository = mock(AuthRepository.class);
        FirebaseUser user = mock(FirebaseUser.class);
        when(user.getUid()).thenReturn("u1");
        when(authRepository.getCurrentUser()).thenReturn(user);
        doAnswer(invocation -> {
            WorkoutRepository.CompletionCallback callback = invocation.getArgument(5);
            callback.onSuccess(true);
            return null;
        }).when(workoutRepository).markExerciseCompleteAndSyncWorkout(
                eq("u1"),
                eq("w1"),
                eq("e1"),
                eq(true),
                eq(WorkoutSession.SOURCE_POSE),
                any(WorkoutRepository.CompletionCallback.class));
        doAnswer(invocation -> {
            ChallengeRepository.SimpleCallback callback = invocation.getArgument(1);
            callback.onSuccess();
            return null;
        }).when(challengeRepository).recordWorkoutCompletion(
                eq("u1"),
                any(ChallengeRepository.SimpleCallback.class));

        SavedStateHandle handle = new SavedStateHandle();
        handle.set("exerciseType", "push_up");
        handle.set("workoutId", "w1");
        handle.set("exerciseId", "e1");
        handle.set("targetReps", 1);
        PoseTrainerViewModel viewModel = new PoseTrainerViewModel(
                workoutRepository,
                challengeRepository,
                authRepository,
                handle);
        viewModel.onPoseFrame(PoseTestFactory.pushUpDown());
        viewModel.onPoseFrame(PoseTestFactory.pushUpDown());
        viewModel.onPoseFrame(PoseTestFactory.pushUpTop());
        viewModel.onPoseFrame(PoseTestFactory.pushUpTop());

        viewModel.confirmExerciseCompletion();

        verify(workoutRepository).markExerciseCompleteAndSyncWorkout(
                eq("u1"),
                eq("w1"),
                eq("e1"),
                eq(true),
                eq(WorkoutSession.SOURCE_POSE),
                any(WorkoutRepository.CompletionCallback.class));
        verify(challengeRepository).recordWorkoutCompletion(
                eq("u1"),
                any(ChallengeRepository.SimpleCallback.class));
        PoseTrainerUiState state = viewModel.getUiState().getValue();
        assertTrue(state.isCompletionRecorded());
        assertFalse(state.isCompletionReady());
    }
}
