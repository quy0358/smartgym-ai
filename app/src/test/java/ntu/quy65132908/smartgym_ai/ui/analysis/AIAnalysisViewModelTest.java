package ntu.quy65132908.smartgym_ai.ui.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.firebase.auth.FirebaseUser;

import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import ntu.quy65132908.smartgym_ai.data.model.ProgressEntry;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.DeepSeekRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ProgressRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;

public class AIAnalysisViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void analyzeForm_validRequestClearsPreviousErrorBeforeCallingRepository() {
        DeepSeekRepository deepSeekRepository = mock(DeepSeekRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ProgressRepository progressRepository = mock(ProgressRepository.class);
        AuthRepository authRepository = mock(AuthRepository.class);
        when(authRepository.getCurrentUser()).thenReturn(null);
        AIAnalysisViewModel viewModel = new AIAnalysisViewModel(
                deepSeekRepository,
                userRepository,
                progressRepository,
                authRepository
        );
        viewModel.analyzeForm("", "");
        assertNotNull(viewModel.getErrorMsg().getValue());

        viewModel.analyzeForm("hit dat", "mo ta cach tap dung ky thuat");

        assertNull(viewModel.getErrorMsg().getValue());
        verify(deepSeekRepository).analyzeForm(
                eq("hit dat"),
                eq("mo ta cach tap dung ky thuat"),
                any()
        );
    }

    @Test
    public void bodyMetrics_profileAndLatestProgressLoaded_exposesDynamicValues() {
        DeepSeekRepository deepSeekRepository = mock(DeepSeekRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ProgressRepository progressRepository = mock(ProgressRepository.class);
        AuthRepository authRepository = mock(AuthRepository.class);
        FirebaseUser firebaseUser = mock(FirebaseUser.class);
        when(firebaseUser.getUid()).thenReturn("uid-1");
        when(authRepository.getCurrentUser()).thenReturn(firebaseUser);

        User user = new User("uid-1", "Test", "test@example.com");
        user.setWeight(70f);
        user.setBmi(22.5f);
        user.setBmiCategory("Bình thường");
        user.setGoal("65kg");
        ProgressEntry latest = new ProgressEntry();
        latest.setBodyFat(18f);
        latest.setLeanMass(57.4f);

        doAnswer(invocation -> {
            UserRepository.UserCallback cb = invocation.getArgument(1);
            cb.onSuccess(user);
            return null;
        }).when(userRepository).getUser(eq("uid-1"), any());
        doAnswer(invocation -> {
            ProgressRepository.ProgressCallback cb = invocation.getArgument(1);
            cb.onSuccess(Collections.singletonList(latest));
            return null;
        }).when(progressRepository).getHistory(eq("uid-1"), any());

        AIAnalysisViewModel viewModel = new AIAnalysisViewModel(
                deepSeekRepository,
                userRepository,
                progressRepository,
                authRepository
        );

        AIAnalysisViewModel.BodyMetricsUiState metrics = viewModel.getBodyMetrics().getValue();
        assertNotNull(metrics);
        assertEquals("Bình thường", metrics.getBodyType());
        assertEquals("BMI: 22.5 • Cân nặng: 70kg • Mục tiêu: 65kg", metrics.getSummary());
        assertEquals("18", metrics.getBodyFat());
        assertEquals("57.4", metrics.getLeanMass());
        assertEquals(Boolean.TRUE, viewModel.getCanGeneratePlan().getValue());
    }

    @Test
    public void bodyMetrics_missingProgressUsesPlaceholders() {
        DeepSeekRepository deepSeekRepository = mock(DeepSeekRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ProgressRepository progressRepository = mock(ProgressRepository.class);
        AuthRepository authRepository = mock(AuthRepository.class);
        FirebaseUser firebaseUser = mock(FirebaseUser.class);
        when(firebaseUser.getUid()).thenReturn("uid-1");
        when(authRepository.getCurrentUser()).thenReturn(firebaseUser);

        User user = new User("uid-1", "Test", "test@example.com");
        user.setBmi(null);
        user.setWeight(null);
        user.setGoal(null);

        doAnswer(invocation -> {
            UserRepository.UserCallback cb = invocation.getArgument(1);
            cb.onSuccess(user);
            return null;
        }).when(userRepository).getUser(eq("uid-1"), any());
        doAnswer(invocation -> {
            ProgressRepository.ProgressCallback cb = invocation.getArgument(1);
            cb.onSuccess(Collections.emptyList());
            return null;
        }).when(progressRepository).getHistory(eq("uid-1"), any());

        AIAnalysisViewModel viewModel = new AIAnalysisViewModel(
                deepSeekRepository,
                userRepository,
                progressRepository,
                authRepository
        );

        AIAnalysisViewModel.BodyMetricsUiState metrics = viewModel.getBodyMetrics().getValue();
        assertNotNull(metrics);
        assertEquals("Chưa có BMI", metrics.getBodyType());
        assertEquals("BMI: -- • Cân nặng: -- • Mục tiêu: --", metrics.getSummary());
        assertEquals("--", metrics.getBodyFat());
        assertEquals("--", metrics.getLeanMass());
    }

    @Test
    public void generateWorkoutPlan_withoutLoadedProfileReportsErrorAndSkipsAiCall() {
        DeepSeekRepository deepSeekRepository = mock(DeepSeekRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ProgressRepository progressRepository = mock(ProgressRepository.class);
        AuthRepository authRepository = mock(AuthRepository.class);
        FirebaseUser firebaseUser = mock(FirebaseUser.class);
        when(firebaseUser.getUid()).thenReturn("uid-1");
        when(authRepository.getCurrentUser()).thenReturn(firebaseUser);

        AIAnalysisViewModel viewModel = new AIAnalysisViewModel(
                deepSeekRepository,
                userRepository,
                progressRepository,
                authRepository
        );

        viewModel.generateWorkoutPlan();

        assertFalse(Boolean.TRUE.equals(viewModel.getCanGeneratePlan().getValue()));
        assertNotNull(viewModel.getPlanError().getValue());
        verify(deepSeekRepository, never()).generateWorkoutPlan(any(), any(), any());
    }

    @Test
    public void analyzeForm_shortInputsExposeFieldErrorsAndSkipAiCall() {
        DeepSeekRepository deepSeekRepository = mock(DeepSeekRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ProgressRepository progressRepository = mock(ProgressRepository.class);
        AuthRepository authRepository = mock(AuthRepository.class);
        when(authRepository.getCurrentUser()).thenReturn(null);
        AIAnalysisViewModel viewModel = new AIAnalysisViewModel(
                deepSeekRepository,
                userRepository,
                progressRepository,
                authRepository
        );

        viewModel.analyzeForm("a", "qua ngan");

        assertNotNull(viewModel.getExerciseNameError().getValue());
        assertNotNull(viewModel.getFormDescriptionError().getValue());
        verify(deepSeekRepository, never()).analyzeForm(any(), any(), any());
    }
}
