package ntu.quy65132908.smartgym_ai.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

@HiltViewModel
public class EditProfileViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final AuthRepository authRepository;

    private final MutableLiveData<User> userData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final SingleLiveEvent<Boolean> saveSuccess = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> errorMessage = new SingleLiveEvent<>();

    public LiveData<User> getUserData() { return userData; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getSaveSuccess() { return saveSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    @Inject
    public EditProfileViewModel(UserRepository userRepository, AuthRepository authRepository) {
        this.userRepository = userRepository;
        this.authRepository = authRepository;
        loadUser();
    }

    private void loadUser() {
        FirebaseUser fb = authRepository.getCurrentUser();
        if (fb == null) return;
        isLoading.setValue(true);
        userRepository.getUser(fb.getUid(), new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                isLoading.postValue(false);
                userData.postValue(user);
            }
            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                errorMessage.postValue(e.getMessage());
            }
        });
    }

    public void saveProfile(String name, String weightStr, String heightStr, String goal) {
        FirebaseUser fb = authRepository.getCurrentUser();
        if (fb == null) return;

        if (name.trim().isEmpty()) {
            errorMessage.setValue("Tên không được để trống");
            return;
        }

        User user = userData.getValue();
        if (user == null) user = new User(fb.getUid(), name, fb.getEmail());

        user.setDisplayName(name.trim());
        user.setGoal(goal.trim().isEmpty() ? null : goal.trim());

        try {
            if (!weightStr.isEmpty()) {
                float weight = Float.parseFloat(weightStr);
                user.setWeight(weight);
            }
            if (!heightStr.isEmpty()) {
                float height = Float.parseFloat(heightStr);
                user.setHeight(height);
            }
            // Calculate BMI if both weight and height available
            if (user.getWeight() != null && user.getHeight() != null && user.getHeight() > 0) {
                float heightM = user.getHeight() / 100f;
                float bmi = user.getWeight() / (heightM * heightM);
                user.setBmi(bmi);
                if (bmi < 18.5f) user.setBmiCategory("Thiếu cân");
                else if (bmi < 25f) user.setBmiCategory("Bình thường");
                else if (bmi < 30f) user.setBmiCategory("Thừa cân");
                else user.setBmiCategory("Béo phì");
            }
        } catch (NumberFormatException e) {
            errorMessage.setValue("Số không hợp lệ");
            return;
        }

        isLoading.setValue(true);
        userRepository.updateUser(fb.getUid(), user, new UserRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                isLoading.postValue(false);
                saveSuccess.postValue(true);
            }
            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                errorMessage.postValue(e.getMessage());
            }
        });
    }
}
