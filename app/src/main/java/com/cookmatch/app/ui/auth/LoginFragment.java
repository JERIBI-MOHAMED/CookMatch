package com.cookmatch.app.ui.auth;

import android.content.Intent;
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
import com.cookmatch.app.databinding.FragmentLoginBinding;
import com.cookmatch.app.ui.main.MainActivity;
import com.google.firebase.auth.FirebaseAuth;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private FirebaseAuth firebaseAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();

        binding.openRegisterButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.registerFragment));

        binding.loginButton.setOnClickListener(v -> signIn());
    }

    private void signIn() {
        String email = String.valueOf(binding.emailInput.getText()).trim();
        String password = String.valueOf(binding.passwordInput.getText()).trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(requireContext(), "Email and password are required", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.loginProgress.setVisibility(View.VISIBLE);
        binding.loginButton.setEnabled(false);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    binding.loginProgress.setVisibility(View.GONE);
                    binding.loginButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        startActivity(new Intent(requireContext(), MainActivity.class));
                        requireActivity().finish();
                        return;
                    }

                    String message = task.getException() != null
                            ? task.getException().getMessage()
                            : "Login failed";
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
