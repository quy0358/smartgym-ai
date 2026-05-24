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

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.databinding.FragmentNutritionBinding;

@AndroidEntryPoint
public class NutritionFragment extends Fragment {
    private FragmentNutritionBinding binding;
    private NutritionViewModel viewModel;
    private FoodLogAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNutritionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(NutritionViewModel.class);
        adapter = new FoodLogAdapter();
        binding.rvFoodLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvFoodLogs.setAdapter(adapter);
        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        setupMealTypeDropdown();
        setupFormWatchers();
        binding.btnAddFood.setOnClickListener(v -> viewModel.addFood(
                text(binding.etFoodName),
                text(binding.etMealType),
                text(binding.etCalories),
                text(binding.etProtein),
                text(binding.etCarbs),
                text(binding.etFat)
        ));
        binding.btnGenerateMealPlan.setOnClickListener(v -> viewModel.generateMealPlan());
        observe();
    }

    private void observe() {
        viewModel.getSummary().observe(getViewLifecycleOwner(), summary -> {
            if (summary == null) return;
            int progress = summary.getCaloriesPercent();
            binding.tvCaloriesValue.setText(getString(
                    R.string.nutrition_calories_format,
                    summary.getCaloriesConsumed(),
                    summary.getGoal().getCalories()));
            binding.tvMacrosValue.setText(getString(
                    R.string.nutrition_macros_format,
                    summary.getProteinConsumed(),
                    summary.getCarbsConsumed(),
                    summary.getFatConsumed()));
            binding.progressCalories.setProgress(progress);
            binding.progressCalories.setContentDescription(getString(
                    R.string.nutrition_calories_a11y,
                    summary.getCaloriesConsumed(),
                    summary.getGoal().getCalories(),
                    progress));
        });
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            boolean hasLogs = !state.getFoodLogs().isEmpty();
            binding.tvFoodLogsEmpty.setVisibility(hasLogs ? View.GONE : View.VISIBLE);
            binding.tvFoodLogsEmpty.setText(state.getEmptyFoodLogsText());
            binding.rvFoodLogs.setVisibility(hasLogs ? View.VISIBLE : View.GONE);
        });
        viewModel.getFoodLogs().observe(getViewLifecycleOwner(), adapter::submitList);
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
    }

    private void setFoodFormEnabled(boolean enabled) {
        binding.etFoodName.setEnabled(enabled);
        binding.etMealType.setEnabled(enabled);
        binding.etCalories.setEnabled(enabled);
        binding.etProtein.setEnabled(enabled);
        binding.etCarbs.setEnabled(enabled);
        binding.etFat.setEnabled(enabled);
    }

    private String text(TextView input) {
        return input.getText() != null ? input.getText().toString() : "";
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
