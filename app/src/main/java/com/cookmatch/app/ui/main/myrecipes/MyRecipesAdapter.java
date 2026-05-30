package com.cookmatch.app.ui.main.myrecipes;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cookmatch.app.databinding.ItemRecipeBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MyRecipesAdapter extends RecyclerView.Adapter<MyRecipesAdapter.ViewHolder> {

    private final List<Map<String, Object>> items = new ArrayList<>();

    public void submitList(List<Map<String, Object>> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRecipeBinding binding = ItemRecipeBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> item = items.get(position);
        holder.binding.recipeTitle.setText(String.valueOf(item.getOrDefault("title", "Untitled")));
        Object ings = item.get("ingredients");
        holder.binding.recipeMeta.setText(ings != null ? "Ingredients: " + ings : "Ingredients: none");
        holder.binding.recipeHint.setText("Shown on Home with your recipes");
        holder.binding.saveBadge.setVisibility(android.view.View.GONE);

        String thumbnailUrl = String.valueOf(item.getOrDefault("thumbnailUrl", ""));
        com.bumptech.glide.Glide.with(holder.binding.recipeImage)
                .load(thumbnailUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .centerCrop()
                .into(holder.binding.recipeImage);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemRecipeBinding binding;

        ViewHolder(@NonNull ItemRecipeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
