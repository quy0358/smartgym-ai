package ntu.quy65132908.smartgym_ai.ui.wellness;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

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

import java.util.Collections;

import ntu.quy65132908.smartgym_ai.data.model.InjuryProfile;
import ntu.quy65132908.smartgym_ai.data.model.Reminder;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ChallengeRepository;
import ntu.quy65132908.smartgym_ai.data.repository.InjuryProfileRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ReminderRepository;
import ntu.quy65132908.smartgym_ai.util.ReminderScheduler;

@RunWith(RobolectricTestRunner.class)
public class WellnessViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private AuthRepository authRepository;
    @Mock private ReminderRepository reminderRepository;
    @Mock private InjuryProfileRepository injuryProfileRepository;
    @Mock private ChallengeRepository challengeRepository;
    @Mock private ReminderScheduler reminderScheduler;
    @Mock private FirebaseUser firebaseUser;

    private Context context;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        when(firebaseUser.getUid()).thenReturn("uid-1");
        when(authRepository.getCurrentUser()).thenReturn(firebaseUser);
        stubInitialLoads();
    }

    @Test
    public void init_withoutCurrentUser_exposesLoggedOutDefaultState() {
        when(authRepository.getCurrentUser()).thenReturn(null);

        WellnessViewModel viewModel = createViewModel();

        WellnessUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertTrue(state.isLoggedOut());
        assertNotNull(state.getReminder());
    }

    @Test
    public void saveReminder_invalidInputs_setsReminderErrorsAndSkipsRepository() {
        WellnessViewModel viewModel = createViewModel();

        viewModel.saveReminder("", "25:00", true, Collections.emptyList());

        WellnessFormErrors errors = viewModel.getFormErrors().getValue();
        assertNotNull(errors);
        assertTrue(errors.hasReminderErrors());
        assertNotNull(errors.getReminderTitleError());
        assertNotNull(errors.getReminderTimeError());
        assertNotNull(errors.getReminderDaysError());
        verify(reminderRepository, never()).upsertReminder(any(), any(), any());
    }

    @Test
    public void saveReminder_disabledAllowsNoDaysAndPersistsDisabledReminder() {
        doAnswer(invocation -> {
            ReminderRepository.SimpleCallback cb = invocation.getArgument(2);
            cb.onSuccess();
            return null;
        }).when(reminderRepository).upsertReminder(eq("uid-1"), any(), any());
        WellnessViewModel viewModel = createViewModel();

        viewModel.saveReminder("Tập chiều", "18:05", false, Collections.emptyList());

        ArgumentCaptor<Reminder> reminderCaptor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository).upsertReminder(eq("uid-1"), reminderCaptor.capture(), any());
        Reminder reminder = reminderCaptor.getValue();
        assertEquals("Tập chiều", reminder.getTitle());
        assertEquals(18, reminder.getHour());
        assertEquals(5, reminder.getMinute());
        assertTrue(!reminder.isEnabled());
        assertTrue(reminder.getDaysOfWeek().isEmpty());
    }

    @Test
    public void saveInjuryProfile_tooLongNotes_setsNotesErrorAndSkipsRepository() {
        WellnessViewModel viewModel = createViewModel();
        StringBuilder notes = new StringBuilder();
        for (int i = 0; i < 501; i++) {
            notes.append('a');
        }

        viewModel.saveInjuryProfile(true, false, false, notes.toString());

        WellnessFormErrors errors = viewModel.getFormErrors().getValue();
        assertNotNull(errors);
        assertTrue(errors.hasInjuryErrors());
        verify(injuryProfileRepository, never()).saveInjuryProfile(any(), any(), any());
    }

    @Test
    public void saveInjuryProfile_validNotesOnly_trimsPersistsAndEmitsSavedEvent() {
        doAnswer(invocation -> {
            InjuryProfileRepository.SimpleCallback cb = invocation.getArgument(2);
            cb.onSuccess();
            return null;
        }).when(injuryProfileRepository).saveInjuryProfile(eq("uid-1"), any(), any());
        WellnessViewModel viewModel = createViewModel();

        viewModel.saveInjuryProfile(false, false, false, "  dau lung khi gap nguoi  ");

        ArgumentCaptor<InjuryProfile> profileCaptor = ArgumentCaptor.forClass(InjuryProfile.class);
        verify(injuryProfileRepository).saveInjuryProfile(eq("uid-1"), profileCaptor.capture(), any());
        InjuryProfile profile = profileCaptor.getValue();
        assertTrue(!profile.isKneeSensitive());
        assertTrue(!profile.isShoulderSensitive());
        assertTrue(!profile.isLowerBackSensitive());
        assertEquals("dau lung khi gap nguoi", profile.getNotes());
        assertEquals(Boolean.TRUE, viewModel.getInjuryProfileSavedEvent().getValue());
        WellnessUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertTrue(!state.isSavingInjury());
    }

    private WellnessViewModel createViewModel() {
        return new WellnessViewModel(
                context,
                authRepository,
                reminderRepository,
                injuryProfileRepository,
                challengeRepository,
                reminderScheduler
        );
    }

    private void stubInitialLoads() {
        doAnswer(invocation -> {
            ReminderRepository.ReminderCallback cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(reminderRepository).getReminder(eq("uid-1"), eq("training"), any());
        doAnswer(invocation -> {
            InjuryProfileRepository.InjuryProfileCallback cb = invocation.getArgument(1);
            cb.onSuccess(new InjuryProfile());
            return null;
        }).when(injuryProfileRepository).getInjuryProfile(eq("uid-1"), any());
        doAnswer(invocation -> {
            ChallengeRepository.ChallengeProgressCallback cb = invocation.getArgument(1);
            cb.onSuccess(Collections.emptyList());
            return null;
        }).when(challengeRepository).getChallengeProgressList(eq("uid-1"), any());
    }
}
