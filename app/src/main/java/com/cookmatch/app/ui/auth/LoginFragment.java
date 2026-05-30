package com.cookmatch.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";
    private static final int RC_GOOGLE_SIGN_IN = 1001;

    private FragmentLoginBinding binding;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;

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
        configureGoogleSignIn();

        binding.openRegisterButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.registerFragment));

        binding.loginButton.setOnClickListener(v -> signIn());
        binding.googleSignInButton.setOnClickListener(v -> startGoogleSignIn());
    }

    private void configureGoogleSignIn() {
        int webClientIdResourceId = getResources().getIdentifier(
                "default_web_client_id",
                "string",
                requireContext().getPackageName()
        );

        if (webClientIdResourceId == 0) {
            binding.googleSignInButton.setEnabled(false);
            binding.googleSignInButton.setText("Google Sign-In not configured");
            return;
        }

        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(webClientIdResourceId))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), signInOptions);
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

    private void startGoogleSignIn() {
        if (googleSignInClient == null) {
            Toast.makeText(requireContext(), "Add SHA-1/SHA-256 in Firebase and replace google-services.json", Toast.LENGTH_LONG).show();
            return;
        }

        startActivityForResult(googleSignInClient.getSignInIntent(), RC_GOOGLE_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != RC_GOOGLE_SIGN_IN) {
            return;
        }

        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException exception) {
            Toast.makeText(requireContext(), "Google Sign-In failed: " + exception.getStatusCode(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Google Sign-In failed", exception);
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        binding.loginProgress.setVisibility(View.VISIBLE);
        binding.loginButton.setEnabled(false);
        binding.googleSignInButton.setEnabled(false);

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    binding.loginProgress.setVisibility(View.GONE);
                    binding.loginButton.setEnabled(true);
                    binding.googleSignInButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        writeFirestoreSmokeTest();
                        startActivity(new Intent(requireContext(), MainActivity.class));
                        requireActivity().finish();
                        return;
                    }

                    String message = task.getException() != null
                            ? task.getException().getMessage()
                            : "Google login failed";
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                });
    }

    private void writeFirestoreSmokeTest() {
        Map<String, Object> testData = new HashMap<>();
        testData.put("message", "Firestore connected");
        testData.put("createdAt", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection("test")
                .document("android-check")
                .set(testData)
                .addOnSuccessListener(unused -> Log.d(TAG, "Firestore smoke write OK"))
                .addOnFailureListener(exception -> Log.e(TAG, "Firestore smoke write failed", exception));

        FirebaseFirestore.getInstance()
                .collection("test")
                .document("android-check")
                .get()
                .addOnSuccessListener(snapshot -> Log.d(TAG, "Firestore smoke read: " + snapshot.getData()))
                .addOnFailureListener(exception -> Log.e(TAG, "Firestore smoke read failed", exception));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
