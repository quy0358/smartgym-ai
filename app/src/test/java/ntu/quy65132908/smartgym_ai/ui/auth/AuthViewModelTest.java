package ntu.quy65132908.smartgym_ai.ui.auth;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

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
        viewModel = new AuthViewModel(RuntimeEnvironment.getApplication(), authRepository);
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

    @Test
    public void resetPassword_emptyEmail_setsError() {
        viewModel.resetPassword("");

        assertEquals("Vui lòng nhập email để đặt lại mật khẩu",
                viewModel.getPasswordResetErrorMessage().getValue());
    }

    @Test
    public void resetPassword_invalidEmail_setsError() {
        viewModel.resetPassword("notanemail");

        assertEquals("Email không hợp lệ", viewModel.getPasswordResetErrorMessage().getValue());
    }

    @Test
    public void resetPassword_validEmail_callsRepository() {
        viewModel.resetPassword("test@email.com");

        verify(authRepository).sendPasswordResetEmail(eq("test@email.com"), any());
    }

    @Test
    public void resetPassword_success_postsDialogSuccessMessage() {
        doAnswer(invocation -> {
            AuthRepository.SimpleCallback callback = invocation.getArgument(1);
            callback.onSuccess();
            return null;
        }).when(authRepository).sendPasswordResetEmail(eq("test@email.com"), any());

        viewModel.resetPassword(" test@email.com ");

        assertEquals("Đã gửi email đặt lại mật khẩu tới test@email.com",
                viewModel.getPasswordResetSuccessMessage().getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.getIsLoading().getValue()));
    }

    @Test
    public void resetPassword_networkError_postsDialogError() {
        doAnswer(invocation -> {
            AuthRepository.SimpleCallback callback = invocation.getArgument(1);
            callback.onError(new FirebaseNetworkException("offline"));
            return null;
        }).when(authRepository).sendPasswordResetEmail(eq("test@email.com"), any());

        viewModel.resetPassword("test@email.com");

        assertEquals("Không có kết nối mạng. Vui lòng kiểm tra lại.",
                viewModel.getPasswordResetErrorMessage().getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.getIsLoading().getValue()));
    }

    @Test
    public void reportGoogleNoCredential_setsActionableError() {
        viewModel.reportGoogleNoCredential();

        assertEquals("Không tìm thấy tài khoản Google trên thiết bị. Hãy thêm tài khoản Google rồi thử lại.",
                viewModel.getErrorMessage().getValue());
    }

    @Test
    public void reportGoogleConfigurationMissing_setsSetupError() {
        viewModel.reportGoogleConfigurationMissing();

        assertEquals("Google Sign-In chưa được cấu hình đúng. Hãy kiểm tra OAuth client và default_web_client_id.",
                viewModel.getErrorMessage().getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.getIsLoading().getValue()));
    }

    @Test
    public void signInWithGoogle_emptyToken_setsFriendlyErrorAndSkipsRepository() {
        viewModel.signInWithGoogle("");

        assertEquals("Không xác thực được tài khoản Google. Vui lòng chọn lại tài khoản.",
                viewModel.getErrorMessage().getValue());
        verify(authRepository, never()).signInWithGoogle(any(), any());
    }

    @Test
    public void reportGoogleSignInCanceled_clearsLoadingWithoutError() {
        viewModel.startGoogleCredentialRequest();

        viewModel.reportGoogleSignInCanceled();

        assertNull(viewModel.getErrorMessage().getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.getIsLoading().getValue()));
    }

    @Test
    public void signInWithGoogle_invalidFirebaseCredential_setsFriendlyError() {
        doAnswer(invocation -> {
            AuthRepository.AuthCallback callback = invocation.getArgument(1);
            callback.onError(new FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "bad"));
            return null;
        }).when(authRepository).signInWithGoogle(eq("id-token"), any());

        viewModel.signInWithGoogle("id-token");

        assertEquals("Không xác thực được tài khoản Google. Vui lòng chọn lại tài khoản.",
                viewModel.getErrorMessage().getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.getIsLoading().getValue()));
    }

    @Test
    public void signInWithGoogle_success_postsAuthSuccess() {
        FirebaseUser user = mock(FirebaseUser.class);
        doAnswer(invocation -> {
            AuthRepository.AuthCallback callback = invocation.getArgument(1);
            callback.onSuccess(user);
            return null;
        }).when(authRepository).signInWithGoogle(eq("id-token"), any());

        viewModel.signInWithGoogle("id-token");

        assertEquals(user, viewModel.getAuthSuccess().getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.getIsLoading().getValue()));
    }

    @Test
    public void reportGoogleSignInFailure_setsVisibleError() {
        viewModel.reportGoogleSignInFailure("Canceled");

        assertEquals("Đăng nhập Google thất bại: Canceled", viewModel.getErrorMessage().getValue());
    }
}
