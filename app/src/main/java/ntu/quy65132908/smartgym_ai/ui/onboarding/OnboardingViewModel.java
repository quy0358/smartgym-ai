package ntu.quy65132908.smartgym_ai.ui.onboarding;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.util.InputValidator;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

@HiltViewModel
public class OnboardingViewModel extends ViewModel {
    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final Context appContext;

    private final MutableLiveData<Boolean> isSaving = new MutableLiveData<>(false);
    private final SingleLiveEvent<Boolean> saveSuccess = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> errorMessage = new SingleLiveEvent<>();

    @Inject
    public OnboardingViewModel(AuthRepository authRepository,
                               UserRepository userRepository,
                               @ApplicationContext Context appContext) {
        this.authRepository = authRepository;
        this.userRepository = userRepository;
        this.appContext = appContext;
    }

    public LiveData<Boolean> getIsSaving() {
        return isSaving;
    }

    public LiveData<Boolean> getSaveSuccess() {
        return saveSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void saveDraft(OnboardingProfileDraft draft) {
        if (draft == null || !draft.isComplete()) {
            errorMessage.setValue(appContext.getString(R.string.onboarding_required_error));
            return;
        }

        FirebaseUser firebaseUser = authRepository.getCurrentUser();
        if (firebaseUser == null) {
            errorMessage.setValue(appContext.getString(R.string.profile_login_required));
            return;
        }

        isSaving.setValue(true);
        userRepository.getUser(firebaseUser.getUid(), new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                saveUser(firebaseUser, user, draft);
            }

            @Override
            public void onError(Exception e) {
                saveUser(firebaseUser, createFallbackUser(firebaseUser), draft);
            }
        });
    }

    private void saveUser(FirebaseUser firebaseUser, User user, OnboardingProfileDraft draft) {
        if (user == null) {
            user = createFallbackUser(firebaseUser);
        }
        user.setUid(firebaseUser.getUid());
        if (user.getEmail().isEmpty()) {
            user.setEmail(firebaseUser.getEmail());
        }
        if (user.getDisplayName().isEmpty()) {
            user.setDisplayName(fallbackName(firebaseUser));
        }
        draft.applyToUser(user);

        userRepository.updateUser(firebaseUser.getUid(), user, new UserRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                isSaving.postValue(false);
                saveSuccess.postValue(true);
            }

            @Override
            public void onError(Exception e) {
                isSaving.postValue(false);
                errorMessage.postValue(appContext.getString(R.string.onboarding_save_error));
            }
        });
    }

    private User createFallbackUser(FirebaseUser firebaseUser) {
        return new User(firebaseUser.getUid(), fallbackName(firebaseUser), firebaseUser.getEmail());
    }

    private String fallbackName(FirebaseUser firebaseUser) {
        String name = InputValidator.sanitizeName(firebaseUser.getDisplayName());
        if (!name.isEmpty()) {
            return name;
        }
        String email = firebaseUser.getEmail();
        return email != null && !email.trim().isEmpty()
                ? email
                : appContext.getString(R.string.post_user_default);
    }
}
