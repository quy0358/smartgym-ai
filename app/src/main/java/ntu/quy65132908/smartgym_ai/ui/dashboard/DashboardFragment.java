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

import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.databinding.FragmentDashboardBinding;
import ntu.quy65132908.smartgym_ai.ui.navigation.BottomNavHost;

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
        weeklyPlanAdapter = new WeeklyPlanAdapter(this::navigateToWorkoutDetail);
        binding.rvWeeklyPlan.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvWeeklyPlan.setAdapter(weeklyPlanAdapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.primary);
        binding.swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.surface_container);
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
    }

    private void setupClickListeners() {
        binding.btnNotification.setVisibility(View.GONE);

        binding.btnStartWorkout.setOnClickListener(v -> {
            DashboardUiState state = viewModel.getUiState().getValue();
            if (state != null && state.getTodayState() == TodayState.WORKOUT) {
                Workout recommendation = state.getAiRecommendation();
                if (recommendation != null) {
                    navigateToWorkoutDetail(recommendation);
                    return;
                }
            }
            navigateToWorkoutTab();
        });

        binding.btnViewAll.setOnClickListener(v -> navigateToWorkoutTab());

        binding.btnCreatePlan.setOnClickListener(v -> navigateToWorkoutTab());
        binding.btnOpenNutrition.setOnClickListener(v ->
                Navigation.findNavController(binding.getRoot()).navigate(R.id.nav_nutrition));
        binding.btnOpenWellness.setOnClickListener(v ->
                Navigation.findNavController(binding.getRoot()).navigate(R.id.nav_wellness));
        binding.btnOpenPose.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("exerciseType", "push_up");
            args.putBoolean("selectionRequired", true);
            args.putString("workoutId", "");
            Navigation.findNavController(binding.getRoot()).navigate(R.id.action_dashboard_to_pose_trainer, args);
        });
    }

    private void navigateToWorkoutDetail(Workout workout) {
        if (workout == null || workout.getId() == null || workout.getId().isEmpty()) {
            Snackbar.make(binding.getRoot(), R.string.error_open_workout, Snackbar.LENGTH_LONG).show();
            return;
        }

        Navigation.findNavController(binding.getRoot()).navigate(
                DashboardFragmentDirections.actionDashboardToWorkoutDetail(
                        workout.getId(),
                        workout.getTitle() != null ? workout.getTitle() : "",
                        workout.getDurationMinutes(),
                        workout.getDayType()));
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::renderState);

        // Error event observer — maps error codes to strings
        viewModel.getErrorEvent().observe(getViewLifecycleOwner(), error -> {
            int msgRes;
            switch (error) {
                case WEEKLY_PLAN_LOAD_FAILED:
                    msgRes = R.string.error_load_weekly_plan;
                    break;
                case PROFILE_LOAD_FAILED:
                    msgRes = R.string.error_load_profile_partial;
                    break;
                case USER_LOAD_FAILED:
                default:
                    msgRes = R.string.error_load_dashboard;
                    break;
            }
            Snackbar.make(binding.getRoot(), msgRes, Snackbar.LENGTH_LONG).show();
        });

        viewModel.getRefreshSuccessEvent().observe(getViewLifecycleOwner(), success ->
                Snackbar.make(binding.getRoot(), R.string.refresh_success, Snackbar.LENGTH_SHORT).show());

        viewModel.getRefreshThrottledEvent().observe(getViewLifecycleOwner(), throttled ->
                Snackbar.make(binding.getRoot(), R.string.refresh_throttled, Snackbar.LENGTH_SHORT).show());

        viewModel.getRequireLoginEvent().observe(getViewLifecycleOwner(), requireLogin -> {
            if (Boolean.TRUE.equals(requireLogin)) {
                Navigation.findNavController(binding.getRoot()).navigate(R.id.action_global_to_login);
            }
        });
    }

    private void renderState(DashboardUiState state) {
        if (state == null) {
            return;
        }

        DashboardRenderer.render(binding, requireContext(), weeklyPlanAdapter, state);

        if (state.isDataStale()) {
            Snackbar.make(binding.getRoot(), R.string.data_offline, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void navigateToWorkoutTab() {
        if (getActivity() instanceof BottomNavHost) {
            ((BottomNavHost) getActivity()).selectTab(R.id.nav_workout);
        }
    }
}
