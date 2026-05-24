package ntu.quy65132908.smartgym_ai.ui.workout;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.DeepSeekRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

@HiltViewModel
public class WorkoutListViewModel extends ViewModel {
    private final WorkoutRepository workoutRepository;
    private final AuthRepository authRepository;
    private final UserRepository userRepository;
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
                                DeepSeekRepository deepSeekRepository) {
        this.workoutRepository = workoutRepository;
        this.authRepository = authRepository;
        this.userRepository = userRepository;
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
                deepSeekRepository.generateWorkoutPlanData(user, user.getGoal(), new DeepSeekRepository.WorkoutPlanCallback() {
                    @Override
                    public void onSuccess(List<Workout> plan) {
                        if (plan == null || plan.isEmpty()) {
                            isCreatingPlan.postValue(false);
                            errorMessage.postValue("AI chưa tạo được kế hoạch phù hợp. Vui lòng thử lại.");
                            return;
                        }
                        saveGeneratedPlan(firebaseUser.getUid(), plan);
                    }

                    @Override
                    public void onError(Exception e) {
                        isCreatingPlan.postValue(false);
                        errorMessage.postValue("Không thể tạo kế hoạch từ AI. Vui lòng thử lại.");
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                isCreatingPlan.postValue(false);
                errorMessage.postValue("Không thể tải hồ sơ để tạo kế hoạch.");
            }
        });
    }

    private void saveGeneratedPlan(String uid, List<Workout> plan) {
        workoutRepository.saveWeeklyPlan(uid, plan, new WorkoutRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                isCreatingPlan.postValue(false);
                loadWorkouts();
            }

            @Override
            public void onError(Exception e) {
                isCreatingPlan.postValue(false);
                errorMessage.postValue("Không thể lưu kế hoạch tập. Vui lòng thử lại.");
            }
        });
    }
}
