package com.cookmatch.app.ui.main.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.cookmatch.app.R;
import com.cookmatch.app.databinding.FragmentScannerBinding;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class ScannerFragment extends Fragment {

    private FragmentScannerBinding binding;
    private ImageCapture imageCapture;
    private TextRecognizer textRecognizer;
    private String lastDetectedText = "";
    private List<String> lastDetectedIngredients = new ArrayList<>();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    showCamera();
                } else {
                    binding.permissionView.setVisibility(View.VISIBLE);
                    binding.cameraPreview.setVisibility(View.GONE);
                    binding.captureButton.setVisibility(View.GONE);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentScannerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        binding.requestPermissionButton.setOnClickListener(v ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA));

        binding.captureButton.setOnClickListener(v -> captureAndAnalyze());

        binding.addToListButton.setOnClickListener(v -> {
            if (lastDetectedIngredients.isEmpty()) {
                Toast.makeText(requireContext(), "No ingredient candidates detected", Toast.LENGTH_SHORT).show();
                return;
            }

            Bundle args = new Bundle();
            args.putString("prefillIngredients", String.join(",", lastDetectedIngredients));
            NavHostFragment.findNavController(this).navigate(R.id.myListFragment, args);
        });

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            showCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void showCamera() {
        binding.permissionView.setVisibility(View.GONE);
        binding.cameraPreview.setVisibility(View.VISIBLE);
        binding.captureButton.setVisibility(View.VISIBLE);

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.cameraPreview.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(), cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(requireContext(),
                        "Camera init failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void captureAndAnalyze() {
        if (imageCapture == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);

        imageCapture.takePicture(
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                        @SuppressWarnings("UnsafeOptInUsageError")
                        InputImage inputImage = InputImage.fromMediaImage(
                                imageProxy.getImage(),
                                imageProxy.getImageInfo().getRotationDegrees());

                        textRecognizer.process(inputImage)
                                .addOnSuccessListener(visionText -> {
                                    binding.progressBar.setVisibility(View.GONE);
                                    String text = visionText.getText();
                                    if (text.isEmpty()) {
                                        binding.detectedText.setText("No text detected.");
                                        binding.addToListButton.setVisibility(View.GONE);
                                    } else {
                                        lastDetectedText = text.trim();
                                        lastDetectedIngredients = parseIngredients(lastDetectedText);

                                        if (lastDetectedIngredients.isEmpty()) {
                                            binding.detectedText.setText(lastDetectedText);
                                            binding.addToListButton.setVisibility(View.GONE);
                                        } else {
                                            binding.detectedText.setText("Detected ingredients: "
                                                    + String.join(", ", lastDetectedIngredients));
                                            binding.addToListButton.setVisibility(View.VISIBLE);
                                        }
                                    }
                                    imageProxy.close();
                                })
                                .addOnFailureListener(e -> {
                                    binding.progressBar.setVisibility(View.GONE);
                                    binding.detectedText.setText("Error: " + e.getMessage());
                                    imageProxy.close();
                                });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(),
                                "Capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private List<String> parseIngredients(String raw) {
        String normalized = raw.toLowerCase(Locale.ROOT)
                .replace("\n", " ")
                .replaceAll("[^a-z, ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.isEmpty()) {
            return new ArrayList<>();
        }

        String[] parts = normalized.split("[, ]+");
        Set<String> unique = new LinkedHashSet<>();
        Set<String> stop = new LinkedHashSet<>(java.util.Arrays.asList(
                "ingredients", "ingredient", "recipe", "instructions", "for", "with",
                "and", "the", "add", "mix", "cook", "step", "salt", "pepper",
                "cup", "cups", "tbsp", "tsp", "ml", "g", "kg"
        ));

        for (String p : parts) {
            String word = p.trim();
            if (word.length() < 3) continue;
            if (stop.contains(word)) continue;
            unique.add(word);
            if (unique.size() >= 12) break;
        }

        return new ArrayList<>(unique);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (textRecognizer != null) textRecognizer.close();
        binding = null;
    }
}
