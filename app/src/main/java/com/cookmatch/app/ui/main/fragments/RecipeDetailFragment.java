package com.cookmatch.app.ui.main.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.cookmatch.app.R;
import com.cookmatch.app.data.repository.UserRepository;
import com.cookmatch.app.databinding.FragmentRecipeDetailBinding;
import com.cookmatch.app.presentation.state.UiState;
import com.cookmatch.app.ui.main.detail.RecipeDetailViewModel;

public class RecipeDetailFragment extends Fragment {

    private FragmentRecipeDetailBinding binding;
    private RecipeDetailViewModel viewModel;
    private final UserRepository userRepository = UserRepository.getInstance();
    private boolean isSaved;
    private String recipeId;

    // Passed via Bundle from HomeFragment / SearchFragment
    public static final String ARG_ID = "recipeId";
    public static final String ARG_TITLE = "recipeTitle";
    public static final String ARG_CATEGORY = "recipeCategory";
    public static final String ARG_THUMBNAIL = "recipeThumbnail";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRecipeDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(RecipeDetailViewModel.class);

        Bundle args = getArguments();
        String id = args != null ? args.getString(ARG_ID, "") : "";
        String title = args != null ? args.getString(ARG_TITLE, "Recipe") : "Recipe";
        String category = args != null ? args.getString(ARG_CATEGORY, "") : "";
        String thumbnail = args != null ? args.getString(ARG_THUMBNAIL, "") : "";
        recipeId = id;

        binding.recipeTitle.setText(title);
        binding.recipeCategory.setText(category == null || category.isEmpty() ? "General" : category);
        binding.recipeId.setText(id);
        binding.recipeIngredients.setText("Loading ingredients...");
        binding.recipeInstructions.setText("Loading instructions...");
        com.bumptech.glide.Glide.with(this)
            .load(thumbnail)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_report_image)
            .centerCrop()
            .into(binding.recipeImage);

        viewModel.loadRecipeDetails(id);
        refreshSavedState();

        viewModel.getRecipeDetailState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            if (state.getStatus() == UiState.Status.LOADING) {
                binding.progressBar.setVisibility(View.VISIBLE);
                return;
            }

            binding.progressBar.setVisibility(View.GONE);

            if (state.getStatus() == UiState.Status.SUCCESS && state.getData() != null) {
                com.cookmatch.app.domain.model.Recipe recipe = state.getData();

                com.bumptech.glide.Glide.with(this)
                        .load(recipe.getThumbnailUrl())
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .centerCrop()
                        .into(binding.recipeImage);

                if (recipe.getInstructions() != null && !recipe.getInstructions().isEmpty()) {
                    binding.recipeInstructions.setText(recipe.getInstructions());
                }

                if (recipe.getIngredients() != null && !recipe.getIngredients().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (String ingredient : recipe.getIngredients()) {
                        sb.append("• ").append(ingredient).append("\n");
                    }
                    binding.recipeIngredients.setText(sb.toString().trim());
                } else {
                    binding.recipeIngredients.setText("No ingredient details available.");
                }
            }
        });

        binding.saveButton.setOnClickListener(v -> {
            if (isSaved) {
                viewModel.unsaveRecipe(recipeId);
            } else {
                viewModel.saveRecipe(recipeId);
            }
        });

        viewModel.getSaveState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            if (state.getStatus() == UiState.Status.SUCCESS) {
                isSaved = !isSaved;
                updateSavedVisual();
                Toast.makeText(requireContext(), isSaved ? "Saved!" : "Removed!", Toast.LENGTH_SHORT).show();
            } else if (state.getStatus() == UiState.Status.ERROR) {
                Toast.makeText(requireContext(), state.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshSavedState() {
        MutableLiveData<UiState<java.util.List<String>>> state = new MutableLiveData<>();
        state.observe(getViewLifecycleOwner(), s -> {
            if (s == null) return;
            if (s.getStatus() == UiState.Status.SUCCESS && s.getData() != null) {
                isSaved = s.getData().contains(recipeId);
                updateSavedVisual();
            }
        });
        userRepository.loadSavedRecipeIds(state);
    }

    private void updateSavedVisual() {
        binding.savedChip.setText(isSaved ? "Saved" : "Not saved");
        binding.savedChip.setChipIconResource(
                isSaved ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline
        );
        binding.saveButton.setText(isSaved ? "Remove From Favorites" : "Save To Favorites");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
