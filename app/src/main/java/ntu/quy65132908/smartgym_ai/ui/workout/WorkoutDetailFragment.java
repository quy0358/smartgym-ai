package ntu.quy65132908.smartgym_ai.ui.workout;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.databinding.FragmentWorkoutDetailBinding;

@AndroidEntryPoint
public class WorkoutDetailFragment extends Fragment {

    private FragmentWorkoutDetailBinding binding;
    private WorkoutViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWorkoutDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(WorkoutViewModel.class);

        ExerciseAdapter adapter = new ExerciseAdapter((exercise, checked) -> {
            // Exercise checked/unchecked - could call viewModel to persist
        });

        binding.rvExercises.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvExercises.setAdapter(adapter);

        viewModel.getExercises().observe(getViewLifecycleOwner(), exercises -> {
            if (exercises != null) adapter.submitList(exercises);
        });

        // Load exercises if workoutId argument is available
        String workoutId = getArguments() != null ? getArguments().getString("workoutId", "") : "";
        if (!workoutId.isEmpty()) {
            viewModel.loadExercises(workoutId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
