package ntu.quy65132908.smartgym_ai.ui.analysis;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.GeminiRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;

@HiltViewModel
public class AIAnalysisViewModel extends ViewModel {
    private final GeminiRepository geminiRepo;
    private final UserRepository userRepo;
    private final AuthRepository authRepo;
    private final MutableLiveData<String> aiResponse = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMsg = new MutableLiveData<>();
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();

    public LiveData<String> getAiResponse() { return aiResponse; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMsg() { return errorMsg; }

    @Inject
    public AIAnalysisViewModel(GeminiRepository geminiRepo, UserRepository userRepo, AuthRepository authRepo) {
        this.geminiRepo = geminiRepo;
        this.userRepo = userRepo;
        this.authRepo = authRepo;
        loadUser();
    }

    private void loadUser() {
        FirebaseUser fb = authRepo.getCurrentUser();
        if (fb == null) return;
        userRepo.getUser(fb.getUid(), new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User u) { currentUser.postValue(u); }
            @Override
            public void onError(Exception e) {}
        });
    }

    public void generateWorkoutPlan() {
        User u = currentUser.getValue();
        if (u == null) { errorMsg.setValue("Đang tải..."); return; }
        isLoading.setValue(true);
        geminiRepo.generateWorkoutPlan(u, u.getGoal(), new GeminiRepository.AiCallback() {
            @Override
            public void onSuccess(String r) { isLoading.postValue(false); aiResponse.postValue(r); }
            @Override
            public void onError(Exception e) { isLoading.postValue(false); errorMsg.postValue(e.getMessage()); }
        });
    }

    public void analyzeForm(String exercise, String desc) {
        if (exercise.trim().isEmpty() || desc.trim().isEmpty()) { errorMsg.setValue("Nhập đủ thông tin"); return; }
        isLoading.setValue(true);
        geminiRepo.analyzeForm(exercise, desc, new GeminiRepository.AiCallback() {
            @Override
            public void onSuccess(String r) { isLoading.postValue(false); aiResponse.postValue(r); }
            @Override
            public void onError(Exception e) { isLoading.postValue(false); errorMsg.postValue(e.getMessage()); }
        });
    }
}
