package com.cookmatch.app.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Room;

import com.cookmatch.app.data.local.RecipeDatabase;
import com.cookmatch.app.data.local.RecipeEntity;
import com.cookmatch.app.data.remote.RecipeApiService;
import com.cookmatch.app.data.remote.RetrofitClient;
import com.cookmatch.app.data.remote.dto.MealDto;
import com.cookmatch.app.data.remote.dto.MealResponse;
import com.cookmatch.app.domain.model.Recipe;
import com.cookmatch.app.presentation.state.UiState;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecipeRepository {

    private static RecipeRepository instance;

    private final RecipeApiService apiService;
    private final RecipeDatabase db;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private RecipeRepository(Context context) {
        apiService = RetrofitClient.getInstance().getApiService();
        db = Room.databaseBuilder(context.getApplicationContext(),
                RecipeDatabase.class, "cookmatch_db")
                .fallbackToDestructiveMigration()
                .build();
    }

    public static RecipeRepository getInstance(Context context) {
        if (instance == null) {
            instance = new RecipeRepository(context);
        }
        return instance;
    }

    /** Fetch from API, cache results, serve from cache on failure. */
    public void searchRecipes(String query, MutableLiveData<UiState<List<Recipe>>> liveData) {
        liveData.setValue(UiState.loading());

        apiService.searchMeals(query).enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(Call<MealResponse> call, Response<MealResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().meals != null) {
                    List<Recipe> recipes = mapDtoList(response.body().meals);
                    appendMatchingUserRecipes(query, recipes, liveData);
                } else {
                    loadOnlyUserRecipes(query, liveData, "No results found");
                }
            }

            @Override
            public void onFailure(Call<MealResponse> call, Throwable t) {
                loadOnlyUserRecipes(query, liveData, "Network error: " + t.getMessage());
            }
        });
    }

    /** Load the home feed: user's own recipes first, then default API recipes. */
    public void loadHomeRecipes(MutableLiveData<UiState<List<Recipe>>> liveData) {
        liveData.setValue(UiState.loading());

        apiService.searchMeals("chicken").enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(Call<MealResponse> call, Response<MealResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().meals != null) {
                    appendAllUserRecipes(mapDtoList(response.body().meals), liveData);
                } else {
                    loadOnlyUserRecipes("", liveData, "No results found");
                }
            }

            @Override
            public void onFailure(Call<MealResponse> call, Throwable t) {
                loadOnlyUserRecipes("", liveData, "Network error: " + t.getMessage());
            }
        });
    }

    /** Resolve a list of recipe IDs into complete recipe cards for Saved screen. */
    public void getRecipesByIds(List<String> ids, MutableLiveData<UiState<List<Recipe>>> liveData) {
        if (ids == null || ids.isEmpty()) {
            liveData.setValue(UiState.empty());
            return;
        }

        liveData.setValue(UiState.loading());

        List<Recipe> fetched = java.util.Collections.synchronizedList(new ArrayList<>());
        AtomicInteger pending = new AtomicInteger(ids.size());

        for (String id : ids) {
            if (isUserRecipeId(id)) {
                firestore.collection("user_recipes")
                        .document(id.substring("user_recipe:".length()))
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists() && doc.getData() != null) {
                                fetched.add(mapUserRecipe(doc.getId(), doc.getData()));
                            }
                            completeIfDone(ids, fetched, liveData, pending);
                        })
                        .addOnFailureListener(e -> completeIfDone(ids, fetched, liveData, pending));
                continue;
            }

            apiService.getMealById(id).enqueue(new Callback<MealResponse>() {
                @Override
                public void onResponse(Call<MealResponse> call, Response<MealResponse> response) {
                    if (response.isSuccessful() && response.body() != null
                            && response.body().meals != null && !response.body().meals.isEmpty()) {
                        MealDto dto = response.body().meals.get(0);
                        fetched.add(new Recipe(dto.id, dto.title, dto.category, dto.thumbnailUrl));
                    }
                    completeIfDone(ids, fetched, liveData, pending);
                }

                @Override
                public void onFailure(Call<MealResponse> call, Throwable t) {
                    completeIfDone(ids, fetched, liveData, pending);
                }
            });
        }
    }

    /** Fetch full details for one recipe ID (ingredients + instructions). */
    public void getRecipeDetail(String id, MutableLiveData<UiState<Recipe>> liveData) {
        if (id == null || id.isEmpty()) {
            liveData.setValue(UiState.error("Invalid recipe id"));
            return;
        }

        if (isUserRecipeId(id)) {
            getUserRecipeDetail(id, liveData);
            return;
        }

        liveData.setValue(UiState.loading());
        apiService.getMealById(id).enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(Call<MealResponse> call, Response<MealResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().meals != null && !response.body().meals.isEmpty()) {
                    Recipe recipe = mapDetailDto(response.body().meals.get(0));
                    liveData.postValue(UiState.success(recipe));
                } else {
                    liveData.postValue(UiState.error("Recipe details not found"));
                }
            }

            @Override
            public void onFailure(Call<MealResponse> call, Throwable t) {
                liveData.postValue(UiState.error("Detail load failed: " + t.getMessage()));
            }
        });
    }

    private void appendMatchingUserRecipes(String query,
                                           List<Recipe> apiRecipes,
                                           MutableLiveData<UiState<List<Recipe>>> liveData) {
        String uid = currentUid();
        if (uid == null) {
            cacheRecipes(apiRecipes);
            liveData.postValue(UiState.success(apiRecipes));
            return;
        }

        firestore.collection("user_recipes")
                .whereEqualTo("createdBy", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Recipe> combined = new ArrayList<>(apiRecipes);
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Recipe recipe = mapUserRecipe(doc.getId(), doc.getData());
                        if (matchesQuery(recipe, query)) {
                            combined.add(0, recipe);
                        }
                    }
                    cacheRecipes(apiRecipes);
                    if (combined.isEmpty()) {
                        liveData.postValue(UiState.empty());
                    } else {
                        liveData.postValue(UiState.success(combined));
                    }
                })
                .addOnFailureListener(e -> {
                    cacheRecipes(apiRecipes);
                    liveData.postValue(UiState.success(apiRecipes));
                });
    }

    private void appendAllUserRecipes(List<Recipe> apiRecipes,
                                      MutableLiveData<UiState<List<Recipe>>> liveData) {
        String uid = currentUid();
        if (uid == null) {
            cacheRecipes(apiRecipes);
            liveData.postValue(UiState.success(apiRecipes));
            return;
        }

        firestore.collection("user_recipes")
                .whereEqualTo("createdBy", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Recipe> combined = new ArrayList<>(apiRecipes);
                    for (QueryDocumentSnapshot doc : snapshot) {
                        combined.add(0, mapUserRecipe(doc.getId(), doc.getData()));
                    }
                    cacheRecipes(apiRecipes);
                    if (combined.isEmpty()) {
                        liveData.postValue(UiState.empty());
                    } else {
                        liveData.postValue(UiState.success(combined));
                    }
                })
                .addOnFailureListener(e -> {
                    cacheRecipes(apiRecipes);
                    liveData.postValue(UiState.success(apiRecipes));
                });
    }

    private void loadOnlyUserRecipes(String query,
                                     MutableLiveData<UiState<List<Recipe>>> liveData,
                                     String fallbackMessage) {
        String uid = currentUid();
        if (uid == null) {
            serveCachedRecipes(liveData, fallbackMessage);
            return;
        }

        firestore.collection("user_recipes")
                .whereEqualTo("createdBy", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Recipe> recipes = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Recipe recipe = mapUserRecipe(doc.getId(), doc.getData());
                        if (matchesQuery(recipe, query)) {
                            recipes.add(recipe);
                        }
                    }
                    if (recipes.isEmpty()) {
                        serveCachedRecipes(liveData, fallbackMessage);
                    } else {
                        liveData.postValue(UiState.success(recipes));
                    }
                })
                .addOnFailureListener(e -> serveCachedRecipes(liveData, fallbackMessage));
    }

    private void getUserRecipeDetail(String id, MutableLiveData<UiState<Recipe>> liveData) {
        String docId = id.substring("user_recipe:".length());
        liveData.setValue(UiState.loading());

        firestore.collection("user_recipes")
                .document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getData() != null) {
                        liveData.postValue(UiState.success(mapUserRecipe(doc.getId(), doc.getData())));
                    } else {
                        liveData.postValue(UiState.error("Recipe details not found"));
                    }
                })
                .addOnFailureListener(e -> liveData.postValue(UiState.error(e.getMessage())));
    }

    private void completeIfDone(List<String> ids,
                                List<Recipe> fetched,
                                MutableLiveData<UiState<List<Recipe>>> liveData,
                                AtomicInteger pending) {
        if (pending.decrementAndGet() != 0) {
            return;
        }

        if (!fetched.isEmpty()) {
            List<Recipe> sorted = sortByIdOrder(ids, fetched);
            cacheRecipes(sorted);
            liveData.postValue(UiState.success(sorted));
            return;
        }

        // Full fallback on local cache when API lookups fail.
        executor.execute(() -> {
            List<RecipeEntity> cached = db.recipeDao().getAll();
            Map<String, RecipeEntity> byId = new HashMap<>();
            for (RecipeEntity e : cached) {
                byId.put(e.id, e);
            }

            List<Recipe> fromCache = new ArrayList<>();
            for (String id : ids) {
                RecipeEntity e = byId.get(id);
                if (e != null) {
                    fromCache.add(new Recipe(e.id, e.title, e.category, e.thumbnailUrl));
                }
            }

            if (!fromCache.isEmpty()) {
                liveData.postValue(UiState.success(fromCache));
            } else {
                liveData.postValue(UiState.error("Could not load saved recipe details"));
            }
        });
    }

    private List<Recipe> sortByIdOrder(List<String> ids, List<Recipe> recipes) {
        Map<String, Recipe> map = new HashMap<>();
        for (Recipe recipe : recipes) {
            map.put(recipe.getId(), recipe);
        }

        List<Recipe> sorted = new ArrayList<>();
        for (String id : ids) {
            Recipe recipe = map.get(id);
            if (recipe != null) {
                sorted.add(recipe);
            }
        }
        return sorted;
    }

    private void serveCachedRecipes(MutableLiveData<UiState<List<Recipe>>> liveData, String reason) {
        executor.execute(() -> {
            List<RecipeEntity> cached = db.recipeDao().getAll();
            if (cached != null && !cached.isEmpty()) {
                liveData.postValue(UiState.success(mapEntityList(cached)));
            } else {
                liveData.postValue(UiState.error(reason));
            }
        });
    }

    private void cacheRecipes(List<Recipe> recipes) {
        executor.execute(() -> {
            List<RecipeEntity> entities = new ArrayList<>();
            for (Recipe r : recipes) {
                entities.add(new RecipeEntity(r.getId(), r.getTitle(),
                        r.getCategory(), r.getThumbnailUrl()));
            }
            db.recipeDao().insertAll(entities);
        });
    }

    private List<Recipe> mapDtoList(List<MealDto> dtos) {
        List<Recipe> list = new ArrayList<>();
        for (MealDto dto : dtos) {
            list.add(new Recipe(dto.id, dto.title, dto.category, dto.thumbnailUrl));
        }
        return list;
    }

    private Recipe mapUserRecipe(String docId, Map<String, Object> data) {
        String title = String.valueOf(data.getOrDefault("title", "Untitled"));
        String instructions = String.valueOf(data.getOrDefault("instructions", ""));
        String thumbnailUrl = String.valueOf(data.getOrDefault("thumbnailUrl", ""));
        if (thumbnailUrl.isEmpty()) {
            thumbnailUrl = String.valueOf(data.getOrDefault("imageUrl", ""));
        }
        List<String> ingredients = new ArrayList<>();
        Object rawIngredients = data.get("ingredients");
        if (rawIngredients instanceof List<?>) {
            for (Object item : (List<?>) rawIngredients) {
                String ingredient = String.valueOf(item).trim();
                if (!ingredient.isEmpty()) {
                    ingredients.add(ingredient);
                }
            }
        }

        return new Recipe(
                "user_recipe:" + docId,
                title,
                "My Recipe",
                thumbnailUrl,
                instructions,
                ingredients
        );
    }

    private boolean matchesQuery(Recipe recipe, String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        return recipe.getTitle().toLowerCase().contains(query.trim().toLowerCase());
    }

    private boolean isUserRecipeId(String id) {
        return id.startsWith("user_recipe:");
    }

    private String currentUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    private Recipe mapDetailDto(MealDto dto) {
        List<String> ingredients = new ArrayList<>();
        addIngredient(ingredients, dto.measure1, dto.ingredient1);
        addIngredient(ingredients, dto.measure2, dto.ingredient2);
        addIngredient(ingredients, dto.measure3, dto.ingredient3);
        addIngredient(ingredients, dto.measure4, dto.ingredient4);
        addIngredient(ingredients, dto.measure5, dto.ingredient5);
        addIngredient(ingredients, dto.measure6, dto.ingredient6);
        addIngredient(ingredients, dto.measure7, dto.ingredient7);
        addIngredient(ingredients, dto.measure8, dto.ingredient8);
        addIngredient(ingredients, dto.measure9, dto.ingredient9);
        addIngredient(ingredients, dto.measure10, dto.ingredient10);
        addIngredient(ingredients, dto.measure11, dto.ingredient11);
        addIngredient(ingredients, dto.measure12, dto.ingredient12);
        addIngredient(ingredients, dto.measure13, dto.ingredient13);
        addIngredient(ingredients, dto.measure14, dto.ingredient14);
        addIngredient(ingredients, dto.measure15, dto.ingredient15);
        addIngredient(ingredients, dto.measure16, dto.ingredient16);
        addIngredient(ingredients, dto.measure17, dto.ingredient17);
        addIngredient(ingredients, dto.measure18, dto.ingredient18);
        addIngredient(ingredients, dto.measure19, dto.ingredient19);
        addIngredient(ingredients, dto.measure20, dto.ingredient20);

        return new Recipe(
                dto.id,
                dto.title,
                dto.category,
                dto.thumbnailUrl,
                dto.instructions != null ? dto.instructions : "",
                ingredients
        );
    }

    private void addIngredient(List<String> target, String measure, String ingredient) {
        if (ingredient == null) return;
        String trimmedIngredient = ingredient.trim();
        if (trimmedIngredient.isEmpty()) return;

        String m = measure != null ? measure.trim() : "";
        target.add((m.isEmpty() ? "" : (m + " ")) + trimmedIngredient);
    }

    private List<Recipe> mapEntityList(List<RecipeEntity> entities) {
        List<Recipe> list = new ArrayList<>();
        for (RecipeEntity e : entities) {
            list.add(new Recipe(e.id, e.title, e.category, e.thumbnailUrl));
        }
        return list;
    }
}
