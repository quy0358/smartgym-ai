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

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.databinding.FragmentAiAnalysisBinding;

@AndroidEntryPoint
public class AIAnalysisFragment extends Fragment {

    private FragmentAiAnalysisBinding binding;
    private AIAnalysisViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAiAnalysisBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AIAnalysisViewModel.class);

        setupMetrics();
        setupButtons();
        observeViewModel();
    }

    private void setupMetrics() {
        TextView fatLabel = binding.statBodyFat.getRoot().findViewById(R.id.tv_stat_label);
        TextView fatValue = binding.statBodyFat.getRoot().findViewById(R.id.tv_stat_value);
        TextView fatUnit = binding.statBodyFat.getRoot().findViewById(R.id.tv_stat_unit);
        fatLabel.setText("TỶ LỆ MỠ");
        fatValue.setText("18");
        fatUnit.setText("%");

        TextView leanLabel = binding.statLeanMass.getRoot().findViewById(R.id.tv_stat_label);
        TextView leanValue = binding.statLeanMass.getRoot().findViewById(R.id.tv_stat_value);
        TextView leanUnit = binding.statLeanMass.getRoot().findViewById(R.id.tv_stat_unit);
        leanLabel.setText("LEAN MASS");
        leanValue.setText("57.4");
        leanUnit.setText("kg");
    }

    private void setupButtons() {
        binding.btnGenerateWorkout.setOnClickListener(v -> viewModel.generateWorkoutPlan());

        binding.btnAnalyzeForm.setOnClickListener(v -> {
            String exercise = binding.etExerciseName.getText().toString().trim();
            String desc = binding.etFormDescription.getText().toString().trim();
            viewModel.analyzeForm(exercise, desc);
        });
    }

    private void observeViewModel() {
        viewModel.getAiResponse().observe(getViewLifecycleOwner(), response -> {
            if (response != null) {
                binding.tvAiResponse.setVisibility(View.VISIBLE);
                binding.tvAiResponse.setText(response);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnGenerateWorkout.setEnabled(!loading);
            binding.btnAnalyzeForm.setEnabled(!loading);
        });

        viewModel.getErrorMsg().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                binding.tvAiResponse.setVisibility(View.VISIBLE);
                binding.tvAiResponse.setText("⚠️ " + err);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
