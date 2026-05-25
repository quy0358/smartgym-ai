package ntu.quy65132908.smartgym_ai.ui.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.Collections;

import ntu.quy65132908.smartgym_ai.data.model.ProgressEntry;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ProgressRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;

@RunWith(RobolectricTestRunner.class)
public class ProfileViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private AuthRepository authRepository;
    @Mock private UserRepository userRepository;
    @Mock private WorkoutRepository workoutRepository;
    @Mock private ProgressRepository progressRepository;
    @Mock private FirebaseUser firebaseUser;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(firebaseUser.getUid()).thenReturn("uid-1");
        when(firebaseUser.getEmail()).thenReturn("test@email.com");
        when(authRepository.getCurrentUser()).thenReturn(firebaseUser);
        stubProfile();
        stubWorkouts(Collections.emptyList());
        stubProgress(Collections.emptyList());
    }

    @Test
    public void loadWorkoutStats_keepsFractionalHours() {
        stubWorkouts(Arrays.asList(completedWorkout(45), completedWorkout(30)));

        ProfileViewModel viewModel = createViewModel();

        ProfileUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertEquals(2, state.getTotalWorkouts());
        assertEquals(1.25f, state.getTotalHours(), 0.001f);
        assertEquals("1.3", viewModel.formatHours(state.getTotalHours()));
    }

    @Test
    public void calculateStreak_sortsAndIgnoresDuplicateSameDayEntries() {
        ProfileViewModel viewModel = createViewModel();
        long day = 24 * 60 * 60 * 1000L;

        assertEquals(3, viewModel.calculateStreak(Arrays.asList(
                entry(day * 2 + 1_000L),
                entry(day * 4 + 4_000L),
                entry(day * 3 + 1_000L),
                entry(day * 4 + 1_000L)
        )));
    }

    @Test
    public void noCurrentUser_exposesLoggedOutState() {
        when(authRepository.getCurrentUser()).thenReturn(null);

        ProfileViewModel viewModel = createViewModel();

        ProfileUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertTrue(state.isLoggedOut());
        assertFalse(state.isLoading());
    }

    @Test
    public void profileLoadError_usesFirebaseFallbackAndFlagsError() {
        doAnswer(invocation -> {
            UserRepository.UserCallback cb = invocation.getArgument(1);
            cb.onError(new Exception("network"));
            return null;
        }).when(userRepository).getUser(eq("uid-1"), any());

        ProfileViewModel viewModel = createViewModel();

        ProfileUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertEquals("test@email.com", state.getDisplayName());
        assertTrue(state.isProfileLoadFailed());
    }

    private ProfileViewModel createViewModel() {
        return new ProfileViewModel(
                RuntimeEnvironment.getApplication(),
                authRepository,
                userRepository,
                workoutRepository,
                progressRepository
        );
    }

    private void stubProfile() {
        doAnswer(invocation -> {
            UserRepository.UserCallback cb = invocation.getArgument(1);
            cb.onSuccess(new User("uid-1", "Quy", "test@email.com"));
            return null;
        }).when(userRepository).getUser(eq("uid-1"), any());
    }

    private void stubWorkouts(java.util.List<Workout> workouts) {
        doAnswer(invocation -> {
            WorkoutRepository.WorkoutListCallback cb = invocation.getArgument(1);
            cb.onSuccess(workouts);
            return null;
        }).when(workoutRepository).getWeeklyPlan(eq("uid-1"), any());
    }

    private void stubProgress(java.util.List<ProgressEntry> entries) {
        doAnswer(invocation -> {
            ProgressRepository.ProgressCallback cb = invocation.getArgument(1);
            cb.onSuccess(entries);
            return null;
        }).when(progressRepository).getHistory(eq("uid-1"), any());
    }

    private Workout completedWorkout(int minutes) {
        Workout workout = new Workout();
        workout.setCompleted(true);
        workout.setDurationMinutes(minutes);
        return workout;
    }

    private ProgressEntry entry(long date) {
        ProgressEntry entry = new ProgressEntry();
        entry.setWeight(70f);
        entry.setDate(date);
        return entry;
    }
}
