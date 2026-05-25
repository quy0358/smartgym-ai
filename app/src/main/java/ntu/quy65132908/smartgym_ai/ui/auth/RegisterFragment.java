package ntu.quy65132908.smartgym_ai.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseUser;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.databinding.FragmentRegisterBinding;
import ntu.quy65132908.smartgym_ai.ui.onboarding.OnboardingDestinationResolver;

@AndroidEntryPoint
public class RegisterFragment extends Fragment {

    @Inject
    UserRepository userRepository;

    private FragmentRegisterBinding binding;
    private AuthViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnRegister.setOnClickListener(v -> {
            String name = binding.etName.getText().toString().trim();
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            viewModel.signUp(name, email, password);
        });

        binding.tvBackLogin.setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack()
        );

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnRegister.setEnabled(!isLoading);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                binding.tvError.setVisibility(View.VISIBLE);
                binding.tvError.setText(error);
            } else {
                binding.tvError.setVisibility(View.GONE);
            }
        });

        viewModel.getAuthSuccess().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                navigateAfterAuth(user);
            }
        });
    }

    private void navigateAfterAuth(FirebaseUser firebaseUser) {
        if (firebaseUser == null || binding == null) {
            return;
        }
        userRepository.getUser(firebaseUser.getUid(), new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (binding != null && isAdded()) {
                    requireActivity().runOnUiThread(() -> navigateToPostAuthDestination(user));
                }
            }

            @Override
            public void onError(Exception e) {
                if (binding != null && isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Navigation.findNavController(binding.getRoot())
                                    .navigate(R.id.action_register_to_onboarding));
                }
            }
        });
    }

    private void navigateToPostAuthDestination(User user) {
        int actionId = OnboardingDestinationResolver.requiresOnboarding(user)
                ? R.id.action_register_to_onboarding
                : R.id.action_register_to_dashboard;
        Navigation.findNavController(binding.getRoot()).navigate(actionId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
