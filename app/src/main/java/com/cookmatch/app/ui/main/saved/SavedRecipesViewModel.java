package com.cookmatch.app.ui.main.saved;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.MutableLiveData;

import com.cookmatch.app.data.repository.RecipeRepository;
import com.cookmatch.app.data.repository.UserRepository;
import com.cookmatch.app.domain.model.Recipe;
import com.cookmatch.app.presentation.state.UiState;

import java.util.List;

public class SavedRecipesViewModel extends AndroidViewModel {

    private final MutableLiveData<UiState<List<String>>> savedIdsState = new MutableLiveData<>();
    private final MutableLiveData<UiState<List<Recipe>>> savedRecipesState = new MutableLiveData<>();
    private final MutableLiveData<UiState<Void>> actionState = new MutableLiveData<>();
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final Observer<UiState<List<String>>> idsObserver;

    public SavedRecipesViewModel(@NonNull Application application) {
        super(application);
        userRepository = UserRepository.getInstance();
        recipeRepository = RecipeRepository.getInstance(application);
        idsObserver = state -> {
            if (state == null) return;
            switch (state.getStatus()) {
                case LOADING:
                    savedRecipesState.postValue(UiState.loading());
                    break;
                case SUCCESS:
                    recipeRepository.getRecipesByIds(state.getData(), savedRecipesState);
                    break;
                case EMPTY:
                    savedRecipesState.postValue(UiState.empty());
                    break;
                case ERROR:
                    savedRecipesState.postValue(UiState.error(state.getMessage()));
                    break;
            }
        };
        savedIdsState.observeForever(idsObserver);
    }

    public LiveData<UiState<List<Recipe>>> getSavedRecipesState() { return savedRecipesState; }
    public LiveData<UiState<Void>> getActionState() { return actionState; }

    public void loadSavedRecipes() {
        userRepository.loadSavedRecipeIds(savedIdsState);
    }

    public void unsaveRecipe(String recipeId) {
        userRepository.unsaveRecipe(recipeId, actionState);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        savedIdsState.removeObserver(idsObserver);
    }
}
