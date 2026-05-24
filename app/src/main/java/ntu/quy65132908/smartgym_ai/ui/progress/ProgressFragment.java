package ntu.quy65132908.smartgym_ai.ui.progress;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
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

    private ActivityResultLauncher<String> beforePhotoPickerLauncher;
    private ActivityResultLauncher<String> afterPhotoPickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        beforePhotoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        viewModel.uploadBeforePhoto(uri);
                    }
                }
        );
        afterPhotoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        viewModel.uploadAfterPhoto(uri);
                    }
                }
        );
    }

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
        observeViewModel();
    }

    private void setupStatLabels() {
        TextView wLabel = binding.statWorkouts.getRoot().findViewById(R.id.tv_stat_label);
        TextView wUnit = binding.statWorkouts.getRoot().findViewById(R.id.tv_stat_unit);
        wLabel.setText("BÀI TẬP");
        wUnit.setText("HOÀN THÀNH");

        TextView sLabel = binding.statStreak.getRoot().findViewById(R.id.tv_stat_label);
        TextView sUnit = binding.statStreak.getRoot().findViewById(R.id.tv_stat_unit);
        sLabel.setText("CHUỖI");
        sUnit.setText("NGÀY");

        TextView cLabel = binding.statCalories.getRoot().findViewById(R.id.tv_stat_label);
        TextView cUnit = binding.statCalories.getRoot().findViewById(R.id.tv_stat_unit);
        cLabel.setText("CALORIES");
        cUnit.setText("ĐỐT CHÁY");
    }

    private void setupClickListeners() {
        binding.btnBeforePhoto.setOnClickListener(v -> beforePhotoPickerLauncher.launch("image/*"));
        binding.btnAfterPhoto.setOnClickListener(v -> afterPhotoPickerLauncher.launch("image/*"));
        binding.btnAddProgress.setOnClickListener(v -> viewModel.addProgressEntry(
                getText(binding.etProgressWeight),
                getText(binding.etProgressBodyFat),
                getText(binding.etProgressLeanMass),
                getText(binding.etProgressNote)
        ));
        binding.btnOpenNutrition.setOnClickListener(v ->
                Navigation.findNavController(binding.getRoot()).navigate(R.id.nav_nutrition));
        binding.btnOpenWellness.setOnClickListener(v ->
                Navigation.findNavController(binding.getRoot()).navigate(R.id.nav_wellness));
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null) {
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            }
        });

        // C2: Dynamic weight display
        viewModel.getCurrentWeight().observe(getViewLifecycleOwner(), weight -> {
            if (weight != null && weight > 0) {
                binding.tvCurrentWeight.setText(String.format(Locale.getDefault(), "%.1f kg", weight));
            } else {
                binding.tvCurrentWeight.setText("-- kg");
            }
        });

        viewModel.getWeightChange().observe(getViewLifecycleOwner(), change -> {
            if (change != null && binding.tvWeightChange != null) {
                if (Math.abs(change) < 0.1f) {
                    binding.tvWeightChange.setText("Không thay đổi so với trước");
                } else if (change < 0) {
                    binding.tvWeightChange.setText(String.format(Locale.getDefault(), "↓ %.1f kg so với trước", Math.abs(change)));
                } else {
                    binding.tvWeightChange.setText(String.format(Locale.getDefault(), "↑ %.1f kg so với trước", change));
                }
            }
        });

        // C4: Dynamic stats
        viewModel.getCompletedWorkouts().observe(getViewLifecycleOwner(), count -> {
            TextView wValue = binding.statWorkouts.getRoot().findViewById(R.id.tv_stat_value);
            wValue.setText(String.valueOf(count != null ? count : 0));
        });

        viewModel.getStreakDays().observe(getViewLifecycleOwner(), streak -> {
            TextView sValue = binding.statStreak.getRoot().findViewById(R.id.tv_stat_value);
            sValue.setText(String.valueOf(streak != null ? streak : 0));
        });

        viewModel.getTotalCalories().observe(getViewLifecycleOwner(), calories -> {
            TextView cValue = binding.statCalories.getRoot().findViewById(R.id.tv_stat_value);
            cValue.setText(String.valueOf(calories != null ? calories : 0));
        });

        // H6: Body Photos rendering
        viewModel.getBeforePhotoUrl().observe(getViewLifecycleOwner(), url -> {
            if (url != null && !url.isEmpty()) {
                binding.ivBeforePhoto.setVisibility(View.VISIBLE);
                binding.layoutBeforePlaceholder.setVisibility(View.GONE);
                Glide.with(this)
                        .load(url)
                        .placeholder(android.R.color.transparent)
                        .into(binding.ivBeforePhoto);
            } else {
                binding.ivBeforePhoto.setVisibility(View.GONE);
                binding.layoutBeforePlaceholder.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getAfterPhotoUrl().observe(getViewLifecycleOwner(), url -> {
            if (url != null && !url.isEmpty()) {
                binding.ivAfterPhoto.setVisibility(View.VISIBLE);
                binding.layoutAfterPlaceholder.setVisibility(View.GONE);
                Glide.with(this)
                        .load(url)
                        .placeholder(android.R.color.transparent)
                        .into(binding.ivAfterPhoto);
            } else {
                binding.ivAfterPhoto.setVisibility(View.GONE);
                binding.layoutAfterPlaceholder.setVisibility(View.VISIBLE);
            }
        });

        // H7: Weight chart
        viewModel.getEntries().observe(getViewLifecycleOwner(), this::setupChart);

        // H8: Error handling
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setupChart(List<ProgressEntry> entriesList) {
        if (entriesList == null || entriesList.isEmpty()) {
            binding.weightChart.setVisibility(View.GONE);
            binding.tvChartPlaceholder.setVisibility(View.VISIBLE);
            return;
        }

        binding.weightChart.setVisibility(View.VISIBLE);
        binding.tvChartPlaceholder.setVisibility(View.GONE);

        // Sort chronologically (ascending by date)
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

        com.github.mikephil.charting.data.LineDataSet dataSet = new com.github.mikephil.charting.data.LineDataSet(chartEntries, "Cân nặng");

        // Premium styling for line
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

        // Shadow/gradient fill
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(primaryColor);
        dataSet.setFillAlpha(35); // Subtle opacity

        com.github.mikephil.charting.data.LineData lineData = new com.github.mikephil.charting.data.LineData(dataSet);
        binding.weightChart.setData(lineData);

        // Chart styling
        binding.weightChart.getDescription().setEnabled(false);
        binding.weightChart.getLegend().setEnabled(false);
        binding.weightChart.setTouchEnabled(true);
        binding.weightChart.setDragEnabled(true);
        binding.weightChart.setScaleEnabled(false);
        binding.weightChart.setPinchZoom(false);
        binding.weightChart.setExtraOffsets(8f, 8f, 8f, 8f);

        // X Axis setup
        com.github.mikephil.charting.components.XAxis xAxis = binding.weightChart.getXAxis();
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.outline));
        xAxis.setTextSize(10f);
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

        // Y Axis Left setup
        com.github.mikephil.charting.components.YAxis yAxisLeft = binding.weightChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setGridColor(ContextCompat.getColor(requireContext(), R.color.surface_container_highest));
        yAxisLeft.setTextColor(ContextCompat.getColor(requireContext(), R.color.outline));
        yAxisLeft.setTextSize(10f);
        yAxisLeft.setLabelCount(5, false);

        // Add padding to min/max
        float minWeight = Float.MAX_VALUE;
        float maxWeight = Float.MIN_VALUE;
        for (ProgressEntry e : sorted) {
            if (e.getWeight() < minWeight) minWeight = e.getWeight();
            if (e.getWeight() > maxWeight) maxWeight = e.getWeight();
        }
        if (minWeight == maxWeight) {
            yAxisLeft.setAxisMinimum(minWeight - 5f);
            yAxisLeft.setAxisMaximum(maxWeight + 5f);
        } else {
            yAxisLeft.setAxisMinimum(minWeight - 2f);
            yAxisLeft.setAxisMaximum(maxWeight + 2f);
        }

        // Y Axis Right setup (disable)
        com.github.mikephil.charting.components.YAxis yAxisRight = binding.weightChart.getAxisRight();
        yAxisRight.setEnabled(false);

        binding.weightChart.animateY(600);
        binding.weightChart.invalidate();
    }

    private String getText(com.google.android.material.textfield.TextInputEditText input) {
        return input.getText() != null ? input.getText().toString() : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
