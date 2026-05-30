package com.cookmatch.app.ui.main.saved;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cookmatch.app.R;

import java.util.ArrayList;
import java.util.List;

public class SavedIdsAdapter extends RecyclerView.Adapter<SavedIdsAdapter.ViewHolder> {

    public interface OnUnsaveClick {
        void onUnsave(String recipeId);
    }

    private final List<String> items = new ArrayList<>();
    private final OnUnsaveClick listener;

    public SavedIdsAdapter(OnUnsaveClick listener) {
        this.listener = listener;
    }

    public void submitList(List<String> ids) {
        items.clear();
        if (ids != null) items.addAll(ids);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String id = items.get(position);
        holder.text.setText("Recipe #" + id);
        holder.itemView.setOnLongClickListener(v -> {
            listener.onUnsave(id);
            return true;
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(android.R.id.text1);
        }
    }
}
