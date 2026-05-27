package ntu.quy65132908.smartgym_ai.ui.progress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import ntu.quy65132908.smartgym_ai.data.model.ProgressEntry;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.model.WorkoutSession;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ProgressRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;

@RunWith(RobolectricTestRunner.class)
public class ProgressViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private ProgressRepository progressRepository;
    @Mock private WorkoutRepository workoutRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthRepository authRepository;
    @Mock private FirebaseUser firebaseUser;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(firebaseUser.getUid()).thenReturn("uid-1");
        when(authRepository.getCurrentUser()).thenReturn(firebaseUser);
        stubInitialLoads();
    }

    @Test
    public void init_withoutCurrentUser_exposesLoggedOutState() {
        when(authRepository.getCurrentUser()).thenReturn(null);

        ProgressViewModel viewModel = createViewModel();

        ProgressUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertTrue(state.isLoggedOut());
        assertTrue(state.getEntries().isEmpty());
        assertEquals("Bạn cần đăng nhập để lưu tiến trình.", viewModel.getMessage().getValue());
    }

    @Test
    public void addProgressEntry_invalidWeight_setsFieldErrorAndSkipsRepository() {
        ProgressViewModel viewModel = createViewModel();

        viewModel.addProgressEntry("10", "", "", "");

        ProgressFormErrors errors = viewModel.getFormErrors().getValue();
        assertNotNull(errors);
        assertEquals("Cân nặng phải từ 20 đến 350 kg.", errors.getWeightError());
        verify(progressRepository, never()).addEntry(any(), any(), any());
    }

    @Test
    public void addProgressEntry_leanMassOverWeight_setsLeanMassError() {
        ProgressViewModel viewModel = createViewModel();

        viewModel.addProgressEntry("70", "", "72", "");

        ProgressFormErrors errors = viewModel.getFormErrors().getValue();
        assertNotNull(errors);
        assertEquals("Lean mass không được lớn hơn cân nặng.", errors.getLeanMassError());
        verify(progressRepository, never()).addEntry(any(), any(), any());
    }

    @Test
    public void addProgressEntry_tooLongNote_setsNoteError() {
        ProgressViewModel viewModel = createViewModel();
        StringBuilder note = new StringBuilder();
        for (int i = 0; i < ProgressViewModel.NOTE_MAX_LENGTH + 1; i++) {
            note.append('a');
        }

        viewModel.addProgressEntry("70", "", "", note.toString());

        ProgressFormErrors errors = viewModel.getFormErrors().getValue();
        assertNotNull(errors);
        assertEquals("Ghi chú tối đa 500 ký tự.", errors.getNoteError());
        verify(progressRepository, never()).addEntry(any(), any(), any());
    }

    @Test
    public void addProgressEntry_decimalComma_callsRepositoryWithParsedValues() {
        ProgressViewModel viewModel = createViewModel();
        doAnswer(invocation -> {
            ProgressRepository.SimpleCallback cb = invocation.getArgument(2);
            cb.onSuccess();
            return null;
        }).when(progressRepository).addEntry(eq("uid-1"), any(), any());

        viewModel.addProgressEntry("72,5", "18,2", "58,1", "Tập tốt");

        ArgumentCaptor<ProgressEntry> entryCaptor = ArgumentCaptor.forClass(ProgressEntry.class);
        verify(progressRepository).addEntry(eq("uid-1"), entryCaptor.capture(), any());
        ProgressEntry saved = entryCaptor.getValue();
        assertEquals(72.5f, saved.getWeight(), 0.001f);
        assertEquals(18.2f, saved.getBodyFat(), 0.001f);
        assertEquals(58.1f, saved.getLeanMass(), 0.001f);
        assertEquals("Đã lưu tiến trình.", viewModel.getMessage().getValue());
        assertTrue(Boolean.TRUE.equals(viewModel.getClearProgressFormEvent().getValue()));
        assertFalse(Boolean.TRUE.equals(viewModel.getIsLoading().getValue()));
    }

    @Test
    public void addProgressEntry_successUpdatesWeightAndChartImmediately() {
        AtomicReference<ProgressRepository.ProgressCallback> reloadCallback = new AtomicReference<>();
        doAnswer(invocation -> {
            reloadCallback.set(invocation.getArgument(1));
            return null;
        }).when(progressRepository).getHistory(eq("uid-1"), any());
        ProgressViewModel viewModel = createViewModel();
        doAnswer(invocation -> {
            ProgressRepository.SimpleCallback cb = invocation.getArgument(2);
            cb.onSuccess();
            return null;
        }).when(progressRepository).addEntry(eq("uid-1"), any(), any());

        viewModel.addProgressEntry("72", "", "", "");

        assertEquals(72f, viewModel.getCurrentWeight().getValue(), 0.001f);
        assertTrue(Boolean.TRUE.equals(viewModel.getHasWeightData().getValue()));
        assertEquals(1, viewModel.getEntries().getValue().size());
        assertEquals(72f, viewModel.getUiState().getValue().getCurrentWeight(), 0.001f);
    }

    @Test
    public void onProgressFormChanged_afterFailedSubmit_clearsFixedFieldError() {
        ProgressViewModel viewModel = createViewModel();

        viewModel.addProgressEntry("10", "", "", "");
        viewModel.onProgressFormChanged("70", "", "", "");

        assertNull(viewModel.getFormErrors().getValue().getWeightError());
    }

    @Test
    public void loadProgress_sortsEntriesAndUsesLatestValues() {
        ProgressEntry oldest = entry(65f, 1_000L);
        ProgressEntry latest = entry(67f, 3_000L);
        ProgressEntry previous = entry(66f, 2_000L);
        stubProgressHistory(Arrays.asList(oldest, latest, previous));

        ProgressViewModel viewModel = createViewModel();

        List<ProgressEntry> sorted = viewModel.getEntries().getValue();
        assertEquals(3_000L, sorted.get(0).getDate());
        assertEquals(67f, viewModel.getCurrentWeight().getValue(), 0.001f);
        assertEquals(1f, viewModel.getWeightChange().getValue(), 0.001f);
    }

    @Test
    public void calculateWeightChange_comparesLatestWithPreviousEntry() {
        ProgressViewModel viewModel = createViewModel();
        ProgressEntry latest = entry(70f, 3_000L);
        ProgressEntry previous = entry(75f, 2_000L);
        ProgressEntry oldest = entry(80f, 1_000L);

        assertEquals(-5f, viewModel.calculateWeightChange(Arrays.asList(oldest, latest, previous)), 0.001f);
    }

    @Test
    public void calculateStreak_ignoresDuplicateSameDayEntries() {
        ProgressViewModel viewModel = createViewModel();
        long day = 24 * 60 * 60 * 1000L;
        ProgressEntry todayMorning = entry(70f, day * 3 + 1000L);
        ProgressEntry todayEvening = entry(71f, day * 3 + 2000L);
        ProgressEntry yesterday = entry(72f, day * 2 + 1000L);
        ProgressEntry twoDaysAgo = entry(73f, day + 1000L);

        assertEquals(3, viewModel.calculateStreak(Arrays.asList(twoDaysAgo, todayEvening, yesterday, todayMorning)));
    }

    @Test
    public void loadProgress_keepsLoadingUntilAllInitialCallbacksComplete() {
        AtomicReference<ProgressRepository.ProgressCallback> progressCallback = new AtomicReference<>();
        AtomicReference<WorkoutRepository.WorkoutSessionListCallback> sessionCallback = new AtomicReference<>();
        AtomicReference<UserRepository.UserCallback> userCallback = new AtomicReference<>();
        doAnswer(invocation -> {
            progressCallback.set(invocation.getArgument(1));
            return null;
        }).when(progressRepository).getHistory(eq("uid-1"), any());
        doAnswer(invocation -> {
            sessionCallback.set(invocation.getArgument(1));
            return null;
        }).when(workoutRepository).getWorkoutSessions(eq("uid-1"), any());
        doAnswer(invocation -> {
            userCallback.set(invocation.getArgument(1));
            return null;
        }).when(userRepository).getUser(eq("uid-1"), any());

        ProgressViewModel viewModel = createViewModel();

        assertTrue(Boolean.TRUE.equals(viewModel.getIsLoading().getValue()));
        progressCallback.get().onSuccess(Collections.emptyList());
        assertTrue(Boolean.TRUE.equals(viewModel.getIsLoading().getValue()));
        sessionCallback.get().onSuccess(Collections.emptyList());
        assertTrue(Boolean.TRUE.equals(viewModel.getIsLoading().getValue()));
        userCallback.get().onSuccess(userWithWeight(72f));
        assertFalse(Boolean.TRUE.equals(viewModel.getIsLoading().getValue()));
    }

    @Test
    public void loadWorkoutStats_usesSessionsAndLatestProgressWeightForCalories() {
        ProgressEntry current = entry(80f, 2_000L);
        stubProgressHistory(Collections.singletonList(current));
        stubWorkoutSessions(Collections.singletonList(session("w1", 30, "high")));

        ProgressViewModel viewModel = createViewModel();

        assertEquals(1, viewModel.getCompletedWorkouts().getValue().intValue());
        assertEquals(336, viewModel.getTotalCalories().getValue().intValue());
    }

    @Test
    public void loadWorkoutStats_usesProfileWeightWhenNoProgressWeight() {
        stubUserProfileWeight(80f);
        stubWorkoutSessions(Collections.singletonList(session("w1", 30, "high")));

        ProgressViewModel viewModel = createViewModel();

        assertEquals(1, viewModel.getCompletedWorkouts().getValue().intValue());
        assertEquals(336, viewModel.getTotalCalories().getValue().intValue());
    }

    @Test
    public void loadWorkoutStats_usesDefaultWeightWhenNoProgressOrProfileWeight() {
        stubUserProfileWeight(null);
        stubWorkoutSessions(Collections.singletonList(session("w1", 30, "high")));

        ProgressViewModel viewModel = createViewModel();

        assertEquals(1, viewModel.getCompletedWorkouts().getValue().intValue());
        assertEquals(294, viewModel.getTotalCalories().getValue().intValue());
    }

    @Test
    public void loadWorkoutStats_countsSessionsEvenWhenWeeklyPlanIsEmpty() {
        stubWorkoutSessions(Arrays.asList(
                session("old-workout-1", 30, "high"),
                session("old-workout-2", 20, "low")));
        doAnswer(invocation -> {
            WorkoutRepository.WorkoutListCallback cb = invocation.getArgument(1);
            cb.onSuccess(Collections.emptyList());
            return null;
        }).when(workoutRepository).getWeeklyPlan(eq("uid-1"), any());

        ProgressViewModel viewModel = createViewModel();

        assertEquals(2, viewModel.getCompletedWorkouts().getValue().intValue());
    }

    @Test
    public void loadWorkoutStats_recalculatesSessionCaloriesWhenWeightArrivesLater() {
        AtomicReference<ProgressRepository.ProgressCallback> progressCallback = new AtomicReference<>();
        doAnswer(invocation -> {
            progressCallback.set(invocation.getArgument(1));
            return null;
        }).when(progressRepository).getHistory(eq("uid-1"), any());
        stubUserProfileWeight(null);
        stubWorkoutSessions(Collections.singletonList(session("w1", 30, "high")));

        ProgressViewModel viewModel = createViewModel();
        assertEquals(294, viewModel.getTotalCalories().getValue().intValue());

        ProgressEntry current = entry(80f, 2_000L);
        progressCallback.get().onSuccess(Collections.singletonList(current));

        assertEquals(336, viewModel.getTotalCalories().getValue().intValue());
    }

    @Test
    public void bodyPhotoControls_areRemovedFromProgressLayout() {
        assertEquals(0, RuntimeEnvironment.getApplication().getResources().getIdentifier(
                "btn_before_photo",
                "id",
                RuntimeEnvironment.getApplication().getPackageName()));
        assertEquals(0, RuntimeEnvironment.getApplication().getResources().getIdentifier(
                "btn_after_photo",
                "id",
                RuntimeEnvironment.getApplication().getPackageName()));
        assertEquals(0, RuntimeEnvironment.getApplication().getResources().getIdentifier(
                "progress_body_photos_title",
                "string",
                RuntimeEnvironment.getApplication().getPackageName()));
    }

    @Test
    public void progressViewModel_hasNoBodyPhotoUploadEntryPoints() {
        assertNoPublicMethod("uploadBeforePhoto");
        assertNoPublicMethod("uploadAfterPhoto");
    }

    private ProgressViewModel createViewModel() {
        return new ProgressViewModel(
                RuntimeEnvironment.getApplication(),
                progressRepository,
                workoutRepository,
                userRepository,
                authRepository
        );
    }

    private void stubInitialLoads() {
        stubProgressHistory(Collections.emptyList());
        stubUserProfileWeight(null);
        stubWorkoutSessions(Collections.emptyList());
    }

    private void stubWorkoutSessions(List<WorkoutSession> sessions) {
        doAnswer(invocation -> {
            WorkoutRepository.WorkoutSessionListCallback cb = invocation.getArgument(1);
            cb.onSuccess(sessions);
            return null;
        }).when(workoutRepository).getWorkoutSessions(eq("uid-1"), any());
    }

    private void stubUserProfileWeight(Float weight) {
        doAnswer(invocation -> {
            UserRepository.UserCallback cb = invocation.getArgument(1);
            cb.onSuccess(userWithWeight(weight));
            return null;
        }).when(userRepository).getUser(eq("uid-1"), any());
    }

    private void stubProgressHistory(List<ProgressEntry> entries) {
        doAnswer(invocation -> {
            ProgressRepository.ProgressCallback cb = invocation.getArgument(1);
            cb.onSuccess(entries);
            return null;
        }).when(progressRepository).getHistory(eq("uid-1"), any());
    }

    private ProgressEntry entry(float weight, long date) {
        ProgressEntry entry = new ProgressEntry();
        entry.setWeight(weight);
        entry.setDate(date);
        return entry;
    }

    private WorkoutSession session(String workoutId, int durationMinutes, String intensity) {
        WorkoutSession session = new WorkoutSession();
        session.setId(workoutId);
        session.setWorkoutId(workoutId);
        session.setCompletedAt(1_000L);
        session.setDurationMinutes(durationMinutes);
        session.setIntensity(intensity);
        session.setSource(WorkoutSession.SOURCE_MANUAL);
        return session;
    }

    private User userWithWeight(Float weight) {
        User user = new User();
        user.setUid("uid-1");
        user.setDisplayName("User");
        user.setEmail("user@example.com");
        user.setWeight(weight);
        return user;
    }

    private void assertNoPublicMethod(String methodName) {
        for (java.lang.reflect.Method method : ProgressViewModel.class.getMethods()) {
            if (method.getName().equals(methodName)) {
                throw new AssertionError(methodName + " should be removed with the body photo feature");
            }
        }
    }
}
