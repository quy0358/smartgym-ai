package ntu.quy65132908.smartgym_ai.ui.dashboard;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;

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

    @Test
    public void avatarLetter_fromDisplayName_takesFirstCharUppercase() {
        User user = new User("test-uid", "Nguyen", "test@email.com");
        DashboardViewModel viewModel = createViewModelWithUser(user, Collections.emptyList());

        assertEquals("N", viewModel.getAvatarLetter().getValue());
    }

    @Test
    public void avatarLetter_nullDisplayName_defaultsToU() {
        User user = new User("test-uid", null, "test@email.com");
        DashboardViewModel viewModel = createViewModelWithUser(user, Collections.emptyList());

        assertEquals("U", viewModel.getAvatarLetter().getValue());
    }

    @Test
    public void avatarLetter_emptyDisplayName_defaultsToU() {
        User user = new User("test-uid", "", "test@email.com");
        DashboardViewModel viewModel = createViewModelWithUser(user, Collections.emptyList());

        assertEquals("U", viewModel.getAvatarLetter().getValue());
    }

    @Test
    public void weight_fromUser_convertsToInteger() {
        User user = new User("test-uid", "Test", "test@email.com");
        user.setWeight(72.5f);
        DashboardViewModel viewModel = createViewModelWithUser(user, Collections.emptyList());

        assertEquals(Integer.valueOf(72), viewModel.getWeight().getValue());
    }

    @Test
    public void weight_nullFromUser_defaultsToZero() {
        User user = new User("test-uid", "Test", "test@email.com");
        user.setWeight(null);
        DashboardViewModel viewModel = createViewModelWithUser(user, Collections.emptyList());

        assertEquals(Integer.valueOf(0), viewModel.getWeight().getValue());
    }

    @Test
    public void bmi_fromUser_exposedCorrectly() {
        User user = new User("test-uid", "Test", "test@email.com");
        user.setBmi(22.5f);
        user.setBmiCategory("Bình thường");
        DashboardViewModel viewModel = createViewModelWithUser(user, Collections.emptyList());

        assertEquals(Float.valueOf(22.5f), viewModel.getBmi().getValue());
        assertEquals("Bình thường", viewModel.getBmiCategory().getValue());
    }

    @Test
    public void goalWeight_parsedFromPureNumber() {
        User user = new User("test-uid", "Test", "test@email.com");
        user.setGoal("65");
        DashboardViewModel viewModel = createViewModelWithUser(user, Collections.emptyList());

        assertEquals(Integer.valueOf(65), viewModel.getGoalWeight().getValue());
    }

    @Test
    public void goalWeight_parsesNumberFromText() {
        User user = new User("test-uid", "Test", "test@email.com");
        user.setGoal("Giảm cân về 60kg");
        DashboardViewModel viewModel = createViewModelWithUser(user, Collections.emptyList());

        assertEquals(Integer.valueOf(60), viewModel.getGoalWeight().getValue());
    }

    @Test
    public void goalWeight_nullGoal_defaultsToZero() {
        User user = new User("test-uid", "Test", "test@email.com");
        user.setGoal(null);
        DashboardViewModel viewModel = createViewModelWithUser(user, Collections.emptyList());

        assertEquals(Integer.valueOf(0), viewModel.getGoalWeight().getValue());
    }

    @Test
    public void weeklyPlan_exposedFromRepository() {
        User user = new User("test-uid", "Test", "test@email.com");
        Workout w1 = new Workout("1", "Upper Body", "Chest focus", "High", 45);
        w1.setDayOfWeek(1);
        Workout w2 = new Workout("2", "Lower Body", "Leg focus", "Medium", 40);
        w2.setDayOfWeek(2);

        DashboardViewModel viewModel = createViewModelWithUser(user, Arrays.asList(w1, w2));

        List<Workout> plan = viewModel.getWeeklyPlan().getValue();
        assertNotNull(plan);
        assertEquals(2, plan.size());
        assertEquals("Upper Body", plan.get(0).getTitle());
    }

    @Test
    public void isLoading_falseAfterDataLoads() {
        User user = new User("test-uid", "Test", "test@email.com");
        DashboardViewModel viewModel = createViewModelWithUser(user, Collections.emptyList());

        assertEquals(Boolean.FALSE, viewModel.getIsLoading().getValue());
    }

    @Test
    public void noCurrentUser_setsLoadingFalse() {
        when(authRepository.getCurrentUser()).thenReturn(null);
        DashboardViewModel viewModel = new DashboardViewModel(authRepository, userRepository, workoutRepository);

        assertEquals(Boolean.FALSE, viewModel.getIsLoading().getValue());
    }

    @Test
    public void userLoadError_setsLoadingFalse() {
        doAnswer(invocation -> {
            UserRepository.UserCallback cb = invocation.getArgument(1);
            cb.onError(new Exception("Network error"));
            return null;
        }).when(userRepository).getUser(eq("test-uid"), any());

        DashboardViewModel viewModel = new DashboardViewModel(authRepository, userRepository, workoutRepository);

        assertEquals(Boolean.FALSE, viewModel.getIsLoading().getValue());
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
    public void parseGoalWeight_variousCases() {
        DashboardViewModel viewModel = createViewModelWithUser(
                new User("test-uid", "Test", "test@email.com"), Collections.emptyList());

        assertEquals(0, viewModel.parseGoalWeight(null));
        assertEquals(0, viewModel.parseGoalWeight(""));
        assertEquals(65, viewModel.parseGoalWeight("65"));
        assertEquals(60, viewModel.parseGoalWeight("Giảm cân về 60kg"));
        assertEquals(75, viewModel.parseGoalWeight("Tăng cân lên 75"));
        assertEquals(0, viewModel.parseGoalWeight("no numbers here"));
    }
}
