package ntu.quy65132908.smartgym_ai.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.databinding.FragmentDashboardBinding;

@AndroidEntryPoint
public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;
    private WeeklyPlanAdapter weeklyPlanAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        setupStatLabels();
        setupRecyclerView();
        setupSwipeRefresh();
        setupClickListeners();
        observeViewModel();
    }

    private void setupStatLabels() {
        binding.statWeight.tvStatLabel.setText(R.string.stat_weight);
        binding.statWeight.tvStatUnit.setText(R.string.unit_kg);
        binding.statBmi.tvStatLabel.setText(R.string.stat_bmi);
        binding.statGoal.tvStatLabel.setText(R.string.stat_goal);
        binding.statGoal.tvStatUnit.setText(R.string.unit_kg);
    }

    private void setupRecyclerView() {
        weeklyPlanAdapter = new WeeklyPlanAdapter();
        binding.rvWeeklyPlan.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvWeeklyPlan.setAdapter(weeklyPlanAdapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.primary);
        binding.swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.surface_container);
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
    }

    private void setupClickListeners() {
        binding.btnStartWorkout.setOnClickListener(v -> {
            Workout recommendation = viewModel.getAiRecommendation().getValue();
            if (recommendation != null && recommendation.getId() != null) {
                Bundle args = new Bundle();
                args.putString("workoutId", recommendation.getId());
                Navigation.findNavController(v).navigate(
                        R.id.action_dashboard_to_workout_detail, args);
            }
        });

        binding.tvViewAll.setOnClickListener(v -> {
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_nav);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_workout);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getUserName().observe(getViewLifecycleOwner(), name ->
                binding.tvGreeting.setText(getString(R.string.greeting_format, name)));

        viewModel.getAvatarLetter().observe(getViewLifecycleOwner(), letter ->
                binding.tvAvatar.setText(letter));

        viewModel.getWeight().observe(getViewLifecycleOwner(), w ->
                binding.statWeight.tvStatValue.setText(String.valueOf(w)));

        viewModel.getBmi().observe(getViewLifecycleOwner(), bmiVal ->
                binding.statBmi.tvStatValue.setText(String.format("%.1f", bmiVal)));

        viewModel.getBmiCategory().observe(getViewLifecycleOwner(), category ->
                binding.statBmi.tvStatUnit.setText(category));

        viewModel.getGoalWeight().observe(getViewLifecycleOwner(), goal ->
                binding.statGoal.tvStatValue.setText(String.valueOf(goal)));

        viewModel.getAiRecommendation().observe(getViewLifecycleOwner(), workout -> {
            if (workout != null) {
                binding.cardAiWorkout.setVisibility(View.VISIBLE);
                binding.tvWorkoutTitle.setText(workout.getTitle());
                int exerciseCount = workout.getExercises() != null ? workout.getExercises().size() : 0;
                binding.tvWorkoutSubtitle.setText(getString(
                        R.string.workout_subtitle_format,
                        exerciseCount,
                        workout.getDurationMinutes(),
                        workout.getIntensity() != null ? workout.getIntensity() : ""));
            } else {
                binding.cardAiWorkout.setVisibility(View.GONE);
            }
        });

        viewModel.getWeeklyPlan().observe(getViewLifecycleOwner(), plan ->
                weeklyPlanAdapter.submitList(plan));

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.scrollView.setVisibility(loading ? View.GONE : View.VISIBLE);
        });

        viewModel.getIsRefreshing().observe(getViewLifecycleOwner(), refreshing ->
                binding.swipeRefresh.setRefreshing(refreshing));

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg ->
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
