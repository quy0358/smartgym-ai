package ntu.quy65132908.smartgym_ai.ui.progress;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.model.ProgressEntry;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ProgressRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;
import ntu.quy65132908.smartgym_ai.util.InputValidator;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

@HiltViewModel
public class ProgressViewModel extends ViewModel {
    private final ProgressRepository progressRepo;
    private final WorkoutRepository workoutRepo;
    private final AuthRepository authRepo;
    private final UserRepository userRepo;
    private final FirebaseStorage storage;

    private final MutableLiveData<List<ProgressEntry>> entries = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // C2: Dynamic weight data
    private final MutableLiveData<Float> currentWeight = new MutableLiveData<>(0f);
    private final MutableLiveData<Float> weightChange = new MutableLiveData<>(0f);
    private final MutableLiveData<Boolean> hasWeightData = new MutableLiveData<>(false);

    // C4: Calculated stats
    private final MutableLiveData<Integer> completedWorkouts = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> streakDays = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> totalCalories = new MutableLiveData<>(0);

    // H6: Body Photos
    private final MutableLiveData<String> beforePhotoUrl = new MutableLiveData<>("");
    private final MutableLiveData<String> afterPhotoUrl = new MutableLiveData<>("");

    // H8: Error handling
    private final SingleLiveEvent<String> errorMessage = new SingleLiveEvent<>();

    public LiveData<List<ProgressEntry>> getEntries() { return entries; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Float> getCurrentWeight() { return currentWeight; }
    public LiveData<Float> getWeightChange() { return weightChange; }
    public LiveData<Boolean> getHasWeightData() { return hasWeightData; }
    public LiveData<Integer> getCompletedWorkouts() { return completedWorkouts; }
    public LiveData<Integer> getStreakDays() { return streakDays; }
    public LiveData<Integer> getTotalCalories() { return totalCalories; }
    public LiveData<String> getBeforePhotoUrl() { return beforePhotoUrl; }
    public LiveData<String> getAfterPhotoUrl() { return afterPhotoUrl; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    @Inject
    public ProgressViewModel(ProgressRepository progressRepo,
                             WorkoutRepository workoutRepo,
                             AuthRepository authRepo,
                             UserRepository userRepo,
                             FirebaseStorage storage) {
        this.progressRepo = progressRepo;
        this.workoutRepo = workoutRepo;
        this.authRepo = authRepo;
        this.userRepo = userRepo;
        this.storage = storage;
        loadProgress();
    }

    public void loadProgress() {
        FirebaseUser u = authRepo.getCurrentUser();
        if (u == null) return;
        String uid = u.getUid();
        isLoading.setValue(true);

        userRepo.getUser(uid, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                beforePhotoUrl.postValue(user.getBeforePhotoUrl() != null ? user.getBeforePhotoUrl() : "");
                afterPhotoUrl.postValue(user.getAfterPhotoUrl() != null ? user.getAfterPhotoUrl() : "");
            }

            @Override
            public void onError(Exception e) {
                beforePhotoUrl.postValue("");
                afterPhotoUrl.postValue("");
            }
        });

        progressRepo.getHistory(uid, new ProgressRepository.ProgressCallback() {
            @Override
            public void onSuccess(List<ProgressEntry> list) {
                isLoading.postValue(false);
                entries.postValue(list);

                if (list != null && !list.isEmpty()) {
                    hasWeightData.postValue(true);

                    // C2: Current weight from latest entry
                    float latestWeight = list.get(0).getWeight();
                    currentWeight.postValue(latestWeight);

                    // C2: Weight change — compare with entry ~30 days ago
                    float change = calculateWeightChange(list);
                    weightChange.postValue(change);

                    // C4: Streak calculation
                    int streak = calculateStreak(list);
                    streakDays.postValue(streak);
                } else {
                    hasWeightData.postValue(false);
                    currentWeight.postValue(0f);
                    weightChange.postValue(0f);
                    streakDays.postValue(0);
                }
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                errorMessage.postValue("Không thể tải tiến trình. Thử lại sau.");
            }
        });

        // C4: Load workout stats for completed count + calories estimate
        loadWorkoutStats(uid);
    }

    private void loadWorkoutStats(String uid) {
        workoutRepo.getWeeklyPlan(uid, new WorkoutRepository.WorkoutListCallback() {
            @Override
            public void onSuccess(List<Workout> workouts) {
                if (workouts != null) {
                    int completed = 0;
                    int calories = 0;
                    for (Workout w : workouts) {
                        if (w.isCompleted()) {
                            completed++;
                            // Estimate: ~7 calories per minute of exercise
                            calories += w.getDurationMinutes() * 7;
                        }
                    }
                    completedWorkouts.postValue(completed);
                    totalCalories.postValue(calories);
                }
            }

            @Override
            public void onError(Exception e) {
                completedWorkouts.postValue(0);
                totalCalories.postValue(0);
            }
        });
    }

    /**
     * Compare latest weight with oldest entry to calculate change.
     * Entries are sorted descending by date.
     */
    float calculateWeightChange(List<ProgressEntry> entries) {
        if (entries == null || entries.size() < 2) return 0f;
        float latest = entries.get(0).getWeight();
        float oldest = entries.get(entries.size() - 1).getWeight();
        return latest - oldest;
    }

    /**
     * Calculate consecutive days with entries (sorted descending by date).
     */
    int calculateStreak(List<ProgressEntry> entries) {
        if (entries == null || entries.isEmpty()) return 0;

        int streak = 1;
        long oneDayMs = 24 * 60 * 60 * 1000L;

        for (int i = 0; i < entries.size() - 1; i++) {
            long diff = entries.get(i).getDate() - entries.get(i + 1).getDate();
            if (diff >= oneDayMs / 2 && diff <= oneDayMs * 3 / 2) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    public void uploadBeforePhoto(Uri uri) {
        uploadPhoto(uri, "before");
    }

    public void uploadAfterPhoto(Uri uri) {
        uploadPhoto(uri, "after");
    }

    public void addProgressEntry(String weightStr, String bodyFatStr, String leanMassStr, String note) {
        FirebaseUser user = authRepo.getCurrentUser();
        if (user == null) {
            errorMessage.setValue("Bạn cần đăng nhập để lưu tiến trình.");
            return;
        }

        Float weight = parseOptionalFloat(weightStr);
        if (weight == null) {
            errorMessage.setValue("Vui lòng nhập cân nặng");
            return;
        }
        if (weight < 20f || weight > 350f) {
            errorMessage.setValue("Cân nặng phải từ 20 đến 350 kg");
            return;
        }

        Float bodyFat = parseOptionalFloat(bodyFatStr);
        if (bodyFat != null && (bodyFat < 0f || bodyFat > 80f)) {
            errorMessage.setValue("Tỷ lệ mỡ phải từ 0 đến 80%");
            return;
        }

        Float leanMass = parseOptionalFloat(leanMassStr);
        if (leanMass != null && (leanMass < 0f || leanMass > 250f)) {
            errorMessage.setValue("Lean mass phải từ 0 đến 250 kg");
            return;
        }

        ProgressEntry entry = new ProgressEntry();
        entry.setUserId(user.getUid());
        entry.setWeight(weight);
        entry.setBodyFat(bodyFat);
        entry.setLeanMass(leanMass);
        entry.setDate(System.currentTimeMillis());
        String sanitizedNote = InputValidator.sanitizeContent(note);
        entry.setNote(sanitizedNote.isEmpty() ? null : sanitizedNote);

        isLoading.setValue(true);
        progressRepo.addEntry(user.getUid(), entry, new ProgressRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                isLoading.postValue(false);
                loadProgress();
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                errorMessage.postValue("Không thể lưu tiến trình. Thử lại sau.");
            }
        });
    }

    private Float parseOptionalFloat(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void uploadPhoto(Uri uri, String type) {
        FirebaseUser u = authRepo.getCurrentUser();
        if (u == null) return;
        String uid = u.getUid();
        isLoading.setValue(true);

        StorageReference ref = storage.getReference().child("users/" + uid + "/" + type + "_photo.jpg");
        ref.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    String url = downloadUri.toString();
                    // Update user in Firestore
                    userRepo.getUser(uid, new UserRepository.UserCallback() {
                        @Override
                        public void onSuccess(User userData) {
                            if ("before".equals(type)) {
                                userData.setBeforePhotoUrl(url);
                            } else {
                                userData.setAfterPhotoUrl(url);
                            }
                            userRepo.updateUser(uid, userData, new UserRepository.SimpleCallback() {
                                @Override
                                public void onSuccess() {
                                    isLoading.postValue(false);
                                    if ("before".equals(type)) {
                                        beforePhotoUrl.postValue(url);
                                    } else {
                                        afterPhotoUrl.postValue(url);
                                    }
                                }

                                @Override
                                public void onError(Exception e) {
                                    isLoading.postValue(false);
                                    errorMessage.postValue("Lỗi lưu ảnh vào CSDL: " + e.getMessage());
                                }
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            isLoading.postValue(false);
                            errorMessage.postValue("Lỗi tải thông tin user: " + e.getMessage());
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    isLoading.postValue(false);
                    errorMessage.postValue("Lỗi tải ảnh lên: " + e.getMessage());
                });
    }
}
