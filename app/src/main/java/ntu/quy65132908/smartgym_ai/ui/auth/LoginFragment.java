package ntu.quy65132908.smartgym_ai.ui.auth;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.databinding.FragmentLoginBinding;

@AndroidEntryPoint
public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthViewModel viewModel;

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

        binding.tvForgotPassword.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            viewModel.resetPassword(email);
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnLogin.setEnabled(!isLoading);
            binding.btnGoogle.setEnabled(!isLoading);
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
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_login_to_dashboard);
            }
        });

        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void signInWithGoogle() {
        CredentialManager credentialManager = CredentialManager.create(requireContext());

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        credentialManager.getCredentialAsync(
                requireContext(),
                request,
                null, // CancellationSignal
                executor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        try {
                            GoogleIdTokenCredential credential = GoogleIdTokenCredential.createFrom(
                                    result.getCredential().getData());
                            String idToken = credential.getIdToken();
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> viewModel.signInWithGoogle(idToken));
                            }
                        } catch (Exception e) {
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() ->
                                        viewModel.reportGoogleSignInFailure(e.getMessage()));
                            }
                        } finally {
                            executor.shutdown();
                        }
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        String message = e instanceof NoCredentialException
                                ? getString(R.string.error_google_no_credential)
                                : e.getMessage();
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() ->
                                    viewModel.reportGoogleSignInFailure(message));
                        }
                        executor.shutdown();
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
