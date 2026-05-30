package com.cookmatch.app.ui.main.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cookmatch.app.databinding.FragmentMyRecipesBinding;
import com.cookmatch.app.presentation.state.UiState;
import com.cookmatch.app.ui.main.myrecipes.MyRecipesAdapter;
import com.cookmatch.app.ui.main.myrecipes.MyRecipesViewModel;

public class MyRecipesFragment extends Fragment {

    private FragmentMyRecipesBinding binding;
    private MyRecipesViewModel viewModel;
    private MyRecipesAdapter adapter;
    private Uri selectedImageUri;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMyRecipesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MyRecipesViewModel.class);
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) {
                        return;
                    }
                    selectedImageUri = uri;
                    binding.selectedImagePreview.setImageURI(uri);
                    binding.imageUrlInput.setText("");
                }
        );

        adapter = new MyRecipesAdapter();
        binding.myRecipesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.myRecipesRecyclerView.setAdapter(adapter);

        binding.addRecipeButton.setOnClickListener(v -> submitRecipe());
        binding.pickImageButton.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        viewModel.getRecipesState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            switch (state.getStatus()) {
                case LOADING:
                    binding.progressBar.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    binding.progressBar.setVisibility(View.GONE);
                    adapter.submitList(state.getData());
                    break;
                case EMPTY:
                    binding.progressBar.setVisibility(View.GONE);
                    adapter.submitList(null);
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), state.getMessage(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        viewModel.getAddState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            if (state.getStatus() == UiState.Status.SUCCESS) {
                clearInputs();
                Toast.makeText(requireContext(), "Recipe added!", Toast.LENGTH_SHORT).show();
            } else if (state.getStatus() == UiState.Status.ERROR) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), state.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.loadMyRecipes();
    }

    private void submitRecipe() {
        String title = String.valueOf(binding.titleInput.getText()).trim();
        String imageUrl = String.valueOf(binding.imageUrlInput.getText()).trim();
        String ingredients = String.valueOf(binding.ingredientsInput.getText()).trim();
        String instructions = String.valueOf(binding.instructionsInput.getText()).trim();

        if (TextUtils.isEmpty(title)) {
            binding.titleInput.setError("Title is required");
            return;
        }

        if (!TextUtils.isEmpty(imageUrl) && !Patterns.WEB_URL.matcher(imageUrl).matches()) {
            binding.imageUrlInput.setError("Enter a valid image URL");
            return;
        }

        viewModel.addRecipe(title, selectedImageUri, imageUrl, ingredients, instructions);
    }

    private void clearInputs() {
        binding.titleInput.setText("");
        binding.imageUrlInput.setText("");
        binding.ingredientsInput.setText("");
        binding.instructionsInput.setText("");
        selectedImageUri = null;
        binding.selectedImagePreview.setImageResource(android.R.drawable.ic_menu_gallery);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
