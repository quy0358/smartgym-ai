package ntu.quy65132908.smartgym_ai.data.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import ntu.quy65132908.smartgym_ai.data.model.FoodLogEntry;
import ntu.quy65132908.smartgym_ai.data.model.FoodNutritionEstimate;
import ntu.quy65132908.smartgym_ai.data.model.MealPlan;
import ntu.quy65132908.smartgym_ai.data.model.NutritionGoal;
import ntu.quy65132908.smartgym_ai.data.model.NutritionSummary;

@Singleton
public class NutritionRepository {
    private static final int MAX_FOOD_NAME_LENGTH = 80;
    private static final int MAX_MEAL_TYPE_LENGTH = 40;
    private static final int MAX_SERVING_TEXT_LENGTH = 80;
    private static final int MAX_CATEGORY_LENGTH = 30;
    private static final int MAX_PLAN_IMPORT_KEY_LENGTH = 120;
    private static final int MAX_NOTES_LENGTH = 300;
    private static final int MAX_MEAL_PLAN_TITLE_LENGTH = 100;
    private static final int MAX_CALORIES_PER_ENTRY = 5000;
    private static final int MAX_MACRO_GRAMS_PER_ENTRY = 1000;
    private static final int MAX_BATCH_FOOD_LOGS = 100;

    private final FirebaseFirestore firestore;

    @Inject
    public NutritionRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public void getTodaySummary(String uid, String dateKey, NutritionSummaryCallback callback) {
        getTodayFoodLogs(uid, dateKey, new FoodLogsCallback() {
            @Override
            public void onSuccess(List<FoodLogEntry> logs) {
                callback.onSuccess(calculateTodaySummary(defaultGoalForWeight(null, "duy trì"), logs));
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    public void getTodayFoodLogs(String uid, String dateKey, FoodLogsCallback callback) {
        getFoodLogsForDate(uid, dateKey, callback);
    }

    public void getFoodLogsForDate(String uid, String dateKey, FoodLogsCallback callback) {
        if (isBlank(uid) || isBlank(dateKey)) {
            callback.onError(new IllegalArgumentException("uid and dateKey are required"));
            return;
        }
        firestore.collection("users").document(uid)
                .collection("nutritionDays").document(dateKey)
                .collection("foodLogs")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<FoodLogEntry> logs = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        FoodLogEntry entry = doc.toObject(FoodLogEntry.class);
                        if (entry != null) {
                            entry.setId(doc.getId());
                            logs.add(entry);
                        }
                    }
                    sortFoodLogsNewestFirst(logs);
                    callback.onSuccess(logs);
                })
                .addOnFailureListener(callback::onError);
    }

    public void saveFoodLog(String uid, String dateKey, FoodLogEntry entry, SimpleCallback callback) {
        if (isBlank(uid) || isBlank(dateKey)) {
            callback.onError(new IllegalArgumentException("uid and dateKey are required"));
            return;
        }
        try {
            sanitizeFoodLogForSave(entry);
        } catch (IllegalArgumentException e) {
            callback.onError(e);
            return;
        }
        firestore.collection("users").document(uid)
                .collection("nutritionDays").document(dateKey)
                .collection("foodLogs")
                .add(entry)
                .addOnSuccessListener(ref -> {
                    entry.setId(ref.getId());
                    callback.onSuccess();
                })
                .addOnFailureListener(callback::onError);
    }

    public void updateFoodLog(String uid, String dateKey, String foodLogId, FoodLogEntry entry, SimpleCallback callback) {
        if (isBlank(uid) || isBlank(dateKey) || isBlank(foodLogId)) {
            callback.onError(new IllegalArgumentException("uid, dateKey and foodLogId are required"));
            return;
        }
        try {
            sanitizeFoodLogForSave(entry);
            entry.setId(foodLogId);
        } catch (IllegalArgumentException e) {
            callback.onError(e);
            return;
        }
        firestore.collection("users").document(uid)
                .collection("nutritionDays").document(dateKey)
                .collection("foodLogs").document(foodLogId)
                .set(entry)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void deleteFoodLog(String uid, String dateKey, String foodLogId, SimpleCallback callback) {
        if (isBlank(uid) || isBlank(dateKey) || isBlank(foodLogId)) {
            callback.onError(new IllegalArgumentException("uid, dateKey and foodLogId are required"));
            return;
        }
        firestore.collection("users").document(uid)
                .collection("nutritionDays").document(dateKey)
                .collection("foodLogs").document(foodLogId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void saveFoodLogs(String uid, String dateKey, List<FoodLogEntry> entries, FoodLogsSaveCallback callback) {
        if (isBlank(uid) || isBlank(dateKey)) {
            callback.onError(new IllegalArgumentException("uid and dateKey are required"));
            return;
        }
        if (entries == null || entries.isEmpty()) {
            callback.onError(new IllegalArgumentException("food log entries are required"));
            return;
        }
        if (entries.size() > MAX_BATCH_FOOD_LOGS) {
            callback.onError(new IllegalArgumentException("too many food log entries"));
            return;
        }

        List<FoodLogEntry> sanitized = new ArrayList<>();
        try {
            for (FoodLogEntry entry : entries) {
                sanitized.add(sanitizeFoodLogForSave(entry));
            }
        } catch (IllegalArgumentException e) {
            callback.onError(e);
            return;
        }

        WriteBatch batch = firestore.batch();
        for (FoodLogEntry entry : sanitized) {
            com.google.firebase.firestore.DocumentReference ref = firestore.collection("users").document(uid)
                    .collection("nutritionDays").document(dateKey)
                    .collection("foodLogs")
                    .document();
            entry.setId(ref.getId());
            batch.set(ref, entry);
        }
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(sanitized))
                .addOnFailureListener(callback::onError);
    }

    public void saveMealPlan(String uid, MealPlan mealPlan, SimpleCallback callback) {
        if (isBlank(uid)) {
            callback.onError(new IllegalArgumentException("uid is required"));
            return;
        }
        try {
            sanitizeMealPlanForSave(mealPlan);
        } catch (IllegalArgumentException e) {
            callback.onError(e);
            return;
        }
        firestore.collection("users").document(uid)
                .collection("mealPlans")
                .add(mealPlan)
                .addOnSuccessListener(ref -> {
                    mealPlan.setId(ref.getId());
                    callback.onSuccess();
                })
                .addOnFailureListener(callback::onError);
    }

    public void getMealPlans(String uid, MealPlansCallback callback) {
        if (isBlank(uid)) {
            callback.onError(new IllegalArgumentException("uid is required"));
            return;
        }
        firestore.collection("users").document(uid)
                .collection("mealPlans")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<MealPlan> plans = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        MealPlan plan = doc.toObject(MealPlan.class);
                        if (plan != null) {
                            plan.setId(doc.getId());
                            plans.add(plan);
                        }
                    }
                    Collections.sort(plans, (left, right) ->
                            Long.compare(
                                    right != null ? right.getCreatedAt() : 0L,
                                    left != null ? left.getCreatedAt() : 0L
                            ));
                    callback.onSuccess(plans);
                })
                .addOnFailureListener(callback::onError);
    }

    public static NutritionSummary calculateTodaySummary(NutritionGoal goal, List<FoodLogEntry> logs) {
        NutritionGoal safeGoal = goal != null ? goal : defaultGoalForWeight(null, "duy trì");
        int calories = 0;
        int protein = 0;
        int carbs = 0;
        int fat = 0;
        if (logs != null) {
            for (FoodLogEntry entry : logs) {
                if (entry == null) {
                    continue;
                }
                calories += Math.max(0, entry.getCalories());
                protein += Math.max(0, entry.getProteinGrams());
                carbs += Math.max(0, entry.getCarbsGrams());
                fat += Math.max(0, entry.getFatGrams());
            }
        }
        return new NutritionSummary(safeGoal, calories, protein, carbs, fat);
    }

    public static NutritionGoal defaultGoalForWeight(Float weightKg, String goalType) {
        float safeWeight = weightKg != null && weightKg > 0 ? weightKg : 70f;
        String normalizedGoal = goalType != null && !goalType.trim().isEmpty() ? goalType.trim() : "duy trì";
        int protein = Math.round(safeWeight * 1.8f);
        int calories = Math.max(1600, Math.round(safeWeight * 30f));
        String normalizedGoalLower = normalizedGoal.toLowerCase(Locale.ROOT);
        if (normalizedGoalLower.contains("giảm")) {
            calories = Math.max(1600, calories - 250);
        } else if (normalizedGoalLower.contains("tăng")) {
            calories += 250;
        }
        int fat = Math.max(45, Math.round(safeWeight));
        int carbs = Math.max(120, Math.round((calories - protein * 4f - fat * 9f) / 4f));
        NutritionGoal goal = new NutritionGoal(calories, protein, carbs, fat, 2500);
        goal.setGoalType(normalizedGoal);
        return goal;
    }

    static FoodLogEntry sanitizeFoodLogForSave(FoodLogEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("food log entry is required");
        }
        String name = trimToMax(entry.getName(), MAX_FOOD_NAME_LENGTH);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("food name is required");
        }
        entry.setName(name);
        entry.setMealType(nonBlank(trimToMax(entry.getMealType(), MAX_MEAL_TYPE_LENGTH), "Bữa ăn"));
        entry.setServingText(emptyToNull(trimToMax(entry.getServingText(), MAX_SERVING_TEXT_LENGTH)));
        entry.setCategory(emptyToNull(normalizeCategory(trimToMax(entry.getCategory(), MAX_CATEGORY_LENGTH))));
        entry.setSource(normalizeSource(entry.getSource()));
        entry.setPlanImportKey(emptyToNull(trimToMax(entry.getPlanImportKey(), MAX_PLAN_IMPORT_KEY_LENGTH)));
        if (entry.getAiConfidence() != null) {
            entry.setAiConfidence(clampFloat(entry.getAiConfidence(), 0f, 1f));
        }
        entry.setNotes(emptyToNull(trimToMax(entry.getNotes(), MAX_NOTES_LENGTH)));
        entry.setCalories(clamp(entry.getCalories(), 0, MAX_CALORIES_PER_ENTRY));
        entry.setProteinGrams(clamp(entry.getProteinGrams(), 0, MAX_MACRO_GRAMS_PER_ENTRY));
        entry.setCarbsGrams(clamp(entry.getCarbsGrams(), 0, MAX_MACRO_GRAMS_PER_ENTRY));
        entry.setFatGrams(clamp(entry.getFatGrams(), 0, MAX_MACRO_GRAMS_PER_ENTRY));
        if (entry.getEatenAt() <= 0L) {
            entry.setEatenAt(System.currentTimeMillis());
        }
        entry.setUpdatedAt(System.currentTimeMillis());
        return entry;
    }

    static MealPlan sanitizeMealPlanForSave(MealPlan mealPlan) {
        if (mealPlan == null) {
            throw new IllegalArgumentException("meal plan is required");
        }
        if (mealPlan.getDays() == null || mealPlan.getDays().isEmpty()) {
            throw new IllegalArgumentException("meal plan days are required");
        }
        mealPlan.setTitle(nonBlank(trimToMax(mealPlan.getTitle(), MAX_MEAL_PLAN_TITLE_LENGTH), "Kế hoạch ăn"));
        if (mealPlan.getCreatedAt() <= 0L) {
            mealPlan.setCreatedAt(System.currentTimeMillis());
        }
        return mealPlan;
    }

    static void sortFoodLogsNewestFirst(List<FoodLogEntry> logs) {
        if (logs == null || logs.size() < 2) {
            return;
        }
        Collections.sort(logs, (left, right) ->
                Long.compare(
                        right != null ? right.getEatenAt() : 0L,
                        left != null ? left.getEatenAt() : 0L
                ));
    }

    private static String trimToMax(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private static String nonBlank(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value.trim() : fallback;
    }

    private static String emptyToNull(String value) {
        return value != null && !value.trim().isEmpty() ? value.trim() : null;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static String normalizeSource(String source) {
        if (source == null || source.trim().isEmpty()) {
            return FoodLogEntry.SOURCE_MANUAL;
        }
        String normalized = source.trim().toUpperCase(Locale.ROOT);
        if (FoodLogEntry.SOURCE_AI.equals(normalized)
                || FoodLogEntry.SOURCE_PLAN.equals(normalized)
                || FoodLogEntry.SOURCE_MANUAL.equals(normalized)) {
            return normalized;
        }
        return FoodLogEntry.SOURCE_MANUAL;
    }

    public static String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return FoodNutritionEstimate.CATEGORY_MIXED;
        }
        String normalized = category.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("protein") || normalized.contains("meat") || normalized.contains("chicken")
                || normalized.contains("fish") || normalized.contains("egg") || normalized.contains("beef")) {
            return FoodNutritionEstimate.CATEGORY_PROTEIN;
        }
        if (normalized.contains("carb") || normalized.contains("rice") || normalized.contains("bread")
                || normalized.contains("noodle") || normalized.contains("starch")) {
            return FoodNutritionEstimate.CATEGORY_CARB;
        }
        if (normalized.contains("veg") || normalized.contains("fruit") || normalized.contains("salad")) {
            return FoodNutritionEstimate.CATEGORY_VEG;
        }
        if (normalized.contains("snack") || normalized.contains("drink") || normalized.contains("dessert")) {
            return FoodNutritionEstimate.CATEGORY_SNACK;
        }
        if (FoodNutritionEstimate.CATEGORY_PROTEIN.equals(normalized)
                || FoodNutritionEstimate.CATEGORY_CARB.equals(normalized)
                || FoodNutritionEstimate.CATEGORY_VEG.equals(normalized)
                || FoodNutritionEstimate.CATEGORY_SNACK.equals(normalized)
                || FoodNutritionEstimate.CATEGORY_MIXED.equals(normalized)) {
            return normalized;
        }
        return FoodNutritionEstimate.CATEGORY_MIXED;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public interface NutritionSummaryCallback {
        void onSuccess(NutritionSummary summary);
        void onError(Exception e);
    }

    public interface FoodLogsCallback {
        void onSuccess(List<FoodLogEntry> logs);
        void onError(Exception e);
    }

    public interface FoodLogsSaveCallback {
        void onSuccess(List<FoodLogEntry> savedEntries);
        void onError(Exception e);
    }

    public interface MealPlansCallback {
        void onSuccess(List<MealPlan> plans);
        void onError(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(Exception e);
    }
}
