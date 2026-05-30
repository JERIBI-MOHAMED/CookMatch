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
                    cacheRecipes(recipes);
                    liveData.postValue(UiState.success(recipes));
                } else {
                    serveCachedRecipes(liveData, "No results found");
                }
            }

            @Override
            public void onFailure(Call<MealResponse> call, Throwable t) {
                serveCachedRecipes(liveData, "Network error: " + t.getMessage());
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
