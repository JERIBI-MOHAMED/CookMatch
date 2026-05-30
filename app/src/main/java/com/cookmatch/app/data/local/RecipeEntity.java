package com.cookmatch.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recipes")
public class RecipeEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public String title;
    public String category;
    public String thumbnailUrl;

    public RecipeEntity(@NonNull String id, String title, String category, String thumbnailUrl) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.thumbnailUrl = thumbnailUrl;
    }
}
