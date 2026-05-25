package ntu.quy65132908.smartgym_ai.ui.auth;

import android.os.CancellationSignal;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseUser;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.databinding.DialogPasswordResetBinding;
import ntu.quy65132908.smartgym_ai.databinding.FragmentLoginBinding;
import ntu.quy65132908.smartgym_ai.ui.onboarding.OnboardingDestinationResolver;

@AndroidEntryPoint
public class LoginFragment extends Fragment {

    @Inject
    UserRepository userRepository;

    private FragmentLoginBinding binding;
    private AuthViewModel viewModel;
    private AlertDialog passwordResetDialog;
    private DialogPasswordResetBinding passwordResetBinding;
    private CancellationSignal googleCancellationSignal;
    private ExecutorService googleExecutor;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupClickListeners();
        observeViewModel();
    }

    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            viewModel.signIn(email, password);
        });

        binding.tvRegister.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_login_to_register)
        );

        binding.btnGoogle.setOnClickListener(v -> signInWithGoogle());

        binding.tvForgotPassword.setOnClickListener(v -> showPasswordResetDialog());
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnLogin.setEnabled(!isLoading);
            binding.btnGoogle.setEnabled(!isLoading);
            updatePasswordResetDialogLoading(isLoading);
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

        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        });

        viewModel.getPasswordResetErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (passwordResetBinding != null) {
                passwordResetBinding.tilResetEmail.setError(error);
            }
        });

        viewModel.getPasswordResetSuccessMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                if (passwordResetDialog != null) {
                    passwordResetDialog.dismiss();
                }
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void showPasswordResetDialog() {
        DialogPasswordResetBinding dialogBinding = DialogPasswordResetBinding.inflate(
                LayoutInflater.from(requireContext()));
        dialogBinding.etResetEmail.setText(getEmailInput());

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.password_reset_title)
                .setMessage(R.string.password_reset_message)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.password_reset_send, null)
                .create();

        dialog.setOnShowListener(d -> {
            passwordResetDialog = dialog;
            passwordResetBinding = dialogBinding;
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (passwordResetBinding == null) {
                    return;
                }
                passwordResetBinding.tilResetEmail.setError(null);
                String email = passwordResetBinding.etResetEmail.getText() != null
                        ? passwordResetBinding.etResetEmail.getText().toString().trim()
                        : "";
                viewModel.resetPassword(email);
            });
            updatePasswordResetDialogLoading(Boolean.TRUE.equals(viewModel.getIsLoading().getValue()));
        });

        dialog.setOnDismissListener(d -> {
            if (passwordResetDialog == dialog) {
                passwordResetDialog = null;
                passwordResetBinding = null;
            }
        });

        dialog.show();
    }

    private void updatePasswordResetDialogLoading(Boolean isLoading) {
        boolean loading = Boolean.TRUE.equals(isLoading);
        if (passwordResetBinding != null) {
            passwordResetBinding.progressReset.setVisibility(loading ? View.VISIBLE : View.GONE);
            passwordResetBinding.etResetEmail.setEnabled(!loading);
        }
        if (passwordResetDialog != null) {
            passwordResetDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!loading);
            passwordResetDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(!loading);
        }
    }

    private void signInWithGoogle() {
        String serverClientId = getString(R.string.default_web_client_id);
        if (!isConfiguredGoogleClientId(serverClientId)) {
            viewModel.reportGoogleConfigurationMissing();
            return;
        }

        viewModel.startGoogleCredentialRequest();
        CredentialManager credentialManager = CredentialManager.create(requireContext());

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        googleCancellationSignal = new CancellationSignal();
        googleExecutor = Executors.newSingleThreadExecutor();
        ExecutorService executor = googleExecutor;
        credentialManager.getCredentialAsync(
                requireContext(),
                request,
                googleCancellationSignal,
                executor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        try {
                            GoogleIdTokenCredential credential = GoogleIdTokenCredential.createFrom(
                                    result.getCredential().getData());
                            String idToken = credential.getIdToken();
                            if (isViewAlive()) {
                                requireActivity().runOnUiThread(() -> viewModel.signInWithGoogle(idToken));
                            }
                        } catch (Exception e) {
                            if (isViewAlive()) {
                                requireActivity().runOnUiThread(() ->
                                        viewModel.reportGoogleCredentialFailure(e.getMessage()));
                            }
                        } finally {
                            shutdownGoogleExecutor(executor);
                        }
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        if (isViewAlive()) {
                            requireActivity().runOnUiThread(() -> {
                                if (e instanceof GetCredentialCancellationException) {
                                    viewModel.reportGoogleSignInCanceled();
                                } else if (e instanceof NoCredentialException) {
                                    viewModel.reportGoogleNoCredential();
                                } else {
                                    viewModel.reportGoogleCredentialFailure(e.getMessage());
                                }
                            });
                        }
                        shutdownGoogleExecutor(executor);
                    }
                }
        );
    }

    private void navigateAfterAuth(FirebaseUser firebaseUser) {
        if (firebaseUser == null || !isViewAlive()) {
            return;
        }
        userRepository.getUser(firebaseUser.getUid(), new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (isViewAlive()) {
                    requireActivity().runOnUiThread(() -> navigateToPostAuthDestination(user));
                }
            }

            @Override
            public void onError(Exception e) {
                if (isViewAlive()) {
                    requireActivity().runOnUiThread(() ->
                            Navigation.findNavController(binding.getRoot())
                                    .navigate(R.id.action_login_to_onboarding));
                }
            }
        });
    }

    private void navigateToPostAuthDestination(User user) {
        int actionId = OnboardingDestinationResolver.requiresOnboarding(user)
                ? R.id.action_login_to_onboarding
                : R.id.action_login_to_dashboard;
        Navigation.findNavController(binding.getRoot()).navigate(actionId);
    }

    private boolean isConfiguredGoogleClientId(String clientId) {
        return clientId != null
                && clientId.endsWith(".apps.googleusercontent.com")
                && !clientId.contains("placeholder");
    }

    private String getEmailInput() {
        return binding.etEmail.getText() != null
                ? binding.etEmail.getText().toString().trim()
                : "";
    }

    private boolean isViewAlive() {
        return isAdded() && binding != null;
    }

    private void shutdownGoogleExecutor(ExecutorService executor) {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        if (googleExecutor == executor) {
            googleExecutor = null;
            googleCancellationSignal = null;
        }
    }

    @Override
    public void onDestroyView() {
        if (googleCancellationSignal != null) {
            googleCancellationSignal.cancel();
        }
        if (googleExecutor != null && !googleExecutor.isShutdown()) {
            googleExecutor.shutdownNow();
        }
        if (passwordResetDialog != null) {
            passwordResetDialog.dismiss();
        }
        super.onDestroyView();
        binding = null;
    }
}
