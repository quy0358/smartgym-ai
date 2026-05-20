package ntu.quy65132908.smartgym_ai.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;

    private final MutableLiveData<String> displayName = new MutableLiveData<>("Người dùng");
    private final MutableLiveData<String> email = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> signedOut = new MutableLiveData<>(false);

    public LiveData<String> getDisplayName() { return displayName; }
    public LiveData<String> getEmail() { return email; }
    public LiveData<Boolean> getSignedOut() { return signedOut; }

    @Inject
    public ProfileViewModel(AuthRepository authRepository, UserRepository userRepository) {
        this.authRepository = authRepository;
        this.userRepository = userRepository;
        loadProfile();
    }

    private void loadProfile() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user != null) {
            email.setValue(user.getEmail());
            userRepository.getUser(user.getUid(), new UserRepository.UserCallback() {
                @Override
                public void onSuccess(ntu.quy65132908.smartgym_ai.data.model.User userData) {
                    displayName.postValue(userData.getDisplayName());
                }

                @Override
                public void onError(Exception e) {
                    displayName.postValue(user.getEmail());
                }
            });
        }
    }

    public void signOut() {
        authRepository.signOut();
        signedOut.setValue(true);
    }
}
