package ntu.quy65132908.smartgym_ai.ui.profile;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.databinding.FragmentEditProfileBinding;

@AndroidEntryPoint
public class EditProfileFragment extends Fragment {

    private FragmentEditProfileBinding binding;
    private EditProfileViewModel viewModel;
    private boolean bindingForm;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(EditProfileViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        setupFormWatchers();
        setupButtons(view);
        observeViewModel(view);
    }

    private void setupButtons(View view) {
        binding.btnSave.setOnClickListener(v -> viewModel.saveProfile(
                text(binding.etDisplayName),
                text(binding.etWeight),
                text(binding.etHeight),
                text(binding.etGoal)
        ));
        binding.btnCancel.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
    }

    private void setupFormWatchers() {
        TextWatcher watcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (bindingForm) {
                    return;
                }
                viewModel.onProfileFormChanged(
                        text(binding.etDisplayName),
                        text(binding.etWeight),
                        text(binding.etHeight),
                        text(binding.etGoal));
            }
        };
        binding.etDisplayName.addTextChangedListener(watcher);
        binding.etWeight.addTextChangedListener(watcher);
        binding.etHeight.addTextChangedListener(watcher);
        binding.etGoal.addTextChangedListener(watcher);
    }

    private void observeViewModel(View view) {
        viewModel.getUserData().observe(getViewLifecycleOwner(), this::bindUser);
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.getFormErrors().observe(getViewLifecycleOwner(), this::renderErrors);
        viewModel.getSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Snackbar.make(binding.getRoot(), R.string.profile_saved, Snackbar.LENGTH_SHORT).show();
                Navigation.findNavController(view).popBackStack();
            }
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), err -> {
            if (err != null && binding != null) {
                Snackbar.make(binding.getRoot(), err, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void bindUser(User user) {
        if (user == null || binding == null) {
            return;
        }
        bindingForm = true;
        binding.etDisplayName.setText(user.getDisplayName());
        binding.etWeight.setText(user.getWeight() != null ? String.valueOf(user.getWeight()) : "");
        binding.etHeight.setText(user.getHeight() != null ? String.valueOf(user.getHeight()) : "");
        binding.etGoal.setText(user.getGoal() != null ? user.getGoal() : "");
        bindingForm = false;
    }

    private void renderState(EditProfileUiState state) {
        if (state == null || binding == null) {
            return;
        }
        boolean busy = state.isLoading() || state.isSaving();
        binding.progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(state.canSave());
        binding.btnSave.setText(state.isSaving()
                ? R.string.profile_saving
                : R.string.profile_save_changes);
        setFormEnabled(!busy && !state.isLoggedOut());
        renderBmi(state);
    }

    private void renderBmi(EditProfileUiState state) {
        if (state.getPreviewBmi() == null) {
            binding.tvBmiDisplay.setText(R.string.profile_bmi_unavailable);
            binding.tvBmiDisplay.setContentDescription(getString(R.string.profile_bmi_unavailable_a11y));
            return;
        }
        String text = String.format(
                Locale.getDefault(),
                getString(R.string.profile_bmi_format),
                state.getPreviewBmi(),
                state.getPreviewBmiCategory());
        binding.tvBmiDisplay.setText(text);
        binding.tvBmiDisplay.setContentDescription(getString(
                R.string.profile_bmi_a11y,
                state.getPreviewBmi(),
                state.getPreviewBmiCategory()));
    }

    private void renderErrors(ProfileFormErrors errors) {
        if (errors == null || binding == null) {
            return;
        }
        binding.tilDisplayName.setError(errors.getDisplayNameError());
        binding.tilWeight.setError(errors.getWeightError());
        binding.tilHeight.setError(errors.getHeightError());
        binding.tilGoal.setError(errors.getGoalError());
    }

    private void setFormEnabled(boolean enabled) {
        binding.etDisplayName.setEnabled(enabled);
        binding.etWeight.setEnabled(enabled);
        binding.etHeight.setEnabled(enabled);
        binding.etGoal.setEnabled(enabled);
        binding.btnCancel.setEnabled(!binding.progressBar.isShown());
    }

    private String text(TextView input) {
        return input.getText() != null ? input.getText().toString() : "";
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
