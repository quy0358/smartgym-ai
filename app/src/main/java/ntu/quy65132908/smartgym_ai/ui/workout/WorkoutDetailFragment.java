package ntu.quy65132908.smartgym_ai.ui.workout;

import android.animation.ObjectAnimator;
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

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Exercise;
import ntu.quy65132908.smartgym_ai.databinding.FragmentWorkoutDetailBinding;
import ntu.quy65132908.smartgym_ai.ui.pose.ExerciseType;
import ntu.quy65132908.smartgym_ai.ui.pose.PoseExerciseResolver;

@AndroidEntryPoint
public class WorkoutDetailFragment extends Fragment {

    private FragmentWorkoutDetailBinding binding;
    private WorkoutViewModel viewModel;
    private ExerciseType selectedPoseType;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWorkoutDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(WorkoutViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        ExerciseAdapter adapter = new ExerciseAdapter((exercise, checked) ->
                viewModel.toggleExercise(exercise.getId(), checked));
        binding.rvExercises.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvExercises.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> render(state, adapter));

        viewModel.getSnackbarMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
            }
        });

        binding.btnStartWorkout.setOnClickListener(v -> {
            if (selectedPoseType == null) {
                Snackbar.make(binding.getRoot(), R.string.pose_no_supported_exercise, Snackbar.LENGTH_LONG).show();
                return;
            }
            Bundle args = new Bundle();
            args.putString("exerciseType", selectedPoseType.getKey());
            Navigation.findNavController(v).navigate(
                    R.id.action_workout_detail_to_pose_trainer,
                    args);
        });
    }

    private void render(WorkoutDetailUiState state, ExerciseAdapter adapter) {
        if (state == null || binding == null) return;

        binding.tvWorkoutSubtitle.setText(state.getSubtitle());
        binding.loadingIndicator.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
        binding.layoutProgress.setVisibility(state.shouldShowProgress() ? View.VISIBLE : View.GONE);

        if (state.getErrorMessage() != null) {
            Snackbar.make(binding.getRoot(), state.getErrorMessage(), Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.action_retry, v -> viewModel.retry())
                    .show();
        }

        boolean isRestDay = state.isRestDay();
        boolean isEmpty = state.isEmpty() && !state.isLoading() && !isRestDay;
        boolean hasExercises = state.hasExercises();
        selectedPoseType = findFirstSupportedPoseType(state.getExercises());
        binding.layoutRestDay.setVisibility(isRestDay ? View.VISIBLE : View.GONE);
        binding.layoutEmptyExercises.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.tvExerciseHeader.setVisibility(hasExercises && !isRestDay ? View.VISIBLE : View.GONE);
        binding.rvExercises.setVisibility(hasExercises && !isRestDay ? View.VISIBLE : View.GONE);
        boolean canStartPose = state.shouldShowPoseAction() && selectedPoseType != null;
        boolean showPoseUnavailable = state.shouldShowPoseAction() && selectedPoseType == null;
        binding.btnStartWorkout.setVisibility(canStartPose ? View.VISIBLE : View.GONE);
        binding.btnStartWorkout.setEnabled(canStartPose);
        binding.tvPoseUnavailable.setVisibility(showPoseUnavailable ? View.VISIBLE : View.GONE);
        if (canStartPose) {
            binding.btnStartWorkout.setText(getString(R.string.pose_start_specific_format, selectedPoseType.getDisplayName()));
        }

        if (state.getExercises() != null) {
            adapter.submitList(new ArrayList<>(state.getExercises()));
        }

        int newProgress = state.getProgressPercent();
        int oldProgress = binding.progressRing.getProgress();
        if (newProgress != oldProgress) {
            ObjectAnimator animator = ObjectAnimator.ofInt(
                    binding.progressRing,
                    "progress",
                    oldProgress,
                    newProgress);
            animator.setDuration(400);
            animator.start();
        }
        binding.tvProgressText.setText(newProgress + "%");
    }

    @Nullable
    private ExerciseType findFirstSupportedPoseType(@Nullable List<Exercise> exercises) {
        if (exercises == null) {
            return null;
        }
        for (Exercise exercise : exercises) {
            ExerciseType type = PoseExerciseResolver.resolve(exercise);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
