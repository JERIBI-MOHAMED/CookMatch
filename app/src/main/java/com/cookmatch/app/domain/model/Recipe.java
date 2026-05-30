package com.cookmatch.app.domain.model;

import java.util.ArrayList;
import java.util.List;

public class Recipe {

    private final String id;
    private final String title;
    private final String category;
    private final String thumbnailUrl;
    private final String instructions;
    private final List<String> ingredients;

    public Recipe(String id, String title, String category, String thumbnailUrl) {
        this(id, title, category, thumbnailUrl, "", new ArrayList<>());
    }

    public Recipe(String id, String title, String category, String thumbnailUrl,
                  String instructions, List<String> ingredients) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.thumbnailUrl = thumbnailUrl;
        this.instructions = instructions;
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getInstructions() { return instructions; }
    public List<String> getIngredients() { return ingredients; }
}
