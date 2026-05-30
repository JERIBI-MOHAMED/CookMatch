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
import com.google.firebase.auth.FirebaseAuth;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private FirebaseAuth firebaseAuth;

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
        String email = String.valueOf(binding.emailInput.getText()).trim();
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

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    binding.registerProgress.setVisibility(View.GONE);
                    binding.createAccountButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        String uid = firebaseAuth.getCurrentUser().getUid();
                        UserRepository.getInstance().saveUserProfile(uid, email, new androidx.lifecycle.MutableLiveData<>());
                        Toast.makeText(requireContext(), "Account created", Toast.LENGTH_SHORT).show();
                        NavHostFragment.findNavController(this).navigate(R.id.loginFragment);
                        return;
                    }

                    String message = task.getException() != null
                            ? task.getException().getMessage()
                            : "Registration failed";
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
