package ntu.quy65132908.smartgym_ai.ui.profile;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.util.InputValidator;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

@HiltViewModel
public class EditProfileViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final Context appContext;

    private final MutableLiveData<User> userData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> canSave = new MutableLiveData<>(false);
    private final MutableLiveData<ProfileFormErrors> formErrors =
            new MutableLiveData<>(ProfileFormErrors.none());
    private final MutableLiveData<EditProfileUiState> uiState =
            new MutableLiveData<>(EditProfileUiState.initial());
    private final SingleLiveEvent<Boolean> saveSuccess = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> errorMessage = new SingleLiveEvent<>();

    private User loadedUser;
    private boolean loading;
    private boolean saving;
    private boolean loggedOut;
    private boolean dirty;
    private boolean valid;
    private boolean hasAttemptedSubmit;
    private String draftName = "";
    private String draftWeight = "";
    private String draftHeight = "";
    private String draftGoal = "";
    private Float previewBmi;
    private String previewBmiCategory = "";

    public LiveData<User> getUserData() { return userData; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getCanSave() { return canSave; }
    public LiveData<ProfileFormErrors> getFormErrors() { return formErrors; }
    public LiveData<EditProfileUiState> getUiState() { return uiState; }
    public LiveData<Boolean> getSaveSuccess() { return saveSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    @Inject
    public EditProfileViewModel(UserRepository userRepository,
                                AuthRepository authRepository,
                                @ApplicationContext Context appContext) {
        this.userRepository = userRepository;
        this.authRepository = authRepository;
        this.appContext = appContext;
        loadUser();
    }

    private void loadUser() {
        FirebaseUser fb = authRepository.getCurrentUser();
        if (fb == null) {
            loggedOut = true;
            loading = false;
            errorMessage.setValue(appContext.getString(R.string.profile_login_required));
            publishState();
            return;
        }

        loading = true;
        publishState();
        userRepository.getUser(fb.getUid(), new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                loadedUser = user;
                if (loadedUser == null) {
                    loadedUser = new User(fb.getUid(), fallbackName(fb), fb.getEmail());
                }
                updateDraftFromUser(loadedUser);
                loading = false;
                userData.postValue(loadedUser);
                formErrors.postValue(ProfileFormErrors.none());
                publishState();
            }

            @Override
            public void onError(Exception e) {
                loadedUser = new User(fb.getUid(), fallbackName(fb), fb.getEmail());
                updateDraftFromUser(loadedUser);
                loading = false;
                userData.postValue(loadedUser);
                errorMessage.postValue(appContext.getString(R.string.profile_load_edit_error));
                publishState();
            }
        });
    }

    public void onProfileFormChanged(String name, String weight, String height, String goal) {
        updateDraft(name, weight, height, goal);
        ProfileFormErrors errors = buildErrors();
        valid = !errors.hasErrors();
        dirty = isDirty();
        recalculatePreview();
        if (hasAttemptedSubmit) {
            formErrors.setValue(errors);
        } else {
            formErrors.setValue(ProfileFormErrors.none());
        }
        publishState();
    }

    public void saveProfile(String name, String weightStr, String heightStr, String goal) {
        updateDraft(name, weightStr, heightStr, goal);
        hasAttemptedSubmit = true;
        ProfileFormErrors errors = buildErrors();
        valid = !errors.hasErrors();
        dirty = isDirty();
        recalculatePreview();
        formErrors.setValue(errors);
        publishState();
        if (errors.hasErrors()) {
            return;
        }

        FirebaseUser fb = authRepository.getCurrentUser();
        if (fb == null) {
            loggedOut = true;
            errorMessage.setValue(appContext.getString(R.string.profile_login_required));
            publishState();
            return;
        }

        User user = loadedUser != null ? loadedUser : new User(fb.getUid(), fallbackName(fb), fb.getEmail());
        user.setUid(fb.getUid());
        if (user.getEmail().isEmpty()) {
            user.setEmail(fb.getEmail());
        }
        applyDraftToUser(user);

        saving = true;
        publishState();
        userRepository.updateUser(fb.getUid(), user, new UserRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                loadedUser = user;
                hasAttemptedSubmit = false;
                dirty = false;
                saving = false;
                userData.postValue(user);
                saveSuccess.postValue(true);
                publishState();
            }

            @Override
            public void onError(Exception e) {
                saving = false;
                errorMessage.postValue(appContext.getString(R.string.profile_save_error));
                publishState();
            }
        });
    }

    private void applyDraftToUser(User user) {
        String name = InputValidator.sanitizeName(draftName);
        String goal = sanitizeGoal(draftGoal);
        Float weight = ProfileMetrics.parseOptionalFloat(draftWeight);
        Float height = ProfileMetrics.parseOptionalFloat(draftHeight);

        user.setDisplayName(name);
        user.setGoal(goal.isEmpty() ? null : goal);
        user.setWeight(weight);
        user.setHeight(height);

        Float bmi = ProfileMetrics.calculateBmiOrNull(weight, height);
        user.setBmi(bmi);
        user.setBmiCategory(bmi != null ? ProfileMetrics.categoryForBmi(bmi) : null);
    }

    private ProfileFormErrors buildErrors() {
        String nameError = null;
        String weightError = null;
        String heightError = null;
        String goalError = null;

        String sanitizedName = InputValidator.sanitizeName(draftName);
        if (sanitizedName.trim().isEmpty()) {
            nameError = appContext.getString(R.string.profile_name_empty);
        } else if (draftName != null && draftName.trim().length() > ProfileMetrics.MAX_NAME_LENGTH) {
            nameError = appContext.getString(R.string.profile_name_length_error);
        }

        Float parsedWeight = ProfileMetrics.parseOptionalFloat(draftWeight);
        if (!isBlank(draftWeight)) {
            if (parsedWeight == null) {
                weightError = appContext.getString(R.string.profile_weight_invalid);
            } else if (parsedWeight < ProfileMetrics.MIN_WEIGHT_KG || parsedWeight > ProfileMetrics.MAX_WEIGHT_KG) {
                weightError = appContext.getString(R.string.profile_weight_range_error);
            }
        }

        Float parsedHeight = ProfileMetrics.parseOptionalFloat(draftHeight);
        if (!isBlank(draftHeight)) {
            if (parsedHeight == null) {
                heightError = appContext.getString(R.string.profile_height_invalid);
            } else if (parsedHeight < ProfileMetrics.MIN_HEIGHT_CM || parsedHeight > ProfileMetrics.MAX_HEIGHT_CM) {
                heightError = appContext.getString(R.string.profile_height_range_error);
            }
        }

        if (draftGoal != null && draftGoal.trim().length() > ProfileMetrics.MAX_GOAL_LENGTH) {
            goalError = appContext.getString(R.string.profile_goal_length_error);
        }

        return new ProfileFormErrors(nameError, weightError, heightError, goalError);
    }

    private void recalculatePreview() {
        Float weight = ProfileMetrics.parseOptionalFloat(draftWeight);
        Float height = ProfileMetrics.parseOptionalFloat(draftHeight);
        previewBmi = ProfileMetrics.calculateBmiOrNull(weight, height);
        previewBmiCategory = previewBmi != null ? ProfileMetrics.categoryForBmi(previewBmi) : "";
    }

    private void updateDraftFromUser(User user) {
        draftName = user.getDisplayName();
        draftWeight = user.getWeight() != null ? String.valueOf(user.getWeight()) : "";
        draftHeight = user.getHeight() != null ? String.valueOf(user.getHeight()) : "";
        draftGoal = user.getGoal() != null ? user.getGoal() : "";
        hasAttemptedSubmit = false;
        dirty = false;
        valid = true;
        recalculatePreview();
    }

    private void updateDraft(String name, String weight, String height, String goal) {
        draftName = name != null ? name : "";
        draftWeight = weight != null ? weight : "";
        draftHeight = height != null ? height : "";
        draftGoal = goal != null ? goal : "";
    }

    private boolean isDirty() {
        if (loadedUser == null) {
            return !isBlank(draftName) || !isBlank(draftWeight) || !isBlank(draftHeight) || !isBlank(draftGoal);
        }
        return !safeEquals(InputValidator.sanitizeName(draftName), loadedUser.getDisplayName())
                || !safeEquals(normalizeNumberText(draftWeight), normalizeFloatText(loadedUser.getWeight()))
                || !safeEquals(normalizeNumberText(draftHeight), normalizeFloatText(loadedUser.getHeight()))
                || !safeEquals(sanitizeGoal(draftGoal), loadedUser.getGoal() != null ? loadedUser.getGoal() : "");
    }

    private void publishState() {
        boolean busy = loading || saving;
        isLoading.postValue(busy);
        canSave.postValue(dirty && valid && !busy && !loggedOut);
        uiState.postValue(new EditProfileUiState(
                loadedUser,
                previewBmi,
                previewBmiCategory,
                loading,
                saving,
                loggedOut,
                dirty,
                valid
        ));
    }

    private String fallbackName(FirebaseUser fb) {
        String name = InputValidator.sanitizeName(fb.getDisplayName());
        if (!name.isEmpty()) {
            return name;
        }
        String email = fb.getEmail();
        return email != null && !email.trim().isEmpty()
                ? email
                : appContext.getString(R.string.post_user_default);
    }

    private String sanitizeGoal(String goal) {
        if (goal == null) {
            return "";
        }
        String trimmed = goal.trim().replaceAll("<[^>]*>", "");
        return trimmed.length() > ProfileMetrics.MAX_GOAL_LENGTH
                ? trimmed.substring(0, ProfileMetrics.MAX_GOAL_LENGTH)
                : trimmed;
    }

    private String normalizeNumberText(String raw) {
        Float value = ProfileMetrics.parseOptionalFloat(raw);
        return value != null ? String.valueOf(value) : "";
    }

    private String normalizeFloatText(Float value) {
        return value != null ? String.valueOf(value) : "";
    }

    private boolean safeEquals(String a, String b) {
        return (a == null ? "" : a).equals(b == null ? "" : b);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
