package ntu.quy65132908.smartgym_ai.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import ntu.quy65132908.smartgym_ai.data.model.ProgressEntry;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ProgressRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final WorkoutRepository workoutRepository;
    private final ProgressRepository progressRepository;

    private final MutableLiveData<String> displayName = new MutableLiveData<>("Người dùng");
    private final MutableLiveData<String> email = new MutableLiveData<>("");
    private final SingleLiveEvent<Boolean> signedOut = new SingleLiveEvent<>();

    // Stats from Firestore
    private final MutableLiveData<Integer> totalWorkouts = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> totalHours = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> streakDays = new MutableLiveData<>(0);

    // H5: Avatar photo URL
    private final MutableLiveData<String> photoUrl = new MutableLiveData<>("");

    public LiveData<String> getDisplayName() { return displayName; }
    public LiveData<String> getEmail() { return email; }
    public LiveData<Boolean> getSignedOut() { return signedOut; }
    public LiveData<Integer> getTotalWorkouts() { return totalWorkouts; }
    public LiveData<Integer> getTotalHours() { return totalHours; }
    public LiveData<Integer> getStreakDays() { return streakDays; }
    public LiveData<String> getPhotoUrl() { return photoUrl; }

    @Inject
    public ProfileViewModel(AuthRepository authRepository,
                            UserRepository userRepository,
                            WorkoutRepository workoutRepository,
                            ProgressRepository progressRepository) {
        this.authRepository = authRepository;
        this.userRepository = userRepository;
        this.workoutRepository = workoutRepository;
        this.progressRepository = progressRepository;
        loadProfile();
    }

    private void loadProfile() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user != null) {
            email.setValue(user.getEmail());
            String uid = user.getUid();

            userRepository.getUser(uid, new UserRepository.UserCallback() {
                @Override
                public void onSuccess(ntu.quy65132908.smartgym_ai.data.model.User userData) {
                    displayName.postValue(userData.getDisplayName());
                    if (userData.getPhotoUrl() != null) {
                        photoUrl.postValue(userData.getPhotoUrl());
                    }
                }

                @Override
                public void onError(Exception e) {
                    displayName.postValue(user.getEmail());
                }
            });

            loadWorkoutStats(uid);
            loadStreakFromProgress(uid);
        }
    }

    private void loadWorkoutStats(String uid) {
        workoutRepository.getWeeklyPlan(uid, new WorkoutRepository.WorkoutListCallback() {
            @Override
            public void onSuccess(List<Workout> workouts) {
                if (workouts != null) {
                    int completedCount = 0;
                    int totalMinutes = 0;
                    for (Workout w : workouts) {
                        if (w.isCompleted()) {
                            completedCount++;
                            totalMinutes += w.getDurationMinutes();
                        }
                    }
                    totalWorkouts.postValue(completedCount);
                    totalHours.postValue(totalMinutes / 60);
                }
            }

            @Override
            public void onError(Exception e) {
                totalWorkouts.postValue(0);
                totalHours.postValue(0);
            }
        });
    }

    private void loadStreakFromProgress(String uid) {
        progressRepository.getHistory(uid, new ProgressRepository.ProgressCallback() {
            @Override
            public void onSuccess(List<ProgressEntry> entries) {
                if (entries != null && !entries.isEmpty()) {
                    int streak = calculateStreak(entries);
                    streakDays.postValue(streak);
                } else {
                    streakDays.postValue(0);
                }
            }

            @Override
            public void onError(Exception e) {
                streakDays.postValue(0);
            }
        });
    }

    /**
     * Calculate consecutive days with progress entries (sorted descending by date).
     */
    int calculateStreak(List<ProgressEntry> entries) {
        if (entries == null || entries.isEmpty()) return 0;

        int streak = 1;
        long oneDayMs = 24 * 60 * 60 * 1000L;

        for (int i = 0; i < entries.size() - 1; i++) {
            long diff = entries.get(i).getDate() - entries.get(i + 1).getDate();
            // Allow a tolerance of 0.5 to 1.5 days gap for "consecutive"
            if (diff >= oneDayMs / 2 && diff <= oneDayMs * 3 / 2) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    public void signOut() {
        authRepository.signOut();
        signedOut.setValue(true);
    }
}
