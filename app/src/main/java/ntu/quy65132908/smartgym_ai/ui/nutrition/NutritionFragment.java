package ntu.quy65132908.smartgym_ai.ui.nutrition;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.FoodLogEntry;
import ntu.quy65132908.smartgym_ai.data.model.FoodNutritionEstimate;
import ntu.quy65132908.smartgym_ai.data.model.MealPlanDay;
import ntu.quy65132908.smartgym_ai.data.model.NutritionGoal;
import ntu.quy65132908.smartgym_ai.databinding.FragmentNutritionBinding;

@AndroidEntryPoint
public class NutritionFragment extends Fragment {
    private static final SimpleDateFormat DATE_KEY_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private FragmentNutritionBinding binding;
    private NutritionViewModel viewModel;
    private FoodLogAdapter adapter;
    private FoodLogAdapter historyAdapter;
    private MealPlanDayAdapter mealPlanDayAdapter;
    private FoodNutritionEstimate pendingEstimate;
    private FoodLogEntry editingEntry;
    private MealPlanDay currentPlanDay;
    private boolean hasResumed;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNutritionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(NutritionViewModel.class);
        adapter = new FoodLogAdapter(new FoodLogAdapter.OnFoodLogActionListener() {
            @Override
            public void onEditFood(FoodLogEntry entry) {
                startEditing(entry);
            }

            @Override
            public void onDeleteFood(FoodLogEntry entry) {
                viewModel.deleteFoodLog(entry);
            }
        });
        binding.rvFoodLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvFoodLogs.setAdapter(adapter);
        historyAdapter = new FoodLogAdapter();
        binding.rvHistoryFoodLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistoryFoodLogs.setAdapter(historyAdapter);
        mealPlanDayAdapter = new MealPlanDayAdapter(day -> viewModel.logMealPlanDay(day));
        binding.rvMealPlanDays.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMealPlanDays.setAdapter(mealPlanDayAdapter);
        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        setupTabs();
        setupMealTypeDropdown();
        setupFormWatchers();
        binding.etHistoryDate.setText(DATE_KEY_FORMAT.format(new Date()));
        binding.btnEstimateFood.setOnClickListener(v -> viewModel.estimateFood(
                text(binding.etFoodName),
                text(binding.etServing),
                text(binding.etMealType)));
        binding.btnAddFood.setOnClickListener(v -> saveFoodFromForm());
        binding.btnCancelEstimate.setOnClickListener(v -> viewModel.cancelPendingEstimate());
        binding.btnGenerateMealPlan.setOnClickListener(v -> viewModel.generateMealPlan());
        binding.btnLogPlanMeal.setOnClickListener(v -> {
            if (currentPlanDay != null) {
                viewModel.logMealPlanDay(currentPlanDay);
            }
        });
        binding.btnLoadHistory.setOnClickListener(v -> viewModel.loadHistory(text(binding.etHistoryDate)));
        observe();
    }

    private void observe() {
        viewModel.getSummary().observe(getViewLifecycleOwner(), summary -> {
            if (summary == null) return;
            int progress = summary.getCaloriesPercent();
            NutritionGoal goal = summary.getGoal();
            binding.tvCaloriesValue.setText(getString(
                    R.string.nutrition_calories_format,
                    summary.getCaloriesConsumed(),
                    goal.getCalories()));
            if (summary.getCaloriesOverTarget() > 0) {
                binding.tvCaloriesOver.setVisibility(View.VISIBLE);
                binding.tvCaloriesOver.setText(getString(
                        R.string.nutrition_over_target_format,
                        summary.getCaloriesOverTarget()));
            } else {
                binding.tvCaloriesOver.setVisibility(View.GONE);
            }
            binding.tvMacrosValue.setText(getString(
                    R.string.nutrition_macros_format,
                    summary.getProteinConsumed(),
                    summary.getCarbsConsumed(),
                    summary.getFatConsumed()));
            binding.progressCalories.setProgress(progress);
            binding.tvProteinProgress.setText(getString(
                    R.string.nutrition_macro_progress_format,
                    getString(R.string.nutrition_protein_hint),
                    summary.getProteinConsumed(),
                    goal.getProteinGrams()));
            binding.progressProtein.setProgress(summary.getProteinPercent());
            binding.tvCarbsProgress.setText(getString(
                    R.string.nutrition_macro_progress_format,
                    getString(R.string.nutrition_carbs_hint),
                    summary.getCarbsConsumed(),
                    goal.getCarbsGrams()));
            binding.progressCarbs.setProgress(summary.getCarbsPercent());
            binding.tvFatProgress.setText(getString(
                    R.string.nutrition_macro_progress_format,
                    getString(R.string.nutrition_fat_hint),
                    summary.getFatConsumed(),
                    goal.getFatGrams()));
            binding.progressFat.setProgress(summary.getFatPercent());
            binding.progressCalories.setContentDescription(getString(
                    R.string.nutrition_calories_a11y,
                    summary.getCaloriesConsumed(),
                    goal.getCalories(),
                    progress));
        });
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            boolean hasLogs = !state.getFoodLogs().isEmpty();
            binding.tvFoodLogsEmpty.setVisibility(hasLogs ? View.GONE : View.VISIBLE);
            binding.tvFoodLogsEmpty.setText(state.getEmptyFoodLogsText());
            binding.rvFoodLogs.setVisibility(hasLogs ? View.VISIBLE : View.GONE);
            currentPlanDay = currentPlanDay(state.getMealPlan());
            boolean hasPlanDays = state.getMealPlan() != null
                    && state.getMealPlan().getDays() != null
                    && !state.getMealPlan().getDays().isEmpty();
            mealPlanDayAdapter.submitList(hasPlanDays ? state.getMealPlan().getDays() : null);
            binding.rvMealPlanDays.setVisibility(hasPlanDays ? View.VISIBLE : View.GONE);
            binding.btnLogPlanMeal.setVisibility(currentPlanDay != null ? View.VISIBLE : View.GONE);
        });
        viewModel.getFoodLogs().observe(getViewLifecycleOwner(), adapter::submitList);
        viewModel.getHistoryFoodLogs().observe(getViewLifecycleOwner(), logs -> {
            boolean hasLogs = logs != null && !logs.isEmpty();
            historyAdapter.submitList(logs);
            binding.rvHistoryFoodLogs.setVisibility(hasLogs ? View.VISIBLE : View.GONE);
            binding.tvHistoryHint.setVisibility(hasLogs ? View.GONE : View.VISIBLE);
            binding.tvHistoryHint.setText(getString(R.string.nutrition_history_empty_for_date));
        });
        viewModel.getHistorySummary().observe(getViewLifecycleOwner(), historySummary -> {
            if (historySummary == null) {
                binding.tvHistorySummary.setVisibility(View.GONE);
                return;
            }
            binding.tvHistorySummary.setVisibility(View.VISIBLE);
            binding.tvHistorySummary.setText(getString(
                    R.string.nutrition_history_summary_format,
                    text(binding.etHistoryDate),
                    historySummary.getCaloriesConsumed(),
                    historySummary.getGoal().getCalories(),
                    historySummary.getProteinConsumed(),
                    historySummary.getCarbsConsumed(),
                    historySummary.getFatConsumed()));
        });
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean busy = Boolean.TRUE.equals(loading);
            binding.progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        });
        viewModel.getIsGeneratingMealPlan().observe(getViewLifecycleOwner(), generating -> {
            boolean busy = Boolean.TRUE.equals(generating);
            binding.btnGenerateMealPlan.setEnabled(!busy);
            binding.btnGenerateMealPlan.setText(busy
                    ? R.string.nutrition_generating_plan
                    : R.string.nutrition_generate_plan);
        });
        viewModel.getIsEstimatingFood().observe(getViewLifecycleOwner(), estimating -> {
            boolean busy = Boolean.TRUE.equals(estimating);
            binding.btnEstimateFood.setEnabled(!busy);
            binding.btnEstimateFood.setText(busy
                    ? R.string.nutrition_estimating_food
                    : R.string.nutrition_estimate_food);
        });
        viewModel.getIsSavingFood().observe(getViewLifecycleOwner(), saving -> {
            boolean busy = Boolean.TRUE.equals(saving);
            setFoodFormEnabled(!busy);
            binding.btnAddFood.setText(busy
                    ? R.string.nutrition_btn_saving_food
                    : R.string.nutrition_btn_add_food);
        });
        viewModel.getCanAddFood().observe(getViewLifecycleOwner(),
                canAdd -> binding.btnAddFood.setEnabled(Boolean.TRUE.equals(canAdd)));
        viewModel.getFormErrors().observe(getViewLifecycleOwner(), this::renderFormErrors);
        viewModel.getClearFoodFormEvent().observe(getViewLifecycleOwner(), shouldClear -> {
            if (Boolean.TRUE.equals(shouldClear)) {
                clearFoodForm();
            }
        });
        viewModel.getPendingEstimate().observe(getViewLifecycleOwner(), estimate -> {
            pendingEstimate = estimate;
            renderPendingEstimate(estimate);
        });
        viewModel.getMealPlanPreview().observe(getViewLifecycleOwner(), preview -> {
            binding.tvMealPlanPreview.setText(preview != null && !preview.isEmpty()
                    ? preview
                    : getString(R.string.nutrition_empty_plan));
        });
        viewModel.getMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setupTabs() {
        binding.tabGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            binding.todayContent.setVisibility(checkedId == R.id.tab_today ? View.VISIBLE : View.GONE);
            binding.planContent.setVisibility(checkedId == R.id.tab_plan ? View.VISIBLE : View.GONE);
            binding.historyContent.setVisibility(checkedId == R.id.tab_history ? View.VISIBLE : View.GONE);
        });
    }

    private void setupMealTypeDropdown() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.nutrition_meal_types,
                android.R.layout.simple_list_item_1);
        binding.etMealType.setAdapter(adapter);
        binding.etMealType.setOnClickListener(v -> binding.etMealType.showDropDown());
    }

    private void setupFormWatchers() {
        TextWatcher watcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                viewModel.onFoodFormChanged(
                        text(binding.etFoodName),
                        text(binding.etMealType),
                        text(binding.etCalories),
                        text(binding.etProtein),
                        text(binding.etCarbs),
                        text(binding.etFat));
            }
        };
        binding.etFoodName.addTextChangedListener(watcher);
        binding.etServing.addTextChangedListener(watcher);
        binding.etMealType.addTextChangedListener(watcher);
        binding.etCalories.addTextChangedListener(watcher);
        binding.etProtein.addTextChangedListener(watcher);
        binding.etCarbs.addTextChangedListener(watcher);
        binding.etFat.addTextChangedListener(watcher);
    }

    private void renderFormErrors(NutritionFormErrors errors) {
        if (errors == null) return;
        binding.tilFoodName.setError(errors.getFoodNameError());
        binding.tilCalories.setError(errors.getCaloriesError());
        binding.tilProtein.setError(errors.getMacroError());
        binding.tilCarbs.setError(errors.getMacroError());
        binding.tilFat.setError(errors.getMacroError());
    }

    private void clearFoodForm() {
        binding.etFoodName.setText("");
        binding.etServing.setText("");
        binding.etCalories.setText("");
        binding.etProtein.setText("");
        binding.etCarbs.setText("");
        binding.etFat.setText("");
        binding.etMealType.setText(getString(R.string.nutrition_default_meal_type), false);
        binding.tilFoodName.setError(null);
        binding.tilCalories.setError(null);
        binding.tilProtein.setError(null);
        binding.tilCarbs.setError(null);
        binding.tilFat.setError(null);
        pendingEstimate = null;
        editingEntry = null;
        renderPendingEstimate(null);
    }

    private void setFoodFormEnabled(boolean enabled) {
        binding.etFoodName.setEnabled(enabled);
        binding.etServing.setEnabled(enabled);
        binding.etMealType.setEnabled(enabled);
        binding.etCalories.setEnabled(enabled);
        binding.etProtein.setEnabled(enabled);
        binding.etCarbs.setEnabled(enabled);
        binding.etFat.setEnabled(enabled);
    }

    private void saveFoodFromForm() {
        if (pendingEstimate != null) {
            viewModel.savePendingEstimate(
                    text(binding.etFoodName),
                    text(binding.etServing),
                    text(binding.etMealType),
                    text(binding.etCalories),
                    text(binding.etProtein),
                    text(binding.etCarbs),
                    text(binding.etFat));
        } else if (editingEntry != null) {
            viewModel.updateFoodLog(
                    editingEntry,
                    text(binding.etFoodName),
                    text(binding.etServing),
                    text(binding.etMealType),
                    text(binding.etCalories),
                    text(binding.etProtein),
                    text(binding.etCarbs),
                    text(binding.etFat));
        } else {
            viewModel.addFood(
                    text(binding.etFoodName),
                    text(binding.etServing),
                    text(binding.etMealType),
                    text(binding.etCalories),
                    text(binding.etProtein),
                    text(binding.etCarbs),
                    text(binding.etFat));
        }
    }

    private void renderPendingEstimate(FoodNutritionEstimate estimate) {
        boolean hasEstimate = estimate != null;
        if (hasEstimate) {
            editingEntry = null;
        }
        binding.estimateReviewCard.setVisibility(hasEstimate ? View.VISIBLE : View.GONE);
        binding.btnCancelEstimate.setVisibility(hasEstimate ? View.VISIBLE : View.GONE);
        binding.btnAddFood.setText(hasEstimate
                ? R.string.nutrition_save_estimate
                : R.string.nutrition_btn_add_food);
        if (!hasEstimate) {
            return;
        }
        binding.etFoodName.setText(estimate.getName());
        binding.etServing.setText(estimate.getServingText());
        binding.etMealType.setText(estimate.getMealType(), false);
        binding.etCalories.setText(String.valueOf(estimate.getCalories()));
        binding.etProtein.setText(String.valueOf(estimate.getProteinGrams()));
        binding.etCarbs.setText(String.valueOf(estimate.getCarbsGrams()));
        binding.etFat.setText(String.valueOf(estimate.getFatGrams()));
        binding.tvEstimateReviewNote.setText(estimate.getNotes() != null && !estimate.getNotes().trim().isEmpty()
                ? estimate.getNotes()
                : getString(R.string.nutrition_estimate_note));
    }

    private void startEditing(FoodLogEntry entry) {
        if (entry == null) {
            return;
        }
        editingEntry = entry;
        viewModel.cancelPendingEstimate();
        binding.etFoodName.setText(entry.getName());
        binding.etServing.setText(entry.getServingText());
        binding.etMealType.setText(entry.getMealType(), false);
        binding.etCalories.setText(String.valueOf(entry.getCalories()));
        binding.etProtein.setText(String.valueOf(entry.getProteinGrams()));
        binding.etCarbs.setText(String.valueOf(entry.getCarbsGrams()));
        binding.etFat.setText(String.valueOf(entry.getFatGrams()));
        binding.btnAddFood.setText(R.string.nutrition_update_food);
    }

    private MealPlanDay currentPlanDay(ntu.quy65132908.smartgym_ai.data.model.MealPlan plan) {
        if (plan == null || plan.getDays() == null) {
            return null;
        }
        int today = currentDayOfWeek();
        MealPlanDay fallback = null;
        for (MealPlanDay day : plan.getDays()) {
            if (day != null && day.getMeals() != null && !day.getMeals().isEmpty()) {
                if (fallback == null) {
                    fallback = day;
                }
                if (day.getDayOfWeek() == today) {
                    return day;
                }
            }
        }
        return fallback;
    }

    private int currentDayOfWeek() {
        int calendarDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        if (calendarDay == Calendar.SUNDAY) {
            return 7;
        }
        return calendarDay - 1;
    }

    private String text(TextView input) {
        return input.getText() != null ? input.getText().toString() : "";
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasResumed && viewModel != null) {
            viewModel.reload();
        }
        hasResumed = true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
