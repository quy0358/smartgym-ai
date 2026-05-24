package ntu.quy65132908.smartgym_ai.ui.workout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import ntu.quy65132908.smartgym_ai.data.model.Exercise;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.DeepSeekRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;

public class WorkoutListViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private WorkoutRepository workoutRepository;
    @Mock private AuthRepository authRepository;
    @Mock private UserRepository userRepository;
    @Mock private DeepSeekRepository deepSeekRepository;
    @Mock private FirebaseUser firebaseUser;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(firebaseUser.getUid()).thenReturn("uid-1");
        when(authRepository.getCurrentUser()).thenReturn(firebaseUser);
    }

    @Test
    public void loadWorkouts_currentUser_exposesWeeklyPlan() {
        Workout workout = new Workout("w1", "Upper Body", "Chest", "Medium", 45);
        doAnswer(invocation -> {
            WorkoutRepository.WorkoutListCallback cb = invocation.getArgument(1);
            cb.onSuccess(Collections.singletonList(workout));
            return null;
        }).when(workoutRepository).getWeeklyPlan(eq("uid-1"), any());

        WorkoutListViewModel viewModel = createViewModel();

        List<Workout> workouts = viewModel.getWorkouts().getValue();
        assertNotNull(workouts);
        assertEquals(1, workouts.size());
        assertEquals("Upper Body", workouts.get(0).getTitle());
        assertEquals(Boolean.FALSE, viewModel.getIsLoading().getValue());
    }

    @Test
    public void loadWorkouts_noCurrentUser_setsLoadingFalse() {
        when(authRepository.getCurrentUser()).thenReturn(null);

        WorkoutListViewModel viewModel = createViewModel();

        assertEquals(Boolean.FALSE, viewModel.getIsLoading().getValue());
    }

    @Test
    public void createPlan_currentUserGeneratesSavesAndReloadsWeeklyPlan() {
        User user = new User("uid-1", "Test", "test@example.com");
        user.setGoal("Core strength");
        Workout workout = new Workout("", "Thu 2 - Core", "Safe pace", "Vua", 45);
        workout.setDayOfWeek(1);
        workout.setExercises(Collections.singletonList(
                new Exercise("", "Plank", 3, 30, null, false)
        ));

        doAnswer(invocation -> {
            WorkoutRepository.WorkoutListCallback cb = invocation.getArgument(1);
            cb.onSuccess(Collections.emptyList());
            return null;
        }).when(workoutRepository).getWeeklyPlan(eq("uid-1"), any());
        doAnswer(invocation -> {
            UserRepository.UserCallback cb = invocation.getArgument(1);
            cb.onSuccess(user);
            return null;
        }).when(userRepository).getUser(eq("uid-1"), any());
        doAnswer(invocation -> {
            DeepSeekRepository.WorkoutPlanCallback cb = invocation.getArgument(2);
            cb.onSuccess(Collections.singletonList(workout));
            return null;
        }).when(deepSeekRepository).generateWorkoutPlanData(eq(user), eq("Core strength"), any());
        doAnswer(invocation -> {
            WorkoutRepository.SimpleCallback cb = invocation.getArgument(2);
            cb.onSuccess();
            return null;
        }).when(workoutRepository).saveWeeklyPlan(eq("uid-1"), any(), any());

        WorkoutListViewModel viewModel = createViewModel();
        viewModel.createPlan();

        verify(userRepository).getUser(eq("uid-1"), any());
        verify(deepSeekRepository).generateWorkoutPlanData(eq(user), eq("Core strength"), any());
        verify(workoutRepository).saveWeeklyPlan(eq("uid-1"), any(), any());
        verify(workoutRepository, org.mockito.Mockito.times(2)).getWeeklyPlan(eq("uid-1"), any());
        assertFalse(Boolean.TRUE.equals(viewModel.getIsCreatingPlan().getValue()));
    }

    @Test
    public void createPlan_withoutCurrentUserSkipsAiAndReportsError() {
        when(authRepository.getCurrentUser()).thenReturn(null);
        WorkoutListViewModel viewModel = createViewModel();

        viewModel.createPlan();

        verify(deepSeekRepository, never()).generateWorkoutPlanData(any(), any(), any());
        assertNotNull(viewModel.getErrorMessage().getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.getIsCreatingPlan().getValue()));
    }

    private WorkoutListViewModel createViewModel() {
        return new WorkoutListViewModel(
                workoutRepository,
                authRepository,
                userRepository,
                deepSeekRepository
        );
    }
}
