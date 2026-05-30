package com.cookmatch.app.ui.main.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cookmatch.app.data.repository.RecipeRepository;
import com.cookmatch.app.domain.model.Recipe;
import com.cookmatch.app.presentation.state.UiState;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final MutableLiveData<UiState<List<Recipe>>> recipesState = new MutableLiveData<>();
    private final RecipeRepository repository;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = RecipeRepository.getInstance(application);
    }

    public LiveData<UiState<List<Recipe>>> getRecipesState() {
        return recipesState;
    }

    public void loadRecipes(String query) {
        repository.searchRecipes(query, recipesState);
    }

    /** Load trending results on first open. */
    public void loadDefault() {
        repository.loadHomeRecipes(recipesState);
    }
}
