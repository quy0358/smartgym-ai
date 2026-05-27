package ntu.quy65132908.smartgym_ai.ui.workout;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.model.InjuryProfile;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.DeepSeekRepository;
import ntu.quy65132908.smartgym_ai.data.repository.InjuryProfileRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;
import ntu.quy65132908.smartgym_ai.ui.analysis.AiErrorMapper;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

@HiltViewModel
public class WorkoutListViewModel extends ViewModel {
    private final WorkoutRepository workoutRepository;
    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final InjuryProfileRepository injuryProfileRepository;
    private final DeepSeekRepository deepSeekRepository;

    private final MutableLiveData<List<Workout>> workouts = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isCreatingPlan = new MutableLiveData<>(false);
    private final SingleLiveEvent<String> errorMessage = new SingleLiveEvent<>();

    public LiveData<List<Workout>> getWorkouts() { return workouts; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsCreatingPlan() { return isCreatingPlan; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    @Inject
    public WorkoutListViewModel(WorkoutRepository workoutRepository,
                                AuthRepository authRepository,
                                UserRepository userRepository,
                                InjuryProfileRepository injuryProfileRepository,
                                DeepSeekRepository deepSeekRepository) {
        this.workoutRepository = workoutRepository;
        this.authRepository = authRepository;
        this.userRepository = userRepository;
        this.injuryProfileRepository = injuryProfileRepository;
        this.deepSeekRepository = deepSeekRepository;
        loadWorkouts();
    }

    public void loadWorkouts() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            isLoading.setValue(false);
            workouts.setValue(Collections.emptyList());
            return;
        }

        isLoading.setValue(true);
        workoutRepository.getWeeklyPlan(user.getUid(), new WorkoutRepository.WorkoutListCallback() {
            @Override
            public void onSuccess(List<Workout> list) {
                workouts.postValue(list != null ? list : Collections.emptyList());
                isLoading.postValue(false);
            }

            @Override
            public void onError(Exception e) {
                workouts.postValue(Collections.emptyList());
                isLoading.postValue(false);
                errorMessage.postValue("Không thể tải kế hoạch tập.");
            }
        });
    }

    public void createPlan() {
        FirebaseUser firebaseUser = authRepository.getCurrentUser();
        if (firebaseUser == null) {
            isCreatingPlan.setValue(false);
            errorMessage.setValue("Bạn cần đăng nhập để tạo kế hoạch.");
            return;
        }

        isCreatingPlan.setValue(true);
        userRepository.getUser(firebaseUser.getUid(), new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                loadInjuryProfileAndCreatePlan(firebaseUser.getUid(), user);
            }

            @Override
            public void onError(Exception e) {
                isCreatingPlan.postValue(false);
                errorMessage.postValue("Không thể tải hồ sơ để tạo kế hoạch.");
            }
        });
    }

    private void loadInjuryProfileAndCreatePlan(String uid, User user) {
        injuryProfileRepository.getInjuryProfile(uid, new InjuryProfileRepository.InjuryProfileCallback() {
            @Override
            public void onSuccess(InjuryProfile injuryProfile) {
                generatePlan(uid, user, injuryProfile);
            }

            @Override
            public void onError(Exception e) {
                isCreatingPlan.postValue(false);
                errorMessage.postValue("Không thể tải hồ sơ an toàn để tạo kế hoạch.");
            }
        });
    }

    private void generatePlan(String uid, User user, InjuryProfile injuryProfile) {
        DeepSeekRepository.WorkoutPlanCallback callback = new DeepSeekRepository.WorkoutPlanCallback() {
            @Override
            public void onSuccess(List<Workout> plan) {
                if (plan == null || plan.isEmpty()) {
                    saveFallbackPlan(uid, user, new IllegalArgumentException("AI response must include a non-empty days array."));
                    return;
                }
                saveGeneratedPlan(uid, plan);
            }

            @Override
            public void onError(Exception e) {
                saveFallbackPlan(uid, user, e);
            }
        };

        if (hasInjuryProfile(injuryProfile)) {
            deepSeekRepository.generateInjuryAwareWorkoutPlan(user, injuryProfile, user.getGoal(), callback);
        } else {
            deepSeekRepository.generateWorkoutPlanData(user, user.getGoal(), callback);
        }
    }

    private boolean hasInjuryProfile(InjuryProfile profile) {
        return profile != null
                && (profile.isKneeSensitive()
                || profile.isShoulderSensitive()
                || profile.isLowerBackSensitive()
                || (profile.getNotes() != null && !profile.getNotes().trim().isEmpty()));
    }

    private void saveGeneratedPlan(String uid, List<Workout> plan) {
        saveGeneratedPlan(uid, plan, null);
    }

    private void saveFallbackPlan(String uid, User user, Exception aiError) {
        List<Workout> fallbackPlan = DeepSeekRepository.buildFallbackWorkoutPlan(
                user,
                user != null ? user.getGoal() : null);
        String message = AiErrorMapper.toUserMessage(aiError)
                + " Đã tạo kế hoạch cơ bản để bạn bắt đầu.";
        saveGeneratedPlan(uid, fallbackPlan, message);
    }

    private void saveGeneratedPlan(String uid, List<Workout> plan, String successMessage) {
        workoutRepository.saveWeeklyPlan(uid, plan, new WorkoutRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                isCreatingPlan.postValue(false);
                loadWorkouts();
                if (successMessage != null && !successMessage.trim().isEmpty()) {
                    errorMessage.postValue(successMessage);
                }
            }

            @Override
            public void onError(Exception e) {
                isCreatingPlan.postValue(false);
                errorMessage.postValue("Không thể lưu kế hoạch tập. Vui lòng thử lại.");
            }
        });
    }
}
