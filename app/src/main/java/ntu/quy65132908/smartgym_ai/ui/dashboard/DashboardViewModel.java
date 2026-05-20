package ntu.quy65132908.smartgym_ai.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;

@HiltViewModel
public class DashboardViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;

    private final MutableLiveData<String> userName = new MutableLiveData<>("Bạn");
    private final MutableLiveData<Float> weight = new MutableLiveData<>(70f);
    private final MutableLiveData<Float> bmi = new MutableLiveData<>(22.5f);

    public LiveData<String> getUserName() { return userName; }
    public LiveData<Float> getWeight() { return weight; }
    public LiveData<Float> getBmi() { return bmi; }

    @Inject
    public DashboardViewModel(AuthRepository authRepository, UserRepository userRepository) {
        this.authRepository = authRepository;
        this.userRepository = userRepository;
        loadUserData();
    }

    private void loadUserData() {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser != null) {
            userRepository.getUser(currentUser.getUid(), new UserRepository.UserCallback() {
                @Override
                public void onSuccess(ntu.quy65132908.smartgym_ai.data.model.User user) {
                    userName.postValue(user.getDisplayName());
                    if (user.getWeight() != null) weight.postValue(user.getWeight());
                    if (user.getBmi() != null) bmi.postValue(user.getBmi());
                }

                @Override
                public void onError(Exception e) {
                    // Use default values
                }
            });
        }
    }
}
