package ntu.quy65132908.smartgym_ai.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.databinding.FragmentEditProfileBinding;

@AndroidEntryPoint
public class EditProfileFragment extends Fragment {

    private FragmentEditProfileBinding binding;
    private EditProfileViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(EditProfileViewModel.class);

        // Populate fields when user data loads
        viewModel.getUserData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                binding.etDisplayName.setText(user.getDisplayName());
                if (user.getWeight() != null) binding.etWeight.setText(String.valueOf(user.getWeight()));
                if (user.getHeight() != null) binding.etHeight.setText(String.valueOf(user.getHeight()));
                if (user.getGoal() != null) binding.etGoal.setText(user.getGoal());
                if (user.getBmi() != null) {
                    binding.tvBmiDisplay.setText(String.format(
                            Locale.getDefault(),
                            "BMI: %.1f (%s)",
                            user.getBmi(),
                            user.getBmiCategory()));
                }
            }
        });

        binding.btnSave.setOnClickListener(v -> {
            String name = binding.etDisplayName.getText().toString();
            String weight = binding.etWeight.getText().toString();
            String height = binding.etHeight.getText().toString();
            String goal = binding.etGoal.getText().toString();
            viewModel.saveProfile(name, weight, height, goal);
        });

        binding.btnCancel.setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack()
        );

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnSave.setEnabled(!loading);
        });

        viewModel.getSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Toast.makeText(requireContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show();
                Navigation.findNavController(view).popBackStack();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
