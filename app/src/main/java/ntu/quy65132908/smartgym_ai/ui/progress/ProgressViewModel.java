package ntu.quy65132908.smartgym_ai.ui.progress;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.ProgressEntry;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ProgressRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;
import ntu.quy65132908.smartgym_ai.util.InputValidator;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

@HiltViewModel
public class ProgressViewModel extends ViewModel {
    static final int NOTE_MAX_LENGTH = 500;

    private final Context appContext;
    private final ProgressRepository progressRepo;
    private final WorkoutRepository workoutRepo;
    private final AuthRepository authRepo;

    private final MutableLiveData<List<ProgressEntry>> entries = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Float> currentWeight = new MutableLiveData<>(0f);
    private final MutableLiveData<Float> weightChange = new MutableLiveData<>(0f);
    private final MutableLiveData<Boolean> hasWeightData = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> completedWorkouts = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> streakDays = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> totalCalories = new MutableLiveData<>(0);
    private final MutableLiveData<ProgressFormErrors> formErrors =
            new MutableLiveData<>(ProgressFormErrors.none());
    private final MutableLiveData<ProgressUiState> uiState =
            new MutableLiveData<>(ProgressUiState.initial());

    private final SingleLiveEvent<String> message = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> clearProgressFormEvent = new SingleLiveEvent<>();

    private List<ProgressEntry> currentEntries = new ArrayList<>();
    private float currentWeightValue;
    private float weightChangeValue;
    private boolean hasWeightDataValue;
    private int completedWorkoutsValue;
    private int trackingStreakDaysValue;
    private int totalCaloriesValue;
    private List<Workout> currentWorkouts = new ArrayList<>();
    private boolean loggedOut;
    private boolean loadingHistory;
    private boolean loadingWorkoutStats;
    private boolean savingProgress;
    private boolean hasAttemptedSubmit;
    private String draftWeight = "";
    private String draftBodyFat = "";
    private String draftLeanMass = "";
    private String draftNote = "";

    @Inject
    public ProgressViewModel(@ApplicationContext Context appContext,
                             ProgressRepository progressRepo,
                             WorkoutRepository workoutRepo,
                             AuthRepository authRepo) {
        this.appContext = appContext;
        this.progressRepo = progressRepo;
        this.workoutRepo = workoutRepo;
        this.authRepo = authRepo;
        publishState();
        loadProgress();
    }

    public LiveData<List<ProgressEntry>> getEntries() { return entries; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Float> getCurrentWeight() { return currentWeight; }
    public LiveData<Float> getWeightChange() { return weightChange; }
    public LiveData<Boolean> getHasWeightData() { return hasWeightData; }
    public LiveData<Integer> getCompletedWorkouts() { return completedWorkouts; }
    public LiveData<Integer> getStreakDays() { return streakDays; }
    public LiveData<Integer> getTotalCalories() { return totalCalories; }
    public LiveData<ProgressFormErrors> getFormErrors() { return formErrors; }
    public LiveData<ProgressUiState> getUiState() { return uiState; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<String> getErrorMessage() { return message; }
    public LiveData<Boolean> getClearProgressFormEvent() { return clearProgressFormEvent; }

    public void onProgressFormChanged(String weight, String bodyFat, String leanMass, String note) {
        updateDraft(weight, bodyFat, leanMass, note);
        formErrors.setValue(hasAttemptedSubmit ? buildErrors() : ProgressFormErrors.none());
    }

    public void loadProgress() {
        FirebaseUser user = authRepo.getCurrentUser();
        if (user == null) {
            loggedOut = true;
            resetProgressData();
            message.setValue(appContext.getString(R.string.progress_login_required));
            publishState();
            return;
        }

        loggedOut = false;
        String uid = user.getUid();
        loadHistory(uid);
        loadWorkoutStats(uid);
    }

    public void addProgressEntry(String weightStr, String bodyFatStr, String leanMassStr, String note) {
        updateDraft(weightStr, bodyFatStr, leanMassStr, note);
        hasAttemptedSubmit = true;
        ProgressFormErrors errors = buildErrors();
        formErrors.setValue(errors);
        if (errors.hasErrors()) {
            return;
        }

        FirebaseUser user = authRepo.getCurrentUser();
        if (user == null) {
            message.setValue(appContext.getString(R.string.progress_login_required));
            return;
        }

        ProgressEntry entry = new ProgressEntry();
        entry.setUserId(user.getUid());
        entry.setWeight(parseOptionalFloat(draftWeight));
        entry.setBodyFat(parseOptionalFloat(draftBodyFat));
        entry.setLeanMass(parseOptionalFloat(draftLeanMass));
        entry.setDate(System.currentTimeMillis());
        String sanitizedNote = InputValidator.sanitizeContent(draftNote);
        entry.setNote(sanitizedNote.isEmpty() ? null : sanitizedNote);

        setSavingProgress(true);
        progressRepo.addEntry(user.getUid(), entry, new ProgressRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                setSavingProgress(false);
                hasAttemptedSubmit = false;
                updateDraft("", "", "", "");
                formErrors.postValue(ProgressFormErrors.none());
                clearProgressFormEvent.postValue(true);
                message.postValue(appContext.getString(R.string.progress_saved));
                loadProgress();
            }

            @Override
            public void onError(Exception e) {
                setSavingProgress(false);
                message.postValue(appContext.getString(R.string.progress_save_error));
            }
        });
    }

    private void loadHistory(String uid) {
        loadingHistory = true;
        publishState();
        progressRepo.getHistory(uid, new ProgressRepository.ProgressCallback() {
            @Override
            public void onSuccess(List<ProgressEntry> list) {
                applyProgressEntries(list);
                loadingHistory = false;
                publishState();
            }

            @Override
            public void onError(Exception e) {
                loadingHistory = false;
                message.postValue(appContext.getString(R.string.progress_load_error));
                publishState();
            }
        });
    }

    private void loadWorkoutStats(String uid) {
        loadingWorkoutStats = true;
        publishState();
        workoutRepo.getWeeklyPlan(uid, new WorkoutRepository.WorkoutListCallback() {
            @Override
            public void onSuccess(List<Workout> workouts) {
                currentWorkouts = workouts != null ? new ArrayList<>(workouts) : new ArrayList<>();
                recalculateWorkoutStats();
                loadingWorkoutStats = false;
                publishState();
            }

            @Override
            public void onError(Exception e) {
                currentWorkouts = new ArrayList<>();
                completedWorkoutsValue = 0;
                totalCaloriesValue = 0;
                loadingWorkoutStats = false;
                publishState();
            }
        });
    }

    private void applyProgressEntries(List<ProgressEntry> rawEntries) {
        currentEntries = sortEntriesNewestFirst(rawEntries);
        hasWeightDataValue = !currentEntries.isEmpty();
        if (!hasWeightDataValue) {
            currentWeightValue = 0f;
            weightChangeValue = 0f;
            trackingStreakDaysValue = 0;
            return;
        }

        currentWeightValue = currentEntries.get(0).getWeight();
        weightChangeValue = calculateWeightChange(currentEntries);
        trackingStreakDaysValue = calculateStreak(currentEntries);
        recalculateWorkoutStats();
    }

    private void resetProgressData() {
        currentEntries = new ArrayList<>();
        currentWeightValue = 0f;
        weightChangeValue = 0f;
        hasWeightDataValue = false;
        completedWorkoutsValue = 0;
        trackingStreakDaysValue = 0;
        totalCaloriesValue = 0;
        currentWorkouts = new ArrayList<>();
        loadingHistory = false;
        loadingWorkoutStats = false;
        savingProgress = false;
    }

    private ProgressFormErrors buildErrors() {
        String weightError = null;
        String bodyFatError = null;
        String leanMassError = null;
        String noteError = null;

        Float parsedWeight = parseOptionalFloat(draftWeight);
        if (isBlank(draftWeight) || parsedWeight == null) {
            weightError = appContext.getString(R.string.progress_weight_invalid);
        } else if (parsedWeight < 20f || parsedWeight > 350f) {
            weightError = appContext.getString(R.string.progress_weight_range_error);
        }

        Float parsedBodyFat = parseOptionalFloat(draftBodyFat);
        if (!isBlank(draftBodyFat)) {
            if (parsedBodyFat == null) {
                bodyFatError = appContext.getString(R.string.progress_body_fat_invalid);
            } else if (parsedBodyFat < 0f || parsedBodyFat > 80f) {
                bodyFatError = appContext.getString(R.string.progress_body_fat_range_error);
            }
        }

        Float parsedLeanMass = parseOptionalFloat(draftLeanMass);
        if (!isBlank(draftLeanMass)) {
            if (parsedLeanMass == null) {
                leanMassError = appContext.getString(R.string.progress_lean_mass_invalid);
            } else if (parsedLeanMass < 0f || parsedLeanMass > 250f) {
                leanMassError = appContext.getString(R.string.progress_lean_mass_range_error);
            } else if (parsedWeight != null && parsedLeanMass > parsedWeight) {
                leanMassError = appContext.getString(R.string.progress_lean_mass_over_weight_error);
            }
        }

        if (draftNote != null && draftNote.trim().length() > NOTE_MAX_LENGTH) {
            noteError = appContext.getString(R.string.progress_note_length_error);
        }

        return new ProgressFormErrors(weightError, bodyFatError, leanMassError, noteError);
    }

    float calculateWeightChange(List<ProgressEntry> entries) {
        List<ProgressEntry> sorted = sortEntriesNewestFirst(entries);
        if (sorted.size() < 2) {
            return 0f;
        }
        return sorted.get(0).getWeight() - sorted.get(1).getWeight();
    }

    int calculateStreak(List<ProgressEntry> entries) {
        List<ProgressEntry> sorted = sortEntriesNewestFirst(entries);
        if (sorted.isEmpty()) {
            return 0;
        }

        List<Long> distinctDays = new ArrayList<>();
        for (ProgressEntry entry : sorted) {
            long dayStart = startOfDay(entry.getDate());
            if (distinctDays.isEmpty() || !distinctDays.get(distinctDays.size() - 1).equals(dayStart)) {
                distinctDays.add(dayStart);
            }
        }

        int streak = 1;
        long expectedPreviousDay = previousDayStart(distinctDays.get(0));
        for (int i = 1; i < distinctDays.size(); i++) {
            if (distinctDays.get(i) == expectedPreviousDay) {
                streak++;
                expectedPreviousDay = previousDayStart(expectedPreviousDay);
            } else {
                break;
            }
        }
        return streak;
    }

    static List<ProgressEntry> sortEntriesNewestFirst(List<ProgressEntry> rawEntries) {
        List<ProgressEntry> sorted = new ArrayList<>();
        if (rawEntries != null) {
            for (ProgressEntry entry : rawEntries) {
                if (entry != null) {
                    sorted.add(entry);
                }
            }
        }
        sorted.sort((a, b) -> Long.compare(b.getDate(), a.getDate()));
        return sorted;
    }

    Float parseOptionalFloat(String raw) {
        if (isBlank(raw)) {
            return null;
        }
        try {
            float parsed = Float.parseFloat(raw.trim().replace(',', '.'));
            if (Float.isNaN(parsed) || Float.isInfinite(parsed)) {
                return null;
            }
            return parsed;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int estimateCalories(Workout workout) {
        int duration = Math.max(0, workout.getDurationMinutes());
        float weightForEstimate = currentWeightValue > 0f ? currentWeightValue : 70f;
        float met = metForWorkout(workout);
        return Math.round(met * 3.5f * weightForEstimate / 200f * duration);
    }

    private void recalculateWorkoutStats() {
        completedWorkoutsValue = 0;
        totalCaloriesValue = 0;
        for (Workout workout : currentWorkouts) {
            if (workout != null && workout.isCompleted()) {
                completedWorkoutsValue++;
                totalCaloriesValue += estimateCalories(workout);
            }
        }
    }

    private float metForWorkout(Workout workout) {
        String intensity = workout.getIntensity();
        if (intensity == null) {
            return 6.5f;
        }
        String normalized = intensity.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("high") || normalized.contains("nặng") || normalized.contains("mạnh")) {
            return 8f;
        }
        if (normalized.contains("low")
                || normalized.contains("nhẹ")
                || normalized.contains("recovery")
                || normalized.contains("phục hồi")) {
            return 4f;
        }
        if (normalized.contains("medium") || normalized.contains("trung")) {
            return 6f;
        }
        return 6.5f;
    }

    private void setSavingProgress(boolean saving) {
        savingProgress = saving;
        publishState();
    }

    private void updateDraft(String weight, String bodyFat, String leanMass, String note) {
        draftWeight = weight != null ? weight : "";
        draftBodyFat = bodyFat != null ? bodyFat : "";
        draftLeanMass = leanMass != null ? leanMass : "";
        draftNote = note != null ? note : "";
    }

    private void publishState() {
        boolean loading = loadingHistory
                || loadingWorkoutStats
                || savingProgress;
        List<ProgressEntry> entriesSnapshot = new ArrayList<>(currentEntries);
        entries.postValue(entriesSnapshot);
        isLoading.postValue(loading);
        currentWeight.postValue(currentWeightValue);
        weightChange.postValue(weightChangeValue);
        hasWeightData.postValue(hasWeightDataValue);
        completedWorkouts.postValue(completedWorkoutsValue);
        streakDays.postValue(trackingStreakDaysValue);
        totalCalories.postValue(totalCaloriesValue);
        uiState.postValue(new ProgressUiState(
                entriesSnapshot,
                currentWeightValue,
                weightChangeValue,
                hasWeightDataValue,
                completedWorkoutsValue,
                trackingStreakDaysValue,
                totalCaloriesValue,
                loading,
                savingProgress,
                loggedOut
        ));
    }

    private static long startOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static long previousDayStart(long dayStart) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dayStart);
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        return calendar.getTimeInMillis();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
