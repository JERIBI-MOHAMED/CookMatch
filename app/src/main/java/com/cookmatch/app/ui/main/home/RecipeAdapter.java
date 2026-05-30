package com.cookmatch.app.ui.main.home;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cookmatch.app.databinding.ItemRecipeBinding;
import com.cookmatch.app.domain.model.Recipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
    }

    public interface OnRecipeLongClickListener {
        void onRecipeLongClick(Recipe recipe);
    }

    public interface OnRecipeSaveToggleListener {
        void onToggleSave(Recipe recipe, boolean currentlySaved);
    }

    private final List<Recipe> items = new ArrayList<>();
    private final Set<String> savedIds = new HashSet<>();
    private final OnRecipeClickListener listener;
    private final OnRecipeLongClickListener longClickListener;
    private final OnRecipeSaveToggleListener saveToggleListener;

    public RecipeAdapter(OnRecipeClickListener listener,
                         OnRecipeLongClickListener longClickListener,
                         OnRecipeSaveToggleListener saveToggleListener) {
        this.listener = listener;
        this.longClickListener = longClickListener;
        this.saveToggleListener = saveToggleListener;
    }

    public void setSavedIds(List<String> ids) {
        savedIds.clear();
        if (ids != null) {
            savedIds.addAll(ids);
        }
        notifyDataSetChanged();
    }

    public void setRecipeSaved(String id, boolean saved) {
        if (saved) {
            savedIds.add(id);
        } else {
            savedIds.remove(id);
        }
        notifyDataSetChanged();
    }

    public void submitList(List<Recipe> recipes) {
        items.clear();
        if (recipes != null) {
            items.addAll(recipes);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRecipeBinding binding = ItemRecipeBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new RecipeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = items.get(position);
        holder.binding.recipeTitle.setText(recipe.getTitle());
        String category = recipe.getCategory() == null || recipe.getCategory().isEmpty()
            ? "Category: Unknown"
            : "Category: " + recipe.getCategory();
        holder.binding.recipeMeta.setText(category);
        boolean isSaved = savedIds.contains(recipe.getId());
        holder.binding.saveBadge.setImageResource(
            isSaved ? com.cookmatch.app.R.drawable.ic_bookmark_filled
                : com.cookmatch.app.R.drawable.ic_bookmark_outline
        );
        holder.binding.saveBadge.setColorFilter(isSaved
            ? android.graphics.Color.parseColor("#FFD166")
            : android.graphics.Color.WHITE);

        com.bumptech.glide.Glide.with(holder.binding.recipeImage)
                .load(recipe.getThumbnailUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .centerCrop()
                .into(holder.binding.recipeImage);

        holder.binding.recipeCard.setOnClickListener(v -> listener.onRecipeClick(recipe));
        holder.binding.recipeCard.setOnLongClickListener(v -> {
            longClickListener.onRecipeLongClick(recipe);
            return true;
        });
        holder.binding.saveBadge.setOnClickListener(v ->
            saveToggleListener.onToggleSave(recipe, savedIds.contains(recipe.getId())));

        holder.binding.recipeCard.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate()
                        .scaleX(0.965f)
                        .scaleY(0.965f)
                        .setInterpolator(new DecelerateInterpolator())
                        .setDuration(90)
                        .start();
            } else if (event.getAction() == MotionEvent.ACTION_CANCEL
                || event.getAction() == MotionEvent.ACTION_UP) {
                v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setInterpolator(new DecelerateInterpolator())
                        .setDuration(150)
                        .start();
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        private final ItemRecipeBinding binding;

        RecipeViewHolder(ItemRecipeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
