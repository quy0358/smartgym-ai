package ntu.quy65132908.smartgym_ai.ui.onboarding;

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

import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.databinding.FragmentOnboardingBinding;
import ntu.quy65132908.smartgym_ai.ui.profile.ProfileMetrics;

@AndroidEntryPoint
public class OnboardingFragment extends Fragment {
    private FragmentOnboardingBinding binding;
    private OnboardingViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);

        setupDefaults();
        setupListeners();
        observeViewModel();
        updateBmiPreview();
    }

    private void setupDefaults() {
        binding.groupGender.check(R.id.btn_gender_male);
        binding.groupGoal.check(R.id.btn_goal_get_fitter);
        binding.groupLevel.check(R.id.btn_level_beginner);
    }

    private void setupListeners() {
        TextWatcher bmiWatcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                updateBmiPreview();
            }
        };
        binding.etHeight.addTextChangedListener(bmiWatcher);
        binding.etWeight.addTextChangedListener(bmiWatcher);
        binding.etTargetWeight.addTextChangedListener(bmiWatcher);

        binding.btnFinishOnboarding.setOnClickListener(v -> {
            OnboardingProfileDraft draft = collectDraft();
            if (!draft.isComplete()) {
                showValidationErrors(draft);
                Snackbar.make(binding.getRoot(), R.string.onboarding_required_error, Snackbar.LENGTH_LONG).show();
                return;
            }
            clearErrors();
            viewModel.saveDraft(draft);
        });
    }

    private void observeViewModel() {
        viewModel.getIsSaving().observe(getViewLifecycleOwner(), saving -> {
            boolean isSaving = Boolean.TRUE.equals(saving);
            binding.progressSaving.setVisibility(isSaving ? View.VISIBLE : View.GONE);
            binding.btnFinishOnboarding.setEnabled(!isSaving);
            binding.btnFinishOnboarding.setText(isSaving
                    ? R.string.profile_saving
                    : R.string.onboarding_finish);
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        });
        viewModel.getSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Navigation.findNavController(binding.getRoot()).navigate(R.id.action_onboarding_to_dashboard);
            }
        });
    }

    private OnboardingProfileDraft collectDraft() {
        OnboardingProfileDraft draft = new OnboardingProfileDraft();
        draft.setGender(selectedGender());
        draft.setBirthDate(textOf(binding.etBirthDate.getText()));
        draft.setHeightCm(ProfileMetrics.parseOptionalFloat(textOf(binding.etHeight.getText())));
        draft.setWeightKg(ProfileMetrics.parseOptionalFloat(textOf(binding.etWeight.getText())));
        draft.setTargetWeightKg(ProfileMetrics.parseOptionalFloat(textOf(binding.etTargetWeight.getText())));
        draft.setGoal(selectedGoal());
        draft.setFitnessLevel(selectedLevel());
        return draft;
    }

    private void updateBmiPreview() {
        OnboardingProfileDraft draft = collectDraft();
        Float bmi = draft.calculateBmiOrNull();
        if (bmi == null) {
            binding.tvBmiPreview.setText(R.string.profile_bmi_unavailable);
            return;
        }
        binding.tvBmiPreview.setText(getString(
                R.string.onboarding_bmi_preview,
                bmi,
                draft.bmiCategoryOrEmpty(),
                targetWeightText()));
    }

    private void showValidationErrors(OnboardingProfileDraft draft) {
        clearErrors();
        if (draft.getBirthDate().isEmpty()) {
            binding.tilBirthDate.setError(getString(R.string.onboarding_birthdate_error));
        }
        if (draft.getHeightCm() == null) {
            binding.tilHeight.setError(getString(R.string.profile_height_invalid));
        }
        if (draft.getWeightKg() == null) {
            binding.tilWeight.setError(getString(R.string.profile_weight_invalid));
        }
        if (draft.getTargetWeightKg() == null) {
            binding.tilTargetWeight.setError(getString(R.string.profile_weight_invalid));
        }
    }

    private void clearErrors() {
        binding.tilBirthDate.setError(null);
        binding.tilHeight.setError(null);
        binding.tilWeight.setError(null);
        binding.tilTargetWeight.setError(null);
    }

    private String selectedGender() {
        int id = binding.groupGender.getCheckedButtonId();
        return id == R.id.btn_gender_female ? "female" : "male";
    }

    private String selectedGoal() {
        int id = binding.groupGoal.getCheckedButtonId();
        if (id == R.id.btn_goal_lose_weight) {
            return getString(R.string.onboarding_goal_lose_weight);
        }
        if (id == R.id.btn_goal_gain_muscle) {
            return getString(R.string.onboarding_goal_gain_muscle);
        }
        return getString(R.string.onboarding_goal_get_fitter);
    }

    private String selectedLevel() {
        int id = binding.groupLevel.getCheckedButtonId();
        if (id == R.id.btn_level_intermediate) {
            return getString(R.string.onboarding_level_intermediate);
        }
        if (id == R.id.btn_level_advanced) {
            return getString(R.string.onboarding_level_advanced);
        }
        return getString(R.string.onboarding_level_beginner);
    }

    private String targetWeightText() {
        String raw = textOf(binding.etTargetWeight.getText());
        return raw.isEmpty() ? "--" : raw;
    }

    private static String textOf(Editable editable) {
        return editable != null ? editable.toString().trim() : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
