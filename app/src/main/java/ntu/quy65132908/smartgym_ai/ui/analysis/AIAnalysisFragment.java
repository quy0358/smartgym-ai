package ntu.quy65132908.smartgym_ai.ui.analysis;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.databinding.FragmentAiAnalysisBinding;
import ntu.quy65132908.smartgym_ai.ui.pose.ExerciseType;
import ntu.quy65132908.smartgym_ai.ui.pose.PoseExerciseResolver;

@AndroidEntryPoint
public class AIAnalysisFragment extends Fragment {

    private FragmentAiAnalysisBinding binding;
    private AIAnalysisViewModel viewModel;
    private boolean canGeneratePlan;
    private boolean planLoading;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAiAnalysisBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AIAnalysisViewModel.class);

        setupMetricLabels();
        setupButtons();
        observeViewModel();
    }

    private void setupMetricLabels() {
        setStatCard(
                binding.statBodyFat.getRoot(),
                getString(R.string.ai_stat_body_fat),
                "--",
                getString(R.string.unit_percent)
        );
        setStatCard(
                binding.statLeanMass.getRoot(),
                getString(R.string.ai_stat_lean_mass),
                "--",
                getString(R.string.unit_kg_upper)
        );
    }

    private void setupButtons() {
        binding.btnGenerateWorkout.setOnClickListener(v -> viewModel.generateWorkoutPlan());

        binding.btnAnalyzeForm.setOnClickListener(v -> {
            String exercise = binding.etExerciseName.getText() != null
                    ? binding.etExerciseName.getText().toString().trim()
                    : "";
            String desc = binding.etFormDescription.getText() != null
                    ? binding.etFormDescription.getText().toString().trim()
                    : "";
            viewModel.analyzeForm(exercise, desc);
        });

        binding.btnOpenPoseTrainer.setOnClickListener(v -> {
            Bundle args = new Bundle();
            ExerciseType exerciseType = inferExerciseType();
            args.putString("exerciseType", exerciseType != null ? exerciseType.getKey() : ExerciseType.PUSH_UP.getKey());
            args.putBoolean("selectionRequired", exerciseType == null);
            Navigation.findNavController(v).navigate(
                    R.id.action_ai_analysis_to_pose_trainer,
                    args);
        });
    }

    private void observeViewModel() {
        viewModel.getBodyMetrics().observe(getViewLifecycleOwner(), metrics -> {
            if (metrics == null) return;
            binding.tvBodyType.setText(metrics.getBodyType());
            binding.tvBodyDesc.setText(metrics.getSummary());
            updateStatCardValue(binding.statBodyFat.getRoot(), metrics.getBodyFat(), getString(R.string.unit_percent));
            updateStatCardValue(binding.statLeanMass.getRoot(), metrics.getLeanMass(), getString(R.string.unit_kg_upper));
        });

        viewModel.getCanGeneratePlan().observe(getViewLifecycleOwner(), enabled -> {
            canGeneratePlan = Boolean.TRUE.equals(enabled);
            updateGenerateButtonState();
        });

        viewModel.getIsPlanLoading().observe(getViewLifecycleOwner(), loading -> {
            planLoading = Boolean.TRUE.equals(loading);
            binding.progressPlan.setVisibility(planLoading ? View.VISIBLE : View.GONE);
            updateGenerateButtonState();
        });

        viewModel.getIsFormLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            binding.progressForm.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnAnalyzeForm.setEnabled(!isLoading);
        });

        viewModel.getPlanResponse().observe(getViewLifecycleOwner(), response ->
                showResponse(binding.tvPlanResponseLabel, binding.tvPlanResponse, response));

        viewModel.getPlanError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.trim().isEmpty()) {
                showResponse(binding.tvPlanResponseLabel, binding.tvPlanResponse, formatError(error));
            }
        });

        viewModel.getFormResponse().observe(getViewLifecycleOwner(), response ->
                showResponse(binding.tvFormResponseLabel, binding.tvFormResponse, response));

        viewModel.getFormError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.trim().isEmpty()) {
                showResponse(binding.tvFormResponseLabel, binding.tvFormResponse, formatError(error));
            }
        });

        viewModel.getExerciseNameError().observe(getViewLifecycleOwner(), error ->
                binding.tilExerciseName.setError(error));

        viewModel.getFormDescriptionError().observe(getViewLifecycleOwner(), error ->
                binding.tilFormDescription.setError(error));
    }

    private void updateGenerateButtonState() {
        binding.btnGenerateWorkout.setEnabled(canGeneratePlan && !planLoading);
    }

    private void setStatCard(View root, String label, String value, String unit) {
        TextView labelView = root.findViewById(R.id.tv_stat_label);
        TextView valueView = root.findViewById(R.id.tv_stat_value);
        TextView unitView = root.findViewById(R.id.tv_stat_unit);
        labelView.setText(label);
        valueView.setText(value);
        unitView.setText(unit);
    }

    private void updateStatCardValue(View root, String value, String unit) {
        TextView valueView = root.findViewById(R.id.tv_stat_value);
        TextView unitView = root.findViewById(R.id.tv_stat_unit);
        valueView.setText(value);
        unitView.setText(unit);
    }

    private void showResponse(TextView labelView, TextView responseView, String text) {
        boolean hasText = text != null && !text.trim().isEmpty();
        labelView.setVisibility(hasText ? View.VISIBLE : View.GONE);
        responseView.setVisibility(hasText ? View.VISIBLE : View.GONE);
        responseView.setText(hasText ? text : null);
    }

    private String formatError(String error) {
        if (error == null || error.trim().isEmpty()) {
            return null;
        }
        return getString(R.string.ai_error_prefix, error);
    }

    private ExerciseType inferExerciseType() {
        String exercise = binding.etExerciseName.getText() != null
                ? binding.etExerciseName.getText().toString().trim()
                : "";
        if (exercise.contains("squat") || exercise.contains("gánh") || exercise.contains("ngồi xổm")) {
            return ExerciseType.SQUAT;
        }
        if (exercise.contains("plank")) {
            return ExerciseType.PLANK;
        }
        return PoseExerciseResolver.resolve(null, exercise);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
