package ntu.quy65132908.smartgym_ai.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

@HiltViewModel
public class DashboardViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final WorkoutRepository workoutRepository;

    // User profile data
    private final MutableLiveData<String> userName = new MutableLiveData<>("Bạn");
    private final MutableLiveData<String> avatarLetter = new MutableLiveData<>("U");
    private final MutableLiveData<Integer> weight = new MutableLiveData<>(0);
    private final MutableLiveData<Float> bmi = new MutableLiveData<>(0f);
    private final MutableLiveData<String> bmiCategory = new MutableLiveData<>("");
    private final MutableLiveData<Integer> goalWeight = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> bmiColorRes = new MutableLiveData<>(R.color.on_surface_variant);
    private final MutableLiveData<String> goalDisplay = new MutableLiveData<>("0");

    // AI Recommendation
    private final MutableLiveData<Workout> aiRecommendation = new MutableLiveData<>(null);

    // Weekly Plan
    private final MutableLiveData<List<Workout>> weeklyPlan = new MutableLiveData<>(Collections.emptyList());

    // UI State
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);
    private final SingleLiveEvent<String> errorMessage = new SingleLiveEvent<>();

    // Public getters
    public LiveData<String> getUserName() { return userName; }
    public LiveData<String> getAvatarLetter() { return avatarLetter; }
    public LiveData<Integer> getWeight() { return weight; }
    public LiveData<Float> getBmi() { return bmi; }
    public LiveData<String> getBmiCategory() { return bmiCategory; }
    public LiveData<Integer> getGoalWeight() { return goalWeight; }
    public LiveData<Integer> getBmiColorRes() { return bmiColorRes; }
    public LiveData<String> getGoalDisplay() { return goalDisplay; }
    public LiveData<Workout> getAiRecommendation() { return aiRecommendation; }
    public LiveData<List<Workout>> getWeeklyPlan() { return weeklyPlan; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsRefreshing() { return isRefreshing; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    @Inject
    public DashboardViewModel(AuthRepository authRepository,
                              UserRepository userRepository,
                              WorkoutRepository workoutRepository) {
        this.authRepository = authRepository;
        this.userRepository = userRepository;
        this.workoutRepository = workoutRepository;
        loadUserData();
    }

    public void refresh() {
        isRefreshing.setValue(true);
        loadUserData();
    }

    private void loadUserData() {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            isLoading.setValue(false);
            isRefreshing.setValue(false);
            return;
        }

        String uid = currentUser.getUid();

        userRepository.getUser(uid, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(ntu.quy65132908.smartgym_ai.data.model.User user) {
                String name = user.getDisplayName();
                userName.postValue(name != null && !name.isEmpty() ? name : "Bạn");
                avatarLetter.postValue(computeAvatarLetter(name));
                weight.postValue(user.getWeight() != null ? user.getWeight().intValue() : 0);
                bmi.postValue(user.getBmi() != null ? user.getBmi() : 0f);
                bmiCategory.postValue(user.getBmiCategory() != null ? user.getBmiCategory() : "");
                goalWeight.postValue(parseGoalWeight(user.getGoal()));
                goalDisplay.postValue(formatGoalDisplay(parseGoalWeight(user.getGoal())));
                bmiColorRes.postValue(computeBmiColor(user.getBmi() != null ? user.getBmi() : 0f));

                loadWeeklyPlan(uid);
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                isRefreshing.postValue(false);
                errorMessage.postValue("Không thể tải dữ liệu. Kéo xuống để thử lại.");
            }
        });
    }

    private void loadWeeklyPlan(String uid) {
        workoutRepository.getWeeklyPlan(uid, new WorkoutRepository.WorkoutListCallback() {
            @Override
            public void onSuccess(List<Workout> workouts) {
                weeklyPlan.postValue(workouts);
                aiRecommendation.postValue(findTodayWorkout(workouts));
                isLoading.postValue(false);
                isRefreshing.postValue(false);
            }

            @Override
            public void onError(Exception e) {
                weeklyPlan.postValue(Collections.emptyList());
                aiRecommendation.postValue(null);
                isLoading.postValue(false);
                isRefreshing.postValue(false);
                errorMessage.postValue("Không thể tải kế hoạch tuần.");
            }
        });
    }

    String computeAvatarLetter(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return "U";
        }
        return String.valueOf(displayName.trim().charAt(0)).toUpperCase();
    }

    int computeBmiColor(float bmiValue) {
        if (bmiValue < 18.5f) return R.color.tertiary;
        if (bmiValue < 25.0f) return R.color.primary;
        if (bmiValue < 30.0f) return R.color.warning;
        return R.color.error;
    }

    String formatGoalDisplay(int goal) {
        if (goal == 0) return "0";
        return "\u2212" + Math.abs(goal);
    }

    int parseGoalWeight(String goal) {
        if (goal == null || goal.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(goal.trim());
        } catch (NumberFormatException ignored) {}

        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(goal);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group());
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private Workout findTodayWorkout(List<Workout> workouts) {
        if (workouts == null || workouts.isEmpty()) {
            return null;
        }
        int todayDow = getTodayDayOfWeek();
        for (Workout w : workouts) {
            if (w.getDayOfWeek() == todayDow) {
                return w;
            }
        }
        return null;
    }

    private int getTodayDayOfWeek() {
        int calDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return calDay == Calendar.SUNDAY ? 7 : calDay - 1;
    }
}
