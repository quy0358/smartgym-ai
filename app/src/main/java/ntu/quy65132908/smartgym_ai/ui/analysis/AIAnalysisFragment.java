package ntu.quy65132908.smartgym_ai.ui.analysis;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.databinding.FragmentAiAnalysisBinding;

@AndroidEntryPoint
public class AIAnalysisFragment extends Fragment {

    private FragmentAiAnalysisBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAiAnalysisBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupMetrics();

        binding.btnAskAi.setOnClickListener(v -> {
            // TODO: Open Gemini AI chat
        });
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
