package ntu.quy65132908.smartgym_ai.ui.dashboard;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;
import ntu.quy65132908.smartgym_ai.util.DateUtils;

public class DashboardViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private AuthRepository authRepository;
    @Mock private UserRepository userRepository;
    @Mock private WorkoutRepository workoutRepository;
    @Mock private FirebaseUser firebaseUser;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(firebaseUser.getUid()).thenReturn("test-uid");
        when(authRepository.getCurrentUser()).thenReturn(firebaseUser);
    }

    private DashboardViewModel createViewModelWithUser(User user, List<Workout> workouts) {
        doAnswer(invocation -> {
            UserRepository.UserCallback cb = invocation.getArgument(1);
            cb.onSuccess(user);
            return null;
        }).when(userRepository).getUser(eq("test-uid"), any());

        doAnswer(invocation -> {
            WorkoutRepository.WorkoutListCallback cb = invocation.getArgument(1);
            cb.onSuccess(workouts);
            return null;
        }).when(workoutRepository).getWeeklyPlan(eq("test-uid"), any());

        return new DashboardViewModel(authRepository, userRepository, workoutRepository);
    }

    private DashboardUiState state(DashboardViewModel viewModel) {
        DashboardUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        return state;
    }

    private Workout workout(String id, String title, int dayOfWeek, int durationMinutes) {
        Workout workout = new Workout(id, title, "", "Medium", durationMinutes);
        workout.setDayOfWeek(dayOfWeek);
        return workout;
    }

    @Test
    public void uiState_profileValuesExposeUserData() {
        User user = new User("test-uid", "Nguyen", "test@email.com");
        user.setWeight(72.5f);
        user.setBmi(22.5f);
        user.setBmiCategory("Bình thường");
        user.setGoal("Giảm cân về 60kg");

        DashboardViewModel viewModel = createViewModelWithUser(user, Collections.emptyList());
        DashboardUiState state = state(viewModel);

        assertEquals("Nguyen", state.getUserName());
        assertEquals(Integer.valueOf(72), state.getWeight());
        assertEquals(Float.valueOf(22.5f), state.getBmi());
        assertEquals("Bình thường", state.getBmiCategory());
        assertEquals("60", state.getGoalDisplay());
        assertEquals(R.color.primary, state.getBmiColorRes());
        assertFalse(state.isInitialLoading());
    }

    @Test
    public void uiState_missingProfileValuesUsePlaceholders() {
        User user = new User("test-uid", null, "test@email.com");
        user.setWeight(null);
        user.setBmi(null);
        user.setGoal(null);

        DashboardViewModel viewModel = createViewModelWithUser(user, Collections.emptyList());
        DashboardUiState state = state(viewModel);

        assertEquals("Bạn", state.getUserName());
        assertNull(state.getWeight());
        assertNull(state.getBmi());
        assertEquals("--", state.getGoalDisplay());
        assertEquals(R.color.on_surface_variant, state.getBmiColorRes());
    }

    @Test
    public void weeklyPlan_exposedFromRepository() {
        User user = new User("test-uid", "Test", "test@email.com");
        Workout w1 = workout("1", "Upper Body", 1, 45);
        Workout w2 = workout("2", "Lower Body", 2, 40);

        DashboardViewModel viewModel = createViewModelWithUser(user, Arrays.asList(w1, w2));

        List<Workout> plan = state(viewModel).getWeeklyPlan();
        assertEquals(2, plan.size());
        assertEquals("Upper Body", plan.get(0).getTitle());
    }

    @Test
    public void noCurrentUser_stopsInitialLoadingAndRequiresLogin() {
        when(authRepository.getCurrentUser()).thenReturn(null);

        DashboardViewModel viewModel = new DashboardViewModel(authRepository, userRepository, workoutRepository);

        assertFalse(state(viewModel).isInitialLoading());
        assertEquals(Boolean.TRUE, viewModel.getRequireLoginEvent().getValue());
    }

    @Test
    public void profileLoadError_fallsBackButStillLoadsWeeklyPlan() {
        Workout today = workout("today", "Full Body", DateUtils.getTodayDayOfWeek(), 35);
        doAnswer(invocation -> {
            UserRepository.UserCallback cb = invocation.getArgument(1);
            cb.onError(new Exception("profile unavailable"));
            return null;
        }).when(userRepository).getUser(eq("test-uid"), any());
        doAnswer(invocation -> {
            WorkoutRepository.WorkoutListCallback cb = invocation.getArgument(1);
            cb.onSuccess(Collections.singletonList(today));
            return null;
        }).when(workoutRepository).getWeeklyPlan(eq("test-uid"), any());

        DashboardViewModel viewModel = new DashboardViewModel(authRepository, userRepository, workoutRepository);

        assertEquals(DashboardError.PROFILE_LOAD_FAILED, viewModel.getErrorEvent().getValue());
        assertEquals("Bạn", state(viewModel).getUserName());
        assertEquals(1, state(viewModel).getWeeklyPlan().size());
        assertEquals(TodayState.WORKOUT, state(viewModel).getTodayState());
    }

    @Test
    public void refreshWeeklyPlanError_keepsExistingPlanAndMarksStale() {
        User user = new User("test-uid", "Test", "test@email.com");
        Workout existing = workout("existing", "Upper Body", DateUtils.getTodayDayOfWeek(), 45);
        AtomicInteger weeklyCalls = new AtomicInteger();

        doAnswer(invocation -> {
            UserRepository.UserCallback cb = invocation.getArgument(1);
            cb.onSuccess(user);
            return null;
        }).when(userRepository).getUser(eq("test-uid"), any());
        doAnswer(invocation -> {
            WorkoutRepository.WorkoutListCallback cb = invocation.getArgument(1);
            if (weeklyCalls.getAndIncrement() == 0) {
                cb.onSuccess(Collections.singletonList(existing));
            } else {
                cb.onError(new Exception("network"));
            }
            return null;
        }).when(workoutRepository).getWeeklyPlan(eq("test-uid"), any());

        DashboardViewModel viewModel = new DashboardViewModel(authRepository, userRepository, workoutRepository);
        viewModel.refresh();

        DashboardUiState state = state(viewModel);
        assertEquals(1, state.getWeeklyPlan().size());
        assertEquals("Upper Body", state.getWeeklyPlan().get(0).getTitle());
        assertTrue(state.isDataStale());
        assertEquals(DashboardError.WEEKLY_PLAN_LOAD_FAILED, viewModel.getErrorEvent().getValue());
    }

    @Test
    public void staleLoadCallback_doesNotOverwriteNewerRefresh() {
        List<UserRepository.UserCallback> userCallbacks = new ArrayList<>();
        List<WorkoutRepository.WorkoutListCallback> weeklyCallbacks = new ArrayList<>();
        doAnswer(invocation -> {
            userCallbacks.add(invocation.getArgument(1));
            return null;
        }).when(userRepository).getUser(eq("test-uid"), any());
        doAnswer(invocation -> {
            weeklyCallbacks.add(invocation.getArgument(1));
            return null;
        }).when(workoutRepository).getWeeklyPlan(eq("test-uid"), any());

        DashboardViewModel viewModel = new DashboardViewModel(authRepository, userRepository, workoutRepository);
        viewModel.refresh();

        assertEquals(2, userCallbacks.size());
        User freshUser = new User("test-uid", "Fresh", "test@email.com");
        Workout freshWorkout = workout("fresh", "Fresh Plan", DateUtils.getTodayDayOfWeek(), 30);
        userCallbacks.get(1).onSuccess(freshUser);
        weeklyCallbacks.get(0).onSuccess(Collections.singletonList(freshWorkout));

        assertEquals("Fresh", state(viewModel).getUserName());
        assertEquals("Fresh Plan", state(viewModel).getAiRecommendation().getTitle());

        User staleUser = new User("test-uid", "Stale", "test@email.com");
        userCallbacks.get(0).onSuccess(staleUser);

        assertEquals("Fresh", state(viewModel).getUserName());
        assertEquals("Fresh Plan", state(viewModel).getAiRecommendation().getTitle());
    }

    @Test
    public void refreshWithinCooldown_emitsThrottleEvent() {
        DashboardViewModel viewModel = createViewModelWithUser(
                new User("test-uid", "Test", "test@email.com"), Collections.emptyList());

        viewModel.refresh();
        viewModel.refresh();

        assertEquals(Boolean.TRUE, viewModel.getRefreshThrottledEvent().getValue());
    }

    @Test
    public void determineTodayState_variousCases() {
        DashboardViewModel viewModel = createViewModelWithUser(
                new User("test-uid", "Test", "test@email.com"), Collections.emptyList());
        int today = DateUtils.getTodayDayOfWeek();
        int otherDay = today == 7 ? 1 : today + 1;

        assertEquals(TodayState.NO_PLAN, viewModel.determineTodayState(Collections.emptyList()));
        assertEquals(TodayState.REST_DAY,
                viewModel.determineTodayState(Collections.singletonList(workout("other", "Other", otherDay, 30))));
        assertEquals(TodayState.REST_DAY,
                viewModel.determineTodayState(Collections.singletonList(workout("rest", "Rest day", today, 0))));
        assertEquals(TodayState.WORKOUT,
                viewModel.determineTodayState(Collections.singletonList(workout("", "Workout without id", today, 30))));
    }

    @Test
    public void computeAvatarLetter_variousCases() {
        DashboardViewModel viewModel = createViewModelWithUser(
                new User("test-uid", "Test", "test@email.com"), Collections.emptyList());

        assertEquals("U", viewModel.computeAvatarLetter(null));
        assertEquals("U", viewModel.computeAvatarLetter(""));
        assertEquals("U", viewModel.computeAvatarLetter("   "));
        assertEquals("A", viewModel.computeAvatarLetter("abc"));
        assertEquals("N", viewModel.computeAvatarLetter("nguyen"));
    }

    @Test
    public void computeAvatarLetter_usesRootLocaleForUppercase() {
        Locale original = Locale.getDefault();
        Locale.setDefault(new Locale("tr", "TR"));
        try {
            DashboardViewModel viewModel = createViewModelWithUser(
                    new User("test-uid", "Test", "test@email.com"), Collections.emptyList());

            assertEquals("I", viewModel.computeAvatarLetter("ipek"));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    public void parseGoalWeight_variousCases() {
        DashboardViewModel viewModel = createViewModelWithUser(
                new User("test-uid", "Test", "test@email.com"), Collections.emptyList());

        assertEquals(0, viewModel.parseGoalWeight(null));
        assertEquals(0, viewModel.parseGoalWeight(""));
        assertEquals(6, viewModel.parseGoalWeight("+6"));
        assertEquals(-3, viewModel.parseGoalWeight("-3"));
        assertEquals(65, viewModel.parseGoalWeight("65"));
        assertEquals(60, viewModel.parseGoalWeight("Giảm cân về 60kg"));
        assertEquals(75, viewModel.parseGoalWeight("Tăng cân lên 75kg"));
        assertEquals(-5, viewModel.parseGoalWeight("Giảm 5kg"));
        assertEquals(5, viewModel.parseGoalWeight("Tăng 5kg"));
        assertEquals(0, viewModel.parseGoalWeight("no numbers here"));
    }

    @Test
    public void computeBmiColor_boundaries() {
        DashboardViewModel viewModel = createViewModelWithUser(
                new User("test-uid", "Test", "test@email.com"), Collections.emptyList());

        assertEquals(R.color.tertiary, viewModel.computeBmiColor(17.5f));
        assertEquals(R.color.primary, viewModel.computeBmiColor(18.5f));
        assertEquals(R.color.primary, viewModel.computeBmiColor(24.9f));
        assertEquals(R.color.warning, viewModel.computeBmiColor(25.0f));
        assertEquals(R.color.error, viewModel.computeBmiColor(30.0f));
    }

    @Test
    public void formatGoalDisplay_variousCases() {
        DashboardViewModel viewModel = createViewModelWithUser(
                new User("test-uid", "Test", "test@email.com"), Collections.emptyList());

        assertEquals("75", viewModel.formatGoalDisplay("Tăng cân lên 75kg"));
        assertEquals("60", viewModel.formatGoalDisplay("Giảm cân về 60kg"));
        assertEquals("65", viewModel.formatGoalDisplay("65"));
        assertEquals("+6", viewModel.formatGoalDisplay("+6"));
        assertEquals("\u22123", viewModel.formatGoalDisplay("-3"));
        assertEquals("\u22125", viewModel.formatGoalDisplay("Giảm 5kg"));
        assertEquals("+5", viewModel.formatGoalDisplay("Tăng 5kg"));
        assertEquals("--", viewModel.formatGoalDisplay(null));
        assertEquals("--", viewModel.formatGoalDisplay(""));
        assertEquals("--", viewModel.formatGoalDisplay("no numbers here"));
    }
}
