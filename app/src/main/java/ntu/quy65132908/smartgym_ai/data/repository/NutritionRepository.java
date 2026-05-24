package ntu.quy65132908.smartgym_ai.data.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import ntu.quy65132908.smartgym_ai.data.model.FoodLogEntry;
import ntu.quy65132908.smartgym_ai.data.model.MealPlan;
import ntu.quy65132908.smartgym_ai.data.model.NutritionGoal;
import ntu.quy65132908.smartgym_ai.data.model.NutritionSummary;

@Singleton
public class NutritionRepository {
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
        firestore.collection("users").document(uid)
                .collection("nutritionDays").document(dateKey)
                .collection("foodLogs")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<FoodLogEntry> logs = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        FoodLogEntry entry = doc.toObject(FoodLogEntry.class);
                        entry.setId(doc.getId());
                        logs.add(entry);
                    }
                    sortFoodLogsNewestFirst(logs);
                    callback.onSuccess(logs);
                })
                .addOnFailureListener(callback::onError);
    }

    public void saveFoodLog(String uid, String dateKey, FoodLogEntry entry, SimpleCallback callback) {
        if (entry.getEatenAt() <= 0) {
            entry.setEatenAt(System.currentTimeMillis());
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

    public void saveMealPlan(String uid, MealPlan mealPlan, SimpleCallback callback) {
        mealPlan.setCreatedAt(System.currentTimeMillis());
        firestore.collection("users").document(uid)
                .collection("mealPlans")
                .add(mealPlan)
                .addOnSuccessListener(ref -> {
                    mealPlan.setId(ref.getId());
                    callback.onSuccess();
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
        if (normalizedGoal.toLowerCase().contains("giảm")) {
            calories = Math.max(1600, calories - 250);
        } else if (normalizedGoal.toLowerCase().contains("tăng")) {
            calories += 250;
        }
        int fat = Math.max(45, Math.round(safeWeight));
        int carbs = Math.max(120, Math.round((calories - protein * 4f - fat * 9f) / 4f));
        NutritionGoal goal = new NutritionGoal(calories, protein, carbs, fat, 2500);
        goal.setGoalType(normalizedGoal);
        return goal;
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

    public interface NutritionSummaryCallback {
        void onSuccess(NutritionSummary summary);
        void onError(Exception e);
    }

    public interface FoodLogsCallback {
        void onSuccess(List<FoodLogEntry> logs);
        void onError(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(Exception e);
    }
}
