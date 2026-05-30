package com.cookmatch.app.ui.main.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cookmatch.app.R;
import com.cookmatch.app.databinding.FragmentSavedRecipesBinding;
import com.cookmatch.app.presentation.state.UiState;
import com.cookmatch.app.ui.main.home.RecipeAdapter;
import com.cookmatch.app.ui.main.saved.SavedRecipesViewModel;

public class SavedRecipesFragment extends Fragment {

    private FragmentSavedRecipesBinding binding;
    private SavedRecipesViewModel viewModel;
    private RecipeAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSavedRecipesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SavedRecipesViewModel.class);

        adapter = new RecipeAdapter(
                recipe -> {
                    Bundle args = new Bundle();
                    args.putString(RecipeDetailFragment.ARG_ID, recipe.getId());
                    args.putString(RecipeDetailFragment.ARG_TITLE, recipe.getTitle());
                    args.putString(RecipeDetailFragment.ARG_CATEGORY, recipe.getCategory());
                    args.putString(RecipeDetailFragment.ARG_THUMBNAIL, recipe.getThumbnailUrl());
                    NavHostFragment.findNavController(this).navigate(R.id.recipeDetailFragment, args);
                },
                recipe -> viewModel.unsaveRecipe(recipe.getId()),
                (recipe, currentlySaved) -> {
                    if (currentlySaved) {
                        viewModel.unsaveRecipe(recipe.getId());
                    }
                }
        );
        binding.savedRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.savedRecyclerView.setAdapter(adapter);

        viewModel.getSavedRecipesState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            switch (state.getStatus()) {
                case LOADING:
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.emptyText.setVisibility(View.GONE);
                    binding.emptyImage.setVisibility(View.GONE);
                    break;
                case SUCCESS:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.emptyText.setVisibility(View.GONE);
                    binding.emptyImage.setVisibility(View.GONE);
                    java.util.List<String> ids = new java.util.ArrayList<>();
                    for (com.cookmatch.app.domain.model.Recipe recipe : state.getData()) {
                        ids.add(recipe.getId());
                    }
                    adapter.setSavedIds(ids);
                    adapter.submitList(state.getData());
                    break;
                case EMPTY:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.emptyText.setVisibility(View.VISIBLE);
                    binding.emptyImage.setVisibility(View.VISIBLE);
                    adapter.submitList(null);
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.emptyImage.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), state.getMessage(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        viewModel.getActionState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            if (state.getStatus() == UiState.Status.SUCCESS) {
                Toast.makeText(requireContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();
                viewModel.loadSavedRecipes();
            }
        });

        viewModel.loadSavedRecipes();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
