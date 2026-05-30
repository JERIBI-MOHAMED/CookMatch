package com.cookmatch.app.ui.main.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cookmatch.app.data.repository.RecipeRepository;
import com.cookmatch.app.data.repository.UserRepository;
import com.cookmatch.app.domain.model.Recipe;
import com.cookmatch.app.presentation.state.UiState;

public class RecipeDetailViewModel extends AndroidViewModel {

    private final MutableLiveData<UiState<Void>> saveState = new MutableLiveData<>();
    private final MutableLiveData<UiState<Recipe>> recipeDetailState = new MutableLiveData<>();
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;

    public RecipeDetailViewModel(@NonNull Application application) {
        super(application);
        userRepository = UserRepository.getInstance();
        recipeRepository = RecipeRepository.getInstance(application);
    }

    public LiveData<UiState<Void>> getSaveState() { return saveState; }
    public LiveData<UiState<Recipe>> getRecipeDetailState() { return recipeDetailState; }

    public void saveRecipe(String recipeId) {
        userRepository.saveRecipe(recipeId, saveState);
    }

    public void unsaveRecipe(String recipeId) {
        userRepository.unsaveRecipe(recipeId, saveState);
    }

    public void loadRecipeDetails(String recipeId) {
        recipeRepository.getRecipeDetail(recipeId, recipeDetailState);
    }
}
