package ntu.quy65132908.smartgym_ai.ui.nutrition;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.FoodLogEntry;
import ntu.quy65132908.smartgym_ai.data.model.FoodNutritionEstimate;
import ntu.quy65132908.smartgym_ai.data.model.Meal;
import ntu.quy65132908.smartgym_ai.data.model.MealPlan;
import ntu.quy65132908.smartgym_ai.data.model.MealPlanDay;
import ntu.quy65132908.smartgym_ai.data.model.NutritionGoal;
import ntu.quy65132908.smartgym_ai.data.model.NutritionSummary;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.DeepSeekRepository;
import ntu.quy65132908.smartgym_ai.data.repository.NutritionRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

@HiltViewModel
public class NutritionViewModel extends ViewModel {
    private static final SimpleDateFormat DATE_KEY_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final int MEAL_PLAN_PREVIEW_MEALS_PER_DAY = 2;

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final NutritionRepository nutritionRepository;
    private final DeepSeekRepository deepSeekRepository;
    private final Context appContext;

    private final MutableLiveData<NutritionSummary> summary = new MutableLiveData<>();
    private final MutableLiveData<List<FoodLogEntry>> foodLogs = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<FoodLogEntry>> historyFoodLogs = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<NutritionSummary> historySummary = new MutableLiveData<>();
    private final MutableLiveData<String> selectedHistoryDateKey = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isSavingFood = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isGeneratingMealPlan = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isEstimatingFood = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isHistoryLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> canAddFood = new MutableLiveData<>(false);
    private final MutableLiveData<NutritionFormErrors> formErrors = new MutableLiveData<>(NutritionFormErrors.none());
    private final MutableLiveData<String> mealPlanPreview = new MutableLiveData<>("");
    private final MutableLiveData<FoodNutritionEstimate> pendingEstimate = new MutableLiveData<>(null);
    private final MutableLiveData<NutritionUiState> uiState = new MutableLiveData<>();
    private final SingleLiveEvent<String> message = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> clearFoodFormEvent = new SingleLiveEvent<>();

    private NutritionGoal goal = NutritionRepository.defaultGoalForWeight(null, "duy trì");
    private MealPlan latestMealPlan;
    private String currentMealPlanPreview = "";
    private User loadedUser;
    private List<FoodLogEntry> currentFoodLogs = new ArrayList<>();
    private boolean foodLogsLoading;
    private boolean mealPlanLoading;
    private boolean historyLoading;
    private boolean savingFood;
    private boolean estimatingFood;
    private boolean hasAttemptedSubmit;
    private String draftName = "";
    private String draftServingText = "";
    private String draftMealType = "";
    private String draftCalories = "";
    private String draftProtein = "";
    private String draftCarbs = "";
    private String draftFat = "";

    @Inject
    public NutritionViewModel(@ApplicationContext Context appContext,
                              AuthRepository authRepository,
                              UserRepository userRepository,
                              NutritionRepository nutritionRepository,
                              DeepSeekRepository deepSeekRepository) {
        this.appContext = appContext;
        this.authRepository = authRepository;
        this.userRepository = userRepository;
        this.nutritionRepository = nutritionRepository;
        this.deepSeekRepository = deepSeekRepository;
        publishSummary();
        publishUiState();
        loadProfile();
        loadTodayFoodLogs();
    }

    public LiveData<NutritionSummary> getSummary() { return summary; }
    public LiveData<List<FoodLogEntry>> getFoodLogs() { return foodLogs; }
    public LiveData<List<FoodLogEntry>> getHistoryFoodLogs() { return historyFoodLogs; }
    public LiveData<NutritionSummary> getHistorySummary() { return historySummary; }
    public LiveData<String> getSelectedHistoryDateKey() { return selectedHistoryDateKey; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsSavingFood() { return isSavingFood; }
    public LiveData<Boolean> getIsGeneratingMealPlan() { return isGeneratingMealPlan; }
    public LiveData<Boolean> getIsEstimatingFood() { return isEstimatingFood; }
    public LiveData<Boolean> getIsHistoryLoading() { return isHistoryLoading; }
    public LiveData<Boolean> getCanAddFood() { return canAddFood; }
    public LiveData<NutritionFormErrors> getFormErrors() { return formErrors; }
    public LiveData<String> getMealPlanPreview() { return mealPlanPreview; }
    public LiveData<FoodNutritionEstimate> getPendingEstimate() { return pendingEstimate; }
    public LiveData<NutritionUiState> getUiState() { return uiState; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<Boolean> getClearFoodFormEvent() { return clearFoodFormEvent; }

    public void reload() {
        loadProfile();
        loadTodayFoodLogs();
    }

    public void onFoodFormChanged(String name, String mealType, String calories, String protein, String carbs, String fat) {
        updateDraft(name, mealType, calories, protein, carbs, fat);
        validateDraft(hasAttemptedSubmit);
    }

    public void addFood(String name, String mealType, String calories, String protein, String carbs, String fat) {
        addFood(name, "", mealType, calories, protein, carbs, fat);
    }

    public void addFood(String name, String servingText, String mealType, String calories, String protein, String carbs, String fat) {
        draftServingText = servingText != null ? servingText : "";
        updateDraft(name, mealType, calories, protein, carbs, fat);
        hasAttemptedSubmit = true;
        if (!validateDraft(true)) {
            return;
        }

        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            message.setValue(appContext.getString(R.string.nutrition_food_login_required));
            return;
        }

        FoodLogEntry entry = new FoodLogEntry(
                null,
                draftName.trim(),
                !isBlank(draftMealType) ? draftMealType.trim() : appContext.getString(R.string.nutrition_fallback_meal_type),
                parsePositiveInt(draftCalories),
                parseNonNegativeInt(draftProtein),
                parseNonNegativeInt(draftCarbs),
                parseNonNegativeInt(draftFat),
                System.currentTimeMillis()
        );
        entry.setServingText(draftServingText);
        entry.setSource(FoodLogEntry.SOURCE_MANUAL);

        saveFoodEntry(user.getUid(), entry);
    }

    public void estimateFood(String name, String servingText, String mealType) {
        String safeName = name != null ? name.trim() : "";
        if (safeName.length() < 2) {
            message.setValue(appContext.getString(R.string.nutrition_food_name_error));
            return;
        }

        setEstimatingFood(true);
        deepSeekRepository.estimateFoodNutritionData(
                safeName,
                servingText,
                !isBlank(mealType) ? mealType.trim() : appContext.getString(R.string.nutrition_default_meal_type),
                goal,
                new DeepSeekRepository.FoodEstimateCallback() {
                    @Override
                    public void onSuccess(FoodNutritionEstimate estimate) {
                        setEstimatingFood(false);
                        pendingEstimate.postValue(estimate);
                    }

                    @Override
                    public void onError(Exception e) {
                        setEstimatingFood(false);
                        message.postValue(NutritionAiErrorMapper.toUserMessage(e));
                    }
                });
    }

    public void savePendingEstimate(String name,
                                    String servingText,
                                    String mealType,
                                    String calories,
                                    String protein,
                                    String carbs,
                                    String fat) {
        updateDraft(name, mealType, calories, protein, carbs, fat);
        draftServingText = servingText != null ? servingText : "";
        hasAttemptedSubmit = true;
        if (!validateDraft(true)) {
            return;
        }

        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            message.setValue(appContext.getString(R.string.nutrition_food_login_required));
            return;
        }

        FoodNutritionEstimate estimate = pendingEstimate.getValue();
        FoodLogEntry entry = new FoodLogEntry(
                null,
                draftName.trim(),
                !isBlank(draftMealType) ? draftMealType.trim() : appContext.getString(R.string.nutrition_fallback_meal_type),
                parsePositiveInt(draftCalories),
                parseNonNegativeInt(draftProtein),
                parseNonNegativeInt(draftCarbs),
                parseNonNegativeInt(draftFat),
                System.currentTimeMillis()
        );
        entry.setServingText(draftServingText);
        entry.setCategory(estimate != null ? estimate.getCategory() : null);
        entry.setAiConfidence(estimate != null ? estimate.getConfidence() : null);
        entry.setNotes(estimate != null ? estimate.getNotes() : null);
        entry.setSource(FoodLogEntry.SOURCE_AI);
        saveFoodEntry(user.getUid(), entry);
        pendingEstimate.setValue(null);
    }

    public void cancelPendingEstimate() {
        pendingEstimate.setValue(null);
    }

    public void deleteFoodLog(FoodLogEntry entry) {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            message.setValue(appContext.getString(R.string.nutrition_food_login_required));
            return;
        }
        if (entry == null || isBlank(entry.getId())) {
            return;
        }
        setSavingFood(true);
        nutritionRepository.deleteFoodLog(user.getUid(), todayKey(), entry.getId(), new NutritionRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                List<FoodLogEntry> current = new ArrayList<>(currentFoodLogs);
                for (int i = current.size() - 1; i >= 0; i--) {
                    FoodLogEntry item = current.get(i);
                    if (item != null && entry.getId().equals(item.getId())) {
                        current.remove(i);
                    }
                }
                setSavingFood(false);
                setFoodLogs(current);
                message.postValue(appContext.getString(R.string.nutrition_food_deleted));
            }

            @Override
            public void onError(Exception e) {
                setSavingFood(false);
                message.postValue(appContext.getString(R.string.nutrition_food_delete_error));
            }
        });
    }

    public void updateFoodLog(FoodLogEntry original,
                              String name,
                              String servingText,
                              String mealType,
                              String calories,
                              String protein,
                              String carbs,
                              String fat) {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            message.setValue(appContext.getString(R.string.nutrition_food_login_required));
            return;
        }
        if (original == null || isBlank(original.getId())) {
            return;
        }
        updateDraft(name, mealType, calories, protein, carbs, fat);
        draftServingText = servingText != null ? servingText : "";
        hasAttemptedSubmit = true;
        if (!validateDraft(true)) {
            return;
        }

        FoodLogEntry updated = new FoodLogEntry(
                original.getId(),
                draftName.trim(),
                !isBlank(draftMealType) ? draftMealType.trim() : appContext.getString(R.string.nutrition_fallback_meal_type),
                parsePositiveInt(draftCalories),
                parseNonNegativeInt(draftProtein),
                parseNonNegativeInt(draftCarbs),
                parseNonNegativeInt(draftFat),
                original.getEatenAt() > 0L ? original.getEatenAt() : System.currentTimeMillis()
        );
        updated.setServingText(draftServingText);
        updated.setCategory(original.getCategory());
        updated.setSource(original.getSource());
        updated.setPlanImportKey(original.getPlanImportKey());
        updated.setAiConfidence(original.getAiConfidence());
        updated.setNotes(original.getNotes());

        setSavingFood(true);
        nutritionRepository.updateFoodLog(user.getUid(), todayKey(), original.getId(), updated, new NutritionRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                List<FoodLogEntry> current = new ArrayList<>(currentFoodLogs);
                for (int i = 0; i < current.size(); i++) {
                    FoodLogEntry item = current.get(i);
                    if (item != null && original.getId().equals(item.getId())) {
                        current.set(i, updated);
                        break;
                    }
                }
                setSavingFood(false);
                setFoodLogs(current);
                clearFoodFormAfterSave();
                message.postValue(appContext.getString(R.string.nutrition_food_updated));
            }

            @Override
            public void onError(Exception e) {
                setSavingFood(false);
                message.postValue(appContext.getString(R.string.nutrition_food_update_error));
            }
        });
    }

    public void logMealPlanMeal(Meal meal) {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            message.setValue(appContext.getString(R.string.nutrition_food_login_required));
            return;
        }
        if (meal == null || isBlank(meal.getName())) {
            return;
        }
        FoodLogEntry entry = new FoodLogEntry(
                null,
                meal.getName(),
                nonBlank(meal.getMealType(), appContext.getString(R.string.nutrition_fallback_meal_type)),
                Math.max(1, meal.getCalories()),
                Math.max(0, meal.getProteinGrams()),
                Math.max(0, meal.getCarbsGrams()),
                Math.max(0, meal.getFatGrams()),
                System.currentTimeMillis()
        );
        entry.setSource(FoodLogEntry.SOURCE_PLAN);
        entry.setNotes(meal.getNotes());
        saveFoodEntry(user.getUid(), entry);
    }

    public void logMealPlanDay(MealPlanDay day) {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            message.setValue(appContext.getString(R.string.nutrition_food_login_required));
            return;
        }
        if (day == null || day.getMeals() == null || day.getMeals().isEmpty()) {
            return;
        }

        Set<String> importedKeys = new HashSet<>();
        for (FoodLogEntry entry : currentFoodLogs) {
            if (entry != null && !isBlank(entry.getPlanImportKey())) {
                importedKeys.add(entry.getPlanImportKey());
            }
        }

        List<FoodLogEntry> entriesToSave = new ArrayList<>();
        for (Meal meal : day.getMeals()) {
            if (meal == null || isBlank(meal.getName())) {
                continue;
            }
            String importKey = mealPlanImportKey(day, meal);
            if (importedKeys.contains(importKey)) {
                continue;
            }
            importedKeys.add(importKey);
            FoodLogEntry entry = new FoodLogEntry(
                    null,
                    meal.getName(),
                    nonBlank(meal.getMealType(), appContext.getString(R.string.nutrition_fallback_meal_type)),
                    Math.max(1, meal.getCalories()),
                    Math.max(0, meal.getProteinGrams()),
                    Math.max(0, meal.getCarbsGrams()),
                    Math.max(0, meal.getFatGrams()),
                    System.currentTimeMillis()
            );
            entry.setSource(FoodLogEntry.SOURCE_PLAN);
            entry.setNotes(meal.getNotes());
            entry.setPlanImportKey(importKey);
            entriesToSave.add(entry);
        }

        if (entriesToSave.isEmpty()) {
            message.setValue(appContext.getString(R.string.nutrition_plan_day_duplicate));
            return;
        }

        savePlanFoodEntries(user.getUid(), entriesToSave);
    }

    private void savePlanFoodEntries(String uid, List<FoodLogEntry> entries) {
        setSavingFood(true);
        nutritionRepository.saveFoodLogs(uid, todayKey(), entries, new NutritionRepository.FoodLogsSaveCallback() {
            @Override
            public void onSuccess(List<FoodLogEntry> savedEntries) {
                List<FoodLogEntry> nextLogs = new ArrayList<>();
                if (savedEntries != null) {
                    nextLogs.addAll(savedEntries);
                }
                nextLogs.addAll(currentFoodLogs);
                setSavingFood(false);
                setFoodLogs(nextLogs);
                message.postValue(appContext.getString(R.string.nutrition_plan_day_logged));
            }

            @Override
            public void onError(Exception e) {
                setSavingFood(false);
                message.postValue(appContext.getString(R.string.nutrition_food_save_error));
            }
        });
    }

    public void loadHistory(String dateKey) {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            message.setValue(appContext.getString(R.string.nutrition_food_login_required));
            return;
        }
        String safeDateKey = dateKey != null ? dateKey.trim() : "";
        if (!safeDateKey.matches("\\d{4}-\\d{2}-\\d{2}")) {
            message.setValue(appContext.getString(R.string.nutrition_history_date_error));
            return;
        }

        setHistoryLoading(true);
        nutritionRepository.getFoodLogsForDate(user.getUid(), safeDateKey, new NutritionRepository.FoodLogsCallback() {
            @Override
            public void onSuccess(List<FoodLogEntry> logs) {
                List<FoodLogEntry> safeLogs = logs != null ? new ArrayList<>(logs) : new ArrayList<>();
                setHistoryLoading(false);
                historyFoodLogs.postValue(safeLogs);
                historySummary.postValue(NutritionRepository.calculateTodaySummary(goal, safeLogs));
                selectedHistoryDateKey.postValue(safeDateKey);
            }

            @Override
            public void onError(Exception e) {
                setHistoryLoading(false);
                message.postValue(appContext.getString(R.string.nutrition_food_logs_load_error));
            }
        });
    }

    public void generateMealPlan() {
        FirebaseUser firebaseUser = authRepository.getCurrentUser();
        if (firebaseUser == null) {
            message.setValue(appContext.getString(R.string.nutrition_login_required));
            return;
        }

        setMealPlanLoading(true);
        deepSeekRepository.generateMealPlanData(loadedUser, goal, new DeepSeekRepository.MealPlanCallback() {
            @Override
            public void onSuccess(MealPlan mealPlan) {
                if (!isUsableMealPlan(mealPlan)) {
                    setMealPlanLoading(false);
                    message.postValue(appContext.getString(R.string.nutrition_plan_empty_error));
                    return;
                }

                latestMealPlan = mealPlan;
                setMealPlanPreview(formatMealPlanPreview(mealPlan));
                publishUiState();
                nutritionRepository.saveMealPlan(firebaseUser.getUid(), mealPlan, new NutritionRepository.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        setMealPlanLoading(false);
                        publishUiState();
                        message.postValue(appContext.getString(R.string.nutrition_plan_saved));
                    }

                    @Override
                    public void onError(Exception e) {
                        setMealPlanLoading(false);
                        message.postValue(appContext.getString(R.string.nutrition_plan_save_error));
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                saveFallbackMealPlan(firebaseUser.getUid(), e);
            }
        });
    }

    private void saveFallbackMealPlan(String uid, Exception aiError) {
        MealPlan fallbackPlan = DeepSeekRepository.buildFallbackMealPlan(loadedUser, goal);
        if (!isUsableMealPlan(fallbackPlan)) {
            setMealPlanLoading(false);
            message.postValue(NutritionAiErrorMapper.toUserMessage(aiError));
            return;
        }
        latestMealPlan = fallbackPlan;
        setMealPlanPreview(formatMealPlanPreview(fallbackPlan));
        publishUiState();
        nutritionRepository.saveMealPlan(uid, fallbackPlan, new NutritionRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                setMealPlanLoading(false);
                publishUiState();
                message.postValue(NutritionAiErrorMapper.toUserMessage(aiError)
                        + " Đã tạo kế hoạch ăn cơ bản để bạn có thể bắt đầu.");
            }

            @Override
            public void onError(Exception e) {
                setMealPlanLoading(false);
                message.postValue(appContext.getString(R.string.nutrition_plan_save_error));
            }
        });
    }

    private void loadProfile() {
        FirebaseUser firebaseUser = authRepository.getCurrentUser();
        if (firebaseUser == null) {
            publishUiState();
            return;
        }

        userRepository.getUser(firebaseUser.getUid(), new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                loadedUser = user;
                goal = NutritionRepository.defaultGoalForWeight(user.getWeight(), user.getGoal());
                publishSummary();
                publishUiState();
            }

            @Override
            public void onError(Exception e) {
                message.postValue(appContext.getString(R.string.nutrition_profile_load_error));
                publishUiState();
            }
        });
    }

    private void loadTodayFoodLogs() {
        FirebaseUser firebaseUser = authRepository.getCurrentUser();
        if (firebaseUser == null) {
            setFoodLogs(new ArrayList<>());
            return;
        }

        setFoodLogsLoading(true);
        nutritionRepository.getTodayFoodLogs(firebaseUser.getUid(), todayKey(), new NutritionRepository.FoodLogsCallback() {
            @Override
            public void onSuccess(List<FoodLogEntry> logs) {
                setFoodLogs(logs);
                setFoodLogsLoading(false);
            }

            @Override
            public void onError(Exception e) {
                setFoodLogsLoading(false);
                message.postValue(appContext.getString(R.string.nutrition_food_logs_load_error));
            }
        });
    }

    private void setFoodLogs(List<FoodLogEntry> logs) {
        currentFoodLogs = logs != null ? new ArrayList<>(logs) : new ArrayList<>();
        foodLogs.postValue(new ArrayList<>(currentFoodLogs));
        publishSummary();
        publishUiState();
    }

    private void addSavedFoodLog(FoodLogEntry entry) {
        List<FoodLogEntry> current = new ArrayList<>(currentFoodLogs);
        current.add(0, entry);
        setFoodLogs(current);
    }

    private void saveFoodEntry(String uid, FoodLogEntry entry) {
        setSavingFood(true);
        nutritionRepository.saveFoodLog(uid, todayKey(), entry, new NutritionRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                addSavedFoodLog(entry);
                setSavingFood(false);
                clearFoodFormAfterSave();
                message.postValue(appContext.getString(R.string.nutrition_food_saved));
            }

            @Override
            public void onError(Exception e) {
                setSavingFood(false);
                message.postValue(appContext.getString(R.string.nutrition_food_save_error));
            }
        });
    }

    private void clearFoodFormAfterSave() {
        hasAttemptedSubmit = false;
        draftServingText = "";
        updateDraft("", appContext.getString(R.string.nutrition_default_meal_type), "", "", "", "");
        formErrors.postValue(NutritionFormErrors.none());
        updateCanAddFood(false);
        pendingEstimate.postValue(null);
        clearFoodFormEvent.postValue(true);
    }

    private boolean validateDraft(boolean publishErrors) {
        NutritionFormErrors errors = buildErrors(draftName, draftCalories, draftProtein, draftCarbs, draftFat);
        if (publishErrors) {
            formErrors.setValue(errors);
        }
        updateCanAddFood(!errors.hasErrors());
        return !errors.hasErrors();
    }

    private NutritionFormErrors buildErrors(String name, String calories, String protein, String carbs, String fat) {
        String nameError = null;
        String caloriesError = null;
        String macroError = null;

        String safeName = name != null ? name.trim() : "";
        if (safeName.length() < 2) {
            nameError = appContext.getString(R.string.nutrition_food_name_error);
        }

        Integer parsedCalories = parseOptionalInt(calories);
        if (parsedCalories == null || parsedCalories <= 0) {
            caloriesError = appContext.getString(R.string.nutrition_calories_error);
        }

        if (!isValidOptionalMacro(protein) || !isValidOptionalMacro(carbs) || !isValidOptionalMacro(fat)) {
            macroError = appContext.getString(R.string.nutrition_macro_error);
        }

        return new NutritionFormErrors(nameError, caloriesError, macroError);
    }

    private boolean isValidOptionalMacro(String value) {
        if (isBlank(value)) {
            return true;
        }
        Integer parsed = parseOptionalInt(value);
        return parsed != null && parsed >= 0;
    }

    private Integer parseOptionalInt(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            float parsed = Float.parseFloat(value.trim());
            if (Float.isNaN(parsed) || Float.isInfinite(parsed)) {
                return null;
            }
            return Math.round(parsed);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parsePositiveInt(String value) {
        Integer parsed = parseOptionalInt(value);
        return parsed != null ? Math.max(1, parsed) : 1;
    }

    private int parseNonNegativeInt(String value) {
        Integer parsed = parseOptionalInt(value);
        return parsed != null ? Math.max(0, parsed) : 0;
    }

    private void updateDraft(String name, String mealType, String calories, String protein, String carbs, String fat) {
        draftName = name != null ? name : "";
        draftMealType = mealType != null ? mealType : "";
        draftCalories = calories != null ? calories : "";
        draftProtein = protein != null ? protein : "";
        draftCarbs = carbs != null ? carbs : "";
        draftFat = fat != null ? fat : "";
    }

    private void updateCanAddFood(boolean formValid) {
        canAddFood.setValue(formValid && !savingFood);
    }

    private void setSavingFood(boolean saving) {
        savingFood = saving;
        isSavingFood.postValue(saving);
        updateCanAddFood(!buildErrors(draftName, draftCalories, draftProtein, draftCarbs, draftFat).hasErrors());
        updateLoading();
        publishUiState();
    }

    private void setFoodLogsLoading(boolean loading) {
        foodLogsLoading = loading;
        updateLoading();
        publishUiState();
    }

    private void setMealPlanLoading(boolean loading) {
        mealPlanLoading = loading;
        isGeneratingMealPlan.postValue(loading);
        updateLoading();
        publishUiState();
    }

    private void setHistoryLoading(boolean loading) {
        historyLoading = loading;
        isHistoryLoading.postValue(loading);
        updateLoading();
        publishUiState();
    }

    private void setEstimatingFood(boolean estimating) {
        estimatingFood = estimating;
        isEstimatingFood.postValue(estimating);
        updateLoading();
        publishUiState();
    }

    private void updateLoading() {
        isLoading.postValue(foodLogsLoading || mealPlanLoading || historyLoading || savingFood || estimatingFood);
    }

    private void publishSummary() {
        summary.postValue(NutritionRepository.calculateTodaySummary(goal, currentFoodLogs));
    }

    private void publishUiState() {
        boolean loggedOut = authRepository.getCurrentUser() == null;
        String emptyText = loggedOut
                ? appContext.getString(R.string.nutrition_empty_food_logs_logged_out)
                : appContext.getString(R.string.nutrition_empty_food_logs);
        uiState.postValue(new NutritionUiState(
                NutritionRepository.calculateTodaySummary(goal, currentFoodLogs),
                currentFoodLogs,
                latestMealPlan,
                currentMealPlanPreview,
                foodLogsLoading || mealPlanLoading || historyLoading || savingFood || estimatingFood,
                savingFood,
                loggedOut,
                foodLogsLoading,
                emptyText
        ));
    }

    private void setMealPlanPreview(String preview) {
        currentMealPlanPreview = preview != null ? preview : "";
        mealPlanPreview.postValue(currentMealPlanPreview);
    }

    static String formatMealPlanPreview(MealPlan mealPlan) {
        if (mealPlan == null || mealPlan.getDays() == null || mealPlan.getDays().isEmpty()) {
            return "";
        }

        StringBuilder preview = new StringBuilder(nonBlank(mealPlan.getTitle(), "Kế hoạch ăn 7 ngày"));
        for (MealPlanDay day : mealPlan.getDays()) {
            if (day == null) {
                continue;
            }
            preview.append('\n')
                    .append(nonBlank(day.getDayLabel(), "Ngày " + day.getDayOfWeek()));
            if (day.getTargetCalories() > 0) {
                preview.append(" · ").append(day.getTargetCalories()).append(" kcal");
            }
            preview.append(": ");

            List<Meal> meals = day.getMeals();
            if (meals == null || meals.isEmpty()) {
                preview.append("Chưa có món");
                continue;
            }
            for (int i = 0; i < meals.size(); i++) {
                Meal meal = meals.get(i);
                if (i > 0) {
                    preview.append("; ");
                }
                preview.append(nonBlank(meal.getMealType(), "Bữa ăn"))
                        .append(" - ")
                        .append(nonBlank(meal.getName(), "Món ăn"));
            }
        }
        return preview.toString();
    }

    private boolean isUsableMealPlan(MealPlan mealPlan) {
        if (mealPlan == null || mealPlan.getDays() == null || mealPlan.getDays().isEmpty()) {
            return false;
        }
        for (MealPlanDay day : mealPlan.getDays()) {
            if (day == null || day.getMeals() == null || day.getMeals().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String mealPlanImportKey(MealPlanDay day, Meal meal) {
        String dayPart = !isBlank(day.getDayLabel())
                ? day.getDayLabel()
                : String.valueOf(day.getDayOfWeek());
        String key = (dayPart + "|"
                + nonBlank(meal.getMealType(), appContext.getString(R.string.nutrition_fallback_meal_type)) + "|"
                + meal.getName()).toLowerCase(Locale.ROOT);
        return key.length() <= 120 ? key : key.substring(0, 120);
    }

    private String todayKey() {
        return DATE_KEY_FORMAT.format(new Date());
    }

    private static String nonBlank(String value, String fallback) {
        return !isBlank(value) ? value.trim() : fallback;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
