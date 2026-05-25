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
import com.google.android.material.snackbar.Snackbar;

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
        setupActions();
        observeViewModel();
    }

    private void setupActions() {
        binding.btnSignOut.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.profile_sign_out_title)
                        .setMessage(R.string.profile_sign_out_message)
                        .setNegativeButton(R.string.action_cancel, null)
                        .setPositiveButton(R.string.sign_out, (dialog, which) -> viewModel.signOut())
                        .show()
        );

        binding.btnEditProfile.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_profile_to_edit_profile)
        );

        binding.btnOpenWellness.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.nav_wellness)
        );
    }

    private void setupStatLabels() {
        setStatCardLabels(
                binding.statTotalWorkouts.getRoot(),
                getString(R.string.stat_workouts),
                getString(R.string.stat_total)
        );
        setStatCardLabels(
                binding.statTotalHours.getRoot(),
                getString(R.string.stat_hours),
                getString(R.string.unit_hours)
        );
        setStatCardLabels(
                binding.statStreakDays.getRoot(),
                getString(R.string.stat_streak),
                getString(R.string.unit_days)
        );
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && binding != null) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        });
        viewModel.getSignedOut().observe(getViewLifecycleOwner(), signedOut -> {
            if (Boolean.TRUE.equals(signedOut)) {
                Navigation.findNavController(requireView()).navigate(R.id.action_global_to_login);
            }
        });
    }

    private void renderState(ProfileUiState state) {
        if (state == null || binding == null) {
            return;
        }

        binding.progressBar.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
        binding.tvProfileName.setText(nonBlank(state.getDisplayName(), getString(R.string.post_user_default)));
        binding.tvProfileEmail.setText(nonBlank(state.getEmail(), getString(R.string.profile_email_unavailable)));
        binding.tvProfileEmail.setContentDescription(getString(
                R.string.profile_email_a11y,
                binding.tvProfileEmail.getText().toString()));

        AvatarHelper.loadAvatar(
                requireContext(),
                state.getPhotoUrl(),
                binding.ivProfileAvatar,
                binding.tvProfileAvatar,
                binding.tvProfileName.getText().toString()
        );
        binding.ivProfileAvatar.setContentDescription(getString(
                R.string.profile_avatar_a11y,
                binding.tvProfileName.getText().toString()));
        binding.tvProfileAvatar.setContentDescription(getString(
                R.string.profile_avatar_a11y,
                binding.tvProfileName.getText().toString()));

        setStatValue(
                binding.statTotalWorkouts.getRoot(),
                String.valueOf(state.getTotalWorkouts()),
                getString(R.string.profile_total_workouts_a11y, state.getTotalWorkouts())
        );
        setStatValue(
                binding.statTotalHours.getRoot(),
                viewModel.formatHours(state.getTotalHours()),
                getString(R.string.profile_total_hours_a11y, viewModel.formatHours(state.getTotalHours()))
        );
        setStatValue(
                binding.statStreakDays.getRoot(),
                String.valueOf(state.getStreakDays()),
                getString(R.string.profile_streak_a11y, state.getStreakDays())
        );
    }

    private void setStatCardLabels(View root, String label, String unit) {
        TextView labelView = root.findViewById(R.id.tv_stat_label);
        TextView unitView = root.findViewById(R.id.tv_stat_unit);
        labelView.setText(label);
        unitView.setText(unit);
    }

    private void setStatValue(View root, String value, String contentDescription) {
        TextView valueView = root.findViewById(R.id.tv_stat_value);
        valueView.setText(value);
        root.setContentDescription(contentDescription);
    }

    private String nonBlank(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value : fallback;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
