package com.cookmatch.app.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.cookmatch.app.R;
import com.cookmatch.app.data.repository.UserRepository;
import com.cookmatch.app.databinding.FragmentRegisterBinding;
import com.cookmatch.app.presentation.state.UiState;
import com.cookmatch.app.ui.main.MainActivity;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private FirebaseAuth firebaseAuth;
    private androidx.lifecycle.MutableLiveData<UiState<Void>> profileSaveState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();

        binding.createAccountButton.setOnClickListener(v -> register());
        binding.backToLoginButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.loginFragment));
    }

    private void register() {
        String email = String.valueOf(binding.emailInput.getText()).trim().toLowerCase();
        String password = String.valueOf(binding.passwordInput.getText()).trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(requireContext(), "Email and password are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(requireContext(), "Password must contain at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.registerProgress.setVisibility(View.VISIBLE);
        binding.createAccountButton.setEnabled(false);
        binding.backToLoginButton.setEnabled(false);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveProfileAndOpenApp(email);
                        return;
                    }

                    finishLoading();
                    Toast.makeText(requireContext(), readableAuthError(task.getException()), Toast.LENGTH_LONG).show();
                });
    }

    private void saveProfileAndOpenApp(String email) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            finishLoading();
            Toast.makeText(requireContext(), "Account created, but user session was not found. Please sign in.", Toast.LENGTH_LONG).show();
            NavHostFragment.findNavController(this).navigate(R.id.loginFragment);
            return;
        }

        profileSaveState = new androidx.lifecycle.MutableLiveData<>();
        profileSaveState.observe(getViewLifecycleOwner(), state -> {
            if (state == null || state.getStatus() == UiState.Status.LOADING) {
                return;
            }

            finishLoading();

            if (state.getStatus() == UiState.Status.SUCCESS) {
                Toast.makeText(requireContext(), "Account created", Toast.LENGTH_SHORT).show();
                startActivity(new android.content.Intent(requireContext(), MainActivity.class));
                requireActivity().finish();
                return;
            }

            Toast.makeText(requireContext(),
                    "Account created, but profile save failed: " + state.getMessage(),
                    Toast.LENGTH_LONG).show();
        });

        UserRepository.getInstance().saveUserProfile(currentUser.getUid(), email, profileSaveState);
    }

    private void finishLoading() {
        if (binding == null) {
            return;
        }

        binding.registerProgress.setVisibility(View.GONE);
        binding.createAccountButton.setEnabled(true);
        binding.backToLoginButton.setEnabled(true);
    }

    private String readableAuthError(Exception exception) {
        if (exception instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) exception).getErrorCode();
            if ("ERROR_OPERATION_NOT_ALLOWED".equals(code)) {
                return "Enable Email/Password sign-in in Firebase Authentication.";
            }
            if ("ERROR_EMAIL_ALREADY_IN_USE".equals(code)) {
                return "This email is already registered. Try signing in.";
            }
            if ("ERROR_INVALID_EMAIL".equals(code)) {
                return "Enter a valid email address.";
            }
            if ("ERROR_WEAK_PASSWORD".equals(code)) {
                return "Password is too weak. Use at least 6 characters.";
            }
            if ("ERROR_NETWORK_REQUEST_FAILED".equals(code)) {
                return "Network error. Check your internet connection.";
            }
        }

        return exception != null && exception.getMessage() != null
                ? exception.getMessage()
                : "Registration failed";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
