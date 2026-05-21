package ntu.quy65132908.smartgym_ai.ui.auth;

import android.util.Log;
import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import ntu.quy65132908.smartgym_ai.util.InputValidator;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;

@HiltViewModel
public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final SingleLiveEvent<String> errorMessage = new SingleLiveEvent<>();
    private final SingleLiveEvent<FirebaseUser> authSuccess = new SingleLiveEvent<>();

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<FirebaseUser> getAuthSuccess() { return authSuccess; }

    @Inject
    public AuthViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public boolean isLoggedIn() {
        return authRepository.isLoggedIn();
    }

    public void signIn(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            errorMessage.setValue("Vui lòng nhập đầy đủ thông tin");
            return;
        }
        if (!isValidEmail(email)) {
            errorMessage.setValue("Email không hợp lệ");
            return;
        }
        isLoading.setValue(true);
        errorMessage.setValue(null);

        authRepository.signIn(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                isLoading.postValue(false);
                authSuccess.postValue(user);
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                errorMessage.postValue(mapFirebaseError(e));
            }
        });
    }

    public void signUp(String name, String email, String password) {
        String sanitizedName = InputValidator.sanitizeName(name);
        if (!InputValidator.isValidName(sanitizedName) || email.isEmpty() || password.isEmpty()) {
            errorMessage.setValue("Vui lòng nhập đầy đủ thông tin");
            return;
        }
        if (!isValidEmail(email)) {
            errorMessage.setValue("Email không hợp lệ");
            return;
        }
        if (password.length() < 6) {
            errorMessage.setValue("Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }
        isLoading.setValue(true);
        errorMessage.setValue(null);

        authRepository.signUp(sanitizedName, email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                isLoading.postValue(false);
                authSuccess.postValue(user);
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                errorMessage.postValue(mapFirebaseError(e));
            }
        });
    }

    public void signInWithGoogle(String idToken) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        authRepository.signInWithGoogle(idToken, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                isLoading.postValue(false);
                authSuccess.postValue(user);
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                errorMessage.postValue("Đăng nhập Google thất bại: " + e.getMessage());
            }
        });
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private String mapFirebaseError(Exception e) {
        Log.e("AuthViewModel", "Auth error: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);

        if (e instanceof FirebaseAuthInvalidUserException) {
            return "Tài khoản không tồn tại";
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return "Email hoặc mật khẩu không đúng";
        } else if (e instanceof FirebaseAuthUserCollisionException) {
            return "Email này đã được sử dụng";
        } else if (e instanceof FirebaseAuthWeakPasswordException) {
            return "Mật khẩu quá yếu";
        } else if (e instanceof FirebaseNetworkException) {
            return "Không có kết nối mạng. Vui lòng kiểm tra lại.";
        } else {
            return "Đã xảy ra lỗi: " + e.getMessage();
        }
    }
}
