package com.cookmatch.app.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {RecipeEntity.class}, version = 2, exportSchema = false)
public abstract class RecipeDatabase extends RoomDatabase {
    public abstract RecipeDao recipeDao();
}
