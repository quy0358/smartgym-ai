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
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.databinding.FragmentWorkoutDetailBinding;
import ntu.quy65132908.smartgym_ai.ui.media.UiImageResolver;
import ntu.quy65132908.smartgym_ai.ui.pose.ExerciseType;
import ntu.quy65132908.smartgym_ai.ui.pose.PoseExerciseResolver;

@AndroidEntryPoint
public class WorkoutDetailFragment extends Fragment {

    private FragmentWorkoutDetailBinding binding;
    private WorkoutViewModel viewModel;
    private ExerciseType selectedPoseType;
    private Exercise selectedPoseExercise;
    private boolean hasResumed;

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
        binding.ivWorkoutHero.setImageResource(UiImageResolver.workoutImageFor(workoutFromArgs()));
        binding.btnEditExercises.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("workoutId", stringArg("workoutId"));
            Navigation.findNavController(v).navigate(
                    R.id.action_workout_detail_to_exercise_library,
                    args);
        });

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
            if (selectedPoseType == null || selectedPoseExercise == null) {
                Snackbar.make(binding.getRoot(), R.string.pose_no_supported_exercise, Snackbar.LENGTH_LONG).show();
                return;
            }
            Bundle args = new Bundle();
            args.putString("exerciseType", selectedPoseType.getKey());
            args.putString("workoutId", stringArg("workoutId"));
            args.putString("exerciseId", safeString(selectedPoseExercise.getId()));
            args.putString("exerciseName", safeString(selectedPoseExercise.getName()));
            args.putBoolean("lockExerciseSelection", true);
            if (selectedPoseType.usesDurationMetric()) {
                args.putInt("targetSeconds", targetSecondsFor(selectedPoseExercise));
            } else {
                args.putInt("targetReps", targetRepsFor(selectedPoseExercise));
            }
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
        selectedPoseExercise = findFirstStartablePoseExercise(state.getExercises());
        selectedPoseType = selectedPoseExercise != null ? PoseExerciseResolver.resolve(selectedPoseExercise) : null;
        boolean hasSupportedPoseExercise = hasSupportedPoseExercise(state.getExercises());
        boolean allSupportedPoseExercisesCompleted = state.shouldShowPoseAction()
                && hasSupportedPoseExercise
                && selectedPoseExercise == null
                && areAllSupportedPoseExercisesCompleted(state.getExercises());
        binding.layoutRestDay.setVisibility(isRestDay ? View.VISIBLE : View.GONE);
        binding.layoutEmptyExercises.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.tvExerciseHeader.setVisibility(hasExercises && !isRestDay ? View.VISIBLE : View.GONE);
        binding.rvExercises.setVisibility(hasExercises && !isRestDay ? View.VISIBLE : View.GONE);
        binding.btnEditExercises.setVisibility(canEditCustomWorkout() ? View.VISIBLE : View.GONE);
        boolean canStartPose = state.shouldShowPoseAction() && selectedPoseType != null && selectedPoseExercise != null;
        boolean showPoseUnavailable = state.shouldShowPoseAction() && selectedPoseExercise == null;
        binding.btnStartWorkout.setVisibility(canStartPose ? View.VISIBLE : View.GONE);
        binding.btnStartWorkout.setEnabled(canStartPose);
        binding.tvPoseUnavailable.setVisibility(showPoseUnavailable ? View.VISIBLE : View.GONE);
        binding.tvPoseUnavailable.setText(allSupportedPoseExercisesCompleted
                ? R.string.pose_all_supported_completed
                : R.string.pose_no_supported_exercise);
        if (canStartPose) {
            String label = selectedPoseExercise.getName() != null && !selectedPoseExercise.getName().trim().isEmpty()
                    ? selectedPoseExercise.getName()
                    : selectedPoseType.getDisplayName();
            binding.btnStartWorkout.setText(getString(R.string.pose_start_specific_format, label));
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
        binding.tvProgressText.setText(getString(R.string.progress_percent_format, newProgress));
    }

    @Nullable
    private Exercise findFirstStartablePoseExercise(@Nullable List<Exercise> exercises) {
        if (exercises == null) {
            return null;
        }
        for (Exercise exercise : exercises) {
            ExerciseType type = PoseExerciseResolver.resolve(exercise);
            if (type != null
                    && !exercise.isCompleted()
                    && !safeString(exercise.getId()).isEmpty()
                    && hasPositiveTarget(exercise, type)) {
                return exercise;
            }
        }
        return null;
    }

    private boolean hasSupportedPoseExercise(@Nullable List<Exercise> exercises) {
        if (exercises == null) {
            return false;
        }
        for (Exercise exercise : exercises) {
            if (PoseExerciseResolver.resolve(exercise) != null) {
                return true;
            }
        }
        return false;
    }

    private boolean areAllSupportedPoseExercisesCompleted(@Nullable List<Exercise> exercises) {
        if (exercises == null) {
            return false;
        }
        boolean foundSupported = false;
        for (Exercise exercise : exercises) {
            if (PoseExerciseResolver.resolve(exercise) == null) {
                continue;
            }
            foundSupported = true;
            if (!exercise.isCompleted()) {
                return false;
            }
        }
        return foundSupported;
    }

    private boolean hasPositiveTarget(@NonNull Exercise exercise, @NonNull ExerciseType type) {
        return type.usesDurationMetric()
                ? targetSecondsFor(exercise) > 0
                : targetRepsFor(exercise) > 0;
    }

    private int targetRepsFor(@NonNull Exercise exercise) {
        return positiveSets(exercise) * Math.max(0, exercise.getReps());
    }

    private int targetSecondsFor(@NonNull Exercise exercise) {
        int seconds = exercise.getDurationSeconds() > 0
                ? exercise.getDurationSeconds()
                : Math.max(0, exercise.getReps());
        return positiveSets(exercise) * seconds;
    }

    private int positiveSets(@NonNull Exercise exercise) {
        return Math.max(1, exercise.getSets());
    }

    private Workout workoutFromArgs() {
        Workout workout = new Workout();
        workout.setTitle(stringArg("workoutTitle"));
        workout.setSubtitle(stringArg("dayType"));
        workout.setDayType(stringArg("dayType"));
        workout.setCustom(booleanArg("isCustom") || isLegacyCustomWorkoutTitle(workout.getTitle()));
        return workout;
    }

    private String stringArg(@NonNull String key) {
        Bundle args = getArguments();
        return args != null ? safeString(args.getString(key)) : "";
    }

    private boolean booleanArg(@NonNull String key) {
        Bundle args = getArguments();
        return args != null && args.getBoolean(key, false);
    }

    private boolean canEditCustomWorkout() {
        return !stringArg("workoutId").isEmpty()
                && Workout.DAY_TYPE_TRAINING.equals(Workout.normalizeDayType(stringArg("dayType")))
                && (booleanArg("isCustom") || isLegacyCustomWorkoutTitle(stringArg("workoutTitle")));
    }

    private boolean isLegacyCustomWorkoutTitle(@Nullable String title) {
        String customTitle = getString(R.string.exercise_custom_title);
        return safeString(title).trim().equalsIgnoreCase(customTitle.trim());
    }

    private static String safeString(@Nullable String value) {
        return value != null ? value : "";
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasResumed && viewModel != null) {
            viewModel.retry();
        }
        hasResumed = true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
