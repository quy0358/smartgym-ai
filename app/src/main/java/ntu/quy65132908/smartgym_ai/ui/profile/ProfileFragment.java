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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.databinding.FragmentProfileBinding;
import ntu.quy65132908.smartgym_ai.util.AvatarHelper;

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

        setupStatLabels();
        observeViewModel();

        // M6: Confirm dialog before sign out
        binding.btnSignOut.setOnClickListener(v ->
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Đăng xuất", (dialog, which) -> viewModel.signOut())
                .show()
        );

        binding.btnEditProfile.setOnClickListener(v ->
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_profile_to_edit_profile)
        );

        binding.btnOpenWellness.setOnClickListener(v ->
            Navigation.findNavController(requireView()).navigate(R.id.nav_wellness)
        );
    }

    private void setupStatLabels() {
        TextView wLabel = binding.statTotalWorkouts.getRoot().findViewById(R.id.tv_stat_label);
        TextView wUnit = binding.statTotalWorkouts.getRoot().findViewById(R.id.tv_stat_unit);
        wLabel.setText("BÀI TẬP");
        wUnit.setText("TỔNG CỘNG");

        TextView hLabel = binding.statTotalHours.getRoot().findViewById(R.id.tv_stat_label);
        TextView hUnit = binding.statTotalHours.getRoot().findViewById(R.id.tv_stat_unit);
        hLabel.setText("GIỜ TẬP");
        hUnit.setText("GIỜ");

        TextView sLabel = binding.statStreakDays.getRoot().findViewById(R.id.tv_stat_label);
        TextView sUnit = binding.statStreakDays.getRoot().findViewById(R.id.tv_stat_unit);
        sLabel.setText("CHUỖI");
        sUnit.setText("NGÀY");
    }

    private void observeViewModel() {
        viewModel.getDisplayName().observe(getViewLifecycleOwner(), name -> {
            binding.tvProfileName.setText(name);
            AvatarHelper.loadAvatar(
                requireContext(),
                viewModel.getPhotoUrl().getValue(),
                binding.ivProfileAvatar,
                binding.tvProfileAvatar,
                name
            );
        });

        viewModel.getPhotoUrl().observe(getViewLifecycleOwner(), url -> {
            AvatarHelper.loadAvatar(
                requireContext(),
                url,
                binding.ivProfileAvatar,
                binding.tvProfileAvatar,
                binding.tvProfileName.getText().toString()
            );
        });

        viewModel.getEmail().observe(getViewLifecycleOwner(), email ->
                binding.tvProfileEmail.setText(email)
        );

        // C1: Observe real stats from Firestore
        viewModel.getTotalWorkouts().observe(getViewLifecycleOwner(), count -> {
            TextView wValue = binding.statTotalWorkouts.getRoot().findViewById(R.id.tv_stat_value);
            wValue.setText(String.valueOf(count));
        });

        viewModel.getTotalHours().observe(getViewLifecycleOwner(), hours -> {
            TextView hValue = binding.statTotalHours.getRoot().findViewById(R.id.tv_stat_value);
            hValue.setText(String.valueOf(hours));
        });

        viewModel.getStreakDays().observe(getViewLifecycleOwner(), streak -> {
            TextView sValue = binding.statStreakDays.getRoot().findViewById(R.id.tv_stat_value);
            sValue.setText(String.valueOf(streak));
        });

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
