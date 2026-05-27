package ntu.quy65132908.smartgym_ai.ui.workout;

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
import ntu.quy65132908.smartgym_ai.databinding.FragmentWorkoutListBinding;
import ntu.quy65132908.smartgym_ai.ui.dashboard.WeeklyPlanAdapter;

@AndroidEntryPoint
public class WorkoutListFragment extends Fragment {
    private FragmentWorkoutListBinding binding;
    private WorkoutListViewModel viewModel;
    private WeeklyPlanAdapter adapter;
    private boolean listLoading;
    private boolean creatingPlan;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWorkoutListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(WorkoutListViewModel.class);

        adapter = new WeeklyPlanAdapter(this::navigateToWorkoutDetail);
        binding.rvWorkouts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvWorkouts.setAdapter(adapter);
        binding.btnCreatePlan.setOnClickListener(v -> viewModel.createPlan());
        binding.btnOpenExerciseLibrary.setOnClickListener(v ->
                Navigation.findNavController(binding.getRoot()).navigate(R.id.nav_exercise_library));

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getWorkouts().observe(getViewLifecycleOwner(), workouts -> {
            boolean isEmpty = workouts == null || workouts.isEmpty();
            binding.rvWorkouts.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.layoutEmptyWorkouts.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            adapter.submitList(workouts);
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            listLoading = Boolean.TRUE.equals(loading);
            updateLoadingUi();
        });

        viewModel.getIsCreatingPlan().observe(getViewLifecycleOwner(), loading -> {
            creatingPlan = Boolean.TRUE.equals(loading);
            updateLoadingUi();
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToWorkoutDetail(Workout workout) {
        if (workout == null || workout.getId() == null || workout.getId().isEmpty()) {
            Snackbar.make(binding.getRoot(), R.string.error_open_workout, Snackbar.LENGTH_LONG).show();
            return;
        }

        Navigation.findNavController(binding.getRoot()).navigate(
                WorkoutListFragmentDirections.actionWorkoutToWorkoutDetail(
                        workout.getId(),
                        workout.getTitle() != null ? workout.getTitle() : "",
                        workout.getDurationMinutes(),
                        workout.getDayType(),
                        workout.isCustom()));
    }

    private void updateLoadingUi() {
        boolean loading = listLoading || creatingPlan;
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnCreatePlan.setEnabled(!creatingPlan);
        binding.btnCreatePlan.setText(creatingPlan ? getString(R.string.creating_plan) : getString(R.string.btn_create_plan));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadWorkouts();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
