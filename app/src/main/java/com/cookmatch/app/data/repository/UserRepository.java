package com.cookmatch.app.data.repository;

import androidx.lifecycle.MutableLiveData;

import com.cookmatch.app.domain.model.User;
import com.cookmatch.app.presentation.state.UiState;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRepository {

    private static UserRepository instance;

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private static final String USERS_COLLECTION = "users";

    private UserRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    /** Create or update user document in Firestore after registration / first login. */
    public void saveUserProfile(String uid, String email,
                                MutableLiveData<UiState<Void>> liveData) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("email", email);
        data.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(USERS_COLLECTION).document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> liveData.postValue(UiState.success(null)))
                .addOnFailureListener(e -> liveData.postValue(UiState.error(e.getMessage())));
    }

    /** Add a recipe ID to the user's saved list. */
    public void saveRecipe(String recipeId, MutableLiveData<UiState<Void>> liveData) {
        String uid = currentUid();
        if (uid == null) { liveData.setValue(UiState.error("Not authenticated")); return; }

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("email", auth.getCurrentUser().getEmail());
        data.put("savedRecipeIds", FieldValue.arrayUnion(recipeId));
        data.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(USERS_COLLECTION).document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> liveData.postValue(UiState.success(null)))
                .addOnFailureListener(e -> liveData.postValue(UiState.error(e.getMessage())));
    }

    /** Remove a recipe ID from the user's saved list. */
    public void unsaveRecipe(String recipeId, MutableLiveData<UiState<Void>> liveData) {
        String uid = currentUid();
        if (uid == null) { liveData.setValue(UiState.error("Not authenticated")); return; }

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("email", auth.getCurrentUser().getEmail());
        data.put("savedRecipeIds", FieldValue.arrayRemove(recipeId));
        data.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(USERS_COLLECTION).document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> liveData.postValue(UiState.success(null)))
                .addOnFailureListener(e -> liveData.postValue(UiState.error(e.getMessage())));
    }

    /** Load saved recipe IDs for the current user. */
    public void loadSavedRecipeIds(MutableLiveData<UiState<List<String>>> liveData) {
        String uid = currentUid();
        if (uid == null) { liveData.setValue(UiState.error("Not authenticated")); return; }

        liveData.setValue(UiState.loading());

        db.collection(USERS_COLLECTION).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        @SuppressWarnings("unchecked")
                        List<String> ids = (List<String>) doc.get("savedRecipeIds");
                        if (ids != null && !ids.isEmpty()) {
                            liveData.postValue(UiState.success(ids));
                        } else {
                            liveData.postValue(UiState.empty());
                        }
                    } else {
                        liveData.postValue(UiState.empty());
                    }
                })
                .addOnFailureListener(e -> liveData.postValue(UiState.error(e.getMessage())));
    }

    private String currentUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
}
