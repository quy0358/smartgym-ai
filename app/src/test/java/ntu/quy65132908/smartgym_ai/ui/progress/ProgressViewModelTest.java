package ntu.quy65132908.smartgym_ai.ui.progress;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

import ntu.quy65132908.smartgym_ai.data.model.ProgressEntry;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ProgressRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;

public class ProgressViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private ProgressRepository progressRepository;
    @Mock private WorkoutRepository workoutRepository;
    @Mock private AuthRepository authRepository;
    @Mock private UserRepository userRepository;
    @Mock private FirebaseStorage storage;
    @Mock private FirebaseUser firebaseUser;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(firebaseUser.getUid()).thenReturn("uid-1");
        when(authRepository.getCurrentUser()).thenReturn(firebaseUser);
    }

    @Test
    public void addProgressEntry_invalidWeight_setsError() {
        ProgressViewModel viewModel = new ProgressViewModel(
                progressRepository, workoutRepository, authRepository, userRepository, storage);

        viewModel.addProgressEntry("10", "", "", "");

        assertEquals("Cân nặng phải từ 20 đến 350 kg", viewModel.getErrorMessage().getValue());
    }

    @Test
    public void addProgressEntry_validInput_callsRepository() {
        ProgressViewModel viewModel = new ProgressViewModel(
                progressRepository, workoutRepository, authRepository, userRepository, storage);

        viewModel.addProgressEntry("72.5", "18", "57", "Tập tốt");

        verify(progressRepository).addEntry(eq("uid-1"), any(ProgressEntry.class), any());
    }

    @Test
    public void calculateWeightChange_latestMinusOldest() {
        ProgressViewModel viewModel = new ProgressViewModel(
                progressRepository, workoutRepository, authRepository, userRepository, storage);
        ProgressEntry latest = new ProgressEntry();
        latest.setWeight(70f);
        ProgressEntry oldest = new ProgressEntry();
        oldest.setWeight(75f);

        assertEquals(-5f, viewModel.calculateWeightChange(Arrays.asList(latest, oldest)), 0.001f);
    }

    @Test
    public void calculateStreak_consecutiveDays_countsEntries() {
        ProgressViewModel viewModel = new ProgressViewModel(
                progressRepository, workoutRepository, authRepository, userRepository, storage);
        long day = 24 * 60 * 60 * 1000L;
        ProgressEntry today = new ProgressEntry();
        today.setDate(day * 3);
        ProgressEntry yesterday = new ProgressEntry();
        yesterday.setDate(day * 2);
        ProgressEntry twoDaysAgo = new ProgressEntry();
        twoDaysAgo.setDate(day);

        assertEquals(3, viewModel.calculateStreak(Arrays.asList(today, yesterday, twoDaysAgo)));
    }
}
