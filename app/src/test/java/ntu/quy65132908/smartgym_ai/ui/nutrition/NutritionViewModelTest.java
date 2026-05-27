package ntu.quy65132908.smartgym_ai.ui.nutrition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.FoodLogEntry;
import ntu.quy65132908.smartgym_ai.data.model.FoodNutritionEstimate;
import ntu.quy65132908.smartgym_ai.data.model.Meal;
import ntu.quy65132908.smartgym_ai.data.model.MealPlan;
import ntu.quy65132908.smartgym_ai.data.model.MealPlanDay;
import ntu.quy65132908.smartgym_ai.data.model.NutritionGoal;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.DeepSeekRepository;
import ntu.quy65132908.smartgym_ai.data.repository.NutritionRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;

@RunWith(RobolectricTestRunner.class)
public class NutritionViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private AuthRepository authRepository;
    @Mock private UserRepository userRepository;
    @Mock private NutritionRepository nutritionRepository;
    @Mock private DeepSeekRepository deepSeekRepository;
    @Mock private FirebaseUser firebaseUser;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(firebaseUser.getUid()).thenReturn("test-uid");
        when(authRepository.getCurrentUser()).thenReturn(firebaseUser);
        when(authRepository.isLoggedIn()).thenReturn(true);
    }

    private NutritionViewModel createViewModel() {
        doAnswer(invocation -> {
            UserRepository.UserCallback cb = invocation.getArgument(1);
            User user = new User("test-uid", "Test", "test@email.com");
            user.setWeight(70f);
            user.setGoal("duy trì");
            cb.onSuccess(user);
            return null;
        }).when(userRepository).getUser(eq("test-uid"), any());

        doAnswer(invocation -> {
            NutritionRepository.FoodLogsCallback cb = invocation.getArgument(2);
            cb.onSuccess(Collections.emptyList());
            return null;
        }).when(nutritionRepository).getTodayFoodLogs(eq("test-uid"), any(), any());

        return new NutritionViewModel(
                RuntimeEnvironment.getApplication(),
                authRepository,
                userRepository,
                nutritionRepository,
                deepSeekRepository
        );
    }

    @Test
    public void formatMealPlanPreview_includesMultipleDaysAndMealCount() {
        MealPlan plan = new MealPlan();
        plan.setTitle("Kế hoạch 7 ngày");
        MealPlanDay day1 = new MealPlanDay();
        day1.setDayOfWeek(1);
        day1.setDayLabel("Thứ 2");
        day1.setTargetCalories(2200);
        day1.setMeals(Arrays.asList(meal("Sáng", "Yến mạch"), meal("Trưa", "Cơm gà"), meal("Tối", "Cá")));
        MealPlanDay day2 = new MealPlanDay();
        day2.setDayOfWeek(2);
        day2.setDayLabel("Thứ 3");
        day2.setMeals(Collections.singletonList(meal("Sáng", "Bánh mì")));
        plan.setDays(Arrays.asList(day1, day2));

        String preview = NutritionViewModel.formatMealPlanPreview(plan);

        assertTrue(preview.contains("Kế hoạch 7 ngày"));
        assertTrue(preview.contains("Thứ 2"));
        assertTrue(preview.contains("Yến mạch"));
        assertTrue(preview.contains("Cá"));
        assertFalse(preview.contains("+"));
        assertTrue(preview.contains("Thứ 3"));
    }

    @Test
    public void formatMealPlanPreview_emptyPlan_returnsEmptyString() {
        assertEquals("", NutritionViewModel.formatMealPlanPreview(new MealPlan()));
    }

    @Test
    public void addFood_invalidNameDoesNotCallRepository() {
        NutritionViewModel viewModel = createViewModel();

        viewModel.addFood("", "Sáng", "300", "20", "30", "10");

        assertEquals(RuntimeEnvironment.getApplication().getString(R.string.nutrition_food_name_error),
                viewModel.getFormErrors().getValue().getFoodNameError());
        assertFalse(Boolean.TRUE.equals(viewModel.getCanAddFood().getValue()));
    }

    @Test
    public void addFood_validInputCallsRepositoryAndProducesPreview() {
        NutritionViewModel viewModel = createViewModel();
        doAnswer(invocation -> {
            NutritionRepository.SimpleCallback cb = invocation.getArgument(3);
            cb.onSuccess();
            return null;
        }).when(nutritionRepository).saveFoodLog(eq("test-uid"), any(), any(), any());

        viewModel.addFood("Yến mạch", "Sáng", "420", "22", "58", "10");

        assertEquals("Đã lưu bữa ăn.",
                viewModel.getMessage().getValue());
        assertTrue(Boolean.TRUE.equals(viewModel.getClearFoodFormEvent().getValue()));
    }

    @Test
    public void addFood_saveFailureKeepsLogsEmptyAndShowsError() {
        NutritionViewModel viewModel = createViewModel();
        doAnswer(invocation -> {
            NutritionRepository.SimpleCallback cb = invocation.getArgument(3);
            cb.onError(new Exception("network"));
            return null;
        }).when(nutritionRepository).saveFoodLog(eq("test-uid"), any(), any(), any());

        viewModel.addFood("Yến mạch", "Sáng", "420", "22", "58", "10");

        assertEquals("Không thể lưu bữa ăn. Vui lòng kiểm tra mạng và thử lại.",
                viewModel.getMessage().getValue());
        assertTrue(viewModel.getFoodLogs().getValue().isEmpty());
        assertFalse(Boolean.TRUE.equals(viewModel.getIsSavingFood().getValue()));
    }

    @Test
    public void formatMealPlanPreview_includesEveryMealWithoutCollapsedCount() {
        MealPlan plan = new MealPlan();
        MealPlanDay day = new MealPlanDay();
        day.setDayOfWeek(1);
        day.setDayLabel("Monday");
        day.setTargetCalories(2200);
        day.setMeals(Arrays.asList(
                meal("Breakfast", "Oats"),
                meal("Lunch", "Chicken rice"),
                meal("Dinner", "Fish and vegetables"),
                meal("Snack", "Yogurt")
        ));
        plan.setDays(Collections.singletonList(day));

        String preview = NutritionViewModel.formatMealPlanPreview(plan);

        assertTrue(preview.contains("Breakfast - Oats"));
        assertTrue(preview.contains("Lunch - Chicken rice"));
        assertTrue(preview.contains("Dinner - Fish and vegetables"));
        assertTrue(preview.contains("Snack - Yogurt"));
        assertFalse(preview.contains("+"));
    }

    @Test
    public void estimateFood_successPublishesReviewEstimate() {
        NutritionViewModel viewModel = createViewModel();
        doAnswer(invocation -> {
            DeepSeekRepository.FoodEstimateCallback cb = invocation.getArgument(4);
            cb.onSuccess(new FoodNutritionEstimate(
                    "Chicken rice",
                    "1 bowl",
                    "Lunch",
                    FoodNutritionEstimate.CATEGORY_PROTEIN,
                    720,
                    38,
                    90,
                    12,
                    0.82f,
                    "Review before saving"
            ));
            return null;
        }).when(deepSeekRepository).estimateFoodNutritionData(any(), any(), any(), any(), any());

        viewModel.estimateFood("Chicken rice", "1 bowl", "Lunch");

        assertFalse(Boolean.TRUE.equals(viewModel.getIsEstimatingFood().getValue()));
        assertEquals("Chicken rice", viewModel.getPendingEstimate().getValue().getName());
        assertEquals(720, viewModel.getPendingEstimate().getValue().getCalories());
    }

    @Test
    public void savePendingEstimate_persistsAiSourceAndClearsReview() {
        NutritionViewModel viewModel = createViewModel();
        doAnswer(invocation -> {
            DeepSeekRepository.FoodEstimateCallback cb = invocation.getArgument(4);
            cb.onSuccess(new FoodNutritionEstimate(
                    "Chicken rice",
                    "1 bowl",
                    "Lunch",
                    FoodNutritionEstimate.CATEGORY_PROTEIN,
                    720,
                    38,
                    90,
                    12,
                    0.82f,
                    "Review before saving"
            ));
            return null;
        }).when(deepSeekRepository).estimateFoodNutritionData(any(), any(), any(), any(), any());
        doAnswer(invocation -> {
            NutritionRepository.SimpleCallback cb = invocation.getArgument(3);
            cb.onSuccess();
            return null;
        }).when(nutritionRepository).saveFoodLog(eq("test-uid"), any(), any(), any());

        viewModel.estimateFood("Chicken rice", "1 bowl", "Lunch");
        viewModel.savePendingEstimate("Chicken rice", "1 bowl", "Lunch", "720", "38", "90", "12");

        assertEquals(RuntimeEnvironment.getApplication().getString(R.string.nutrition_food_saved),
                viewModel.getMessage().getValue());
        assertEquals(null, viewModel.getPendingEstimate().getValue());
    }

    @Test
    public void deleteFoodLog_successRemovesItemFromTodayLogs() {
        FoodLogEntry saved = new FoodLogEntry("food-1", "Chicken rice", "Lunch", 720, 38, 90, 12, 1L);
        doAnswer(invocation -> {
            UserRepository.UserCallback cb = invocation.getArgument(1);
            cb.onSuccess(new User("test-uid", "Test", "test@email.com"));
            return null;
        }).when(userRepository).getUser(eq("test-uid"), any());
        doAnswer(invocation -> {
            NutritionRepository.FoodLogsCallback cb = invocation.getArgument(2);
            cb.onSuccess(Collections.singletonList(saved));
            return null;
        }).when(nutritionRepository).getTodayFoodLogs(eq("test-uid"), any(), any());
        NutritionViewModel viewModel = new NutritionViewModel(
                RuntimeEnvironment.getApplication(),
                authRepository,
                userRepository,
                nutritionRepository,
                deepSeekRepository
        );
        doAnswer(invocation -> {
            NutritionRepository.SimpleCallback cb = invocation.getArgument(3);
            cb.onSuccess();
            return null;
        }).when(nutritionRepository).deleteFoodLog(eq("test-uid"), any(), eq("food-1"), any());

        viewModel.deleteFoodLog(saved);

        assertTrue(viewModel.getFoodLogs().getValue().isEmpty());
    }

    @Test
    public void updateFoodLog_successReplacesItemInTodayLogs() {
        FoodLogEntry saved = new FoodLogEntry("food-1", "Chicken rice", "Lunch", 720, 38, 90, 12, 1L);
        doAnswer(invocation -> {
            UserRepository.UserCallback cb = invocation.getArgument(1);
            cb.onSuccess(new User("test-uid", "Test", "test@email.com"));
            return null;
        }).when(userRepository).getUser(eq("test-uid"), any());
        doAnswer(invocation -> {
            NutritionRepository.FoodLogsCallback cb = invocation.getArgument(2);
            cb.onSuccess(Collections.singletonList(saved));
            return null;
        }).when(nutritionRepository).getTodayFoodLogs(eq("test-uid"), any(), any());
        NutritionViewModel viewModel = new NutritionViewModel(
                RuntimeEnvironment.getApplication(),
                authRepository,
                userRepository,
                nutritionRepository,
                deepSeekRepository
        );
        doAnswer(invocation -> {
            NutritionRepository.SimpleCallback cb = invocation.getArgument(4);
            cb.onSuccess();
            return null;
        }).when(nutritionRepository).updateFoodLog(eq("test-uid"), any(), eq("food-1"), any(), any());

        viewModel.updateFoodLog(saved, "Chicken rice extra", "1.5 bowls", "Lunch", "900", "50", "110", "16");

        FoodLogEntry updated = viewModel.getFoodLogs().getValue().get(0);
        assertEquals("Chicken rice extra", updated.getName());
        assertEquals("1.5 bowls", updated.getServingText());
        assertEquals(900, updated.getCalories());
    }

    @Test
    public void generateMealPlan_emptyResponseShowsFriendlyError() {
        NutritionViewModel viewModel = createViewModel();
        doAnswer(invocation -> {
            DeepSeekRepository.MealPlanCallback cb = invocation.getArgument(2);
            cb.onSuccess(new MealPlan());
            return null;
        }).when(deepSeekRepository).generateMealPlanData(any(), any(), any());

        viewModel.generateMealPlan();

        assertEquals("AI chưa tạo được kế hoạch ăn hợp lệ. Vui lòng thử lại.",
                viewModel.getMessage().getValue());
    }

    @Test
    public void generateMealPlan_setsLoadingUntilCallbackCompletes() {
        NutritionViewModel viewModel = createViewModel();
        AtomicReference<DeepSeekRepository.MealPlanCallback> aiCallback = new AtomicReference<>();
        doAnswer(invocation -> {
            aiCallback.set(invocation.getArgument(2));
            return null;
        }).when(deepSeekRepository).generateMealPlanData(any(), any(), any());
        doAnswer(invocation -> {
            NutritionRepository.SimpleCallback cb = invocation.getArgument(2);
            cb.onSuccess();
            return null;
        }).when(nutritionRepository).saveMealPlan(eq("test-uid"), any(), any());

        viewModel.generateMealPlan();

        assertTrue(Boolean.TRUE.equals(viewModel.getIsGeneratingMealPlan().getValue()));
        assertTrue(Boolean.TRUE.equals(viewModel.getIsLoading().getValue()));

        aiCallback.get().onSuccess(mealPlanWithOneDay());

        assertFalse(Boolean.TRUE.equals(viewModel.getIsGeneratingMealPlan().getValue()));
        assertFalse(Boolean.TRUE.equals(viewModel.getIsLoading().getValue()));
        assertTrue(viewModel.getMealPlanPreview().getValue().contains("Thứ 2"));
    }

    @Test
    public void generateMealPlan_dayWithoutMealsShowsFriendlyError() {
        NutritionViewModel viewModel = createViewModel();
        MealPlan plan = new MealPlan();
        MealPlanDay day = new MealPlanDay();
        day.setDayLabel("Thứ 2");
        plan.setDays(Collections.singletonList(day));
        doAnswer(invocation -> {
            DeepSeekRepository.MealPlanCallback cb = invocation.getArgument(2);
            cb.onSuccess(plan);
            return null;
        }).when(deepSeekRepository).generateMealPlanData(any(), any(), any());

        viewModel.generateMealPlan();

        assertEquals("AI chưa tạo được kế hoạch ăn hợp lệ. Vui lòng thử lại.",
                viewModel.getMessage().getValue());
    }

    @Test
    public void generateMealPlan_aiErrorSavesFallbackMealPlan() {
        NutritionViewModel viewModel = createViewModel();
        AtomicReference<MealPlan> savedPlan = new AtomicReference<>();
        doAnswer(invocation -> {
            DeepSeekRepository.MealPlanCallback cb = invocation.getArgument(2);
            cb.onError(new IllegalStateException("DeepSeek API key is not configured."));
            return null;
        }).when(deepSeekRepository).generateMealPlanData(any(), any(), any());
        doAnswer(invocation -> {
            savedPlan.set(invocation.getArgument(1));
            NutritionRepository.SimpleCallback cb = invocation.getArgument(2);
            cb.onSuccess();
            return null;
        }).when(nutritionRepository).saveMealPlan(eq("test-uid"), any(), any());

        viewModel.generateMealPlan();

        assertFalse(Boolean.TRUE.equals(viewModel.getIsGeneratingMealPlan().getValue()));
        assertTrue(viewModel.getMessage().getValue().contains("kế hoạch ăn cơ bản"));
        assertEquals(7, savedPlan.get().getDays().size());
        assertTrue(viewModel.getMealPlanPreview().getValue().contains("Kế hoạch ăn cơ bản 7 ngày"));
    }

    @Test
    public void logMealPlanDay_savesAllMealsAndSkipsDuplicates() {
        NutritionViewModel viewModel = createViewModel();
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<FoodLogEntry> entries = invocation.getArgument(2);
            NutritionRepository.FoodLogsSaveCallback cb = invocation.getArgument(3);
            cb.onSuccess(entries);
            return null;
        }).when(nutritionRepository).saveFoodLogs(eq("test-uid"), any(), any(), any());
        MealPlanDay day = new MealPlanDay();
        day.setMeals(Arrays.asList(
                meal("Breakfast", "Oats", 420),
                meal("Lunch", "Chicken rice", 610),
                meal("Dinner", "Fish and vegetables", 560)
        ));

        viewModel.logMealPlanDay(day);
        viewModel.logMealPlanDay(day);

        verify(nutritionRepository, times(1)).saveFoodLogs(eq("test-uid"), any(), any(), any());
        List<FoodLogEntry> logs = viewModel.getFoodLogs().getValue();
        assertEquals(3, logs.size());
        assertTrue(containsFood(logs, "Oats"));
        assertTrue(containsFood(logs, "Chicken rice"));
        assertTrue(containsFood(logs, "Fish and vegetables"));
    }

    @Test
    public void loadHistory_loadsSelectedDateWithoutChangingTodayLogs() {
        FoodLogEntry today = new FoodLogEntry("today-1", "Today oats", "Breakfast", 420, 22, 58, 10, 1L);
        doAnswer(invocation -> {
            UserRepository.UserCallback cb = invocation.getArgument(1);
            cb.onSuccess(new User("test-uid", "Test", "test@email.com"));
            return null;
        }).when(userRepository).getUser(eq("test-uid"), any());
        doAnswer(invocation -> {
            NutritionRepository.FoodLogsCallback cb = invocation.getArgument(2);
            cb.onSuccess(Collections.singletonList(today));
            return null;
        }).when(nutritionRepository).getTodayFoodLogs(eq("test-uid"), any(), any());
        NutritionViewModel viewModel = new NutritionViewModel(
                RuntimeEnvironment.getApplication(),
                authRepository,
                userRepository,
                nutritionRepository,
                deepSeekRepository
        );
        FoodLogEntry history = new FoodLogEntry("history-1", "History rice", "Lunch", 610, 48, 72, 14, 2L);
        doAnswer(invocation -> {
            NutritionRepository.FoodLogsCallback cb = invocation.getArgument(2);
            cb.onSuccess(Collections.singletonList(history));
            return null;
        }).when(nutritionRepository).getFoodLogsForDate(eq("test-uid"), eq("2026-05-26"), any());

        viewModel.loadHistory("2026-05-26");

        assertEquals("Today oats", viewModel.getFoodLogs().getValue().get(0).getName());
        assertEquals("History rice", viewModel.getHistoryFoodLogs().getValue().get(0).getName());
        assertEquals(610, viewModel.getHistorySummary().getValue().getCaloriesConsumed());
        assertEquals("2026-05-26", viewModel.getSelectedHistoryDateKey().getValue());
    }

    private Meal meal(String type, String name) {
        Meal meal = new Meal();
        meal.setMealType(type);
        meal.setName(name);
        return meal;
    }

    private Meal meal(String type, String name, int calories) {
        Meal meal = meal(type, name);
        meal.setCalories(calories);
        meal.setProteinGrams(20);
        meal.setCarbsGrams(30);
        meal.setFatGrams(10);
        return meal;
    }

    private boolean containsFood(List<FoodLogEntry> logs, String name) {
        for (FoodLogEntry log : logs) {
            if (log != null && name.equals(log.getName())) {
                return true;
            }
        }
        return false;
    }

    private MealPlan mealPlanWithOneDay() {
        MealPlan plan = new MealPlan();
        plan.setTitle("Kế hoạch ăn");
        MealPlanDay day = new MealPlanDay();
        day.setDayOfWeek(1);
        day.setDayLabel("Thứ 2");
        day.setMeals(Collections.singletonList(meal("Sáng", "Yến mạch")));
        plan.setDays(Collections.singletonList(day));
        return plan;
    }
}
