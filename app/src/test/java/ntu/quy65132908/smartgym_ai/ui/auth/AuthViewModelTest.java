package ntu.quy65132908.smartgym_ai.ui.auth;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;

@RunWith(RobolectricTestRunner.class)
public class AuthViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private AuthRepository authRepository;

    private AuthViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new AuthViewModel(authRepository);
    }

    // ─── signIn tests ──────────────────────────────────────────────

    @Test
    public void signIn_emptyEmail_setsError() {
        viewModel.signIn("", "password");
        assertEquals("Vui lòng nhập đầy đủ thông tin", viewModel.getErrorMessage().getValue());
    }

    @Test
    public void signIn_emptyPassword_setsError() {
        viewModel.signIn("test@email.com", "");
        assertEquals("Vui lòng nhập đầy đủ thông tin", viewModel.getErrorMessage().getValue());
    }

    @Test
    public void signIn_invalidEmail_setsError() {
        viewModel.signIn("notanemail", "password");
        assertEquals("Email không hợp lệ", viewModel.getErrorMessage().getValue());
    }

    @Test
    public void signIn_validInput_callsRepository() {
        viewModel.signIn("test@email.com", "password123");
        verify(authRepository).signIn(eq("test@email.com"), eq("password123"), any());
    }

    // ─── signUp tests ──────────────────────────────────────────────

    @Test
    public void signUp_emptyName_setsError() {
        viewModel.signUp("", "test@email.com", "password123");
        assertEquals("Vui lòng nhập đầy đủ thông tin", viewModel.getErrorMessage().getValue());
    }

    @Test
    public void signUp_shortPassword_setsError() {
        viewModel.signUp("John", "test@email.com", "12345");
        assertEquals("Mật khẩu phải có ít nhất 6 ký tự", viewModel.getErrorMessage().getValue());
    }

    @Test
    public void signUp_validInput_callsRepository() {
        viewModel.signUp("John", "test@email.com", "password123");
        verify(authRepository).signUp(eq("John"), eq("test@email.com"), eq("password123"), any());
    }

    @Test
    public void signUp_htmlInName_sanitizes() {
        viewModel.signUp("<b>John</b>", "test@email.com", "password123");
        verify(authRepository).signUp(eq("John"), eq("test@email.com"), eq("password123"), any());
    }

    // ─── isLoggedIn tests ──────────────────────────────────────────

    @Test
    public void isLoggedIn_delegatesToRepository() {
        when(authRepository.isLoggedIn()).thenReturn(true);
        assertTrue(viewModel.isLoggedIn());
        verify(authRepository).isLoggedIn();
    }
}
