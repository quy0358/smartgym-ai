package ntu.quy65132908.smartgym_ai.ui.dashboard;

import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.Locale;

import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.databinding.FragmentDashboardBinding;
import ntu.quy65132908.smartgym_ai.util.AvatarHelper;

final class DashboardRenderer {

    private DashboardRenderer() {}

    static void render(FragmentDashboardBinding binding,
                       Context context,
                       WeeklyPlanAdapter weeklyPlanAdapter,
                       DashboardUiState state) {
        if (state == null) {
            return;
        }

        AvatarHelper.loadAvatar(
                context,
                state.getPhotoUrl(),
                binding.ivAvatar,
                binding.tvAvatar,
                state.getUserName());
        binding.tvGreeting.setText(context.getString(R.string.greeting_format, state.getUserName()));
        binding.tvGreeting.setContentDescription(context.getString(R.string.greeting_a11y, state.getUserName()));

        renderStats(binding, context, state);
        renderTodayCard(binding, context, state);
        renderWeeklyPlan(binding, weeklyPlanAdapter, state);
        renderLoading(binding, state);
    }

    private static void renderStats(FragmentDashboardBinding binding,
                                    Context context,
                                    DashboardUiState state) {
        Integer weight = state.getWeight();
        boolean hasWeight = weight != null;
        String weightText = hasWeight ? String.valueOf(weight) : context.getString(R.string.value_unavailable);
        binding.statWeight.tvStatValue.setText(weightText);
        binding.statWeight.tvStatUnit.setVisibility(hasWeight ? View.VISIBLE : View.INVISIBLE);
        binding.statWeight.getRoot().setContentDescription(hasWeight
                ? context.getString(R.string.stat_weight_a11y, weightText)
                : context.getString(R.string.stat_weight_unavailable_a11y));

        Float bmi = state.getBmi();
        boolean hasBmi = bmi != null;
        String bmiText = hasBmi
                ? String.format(Locale.getDefault(), "%.1f", bmi)
                : context.getString(R.string.value_unavailable);
        String bmiCategory = state.getBmiCategory();
        String bmiUnitText = hasBmi && bmiCategory != null && !bmiCategory.isEmpty()
                ? bmiCategory
                : context.getString(R.string.value_unavailable);
        binding.statBmi.tvStatValue.setText(bmiText);
        binding.statBmi.tvStatUnit.setText(bmiUnitText);
        int bmiColor = ContextCompat.getColor(context, state.getBmiColorRes());
        binding.statBmi.tvStatValue.setTextColor(bmiColor);
        binding.statBmi.tvStatUnit.setTextColor(bmiColor);
        binding.statBmi.getRoot().setContentDescription(hasBmi
                ? context.getString(R.string.stat_bmi_a11y, bmiText, bmiUnitText)
                : context.getString(R.string.stat_bmi_unavailable_a11y));

        String goalDisplay = state.getGoalDisplay();
        boolean hasGoal = goalDisplay != null
                && !goalDisplay.equals(context.getString(R.string.value_unavailable))
                && !goalDisplay.equals("--");
        binding.statGoal.tvStatValue.setText(hasGoal ? goalDisplay : context.getString(R.string.value_unavailable));
        binding.statGoal.tvStatUnit.setVisibility(hasGoal ? View.VISIBLE : View.INVISIBLE);
        binding.statGoal.getRoot().setContentDescription(hasGoal
                ? context.getString(R.string.stat_goal_a11y, goalDisplay)
                : context.getString(R.string.stat_goal_unavailable_a11y));
    }

    private static void renderTodayCard(FragmentDashboardBinding binding,
                                        Context context,
                                        DashboardUiState state) {
        switch (state.getTodayState()) {
            case WORKOUT:
                binding.cardAiWorkout.setVisibility(View.VISIBLE);
                binding.cardRestDay.setVisibility(View.GONE);
                renderWorkoutRecommendation(binding, context, state.getAiRecommendation());
                break;
            case REST_DAY:
                binding.cardAiWorkout.setVisibility(View.GONE);
                binding.cardRestDay.setVisibility(View.VISIBLE);
                binding.cardRestDay.setContentDescription(context.getString(R.string.rest_day_a11y));
                break;
            case NO_PLAN:
            default:
                binding.cardAiWorkout.setVisibility(View.VISIBLE);
                binding.cardRestDay.setVisibility(View.GONE);
                binding.tvWorkoutTitle.setText(R.string.ai_empty_workout_title);
                binding.tvWorkoutSubtitle.setText(R.string.ai_empty_workout_subtitle);
                binding.btnStartWorkout.setText(R.string.btn_create_plan);
                break;
        }
    }

    private static void renderWorkoutRecommendation(FragmentDashboardBinding binding,
                                                    Context context,
                                                    @Nullable Workout workout) {
        if (workout == null) {
            binding.tvWorkoutTitle.setText(R.string.ai_empty_workout_title);
            binding.tvWorkoutSubtitle.setText(R.string.ai_empty_workout_subtitle);
            binding.btnStartWorkout.setText(R.string.btn_create_plan);
            return;
        }

        binding.tvWorkoutTitle.setText(workout.getTitle() != null && !workout.getTitle().isEmpty()
                ? workout.getTitle()
                : context.getString(R.string.workout_untitled));
        int exerciseCount = workout.getExerciseCount();
        String intensity = workout.getIntensity() != null ? workout.getIntensity() : "";
        if (exerciseCount > 0) {
            binding.tvWorkoutSubtitle.setText(context.getString(
                    R.string.workout_subtitle_format,
                    exerciseCount,
                    workout.getDurationMinutes(),
                    intensity));
        } else {
            binding.tvWorkoutSubtitle.setText(context.getString(
                    R.string.workout_subtitle_without_count_format,
                    workout.getDurationMinutes(),
                    intensity));
        }
        binding.btnStartWorkout.setText(R.string.btn_start);
    }

    private static void renderWeeklyPlan(FragmentDashboardBinding binding,
                                         WeeklyPlanAdapter weeklyPlanAdapter,
                                         DashboardUiState state) {
        boolean isEmpty = state.getWeeklyPlan().isEmpty();
        binding.rvWeeklyPlan.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.layoutEmptyPlan.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        weeklyPlanAdapter.submitList(state.getWeeklyPlan());
    }

    private static void renderLoading(FragmentDashboardBinding binding, DashboardUiState state) {
        boolean loading = state.isInitialLoading();
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.tvLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.scrollView.setVisibility(loading ? View.GONE : View.VISIBLE);
        binding.swipeRefresh.setRefreshing(state.isRefreshing());
    }
}
