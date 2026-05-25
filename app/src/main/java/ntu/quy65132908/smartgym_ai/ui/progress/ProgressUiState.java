package ntu.quy65132908.smartgym_ai.ui.progress;

import java.util.ArrayList;
import java.util.List;

import ntu.quy65132908.smartgym_ai.data.model.ProgressEntry;

public class ProgressUiState {
    private final List<ProgressEntry> entries;
    private final float currentWeight;
    private final float weightChange;
    private final boolean hasWeightData;
    private final int completedWorkouts;
    private final int trackingStreakDays;
    private final int totalCalories;
    private final boolean loading;
    private final boolean savingProgress;
    private final boolean loggedOut;

    public ProgressUiState(List<ProgressEntry> entries,
                           float currentWeight,
                           float weightChange,
                           boolean hasWeightData,
                           int completedWorkouts,
                           int trackingStreakDays,
                           int totalCalories,
                           boolean loading,
                           boolean savingProgress,
                           boolean loggedOut) {
        this.entries = entries != null ? new ArrayList<>(entries) : new ArrayList<>();
        this.currentWeight = currentWeight;
        this.weightChange = weightChange;
        this.hasWeightData = hasWeightData;
        this.completedWorkouts = completedWorkouts;
        this.trackingStreakDays = trackingStreakDays;
        this.totalCalories = totalCalories;
        this.loading = loading;
        this.savingProgress = savingProgress;
        this.loggedOut = loggedOut;
    }

    public List<ProgressEntry> getEntries() { return new ArrayList<>(entries); }
    public float getCurrentWeight() { return currentWeight; }
    public float getWeightChange() { return weightChange; }
    public boolean hasWeightData() { return hasWeightData; }
    public int getCompletedWorkouts() { return completedWorkouts; }
    public int getTrackingStreakDays() { return trackingStreakDays; }
    public int getTotalCalories() { return totalCalories; }
    public boolean isLoading() { return loading; }
    public boolean isSavingProgress() { return savingProgress; }
    public boolean isLoggedOut() { return loggedOut; }

    public static ProgressUiState initial() {
        return new ProgressUiState(
                new ArrayList<>(),
                0f,
                0f,
                false,
                0,
                0,
                0,
                false,
                false,
                false
        );
    }
}
