package ntu.quy65132908.smartgym_ai.ui.dashboard;

import java.util.Collections;
import java.util.List;

import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Workout;

/**
 * Model render duy nhất cho màn hình Dashboard.
 */
public class DashboardUiState {
    private final String userName;
    private final String photoUrl;
    private final Integer weight;
    private final Float bmi;
    private final String bmiCategory;
    private final int bmiColorRes;
    private final String goalDisplay;
    private final TodayState todayState;
    private final Workout aiRecommendation;
    private final List<Workout> weeklyPlan;
    private final boolean initialLoading;
    private final boolean refreshing;
    private final boolean dataStale;

    public DashboardUiState(String userName,
                            String photoUrl,
                            Integer weight,
                            Float bmi,
                            String bmiCategory,
                            int bmiColorRes,
                            String goalDisplay,
                            TodayState todayState,
                            Workout aiRecommendation,
                            List<Workout> weeklyPlan,
                            boolean initialLoading,
                            boolean refreshing,
                            boolean dataStale) {
        this.userName = userName;
        this.photoUrl = photoUrl;
        this.weight = weight;
        this.bmi = bmi;
        this.bmiCategory = bmiCategory;
        this.bmiColorRes = bmiColorRes;
        this.goalDisplay = goalDisplay;
        this.todayState = todayState;
        this.aiRecommendation = aiRecommendation;
        this.weeklyPlan = weeklyPlan != null ? weeklyPlan : Collections.emptyList();
        this.initialLoading = initialLoading;
        this.refreshing = refreshing;
        this.dataStale = dataStale;
    }

    public static DashboardUiState initial() {
        return new DashboardUiState(
                "Bạn",
                "",
                null,
                null,
                "",
                R.color.on_surface_variant,
                "--",
                TodayState.NO_PLAN,
                null,
                Collections.emptyList(),
                true,
                false,
                false
        );
    }

    public String getUserName() { return userName; }
    public String getPhotoUrl() { return photoUrl; }
    public Integer getWeight() { return weight; }
    public Float getBmi() { return bmi; }
    public String getBmiCategory() { return bmiCategory; }
    public int getBmiColorRes() { return bmiColorRes; }
    public String getGoalDisplay() { return goalDisplay; }
    public TodayState getTodayState() { return todayState; }
    public Workout getAiRecommendation() { return aiRecommendation; }
    public List<Workout> getWeeklyPlan() { return weeklyPlan; }
    public boolean isInitialLoading() { return initialLoading; }
    public boolean isRefreshing() { return refreshing; }
    public boolean isDataStale() { return dataStale; }
}
