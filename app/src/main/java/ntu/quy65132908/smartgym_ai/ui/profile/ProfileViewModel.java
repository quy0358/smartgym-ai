package ntu.quy65132908.smartgym_ai.ui.profile;

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
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;
import ntu.quy65132908.smartgym_ai.util.InputValidator;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final Context appContext;
    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final WorkoutRepository workoutRepository;
    private final ProgressRepository progressRepository;

    private final MutableLiveData<String> displayName = new MutableLiveData<>("");
    private final MutableLiveData<String> email = new MutableLiveData<>("");
    private final MutableLiveData<Integer> totalWorkouts = new MutableLiveData<>(0);
    private final MutableLiveData<Float> totalHours = new MutableLiveData<>(0f);
    private final MutableLiveData<Integer> streakDays = new MutableLiveData<>(0);
    private final MutableLiveData<String> photoUrl = new MutableLiveData<>("");
    private final MutableLiveData<ProfileUiState> uiState = new MutableLiveData<>(ProfileUiState.initial());

    private final SingleLiveEvent<Boolean> signedOut = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> message = new SingleLiveEvent<>();

    private String displayNameValue = "";
    private String emailValue = "";
    private String photoUrlValue = "";
    private int totalWorkoutsValue;
    private float totalHoursValue;
    private int streakDaysValue;
    private boolean loggedOut;
    private boolean loadingProfile;
    private boolean loadingWorkoutStats;
    private boolean loadingProgressStats;
    private boolean profileLoadFailed;
    private boolean workoutStatsLoadFailed;
    private boolean progressStatsLoadFailed;

    public LiveData<String> getDisplayName() { return displayName; }
    public LiveData<String> getEmail() { return email; }
    public LiveData<Boolean> getSignedOut() { return signedOut; }
    public LiveData<Integer> getTotalWorkouts() { return totalWorkouts; }
    public LiveData<Float> getTotalHours() { return totalHours; }
    public LiveData<Integer> getStreakDays() { return streakDays; }
    public LiveData<String> getPhotoUrl() { return photoUrl; }
    public LiveData<ProfileUiState> getUiState() { return uiState; }
    public LiveData<String> getMessage() { return message; }

    @Inject
    public ProfileViewModel(@ApplicationContext Context appContext,
                            AuthRepository authRepository,
                            UserRepository userRepository,
                            WorkoutRepository workoutRepository,
                            ProgressRepository progressRepository) {
        this.appContext = appContext;
        this.authRepository = authRepository;
        this.userRepository = userRepository;
        this.workoutRepository = workoutRepository;
        this.progressRepository = progressRepository;
        loadProfile();
    }

    public void loadProfile() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            loggedOut = true;
            displayNameValue = appContext.getString(R.string.post_user_default);
            emailValue = "";
            photoUrlValue = "";
            totalWorkoutsValue = 0;
            totalHoursValue = 0f;
            streakDaysValue = 0;
            loadingProfile = false;
            loadingWorkoutStats = false;
            loadingProgressStats = false;
            message.setValue(appContext.getString(R.string.profile_login_required));
            publishState();
            return;
        }

        loggedOut = false;
        profileLoadFailed = false;
        workoutStatsLoadFailed = false;
        progressStatsLoadFailed = false;
        emailValue = user.getEmail() != null ? user.getEmail() : "";
        displayNameValue = fallbackName(user);
        photoUrlValue = "";
        String uid = user.getUid();

        loadUserProfile(uid, user);
        loadWorkoutStats(uid);
        loadStreakFromProgress(uid);
        publishState();
    }

    private void loadUserProfile(String uid, FirebaseUser firebaseUser) {
        loadingProfile = true;
        publishState();
        userRepository.getUser(uid, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(ntu.quy65132908.smartgym_ai.data.model.User userData) {
                String resolvedName = userData != null
                        ? InputValidator.sanitizeName(userData.getDisplayName())
                        : "";
                displayNameValue = !resolvedName.isEmpty() ? resolvedName : fallbackName(firebaseUser);
                photoUrlValue = userData != null && userData.getPhotoUrl() != null
                        ? userData.getPhotoUrl()
                        : "";
                profileLoadFailed = false;
                loadingProfile = false;
                publishState();
            }

            @Override
            public void onError(Exception e) {
                displayNameValue = fallbackName(firebaseUser);
                profileLoadFailed = true;
                loadingProfile = false;
                message.postValue(appContext.getString(R.string.profile_load_error));
                publishState();
            }
        });
    }

    private void loadWorkoutStats(String uid) {
        loadingWorkoutStats = true;
        publishState();
        workoutRepository.getWeeklyPlan(uid, new WorkoutRepository.WorkoutListCallback() {
            @Override
            public void onSuccess(List<Workout> workouts) {
                totalWorkoutsValue = 0;
                int totalMinutes = 0;
                if (workouts != null) {
                    for (Workout workout : workouts) {
                        if (workout != null && workout.isCompleted()) {
                            totalWorkoutsValue++;
                            totalMinutes += Math.max(0, workout.getDurationMinutes());
                        }
                    }
                }
                totalHoursValue = totalMinutes / 60f;
                workoutStatsLoadFailed = false;
                loadingWorkoutStats = false;
                publishState();
            }

            @Override
            public void onError(Exception e) {
                totalWorkoutsValue = 0;
                totalHoursValue = 0f;
                workoutStatsLoadFailed = true;
                loadingWorkoutStats = false;
                message.postValue(appContext.getString(R.string.profile_stats_load_error));
                publishState();
            }
        });
    }

    private void loadStreakFromProgress(String uid) {
        loadingProgressStats = true;
        publishState();
        progressRepository.getHistory(uid, new ProgressRepository.ProgressCallback() {
            @Override
            public void onSuccess(List<ProgressEntry> entries) {
                streakDaysValue = calculateStreak(entries);
                progressStatsLoadFailed = false;
                loadingProgressStats = false;
                publishState();
            }

            @Override
            public void onError(Exception e) {
                streakDaysValue = 0;
                progressStatsLoadFailed = true;
                loadingProgressStats = false;
                message.postValue(appContext.getString(R.string.profile_stats_load_error));
                publishState();
            }
        });
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

    String formatHours(float hours) {
        if (Math.abs(hours - Math.round(hours)) < 0.05f) {
            return String.valueOf(Math.round(hours));
        }
        return String.format(Locale.getDefault(), "%.1f", hours);
    }

    public void signOut() {
        authRepository.signOut();
        signedOut.setValue(true);
    }

    private void publishState() {
        boolean loading = loadingProfile || loadingWorkoutStats || loadingProgressStats;
        boolean statsLoadFailed = workoutStatsLoadFailed || progressStatsLoadFailed;
        displayName.postValue(displayNameValue);
        email.postValue(emailValue);
        photoUrl.postValue(photoUrlValue);
        totalWorkouts.postValue(totalWorkoutsValue);
        totalHours.postValue(totalHoursValue);
        streakDays.postValue(streakDaysValue);
        uiState.postValue(new ProfileUiState(
                displayNameValue,
                emailValue,
                photoUrlValue,
                totalWorkoutsValue,
                totalHoursValue,
                streakDaysValue,
                loading,
                loggedOut,
                profileLoadFailed,
                statsLoadFailed
        ));
    }

    private String fallbackName(FirebaseUser user) {
        String name = InputValidator.sanitizeName(user.getDisplayName());
        if (!name.isEmpty()) {
            return name;
        }
        String userEmail = user.getEmail();
        if (userEmail != null && !userEmail.trim().isEmpty()) {
            return userEmail;
        }
        return appContext.getString(R.string.post_user_default);
    }

    private static List<ProgressEntry> sortEntriesNewestFirst(List<ProgressEntry> entries) {
        List<ProgressEntry> sorted = new ArrayList<>();
        if (entries != null) {
            for (ProgressEntry entry : entries) {
                if (entry != null) {
                    sorted.add(entry);
                }
            }
        }
        sorted.sort((a, b) -> Long.compare(b.getDate(), a.getDate()));
        return sorted;
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
}
