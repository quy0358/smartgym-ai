package ntu.quy65132908.smartgym_ai.ui.analysis;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.model.InjuryProfile;
import ntu.quy65132908.smartgym_ai.data.model.ProgressEntry;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.DeepSeekRepository;
import ntu.quy65132908.smartgym_ai.data.repository.InjuryProfileRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ProgressRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;

@HiltViewModel
public class AIAnalysisViewModel extends ViewModel {
    private static final String PLACEHOLDER = "--";
    private static final int EXERCISE_NAME_MIN = 2;
    private static final int EXERCISE_NAME_MAX = 80;
    private static final int FORM_DESCRIPTION_MIN = 20;
    private static final int FORM_DESCRIPTION_MAX = 1000;
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat(
            "0.#",
            DecimalFormatSymbols.getInstance(Locale.US)
    );

    private final DeepSeekRepository deepSeekRepo;
    private final UserRepository userRepo;
    private final InjuryProfileRepository injuryProfileRepository;
    private final ProgressRepository progressRepo;
    private final AuthRepository authRepo;

    private final MutableLiveData<BodyMetricsUiState> bodyMetrics =
            new MutableLiveData<>(BodyMetricsUiState.empty());
    private final MutableLiveData<Boolean> canGeneratePlan = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isProfileLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> planResponse = new MutableLiveData<>();
    private final MutableLiveData<String> formResponse = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPlanLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isFormLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> planError = new MutableLiveData<>();
    private final MutableLiveData<String> formError = new MutableLiveData<>();
    private final MutableLiveData<String> exerciseNameError = new MutableLiveData<>();
    private final MutableLiveData<String> formDescriptionError = new MutableLiveData<>();

    // Trạng thái tổng hợp cũ được giữ cho bộ quan sát và kiểm thử hiện có.
    private final MutableLiveData<String> aiResponse = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMsg = new MutableLiveData<>();
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private User loadedUser;

    public LiveData<BodyMetricsUiState> getBodyMetrics() { return bodyMetrics; }
    public LiveData<Boolean> getCanGeneratePlan() { return canGeneratePlan; }
    public LiveData<Boolean> getIsProfileLoading() { return isProfileLoading; }
    public LiveData<String> getPlanResponse() { return planResponse; }
    public LiveData<String> getFormResponse() { return formResponse; }
    public LiveData<Boolean> getIsPlanLoading() { return isPlanLoading; }
    public LiveData<Boolean> getIsFormLoading() { return isFormLoading; }
    public LiveData<String> getPlanError() { return planError; }
    public LiveData<String> getFormError() { return formError; }
    public LiveData<String> getExerciseNameError() { return exerciseNameError; }
    public LiveData<String> getFormDescriptionError() { return formDescriptionError; }
    public LiveData<String> getAiResponse() { return aiResponse; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMsg() { return errorMsg; }

    public void reloadProfile() {
        loadUser();
    }

    @Inject
    public AIAnalysisViewModel(DeepSeekRepository deepSeekRepo,
                               UserRepository userRepo,
                               InjuryProfileRepository injuryProfileRepository,
                               ProgressRepository progressRepo,
                               AuthRepository authRepo) {
        this.deepSeekRepo = deepSeekRepo;
        this.userRepo = userRepo;
        this.injuryProfileRepository = injuryProfileRepository;
        this.progressRepo = progressRepo;
        this.authRepo = authRepo;
        loadUser();
    }

    private void loadUser() {
        FirebaseUser fb = authRepo.getCurrentUser();
        if (fb == null) {
            bodyMetrics.setValue(BodyMetricsUiState.empty());
            canGeneratePlan.setValue(false);
            isProfileLoading.setValue(false);
            return;
        }

        isProfileLoading.setValue(true);
        canGeneratePlan.setValue(false);
        userRepo.getUser(fb.getUid(), new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                loadedUser = user;
                currentUser.postValue(user);
                bodyMetrics.postValue(buildBodyMetrics(user, null));
                loadLatestProgress(fb.getUid(), user);
            }

            @Override
            public void onError(Exception e) {
                isProfileLoading.postValue(false);
                canGeneratePlan.postValue(false);
                bodyMetrics.postValue(BodyMetricsUiState.empty());
                planError.postValue("Không thể tải hồ sơ người dùng.");
                errorMsg.postValue("Không thể tải hồ sơ người dùng.");
            }
        });
    }

    private void loadLatestProgress(String uid, User user) {
        progressRepo.getHistory(uid, new ProgressRepository.ProgressCallback() {
            @Override
            public void onSuccess(List<ProgressEntry> entries) {
                ProgressEntry latest = entries != null && !entries.isEmpty() ? entries.get(0) : null;
                bodyMetrics.postValue(buildBodyMetrics(user, latest));
                isProfileLoading.postValue(false);
                canGeneratePlan.postValue(true);
            }

            @Override
            public void onError(Exception e) {
                bodyMetrics.postValue(buildBodyMetrics(user, null));
                isProfileLoading.postValue(false);
                canGeneratePlan.postValue(true);
            }
        });
    }

    public void generateWorkoutPlan() {
        User user = loadedUser != null ? loadedUser : currentUser.getValue();
        clearPlanState();
        if (user == null) {
            String message = "Đang tải hồ sơ. Vui lòng thử lại sau.";
            planError.setValue(message);
            errorMsg.setValue(message);
            return;
        }

        setPlanLoading(true);
        FirebaseUser firebaseUser = authRepo.getCurrentUser();
        if (firebaseUser != null) {
            injuryProfileRepository.getInjuryProfile(firebaseUser.getUid(), new InjuryProfileRepository.InjuryProfileCallback() {
                @Override
                public void onSuccess(InjuryProfile injuryProfile) {
                    if (hasInjuryProfile(injuryProfile)) {
                        runInjuryAwareWorkoutPlan(user, injuryProfile);
                    } else {
                        runStandardWorkoutPlan(user);
                    }
                }

                @Override
                public void onError(Exception e) {
                    postPlanLoading(false);
                    String message = "Không thể tải hồ sơ an toàn.";
                    planError.postValue(message);
                    errorMsg.postValue(message);
                }
            });
            return;
        }
        runStandardWorkoutPlan(user);
    }

    private void runStandardWorkoutPlan(User user) {
        deepSeekRepo.generateWorkoutPlan(user, user.getGoal(), new DeepSeekRepository.AiCallback() {
            @Override
            public void onSuccess(String response) {
                postPlanLoading(false);
                planError.postValue(null);
                errorMsg.postValue(null);
                planResponse.postValue(response);
                aiResponse.postValue(response);
            }

            @Override
            public void onError(Exception e) {
                postPlanLoading(false);
                planResponse.postValue(null);
                aiResponse.postValue(null);
                String message = AiErrorMapper.toUserMessage(e);
                planError.postValue(message);
                errorMsg.postValue(message);
            }
        });
    }

    private void runInjuryAwareWorkoutPlan(User user, InjuryProfile injuryProfile) {
        deepSeekRepo.generateInjuryAwareWorkoutPlan(user, injuryProfile, user.getGoal(), new DeepSeekRepository.WorkoutPlanCallback() {
            @Override
            public void onSuccess(List<Workout> plan) {
                String response = DeepSeekRepository.formatWorkoutPlanForDisplay(plan);
                postPlanLoading(false);
                planError.postValue(null);
                errorMsg.postValue(null);
                planResponse.postValue(response);
                aiResponse.postValue(response);
            }

            @Override
            public void onError(Exception e) {
                postPlanLoading(false);
                planResponse.postValue(null);
                aiResponse.postValue(null);
                String message = AiErrorMapper.toUserMessage(e);
                planError.postValue(message);
                errorMsg.postValue(message);
            }
        });
    }

    private boolean hasInjuryProfile(InjuryProfile profile) {
        return profile != null
                && (profile.isKneeSensitive()
                || profile.isShoulderSensitive()
                || profile.isLowerBackSensitive()
                || (profile.getNotes() != null && !profile.getNotes().trim().isEmpty()));
    }

    public void analyzeForm(String exercise, String desc) {
        clearFormState();
        String normalizedExercise = exercise != null ? exercise.trim() : "";
        String normalizedDescription = desc != null ? desc.trim() : "";

        boolean valid = validateForm(normalizedExercise, normalizedDescription);
        if (!valid) {
            String message = "Nhập đủ thông tin hợp lệ";
            formError.setValue(message);
            errorMsg.setValue(message);
            return;
        }

        setFormLoading(true);
        deepSeekRepo.analyzeForm(normalizedExercise, normalizedDescription, new DeepSeekRepository.AiCallback() {
            @Override
            public void onSuccess(String response) {
                postFormLoading(false);
                formError.postValue(null);
                errorMsg.postValue(null);
                formResponse.postValue(response);
                aiResponse.postValue(response);
            }

            @Override
            public void onError(Exception e) {
                postFormLoading(false);
                formResponse.postValue(null);
                aiResponse.postValue(null);
                String message = AiErrorMapper.toUserMessage(e);
                formError.postValue(message);
                errorMsg.postValue(message);
            }
        });
    }

    private boolean validateForm(String exercise, String description) {
        boolean valid = true;
        if (exercise.length() < EXERCISE_NAME_MIN) {
            exerciseNameError.setValue("Tên bài tập cần ít nhất 2 ký tự");
            valid = false;
        } else if (exercise.length() > EXERCISE_NAME_MAX) {
            exerciseNameError.setValue("Tên bài tập tối đa 80 ký tự");
            valid = false;
        }

        if (description.length() < FORM_DESCRIPTION_MIN) {
            formDescriptionError.setValue("Mô tả cần ít nhất 20 ký tự");
            valid = false;
        } else if (description.length() > FORM_DESCRIPTION_MAX) {
            formDescriptionError.setValue("Mô tả tối đa 1000 ký tự");
            valid = false;
        }
        return valid;
    }

    private void clearPlanState() {
        planResponse.setValue(null);
        planError.setValue(null);
        aiResponse.setValue(null);
        errorMsg.setValue(null);
    }

    private void clearFormState() {
        formResponse.setValue(null);
        formError.setValue(null);
        exerciseNameError.setValue(null);
        formDescriptionError.setValue(null);
        aiResponse.setValue(null);
        errorMsg.setValue(null);
    }

    private void setPlanLoading(boolean loading) {
        isPlanLoading.setValue(loading);
        isLoading.setValue(loading || Boolean.TRUE.equals(isFormLoading.getValue()));
    }

    private void setFormLoading(boolean loading) {
        isFormLoading.setValue(loading);
        isLoading.setValue(loading || Boolean.TRUE.equals(isPlanLoading.getValue()));
    }

    private void postPlanLoading(boolean loading) {
        isPlanLoading.postValue(loading);
        isLoading.postValue(loading || Boolean.TRUE.equals(isFormLoading.getValue()));
    }

    private void postFormLoading(boolean loading) {
        isFormLoading.postValue(loading);
        isLoading.postValue(loading || Boolean.TRUE.equals(isPlanLoading.getValue()));
    }

    private BodyMetricsUiState buildBodyMetrics(User user, ProgressEntry latestProgress) {
        String bodyType = user != null && user.getBmi() != null
                ? nonBlank(user.getBmiCategory(), "Chỉ số BMI")
                : "Chưa có BMI";
        String bmi = user != null && user.getBmi() != null ? formatNumber(user.getBmi()) : PLACEHOLDER;
        String weight = user != null && user.getWeight() != null
                ? formatNumber(user.getWeight()) + "kg"
                : PLACEHOLDER;
        String goal = user != null && user.getGoal() != null && !user.getGoal().trim().isEmpty()
                ? user.getGoal().trim()
                : PLACEHOLDER;
        String summary = "BMI: " + bmi + " • Cân nặng: " + weight + " • Mục tiêu: " + goal;
        String bodyFat = latestProgress != null && latestProgress.getBodyFat() != null
                ? formatNumber(latestProgress.getBodyFat())
                : PLACEHOLDER;
        String leanMass = latestProgress != null && latestProgress.getLeanMass() != null
                ? formatNumber(latestProgress.getLeanMass())
                : PLACEHOLDER;
        return new BodyMetricsUiState(bodyType, summary, bodyFat, leanMass);
    }

    private static String nonBlank(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value.trim() : fallback;
    }

    private static String formatNumber(Float value) {
        return NUMBER_FORMAT.format(value);
    }

    public static class BodyMetricsUiState {
        private final String bodyType;
        private final String summary;
        private final String bodyFat;
        private final String leanMass;

        BodyMetricsUiState(String bodyType, String summary, String bodyFat, String leanMass) {
            this.bodyType = bodyType;
            this.summary = summary;
            this.bodyFat = bodyFat;
            this.leanMass = leanMass;
        }

        static BodyMetricsUiState empty() {
            return new BodyMetricsUiState(
                    "Chưa có BMI",
                    "BMI: -- • Cân nặng: -- • Mục tiêu: --",
                    PLACEHOLDER,
                    PLACEHOLDER
            );
        }

        public String getBodyType() { return bodyType; }
        public String getSummary() { return summary; }
        public String getBodyFat() { return bodyFat; }
        public String getLeanMass() { return leanMass; }
    }
}
