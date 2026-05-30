package com.cookmatch.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MealResponse {

    @SerializedName("meals")
    public List<MealDto> meals;
}
