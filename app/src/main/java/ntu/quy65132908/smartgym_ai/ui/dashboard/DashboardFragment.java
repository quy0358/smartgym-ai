package ntu.quy65132908.smartgym_ai.ui.dashboard;

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
import androidx.recyclerview.widget.LinearLayoutManager;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.databinding.FragmentDashboardBinding;

@AndroidEntryPoint
public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        setupStatCards();
        setupRecyclerView();
        observeViewModel();

        binding.btnStartWorkout.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_dashboard_to_workout_detail)
        );

        binding.cardAiWorkout.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_dashboard_to_ai_analysis)
        );
    }

    private void setupStatCards() {
        // Weight stat
        TextView weightLabel = binding.statWeight.getRoot().findViewById(R.id.tv_stat_label);
        TextView weightValue = binding.statWeight.getRoot().findViewById(R.id.tv_stat_value);
        TextView weightUnit = binding.statWeight.getRoot().findViewById(R.id.tv_stat_unit);
        weightLabel.setText("CÂN NẶNG");
        weightValue.setText("70");
        weightUnit.setText("kg");

        // BMI stat
        TextView bmiLabel = binding.statBmi.getRoot().findViewById(R.id.tv_stat_label);
        TextView bmiValue = binding.statBmi.getRoot().findViewById(R.id.tv_stat_value);
        TextView bmiUnit = binding.statBmi.getRoot().findViewById(R.id.tv_stat_unit);
        bmiLabel.setText("BMI");
        bmiValue.setText("22.5");
        bmiUnit.setText("Bình thường");

        // Goal stat
        TextView goalLabel = binding.statGoal.getRoot().findViewById(R.id.tv_stat_label);
        TextView goalValue = binding.statGoal.getRoot().findViewById(R.id.tv_stat_value);
        TextView goalUnit = binding.statGoal.getRoot().findViewById(R.id.tv_stat_unit);
        goalLabel.setText("MỤC TIÊU");
        goalValue.setText("65");
        goalUnit.setText("kg");
    }

    private void setupRecyclerView() {
        binding.rvWeeklyPlan.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvWeeklyPlan.setAdapter(new WeeklyPlanAdapter());
    }

    private void observeViewModel() {
        viewModel.getUserName().observe(getViewLifecycleOwner(), name ->
                binding.tvGreeting.setText("Chào " + name + "! 💪")
        );

        viewModel.getWeight().observe(getViewLifecycleOwner(), weight -> {
            TextView weightValue = binding.statWeight.getRoot().findViewById(R.id.tv_stat_value);
            weightValue.setText(String.valueOf(weight.intValue()));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
