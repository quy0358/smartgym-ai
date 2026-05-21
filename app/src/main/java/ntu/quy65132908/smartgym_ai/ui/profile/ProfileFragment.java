package ntu.quy65132908.smartgym_ai.ui.profile;

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

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.databinding.FragmentProfileBinding;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupStats();
        observeViewModel();

        binding.btnSignOut.setOnClickListener(v -> viewModel.signOut());

        binding.btnEditProfile.setOnClickListener(v -> {
            // TODO: Open edit profile screen
        });
    }

    private void setupStats() {
        TextView wLabel = binding.statTotalWorkouts.getRoot().findViewById(R.id.tv_stat_label);
        TextView wValue = binding.statTotalWorkouts.getRoot().findViewById(R.id.tv_stat_value);
        TextView wUnit = binding.statTotalWorkouts.getRoot().findViewById(R.id.tv_stat_unit);
        wLabel.setText("BÀI TẬP");
        wValue.setText("48");
        wUnit.setText("tổng cộng");

        TextView hLabel = binding.statTotalHours.getRoot().findViewById(R.id.tv_stat_label);
        TextView hValue = binding.statTotalHours.getRoot().findViewById(R.id.tv_stat_value);
        TextView hUnit = binding.statTotalHours.getRoot().findViewById(R.id.tv_stat_unit);
        hLabel.setText("GIỜ TẬP");
        hValue.setText("36");
        hUnit.setText("giờ");

        TextView sLabel = binding.statStreakDays.getRoot().findViewById(R.id.tv_stat_label);
        TextView sValue = binding.statStreakDays.getRoot().findViewById(R.id.tv_stat_value);
        TextView sUnit = binding.statStreakDays.getRoot().findViewById(R.id.tv_stat_unit);
        sLabel.setText("CHUỖI");
        sValue.setText("12");
        sUnit.setText("ngày");
    }

    private void observeViewModel() {
        viewModel.getDisplayName().observe(getViewLifecycleOwner(), name -> {
            binding.tvProfileName.setText(name);
            binding.tvProfileAvatar.setText(name.isEmpty() ? "U" : String.valueOf(name.charAt(0)));
        });

        viewModel.getEmail().observe(getViewLifecycleOwner(), email ->
                binding.tvProfileEmail.setText(email)
        );

        viewModel.getSignedOut().observe(getViewLifecycleOwner(), signedOut -> {
            if (signedOut != null && signedOut) {
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_global_to_login);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
