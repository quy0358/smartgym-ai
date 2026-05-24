package ntu.quy65132908.smartgym_ai.ui.dashboard;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ContextThemeWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;

import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.databinding.FragmentDashboardBinding;

@RunWith(RobolectricTestRunner.class)
public class DashboardRendererTest {

    private Context context;
    private FragmentDashboardBinding binding;
    private WeeklyPlanAdapter adapter;

    @Before
    public void setup() {
        context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_SmartGymAI);
        binding = FragmentDashboardBinding.inflate(LayoutInflater.from(context));
        adapter = new WeeklyPlanAdapter();
    }

    @Test
    public void renderNoPlan_showsEmptyWorkoutCardAndEmptyPlan() {
        DashboardUiState state = new DashboardUiState(
                "Bạn",
                "",
                null,
                null,
                "",
                R.color.on_surface_variant,
                "--",
                TodayState.NO_PLAN,
                null,
                Collections.emptyList(),
                false,
                false,
                false
        );

        DashboardRenderer.render(binding, context, adapter, state);

        assertEquals(View.VISIBLE, binding.cardAiWorkout.getVisibility());
        assertEquals(View.GONE, binding.cardRestDay.getVisibility());
        assertEquals(context.getString(R.string.ai_empty_workout_title), binding.tvWorkoutTitle.getText().toString());
        assertEquals(context.getString(R.string.btn_create_plan), binding.btnStartWorkout.getText().toString());
        assertEquals(View.VISIBLE, binding.layoutEmptyPlan.getVisibility());
        assertEquals(View.GONE, binding.rvWeeklyPlan.getVisibility());
    }

    @Test
    public void renderWorkout_showsRecommendationAndPlanList() {
        Workout workout = new Workout("today", "Upper Body", "", "Medium", 45);
        workout.setExerciseCount(4);

        DashboardUiState state = new DashboardUiState(
                "Quy",
                "",
                72,
                22.5f,
                "Bình thường",
                R.color.primary,
                "65",
                TodayState.WORKOUT,
                workout,
                Collections.singletonList(workout),
                false,
                false,
                false
        );

        DashboardRenderer.render(binding, context, adapter, state);

        assertEquals(View.VISIBLE, binding.cardAiWorkout.getVisibility());
        assertEquals(View.GONE, binding.cardRestDay.getVisibility());
        assertEquals("Upper Body", binding.tvWorkoutTitle.getText().toString());
        assertEquals(context.getString(R.string.btn_start), binding.btnStartWorkout.getText().toString());
        assertEquals(View.GONE, binding.layoutEmptyPlan.getVisibility());
        assertEquals(View.VISIBLE, binding.rvWeeklyPlan.getVisibility());
        assertEquals(context.getString(R.string.stat_weight_a11y, "72"),
                binding.statWeight.getRoot().getContentDescription().toString());
    }

    @Test
    public void renderRestDay_showsRestCard() {
        DashboardUiState state = new DashboardUiState(
                "Quy",
                "",
                null,
                null,
                "",
                R.color.on_surface_variant,
                "--",
                TodayState.REST_DAY,
                null,
                Collections.singletonList(new Workout("rest", "Rest day", "", "", 0)),
                false,
                false,
                false
        );

        DashboardRenderer.render(binding, context, adapter, state);

        assertEquals(View.GONE, binding.cardAiWorkout.getVisibility());
        assertEquals(View.VISIBLE, binding.cardRestDay.getVisibility());
        assertEquals(context.getString(R.string.rest_day_a11y),
                binding.cardRestDay.getContentDescription().toString());
    }

    @Test
    public void renderInitialLoading_hidesContent() {
        DashboardUiState state = DashboardUiState.initial();

        DashboardRenderer.render(binding, context, adapter, state);

        assertEquals(View.VISIBLE, binding.progressBar.getVisibility());
        assertEquals(View.VISIBLE, binding.tvLoading.getVisibility());
        assertEquals(View.GONE, binding.scrollView.getVisibility());
    }
}
