package ntu.quy65132908.smartgym_ai.ui.nutrition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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
import java.util.concurrent.atomic.AtomicReference;

import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.FoodLogEntry;
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
        assertTrue(preview.contains("+1 bữa"));
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

    private Meal meal(String type, String name) {
        Meal meal = new Meal();
        meal.setMealType(type);
        meal.setName(name);
        return meal;
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
