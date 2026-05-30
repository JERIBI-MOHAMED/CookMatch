package com.cookmatch.app.data.remote;

import com.cookmatch.app.data.remote.dto.MealResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RecipeApiService {

    @GET("api/json/v1/1/search.php")
    Call<MealResponse> searchMeals(@Query("s") String query);

    @GET("api/json/v1/1/filter.php")
    Call<MealResponse> filterByIngredient(@Query("i") String ingredient);

    @GET("api/json/v1/1/lookup.php")
    Call<MealResponse> getMealById(@Query("i") String id);
}
