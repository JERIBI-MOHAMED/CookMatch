package com.cookmatch.app.ui.main.community;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommunityViewModel extends AndroidViewModel {

    private final MutableLiveData<UiState<List<Map<String, Object>>>> postsState = new MutableLiveData<>();
    private final MutableLiveData<UiState<Void>> shareState = new MutableLiveData<>();

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public CommunityViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<UiState<List<Map<String, Object>>>> getPostsState() { return postsState; }
    public LiveData<UiState<Void>> getShareState() { return shareState; }

    public void loadCommunityRecipes() {
        postsState.setValue(UiState.loading());
        db.collection("community_recipes")
                .orderBy("sharedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Map<String, Object> data = new HashMap<>(doc.getData());
                        data.put("docId", doc.getId());
                        list.add(data);
                    }
                    if (list.isEmpty()) postsState.postValue(UiState.empty());
                    else postsState.postValue(UiState.success(list));
                })
                .addOnFailureListener(e -> postsState.postValue(UiState.error(e.getMessage())));
    }

    public void shareRecipe(String title) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) { shareState.setValue(UiState.error("Not authenticated")); return; }

        shareState.setValue(UiState.loading());

        String shareName = auth.getCurrentUser().getDisplayName();
        if (shareName == null || shareName.trim().isEmpty()) {
            String email = auth.getCurrentUser().getEmail();
            shareName = email != null && email.contains("@")
                    ? email.substring(0, email.indexOf("@"))
                    : "CookMatcher";
        }

        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("sharedBy", uid);
        data.put("sharedByName", shareName);
        data.put("sharedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        data.put("likes", 0);

        db.collection("community_recipes").add(data)
                .addOnSuccessListener(ref -> {
                    shareState.postValue(UiState.success(null));
                    loadCommunityRecipes();
                })
                .addOnFailureListener(e -> shareState.postValue(UiState.error(e.getMessage())));
    }
}
