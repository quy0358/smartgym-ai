package ntu.quy65132908.smartgym_ai.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;
import ntu.quy65132908.smartgym_ai.util.DateUtils;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

/**
 * ViewModel cho tab Trang chủ của Dashboard.
 *
 * Luồng dữ liệu:
 * startLoad() → [UserRepository.getUser]
 *   → loadWeeklyPlan()
 *   → [WorkoutRepository.getWeeklyPlan]
 *   → phát một DashboardUiState duy nhất
 * Nếu tải hồ sơ lỗi, dùng dữ liệu hồ sơ mặc định để kế hoạch tuần vẫn render được.
 *
 * Quyết định thiết kế:
 * - Tải trong constructor là có chủ đích vì ViewModel sống qua thay đổi cấu hình.
 * - Một trạng thái UI duy nhất giúp đồng bộ thẻ hôm nay, kế hoạch, hồ sơ và cờ loading.
 * - SingleLiveEvent cho lỗi chấp nhận mất sự kiện nếu Fragment đang ở nền.
 * - Mã thế hệ request giúp bỏ qua callback async cũ.
 * - Cooldown refresh tối thiểu 5 giây để tránh lãng phí quota Firestore.
 */
@HiltViewModel
public class DashboardViewModel extends ViewModel {

    private static final Pattern GOAL_NUMBER_PATTERN = Pattern.compile("[-+]?\\d+");
    private static final String PLACEHOLDER = "--";
    private static final String UNICODE_MINUS = "\u2212";
    private static final long REFRESH_COOLDOWN_MS = 5000; // 5 giây

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final WorkoutRepository workoutRepository;

    private final MutableLiveData<DashboardUiState> uiState = new MutableLiveData<>(DashboardUiState.initial());
    private final SingleLiveEvent<DashboardError> errorEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> requireLoginEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> refreshSuccessEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> refreshThrottledEvent = new SingleLiveEvent<>();

    private long lastRefreshTime = 0;
    private boolean isLoadInProgress = false;
    private int loadGeneration = 0;

    public LiveData<DashboardUiState> getUiState() { return uiState; }
    public LiveData<DashboardError> getErrorEvent() { return errorEvent; }
    public LiveData<Boolean> getRequireLoginEvent() { return requireLoginEvent; }
    public LiveData<Boolean> getRefreshSuccessEvent() { return refreshSuccessEvent; }
    public LiveData<Boolean> getRefreshThrottledEvent() { return refreshThrottledEvent; }

    @Inject
    public DashboardViewModel(AuthRepository authRepository,
                              UserRepository userRepository,
                              WorkoutRepository workoutRepository) {
        this.authRepository = authRepository;
        this.userRepository = userRepository;
        this.workoutRepository = workoutRepository;

        startLoad(false);
    }

    public void refresh() {
        long now = System.currentTimeMillis();
        DashboardUiState current = currentState();
        if (now - lastRefreshTime < REFRESH_COOLDOWN_MS) {
            if (!current.isRefreshing()) {
                publishState(copyState(current, null, false, null));
            }
            refreshThrottledEvent.setValue(true);
            return;
        }
        lastRefreshTime = now;
        startLoad(true);
    }

    public void reload() {
        startLoad(false);
    }

    private void startLoad(boolean refresh) {
        isLoadInProgress = true;
        int generation = ++loadGeneration;
        DashboardUiState current = currentState();
        publishState(copyState(current, current.isInitialLoading() && !hasDashboardData(current), refresh, null));

        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            publishState(copyState(currentState(), false, false, null));
            isLoadInProgress = false;
            requireLoginEvent.setValue(true);
            return;
        }

        String uid = currentUser.getUid();

        userRepository.getUser(uid, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (!isCurrentGeneration(generation)) {
                    return;
                }
                loadWeeklyPlan(uid, generation, user, refresh, false);
            }

            @Override
            public void onError(Exception e) {
                if (!isCurrentGeneration(generation)) {
                    return;
                }
                errorEvent.postValue(DashboardError.PROFILE_LOAD_FAILED);
                loadWeeklyPlan(uid, generation, null, refresh, true);
            }
        });
    }

    private void loadWeeklyPlan(String uid, int generation, User user, boolean refresh, boolean profileFallback) {
        workoutRepository.getWeeklyPlan(uid, new WorkoutRepository.WorkoutListCallback() {
            @Override
            public void onSuccess(List<Workout> workouts) {
                if (!isCurrentGeneration(generation)) {
                    return;
                }
                List<Workout> safeWorkouts = workouts != null ? workouts : Collections.emptyList();

                TodayState state = determineTodayState(safeWorkouts);
                Workout recommendation = state == TodayState.WORKOUT ? findTodayWorkout(safeWorkouts) : null;
                DashboardUiState next = buildState(user, safeWorkouts, state, recommendation,
                        false, false, false);
                publishState(next);

                isLoadInProgress = false;
                if (refresh) {
                    refreshSuccessEvent.postValue(true);
                }
            }

            @Override
            public void onError(Exception e) {
                if (!isCurrentGeneration(generation)) {
                    return;
                }
                DashboardUiState current = currentState();
                DashboardUiState profileState = applyProfileToExistingState(user, current, hasDashboardData(current));
                publishState(copyState(profileState, false, false, hasDashboardData(profileState)));
                isLoadInProgress = false;
                errorEvent.postValue(DashboardError.WEEKLY_PLAN_LOAD_FAILED);
            }
        });
    }

    private DashboardUiState buildState(User user,
                                        List<Workout> workouts,
                                        TodayState state,
                                        Workout recommendation,
                                        boolean initialLoading,
                                        boolean refreshing,
                                        boolean dataStale) {
        String name = displayNameOrDefault(user);
        Float userBmi = user != null ? user.getBmi() : null;
        return new DashboardUiState(
                name,
                user != null && user.getPhotoUrl() != null ? user.getPhotoUrl() : "",
                user != null && user.getWeight() != null ? user.getWeight().intValue() : null,
                userBmi,
                userBmi != null ? user.getBmiCategory() : "",
                userBmi != null ? computeBmiColor(userBmi) : R.color.on_surface_variant,
                formatUserGoalDisplay(user),
                state,
                recommendation,
                workouts,
                initialLoading,
                refreshing,
                dataStale
        );
    }

    private DashboardUiState applyProfileToExistingState(User user, DashboardUiState current, boolean dataStale) {
        if (user == null) {
            return copyState(current, false, false, dataStale);
        }

        Float userBmi = user.getBmi();
        return new DashboardUiState(
                displayNameOrDefault(user),
                user.getPhotoUrl() != null ? user.getPhotoUrl() : "",
                user.getWeight() != null ? user.getWeight().intValue() : null,
                userBmi,
                userBmi != null ? user.getBmiCategory() : "",
                userBmi != null ? computeBmiColor(userBmi) : R.color.on_surface_variant,
                formatUserGoalDisplay(user),
                current.getTodayState(),
                current.getAiRecommendation(),
                current.getWeeklyPlan(),
                false,
                false,
                dataStale
        );
    }

    private DashboardUiState copyState(DashboardUiState state,
                                       Boolean initialLoading,
                                       Boolean refreshing,
                                       Boolean dataStale) {
        return new DashboardUiState(
                state.getUserName(),
                state.getPhotoUrl(),
                state.getWeight(),
                state.getBmi(),
                state.getBmiCategory(),
                state.getBmiColorRes(),
                state.getGoalDisplay(),
                state.getTodayState(),
                state.getAiRecommendation(),
                state.getWeeklyPlan(),
                initialLoading != null ? initialLoading : state.isInitialLoading(),
                refreshing != null ? refreshing : state.isRefreshing(),
                dataStale != null ? dataStale : state.isDataStale()
        );
    }

    private DashboardUiState currentState() {
        DashboardUiState state = uiState.getValue();
        return state != null ? state : DashboardUiState.initial();
    }

    private void publishState(DashboardUiState state) {
        uiState.setValue(state);
    }

    private boolean isCurrentGeneration(int generation) {
        return generation == loadGeneration;
    }

    private boolean hasDashboardData(DashboardUiState state) {
        return state != null && (state.getWeight() != null
                || state.getBmi() != null
                || !state.getWeeklyPlan().isEmpty()
                || state.getAiRecommendation() != null);
    }

    private String displayNameOrDefault(User user) {
        if (user == null || user.getDisplayName() == null || user.getDisplayName().trim().isEmpty()) {
            return "Bạn";
        }
        return user.getDisplayName();
    }

    TodayState determineTodayState(List<Workout> workouts) {
        if (workouts == null || workouts.isEmpty()) {
            return TodayState.NO_PLAN;
        }

        Workout todayWorkout = findTodayWorkout(workouts);
        if (todayWorkout == null) {
            // Có kế hoạch nhưng không có mục cho hôm nay, xem như ngày nghỉ ngầm.
            return TodayState.REST_DAY;
        }

        if (todayWorkout.isRestDay()
                || DateUtils.isRestDayWorkout(todayWorkout.getTitle(), todayWorkout.getDurationMinutes())) {
            return TodayState.REST_DAY;
        }

        return TodayState.WORKOUT;
    }

    String computeAvatarLetter(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return "U";
        }
        return String.valueOf(displayName.trim().charAt(0)).toUpperCase(Locale.ROOT);
    }

    int computeBmiColor(float bmiValue) {
        if (bmiValue < 18.5f) return R.color.tertiary;
        if (bmiValue < 25.0f) return R.color.primary;
        if (bmiValue < 30.0f) return R.color.warning;
        return R.color.error;
    }

    String formatGoalDisplay(String goal) {
        GoalValue parsed = parseGoal(goal);
        if (parsed == null) {
            return PLACEHOLDER;
        }
        if (parsed.isDelta) {
            return formatSignedGoal(parsed.value);
        }
        return String.valueOf(Math.abs(parsed.value));
    }

    String formatUserGoalDisplay(User user) {
        if (user == null) {
            return PLACEHOLDER;
        }
        if (user.getTargetWeight() != null) {
            return formatWeight(user.getTargetWeight());
        }
        return formatGoalDisplay(user.getGoal());
    }

    private String formatWeight(Float value) {
        if (value == null) {
            return PLACEHOLDER;
        }
        if (Math.abs(value - Math.round(value)) < 0.05f) {
            return String.valueOf(Math.round(value));
        }
        return String.format(Locale.getDefault(), "%.1f", value);
    }

    int parseGoalWeight(String goal) {
        Integer parsed = parseGoalWeightOrNull(goal);
        return parsed != null ? parsed : 0;
    }

    Integer parseGoalWeightOrNull(String goal) {
        GoalValue parsed = parseGoal(goal);
        return parsed != null ? parsed.value : null;
    }

    private GoalValue parseGoal(String goal) {
        if (goal == null || goal.trim().isEmpty()) {
            return null;
        }

        Matcher matcher = GOAL_NUMBER_PATTERN.matcher(goal.trim());
        if (!matcher.find()) {
            return null;
        }

        String numberToken = matcher.group();
        int value;
        try {
            value = Integer.parseInt(numberToken);
        } catch (NumberFormatException ignored) {
            return null;
        }

        boolean explicitSign = numberToken.startsWith("+") || numberToken.startsWith("-");
        String normalized = normalizeGoal(goal);
        boolean targetGoal = hasTargetKeyword(normalized);
        boolean deltaGoal = explicitSign || (hasDeltaKeyword(normalized) && !targetGoal);

        if (deltaGoal && !explicitSign && containsWord(normalized, "giam")) {
            value = -Math.abs(value);
        }

        return new GoalValue(value, deltaGoal);
    }

    private String formatSignedGoal(int value) {
        if (value < 0) {
            return UNICODE_MINUS + Math.abs(value);
        }
        if (value > 0) {
            return "+" + value;
        }
        return "0";
    }

    private String normalizeGoal(String goal) {
        String decomposed = Normalizer.normalize(goal, Normalizer.Form.NFD);
        return decomposed.replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private boolean hasDeltaKeyword(String normalizedGoal) {
        return containsWord(normalizedGoal, "giam") || containsWord(normalizedGoal, "tang");
    }

    private boolean hasTargetKeyword(String normalizedGoal) {
        return containsWord(normalizedGoal, "ve")
                || containsWord(normalizedGoal, "xuong")
                || containsWord(normalizedGoal, "len")
                || containsWord(normalizedGoal, "den")
                || normalizedGoal.contains("muc tieu")
                || normalizedGoal.contains("target");
    }

    private boolean containsWord(String normalizedGoal, String word) {
        Pattern pattern = Pattern.compile("(^|\\W)" + Pattern.quote(word) + "(\\W|$)");
        return pattern.matcher(normalizedGoal).find();
    }

    private static class GoalValue {
        final int value;
        final boolean isDelta;

        GoalValue(int value, boolean isDelta) {
            this.value = value;
            this.isDelta = isDelta;
        }
    }

    private Workout findTodayWorkout(List<Workout> workouts) {
        if (workouts == null || workouts.isEmpty()) {
            return null;
        }
        int todayDow = DateUtils.getTodayDayOfWeek();
        for (Workout w : workouts) {
            if (w.getDayOfWeek() == todayDow) {
                return w;
            }
        }
        return null;
    }
}
