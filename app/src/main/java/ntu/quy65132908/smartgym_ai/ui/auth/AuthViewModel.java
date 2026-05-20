package ntu.quy65132908.smartgym_ai.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;

@HiltViewModel
public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<FirebaseUser> authSuccess = new MutableLiveData<>();

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
                errorMessage.postValue("Đăng nhập thất bại: " + e.getMessage());
            }
        });
    }

    public void signUp(String name, String email, String password) {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            errorMessage.setValue("Vui lòng nhập đầy đủ thông tin");
            return;
        }
        if (password.length() < 6) {
            errorMessage.setValue("Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }
        isLoading.setValue(true);
        errorMessage.setValue(null);

        authRepository.signUp(name, email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                isLoading.postValue(false);
                authSuccess.postValue(user);
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                errorMessage.postValue("Đăng ký thất bại: " + e.getMessage());
            }
        });
    }
}
