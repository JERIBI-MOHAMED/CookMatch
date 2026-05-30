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
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cookmatch.app.R;
import com.cookmatch.app.data.repository.UserRepository;
import com.cookmatch.app.databinding.FragmentHomeBinding;
import com.cookmatch.app.presentation.state.UiState;
import com.cookmatch.app.ui.main.home.HomeViewModel;
import com.cookmatch.app.ui.main.home.RecipeAdapter;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private RecipeAdapter adapter;
    private final com.cookmatch.app.data.repository.UserRepository userRepository =
            com.cookmatch.app.data.repository.UserRepository.getInstance();
    private final MutableLiveData<UiState<java.util.List<String>>> savedIdsState = new MutableLiveData<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        adapter = new RecipeAdapter(
                recipe -> {
                    Bundle args = new Bundle();
                    args.putString(RecipeDetailFragment.ARG_ID, recipe.getId());
                    args.putString(RecipeDetailFragment.ARG_TITLE, recipe.getTitle());
                    args.putString(RecipeDetailFragment.ARG_CATEGORY, recipe.getCategory());
                    args.putString(RecipeDetailFragment.ARG_THUMBNAIL, recipe.getThumbnailUrl());
                    NavHostFragment.findNavController(this).navigate(R.id.recipeDetailFragment, args);
                },
                recipe -> {
                    MutableLiveData<UiState<Void>> saveState = new MutableLiveData<>();
                    saveState.observe(getViewLifecycleOwner(), state -> {
                        if (state == null) return;
                        if (state.getStatus() == UiState.Status.SUCCESS) {
                            Toast.makeText(requireContext(), "Saved to favorites", Toast.LENGTH_SHORT).show();
                            adapter.setRecipeSaved(recipe.getId(), true);
                        } else if (state.getStatus() == UiState.Status.ERROR) {
                            Toast.makeText(requireContext(), state.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    UserRepository.getInstance().saveRecipe(recipe.getId(), saveState);
                },
                (recipe, currentlySaved) -> {
                    MutableLiveData<UiState<Void>> actionState = new MutableLiveData<>();
                    actionState.observe(getViewLifecycleOwner(), state -> {
                        if (state == null) return;
                        if (state.getStatus() == UiState.Status.SUCCESS) {
                            boolean nowSaved = !currentlySaved;
                            adapter.setRecipeSaved(recipe.getId(), nowSaved);
                            Toast.makeText(requireContext(),
                                    nowSaved ? "Saved" : "Removed",
                                    Toast.LENGTH_SHORT).show();
                        } else if (state.getStatus() == UiState.Status.ERROR) {
                            Toast.makeText(requireContext(), state.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                    if (currentlySaved) {
                        userRepository.unsaveRecipe(recipe.getId(), actionState);
                    } else {
                        userRepository.saveRecipe(recipe.getId(), actionState);
                    }
                }
        );

        binding.recipesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recipesRecyclerView.setAdapter(adapter);

        viewModel.getRecipesState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }

            if (state.getStatus() == UiState.Status.LOADING) {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.errorText.setVisibility(View.GONE);
                binding.emptyStateContainer.setVisibility(View.GONE);
                binding.recipesRecyclerView.setVisibility(View.GONE);
                return;
            }

            if (state.getStatus() == UiState.Status.ERROR) {
                binding.progressBar.setVisibility(View.GONE);
                binding.errorText.setVisibility(View.VISIBLE);
                binding.errorText.setText(state.getMessage());
                binding.emptyStateContainer.setVisibility(View.GONE);
                binding.recipesRecyclerView.setVisibility(View.GONE);
                return;
            }

            if (state.getStatus() == UiState.Status.SUCCESS) {
                binding.progressBar.setVisibility(View.GONE);
                binding.errorText.setVisibility(View.GONE);
                if (state.getData() == null || state.getData().isEmpty()) {
                    binding.emptyStateContainer.setVisibility(View.VISIBLE);
                    binding.recipesRecyclerView.setVisibility(View.GONE);
                } else {
                    binding.emptyStateContainer.setVisibility(View.GONE);
                    binding.recipesRecyclerView.setVisibility(View.VISIBLE);
                    adapter.submitList(state.getData());
                }
            }

            if (state.getStatus() == UiState.Status.EMPTY) {
                binding.progressBar.setVisibility(View.GONE);
                binding.errorText.setVisibility(View.GONE);
                binding.emptyStateContainer.setVisibility(View.VISIBLE);
                binding.recipesRecyclerView.setVisibility(View.GONE);
            }
        });

        savedIdsState.observe(getViewLifecycleOwner(), state -> {
            if (state != null && state.getStatus() == UiState.Status.SUCCESS) {
                adapter.setSavedIds(state.getData());
            }
        });
        userRepository.loadSavedRecipeIds(savedIdsState);

        viewModel.loadDefault();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        userRepository.loadSavedRecipeIds(savedIdsState);
    }
}
