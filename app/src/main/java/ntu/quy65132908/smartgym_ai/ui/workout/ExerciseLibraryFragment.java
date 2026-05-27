package ntu.quy65132908.smartgym_ai.ui.workout;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import ntu.quy65132908.smartgym_ai.databinding.FragmentExerciseLibraryBinding;

@AndroidEntryPoint
public class ExerciseLibraryFragment extends Fragment {
    private FragmentExerciseLibraryBinding binding;
    private ExerciseLibraryViewModel viewModel;
    private ExerciseCatalogAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentExerciseLibraryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ExerciseLibraryViewModel.class);
        adapter = new ExerciseCatalogAdapter(item -> viewModel.toggle(item));
        binding.rvExerciseCatalog.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvExerciseCatalog.setAdapter(adapter);
        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        if (viewModel.isEditMode()) {
            binding.btnSaveCustomWorkout.setText(R.string.exercise_update_custom_workout);
        }
        binding.btnSaveCustomWorkout.setOnClickListener(v -> viewModel.saveSelectedWorkout());
        setupFilterChips();
        binding.etExerciseSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.search(s != null ? s.toString() : "");
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        observe();
    }

    private void setupFilterChips() {
        binding.chipFilterAll.setOnClickListener(v -> viewModel.clearFilters());
        binding.chipFilterPose.setOnClickListener(v -> viewModel.showPoseSupportedOnly());
        binding.chipFilterNoEquipment.setOnClickListener(v ->
                viewModel.filterEquipment(getString(R.string.exercise_filter_no_equipment)));
        binding.chipFilterDumbbell.setOnClickListener(v ->
                viewModel.filterEquipment(getString(R.string.exercise_filter_dumbbell)));
        binding.chipFilterBeginner.setOnClickListener(v ->
                viewModel.filterDifficulty(getString(R.string.exercise_filter_beginner)));
        binding.chipFilterIntermediate.setOnClickListener(v ->
                viewModel.filterDifficulty(getString(R.string.exercise_filter_intermediate)));
    }

    private void observe() {
        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            boolean isEmpty = items == null || items.isEmpty();
            binding.rvExerciseCatalog.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.tvEmptyCatalog.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            adapter.submitList(items);
        });
        viewModel.getSelectedIds().observe(getViewLifecycleOwner(), adapter::setSelectedIds);
        viewModel.getSelectedCount().observe(getViewLifecycleOwner(), count ->
                binding.tvSelectedCount.setText(getString(R.string.exercise_selected_count_format, count != null ? count : 0)));
        viewModel.getCanSave().observe(getViewLifecycleOwner(), canSave ->
                binding.btnSaveCustomWorkout.setEnabled(Boolean.TRUE.equals(canSave)));
        viewModel.getIsSaving().observe(getViewLifecycleOwner(), saving -> {
            boolean loading = Boolean.TRUE.equals(saving);
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            if (loading) {
                binding.btnSaveCustomWorkout.setEnabled(false);
            }
        });
        viewModel.getMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
        viewModel.getSaveComplete().observe(getViewLifecycleOwner(), complete -> {
            if (Boolean.TRUE.equals(complete)) {
                Navigation.findNavController(binding.getRoot()).navigateUp();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
