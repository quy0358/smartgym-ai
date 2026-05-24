package ntu.quy65132908.smartgym_ai.ui.workout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import ntu.quy65132908.smartgym_ai.data.model.Exercise;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ChallengeRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;

public class WorkoutViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private WorkoutRepository workoutRepository;
    @Mock private ChallengeRepository challengeRepository;
    @Mock private AuthRepository authRepository;
    @Mock private FirebaseUser firebaseUser;

    private SavedStateHandle savedState;
    private WorkoutViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(firebaseUser.getUid()).thenReturn("uid-1");
        when(authRepository.getCurrentUser()).thenReturn(firebaseUser);

        savedState = new SavedStateHandle();
        savedState.set("workoutId", "workout-1");
        savedState.set("workoutTitle", "Arm day");
        savedState.set("workoutDuration", 30);
    }

    @Test
    public void init_extractsArgsFromSavedStateHandle() {
        stubLoadSuccess(Collections.emptyList());

        createViewModel();

        WorkoutDetailUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertTrue(state.getSubtitle().contains("Arm day"));
        assertTrue(state.getSubtitle().contains("30"));
    }

    @Test
    public void loadExercises_success_emitsSuccessState() {
        List<Exercise> exercises = Arrays.asList(
                new Exercise("e1", "Push up", 3, 12, null, false),
                new Exercise("e2", "Squat", 3, 10, 20f, true));
        stubLoadSuccess(exercises);

        createViewModel();

        WorkoutDetailUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertFalse(state.isLoading());
        assertNull(state.getErrorMessage());
        assertEquals(2, state.getExercises().size());
        assertEquals(50, state.getProgressPercent());
    }

    @Test
    public void loadExercises_success_preservesPoseTypeKey() {
        Exercise exercise = new Exercise("e1", "Plank", 3, 30, null, false);
        exercise.setPoseTypeKey("plank");
        stubLoadSuccess(Collections.singletonList(exercise));

        createViewModel();

        WorkoutDetailUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertEquals("plank", state.getExercises().get(0).getPoseTypeKey());
    }

    @Test
    public void loadExercises_error_emitsErrorState() {
        doAnswer(invocation -> {
            WorkoutRepository.ExerciseListCallback cb = invocation.getArgument(2);
            cb.onError(new Exception("Network error"));
            return null;
        }).when(workoutRepository).getExercises(eq("uid-1"), eq("workout-1"), any());

        createViewModel();

        WorkoutDetailUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertFalse(state.isLoading());
        assertNotNull(state.getErrorMessage());
        assertNull(state.getExercises());
    }

    @Test
    public void loadExercises_empty_emitsSuccessWithEmptyList() {
        stubLoadSuccess(Collections.emptyList());

        createViewModel();

        WorkoutDetailUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertTrue(state.isEmpty());
        assertEquals(0, state.getProgressPercent());
    }

    @Test
    public void init_restDayArg_emitsRestStateWithoutLoadingExercises() {
        savedState.set("dayType", Workout.DAY_TYPE_REST);

        createViewModel();

        verify(workoutRepository, never()).getExercises(anyString(), anyString(), any());
        WorkoutDetailUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertTrue(state.isRestDay());
        assertFalse(state.shouldShowProgress());
        assertFalse(state.shouldShowPoseAction());
    }

    @Test
    public void toggleExercise_optimistic_updatesImmediately() {
        List<Exercise> exercises = Arrays.asList(
                new Exercise("e1", "Push up", 3, 12, null, false),
                new Exercise("e2", "Squat", 3, 10, null, false));
        stubLoadSuccess(exercises);
        doNothing().when(workoutRepository).markExerciseCompleteAndSyncWorkout(
                anyString(), anyString(), anyString(), anyBoolean(), any());

        createViewModel();
        viewModel.toggleExercise("e1", true);

        WorkoutDetailUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertTrue(state.getExercises().get(0).isCompleted());
        assertEquals(50, state.getProgressPercent());
    }

    @Test
    public void toggleExercise_failure_revertsState() {
        List<Exercise> exercises = Collections.singletonList(
                new Exercise("e1", "Push up", 3, 12, null, false));
        stubLoadSuccess(exercises);
        doAnswer(invocation -> {
            WorkoutRepository.SimpleCallback cb = invocation.getArgument(4);
            cb.onError(new Exception("Write failed"));
            return null;
        }).when(workoutRepository).markExerciseCompleteAndSyncWorkout(
                anyString(), anyString(), anyString(), anyBoolean(), any());

        createViewModel();
        viewModel.toggleExercise("e1", true);

        WorkoutDetailUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertFalse(state.getExercises().get(0).isCompleted());
        assertEquals(0, state.getProgressPercent());
    }

    @Test
    public void toggleExercise_updatesProgressPercent() {
        List<Exercise> exercises = Arrays.asList(
                new Exercise("e1", "A", 3, 12, null, false),
                new Exercise("e2", "B", 3, 10, null, false),
                new Exercise("e3", "C", 3, 8, null, false));
        stubLoadSuccess(exercises);
        doNothing().when(workoutRepository).markExerciseCompleteAndSyncWorkout(
                anyString(), anyString(), anyString(), anyBoolean(), any());

        createViewModel();
        assertEquals(0, viewModel.getUiState().getValue().getProgressPercent());

        viewModel.toggleExercise("e1", true);
        assertEquals(33, viewModel.getUiState().getValue().getProgressPercent());

        viewModel.toggleExercise("e2", true);
        assertEquals(66, viewModel.getUiState().getValue().getProgressPercent());

        viewModel.toggleExercise("e3", true);
        assertEquals(100, viewModel.getUiState().getValue().getProgressPercent());
    }

    @Test
    public void toggleExercise_successCompletesWorkout_recordsChallengeProgress() {
        List<Exercise> exercises = Arrays.asList(
                new Exercise("e1", "A", 3, 12, null, false),
                new Exercise("e2", "B", 3, 10, null, true));
        stubLoadSuccess(exercises);
        doAnswer(invocation -> {
            WorkoutRepository.SimpleCallback cb = invocation.getArgument(4);
            cb.onSuccess();
            return null;
        }).when(workoutRepository).markExerciseCompleteAndSyncWorkout(
                anyString(), anyString(), anyString(), anyBoolean(), any());

        createViewModel();
        viewModel.toggleExercise("e1", true);

        verify(challengeRepository).recordWorkoutCompletion(eq("uid-1"), any());
    }

    @Test
    public void toggleExercise_successDoesNotCompleteWorkout_skipsChallengeProgress() {
        List<Exercise> exercises = Arrays.asList(
                new Exercise("e1", "A", 3, 12, null, false),
                new Exercise("e2", "B", 3, 10, null, false));
        stubLoadSuccess(exercises);
        doAnswer(invocation -> {
            WorkoutRepository.SimpleCallback cb = invocation.getArgument(4);
            cb.onSuccess();
            return null;
        }).when(workoutRepository).markExerciseCompleteAndSyncWorkout(
                anyString(), anyString(), anyString(), anyBoolean(), any());

        createViewModel();
        viewModel.toggleExercise("e1", true);

        verify(challengeRepository, never()).recordWorkoutCompletion(anyString(), any());
    }

    @Test
    public void retry_afterError_reloadsExercises() {
        List<Exercise> exercises = Collections.singletonList(
                new Exercise("e1", "Push up", 3, 12, null, false));
        AtomicInteger calls = new AtomicInteger(0);
        doAnswer(invocation -> {
            WorkoutRepository.ExerciseListCallback cb = invocation.getArgument(2);
            if (calls.getAndIncrement() == 0) {
                cb.onError(new Exception("fail"));
            } else {
                cb.onSuccess(exercises);
            }
            return null;
        }).when(workoutRepository).getExercises(eq("uid-1"), eq("workout-1"), any());

        createViewModel();
        assertNotNull(viewModel.getUiState().getValue().getErrorMessage());

        viewModel.retry();

        WorkoutDetailUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertNull(state.getErrorMessage());
        assertEquals(1, state.getExercises().size());
        assertEquals(2, calls.get());
    }

    @Test
    public void nullUser_doesNotCrash() {
        when(authRepository.getCurrentUser()).thenReturn(null);

        createViewModel();

        verify(workoutRepository, never()).getExercises(anyString(), anyString(), any());
    }

    private void createViewModel() {
        viewModel = new WorkoutViewModel(workoutRepository, challengeRepository, authRepository, savedState);
    }

    private void stubLoadSuccess(List<Exercise> exercises) {
        doAnswer(invocation -> {
            WorkoutRepository.ExerciseListCallback cb = invocation.getArgument(2);
            cb.onSuccess(exercises);
            return null;
        }).when(workoutRepository).getExercises(eq("uid-1"), eq("workout-1"), any());
    }
}
