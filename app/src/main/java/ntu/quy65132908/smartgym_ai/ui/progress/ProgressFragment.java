package ntu.quy65132908.smartgym_ai.ui.progress;

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
import ntu.quy65132908.smartgym_ai.databinding.FragmentProgressBinding;

@AndroidEntryPoint
public class ProgressFragment extends Fragment {

    private FragmentProgressBinding binding;
    private ProgressViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProgressBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProgressViewModel.class);

        setupStats();
        observeViewModel();
    }

    private void setupStats() {
        TextView wLabel = binding.statWorkouts.getRoot().findViewById(R.id.tv_stat_label);
        TextView wValue = binding.statWorkouts.getRoot().findViewById(R.id.tv_stat_value);
        TextView wUnit = binding.statWorkouts.getRoot().findViewById(R.id.tv_stat_unit);
        wLabel.setText("BÀI TẬP");
        wValue.setText("0");
        wUnit.setText("hoàn thành");

        TextView sLabel = binding.statStreak.getRoot().findViewById(R.id.tv_stat_label);
        TextView sValue = binding.statStreak.getRoot().findViewById(R.id.tv_stat_value);
        TextView sUnit = binding.statStreak.getRoot().findViewById(R.id.tv_stat_unit);
        sLabel.setText("CHUỖI");
        sValue.setText("0");
        sUnit.setText("ngày");

        TextView cLabel = binding.statCalories.getRoot().findViewById(R.id.tv_stat_label);
        TextView cValue = binding.statCalories.getRoot().findViewById(R.id.tv_stat_value);
        TextView cUnit = binding.statCalories.getRoot().findViewById(R.id.tv_stat_unit);
        cLabel.setText("CALORIES");
        cValue.setText("0");
        cUnit.setText("đốt cháy");
    }

    private void observeViewModel() {
        viewModel.getEntries().observe(getViewLifecycleOwner(), entries -> {
            if (entries != null && !entries.isEmpty()) {
                TextView wValue = binding.statWorkouts.getRoot().findViewById(R.id.tv_stat_value);
                wValue.setText(String.valueOf(entries.size()));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
