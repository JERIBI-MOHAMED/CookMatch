package com.cookmatch.app.ui.main.myrecipes;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cookmatch.app.presentation.state.UiState;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyRecipesViewModel extends AndroidViewModel {

    private final MutableLiveData<UiState<List<Map<String, Object>>>> recipesState = new MutableLiveData<>();
    private final MutableLiveData<UiState<Void>> addState = new MutableLiveData<>();

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public MyRecipesViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<UiState<List<Map<String, Object>>>> getRecipesState() { return recipesState; }
    public LiveData<UiState<Void>> getAddState() { return addState; }

    public void loadMyRecipes() {
        String uid = currentUid();
        if (uid == null) { recipesState.setValue(UiState.error("Not authenticated")); return; }

        recipesState.setValue(UiState.loading());
        db.collection("user_recipes")
                .whereEqualTo("createdBy", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        list.add(doc.getData());
                    }
                    if (list.isEmpty()) {
                        recipesState.postValue(UiState.empty());
                    } else {
                        recipesState.postValue(UiState.success(list));
                    }
                })
                .addOnFailureListener(e -> recipesState.postValue(UiState.error(e.getMessage())));
    }

    public void addRecipe(String title, String ingredients, String instructions) {
        String uid = currentUid();
        if (uid == null) { addState.setValue(UiState.error("Not authenticated")); return; }

        addState.setValue(UiState.loading());

        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("ingredients", Arrays.asList(ingredients.split(",")));
        data.put("instructions", instructions);
        data.put("createdBy", uid);
        data.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("user_recipes")
                .add(data)
                .addOnSuccessListener(ref -> {
                    addState.postValue(UiState.success(null));
                    loadMyRecipes();
                })
                .addOnFailureListener(e -> addState.postValue(UiState.error(e.getMessage())));
    }

    private String currentUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
}
