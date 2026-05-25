package ntu.quy65132908.smartgym_ai.ui.progress;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.ProgressEntry;
import ntu.quy65132908.smartgym_ai.databinding.FragmentProgressBinding;

@AndroidEntryPoint
public class ProgressFragment extends Fragment {

    private FragmentProgressBinding binding;
    private ProgressViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProgressBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProgressViewModel.class);

        setupStatLabels();
        setupClickListeners();
        setupFormWatchers();
        observeViewModel();
    }

    private void setupStatLabels() {
        TextView wLabel = binding.statWorkouts.getRoot().findViewById(R.id.tv_stat_label);
        TextView wUnit = binding.statWorkouts.getRoot().findViewById(R.id.tv_stat_unit);
        wLabel.setText(R.string.progress_stat_workouts);
        wUnit.setText(R.string.progress_stat_workouts_unit);

        TextView sLabel = binding.statStreak.getRoot().findViewById(R.id.tv_stat_label);
        TextView sUnit = binding.statStreak.getRoot().findViewById(R.id.tv_stat_unit);
        sLabel.setText(R.string.progress_stat_tracking);
        sUnit.setText(R.string.progress_stat_tracking_unit);

        TextView cLabel = binding.statCalories.getRoot().findViewById(R.id.tv_stat_label);
        TextView cUnit = binding.statCalories.getRoot().findViewById(R.id.tv_stat_unit);
        cLabel.setText(R.string.progress_stat_calories);
        cUnit.setText(R.string.progress_stat_calories_unit);
    }

    private void setupClickListeners() {
        binding.btnAddProgress.setOnClickListener(v -> viewModel.addProgressEntry(
                text(binding.etProgressWeight),
                text(binding.etProgressBodyFat),
                text(binding.etProgressLeanMass),
                text(binding.etProgressNote)
        ));
        binding.btnOpenNutrition.setOnClickListener(v ->
                Navigation.findNavController(binding.getRoot()).navigate(R.id.nav_nutrition));
        binding.btnOpenWellness.setOnClickListener(v ->
                Navigation.findNavController(binding.getRoot()).navigate(R.id.nav_wellness));
    }

    private void setupFormWatchers() {
        TextWatcher watcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                viewModel.onProgressFormChanged(
                        text(binding.etProgressWeight),
                        text(binding.etProgressBodyFat),
                        text(binding.etProgressLeanMass),
                        text(binding.etProgressNote));
            }
        };
        binding.etProgressWeight.addTextChangedListener(watcher);
        binding.etProgressBodyFat.addTextChangedListener(watcher);
        binding.etProgressLeanMass.addTextChangedListener(watcher);
        binding.etProgressNote.addTextChangedListener(watcher);
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.getFormErrors().observe(getViewLifecycleOwner(), this::renderFormErrors);
        viewModel.getClearProgressFormEvent().observe(getViewLifecycleOwner(), shouldClear -> {
            if (Boolean.TRUE.equals(shouldClear)) {
                clearProgressForm();
            }
        });
        viewModel.getMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void renderState(ProgressUiState state) {
        if (state == null || binding == null) {
            return;
        }

        boolean busy = state.isLoading();
        binding.progressOverlay.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.btnAddProgress.setText(state.isSavingProgress()
                ? R.string.progress_saving
                : R.string.progress_save_button);
        setProgressFormEnabled(!busy);

        binding.tvCurrentWeight.setText(state.hasWeightData()
                ? getString(R.string.progress_current_weight_format, state.getCurrentWeight())
                : getString(R.string.progress_weight_unavailable));
        renderWeightChange(state);
        renderStats(state);

        binding.tvChartPlaceholder.setText(state.isLoggedOut()
                ? R.string.progress_logged_out_empty_chart
                : R.string.progress_chart_empty);
        setupChart(state.getEntries());
    }

    private void renderWeightChange(ProgressUiState state) {
        if (!state.hasWeightData()) {
            binding.tvWeightChange.setText(state.isLoading()
                    ? getString(R.string.progress_loading)
                    : getString(R.string.progress_no_weight_data));
            return;
        }
        if (state.getEntries().size() < 2) {
            binding.tvWeightChange.setText(R.string.progress_weight_no_previous);
            return;
        }

        float change = state.getWeightChange();
        if (Math.abs(change) < 0.1f) {
            binding.tvWeightChange.setText(R.string.progress_weight_no_change);
        } else if (change < 0) {
            binding.tvWeightChange.setText(getString(
                    R.string.progress_weight_decreased_format,
                    Math.abs(change)));
        } else {
            binding.tvWeightChange.setText(getString(
                    R.string.progress_weight_increased_format,
                    change));
        }
    }

    private void renderStats(ProgressUiState state) {
        setStatValue(binding.statWorkouts.getRoot(), state.getCompletedWorkouts());
        setStatValue(binding.statStreak.getRoot(), state.getTrackingStreakDays());
        setStatValue(binding.statCalories.getRoot(), state.getTotalCalories());
        binding.statWorkouts.getRoot().setContentDescription(getString(
                R.string.progress_stat_workouts_a11y,
                state.getCompletedWorkouts()));
        binding.statStreak.getRoot().setContentDescription(getString(
                R.string.progress_stat_tracking_a11y,
                state.getTrackingStreakDays()));
        binding.statCalories.getRoot().setContentDescription(getString(
                R.string.progress_stat_calories_a11y,
                state.getTotalCalories()));
    }

    private void renderFormErrors(ProgressFormErrors errors) {
        if (errors == null || binding == null) {
            return;
        }
        binding.tilProgressWeight.setError(errors.getWeightError());
        binding.tilProgressBodyFat.setError(errors.getBodyFatError());
        binding.tilProgressLeanMass.setError(errors.getLeanMassError());
        binding.tilProgressNote.setError(errors.getNoteError());
    }

    private void clearProgressForm() {
        binding.etProgressWeight.setText("");
        binding.etProgressBodyFat.setText("");
        binding.etProgressLeanMass.setText("");
        binding.etProgressNote.setText("");
        binding.tilProgressWeight.setError(null);
        binding.tilProgressBodyFat.setError(null);
        binding.tilProgressLeanMass.setError(null);
        binding.tilProgressNote.setError(null);
    }

    private void setProgressFormEnabled(boolean enabled) {
        binding.etProgressWeight.setEnabled(enabled);
        binding.etProgressBodyFat.setEnabled(enabled);
        binding.etProgressLeanMass.setEnabled(enabled);
        binding.etProgressNote.setEnabled(enabled);
        binding.btnAddProgress.setEnabled(enabled);
    }

    private void setStatValue(View statRoot, int value) {
        TextView valueView = statRoot.findViewById(R.id.tv_stat_value);
        valueView.setText(String.valueOf(value));
    }

    private void setupChart(List<ProgressEntry> entriesList) {
        if (entriesList == null || entriesList.isEmpty()) {
            binding.weightChart.setVisibility(View.GONE);
            binding.tvChartPlaceholder.setVisibility(View.VISIBLE);
            return;
        }

        binding.weightChart.setVisibility(View.VISIBLE);
        binding.tvChartPlaceholder.setVisibility(View.GONE);

        List<ProgressEntry> sorted = new ArrayList<>(entriesList);
        Collections.sort(sorted, (a, b) -> Long.compare(a.getDate(), b.getDate()));

        List<com.github.mikephil.charting.data.Entry> chartEntries = new ArrayList<>();
        final List<String> dates = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());

        for (int i = 0; i < sorted.size(); i++) {
            ProgressEntry entry = sorted.get(i);
            chartEntries.add(new com.github.mikephil.charting.data.Entry(i, entry.getWeight()));
            dates.add(sdf.format(new Date(entry.getDate())));
        }

        com.github.mikephil.charting.data.LineDataSet dataSet =
                new com.github.mikephil.charting.data.LineDataSet(
                        chartEntries,
                        getString(R.string.progress_weight_chart_data_label));

        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);

        dataSet.setColor(primaryColor);
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(primaryColor);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(ContextCompat.getColor(requireContext(), R.color.background));
        dataSet.setCircleHoleRadius(2f);
        dataSet.setDrawValues(false);
        dataSet.setMode(com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(primaryColor);
        dataSet.setFillAlpha(35);

        binding.weightChart.setData(new com.github.mikephil.charting.data.LineData(dataSet));
        binding.weightChart.getDescription().setEnabled(false);
        binding.weightChart.getLegend().setEnabled(false);
        binding.weightChart.setTouchEnabled(true);
        binding.weightChart.setDragEnabled(true);
        binding.weightChart.setScaleEnabled(false);
        binding.weightChart.setPinchZoom(false);
        binding.weightChart.setExtraOffsets(8f, 8f, 8f, 8f);

        com.github.mikephil.charting.components.XAxis xAxis = binding.weightChart.getXAxis();
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.outline));
        xAxis.setTextSize(10f);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setLabelCount(Math.min(dates.size(), 5), true);
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = Math.round(value);
                if (index >= 0 && index < dates.size()) {
                    return dates.get(index);
                }
                return "";
            }
        });

        com.github.mikephil.charting.components.YAxis yAxisLeft = binding.weightChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setGridColor(ContextCompat.getColor(requireContext(), R.color.surface_container_highest));
        yAxisLeft.setTextColor(ContextCompat.getColor(requireContext(), R.color.outline));
        yAxisLeft.setTextSize(10f);
        yAxisLeft.setLabelCount(5, false);

        float minWeight = Float.MAX_VALUE;
        float maxWeight = Float.MIN_VALUE;
        for (ProgressEntry entry : sorted) {
            minWeight = Math.min(minWeight, entry.getWeight());
            maxWeight = Math.max(maxWeight, entry.getWeight());
        }
        if (minWeight == maxWeight) {
            yAxisLeft.setAxisMinimum(minWeight - 5f);
            yAxisLeft.setAxisMaximum(maxWeight + 5f);
        } else {
            yAxisLeft.setAxisMinimum(minWeight - 2f);
            yAxisLeft.setAxisMaximum(maxWeight + 2f);
        }

        binding.weightChart.getAxisRight().setEnabled(false);
        binding.weightChart.animateY(600);
        binding.weightChart.invalidate();
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
