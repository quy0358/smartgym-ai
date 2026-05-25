package ntu.quy65132908.smartgym_ai.ui.auth;

import android.content.Context;
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
import dagger.hilt.android.qualifiers.ApplicationContext;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;

@HiltViewModel
public class AuthViewModel extends ViewModel {

    private final Context appContext;
    private final AuthRepository authRepository;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final SingleLiveEvent<String> errorMessage = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> successMessage = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> passwordResetErrorMessage = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> passwordResetSuccessMessage = new SingleLiveEvent<>();
    private final SingleLiveEvent<FirebaseUser> authSuccess = new SingleLiveEvent<>();

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<String> getPasswordResetErrorMessage() { return passwordResetErrorMessage; }
    public LiveData<String> getPasswordResetSuccessMessage() { return passwordResetSuccessMessage; }
    public LiveData<FirebaseUser> getAuthSuccess() { return authSuccess; }

    @Inject
    public AuthViewModel(@ApplicationContext Context context, AuthRepository authRepository) {
        this.appContext = context.getApplicationContext();
        this.authRepository = authRepository;
    }

    public boolean isLoggedIn() {
        return authRepository.isLoggedIn();
    }

    public void signIn(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            errorMessage.setValue(getString(R.string.error_empty_fields));
            return;
        }
        if (!isValidEmail(email)) {
            errorMessage.setValue(getString(R.string.error_invalid_email));
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
            errorMessage.setValue(getString(R.string.error_empty_fields));
            return;
        }
        if (!isValidEmail(email)) {
            errorMessage.setValue(getString(R.string.error_invalid_email));
            return;
        }
        if (password.length() < 6) {
            errorMessage.setValue(getString(R.string.error_weak_password));
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
        if (idToken == null || idToken.trim().isEmpty()) {
            isLoading.setValue(false);
            errorMessage.setValue(getString(R.string.error_google_invalid_credential));
            return;
        }
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
                errorMessage.postValue(mapGoogleAuthError(e));
            }
        });
    }

    public void resetPassword(String email) {
        String sanitizedEmail = email != null ? email.trim() : "";
        if (sanitizedEmail.isEmpty()) {
            passwordResetErrorMessage.setValue(getString(R.string.error_reset_email_required));
            return;
        }
        if (!isValidEmail(sanitizedEmail)) {
            passwordResetErrorMessage.setValue(getString(R.string.error_invalid_email));
            return;
        }

        isLoading.setValue(true);
        passwordResetErrorMessage.setValue(null);
        authRepository.sendPasswordResetEmail(sanitizedEmail, new AuthRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                isLoading.postValue(false);
                passwordResetSuccessMessage.postValue(
                        getString(R.string.password_reset_email_sent, sanitizedEmail));
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                passwordResetErrorMessage.postValue(mapFirebaseError(e));
            }
        });
    }

    public void startGoogleCredentialRequest() {
        isLoading.setValue(true);
        errorMessage.setValue(null);
    }

    public void reportGoogleConfigurationMissing() {
        isLoading.setValue(false);
        errorMessage.setValue(getString(R.string.error_google_config_missing));
    }

    public void reportGoogleNoCredential() {
        isLoading.setValue(false);
        errorMessage.setValue(getString(R.string.error_google_no_credential));
    }

    public void reportGoogleSignInCanceled() {
        isLoading.setValue(false);
        errorMessage.setValue(null);
    }

    public void reportGoogleCredentialFailure(String detail) {
        isLoading.setValue(false);
        String safeDetail = sanitizeErrorDetail(detail);
        errorMessage.setValue(getString(R.string.error_google_credential, safeDetail));
    }

    public void reportGoogleSignInFailure(String detail) {
        isLoading.setValue(false);
        String safeDetail = sanitizeErrorDetail(detail);
        errorMessage.setValue(getString(R.string.error_google_sign_in, safeDetail));
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private String mapGoogleAuthError(Exception e) {
        Log.e("AuthViewModel", "Google auth error: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);

        if (e instanceof FirebaseNetworkException) {
            return getString(R.string.error_no_network);
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return getString(R.string.error_google_invalid_credential);
        } else {
            return getString(R.string.error_google_sign_in, sanitizeErrorDetail(e.getMessage()));
        }
    }

    private String mapFirebaseError(Exception e) {
        Log.e("AuthViewModel", "Auth error: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);

        if (e instanceof FirebaseAuthInvalidUserException) {
            return getString(R.string.error_account_not_found);
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return getString(R.string.error_wrong_credentials);
        } else if (e instanceof FirebaseAuthUserCollisionException) {
            return getString(R.string.error_email_in_use);
        } else if (e instanceof FirebaseAuthWeakPasswordException) {
            return getString(R.string.error_password_too_weak);
        } else if (e instanceof FirebaseNetworkException) {
            return getString(R.string.error_no_network);
        } else {
            return getString(R.string.error_generic, sanitizeErrorDetail(e.getMessage()));
        }
    }

    private String sanitizeErrorDetail(String detail) {
        return detail != null && !detail.trim().isEmpty()
                ? detail.trim()
                : getString(R.string.error_unknown_reason);
    }

    private String getString(int resId) {
        return appContext.getString(resId);
    }

    private String getString(int resId, Object... args) {
        return appContext.getString(resId, args);
    }
}
