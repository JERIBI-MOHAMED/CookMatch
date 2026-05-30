package com.cookmatch.app.ui.main.mylist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class IngredientsAdapter extends RecyclerView.Adapter<IngredientsAdapter.ViewHolder> {

    public interface OnRemoveClick {
        void onRemove(String ingredient);
    }

    private final List<String> items = new ArrayList<>();
    private final OnRemoveClick listener;

    public IngredientsAdapter(OnRemoveClick listener) {
        this.listener = listener;
    }

    public void add(String ingredient) {
        if (!items.contains(ingredient)) {
            items.add(ingredient);
            notifyItemInserted(items.size() - 1);
        }
    }

    public void remove(String ingredient) {
        int idx = items.indexOf(ingredient);
        if (idx >= 0) {
            items.remove(idx);
            notifyItemRemoved(idx);
        }
    }

    public List<String> getItems() {
        return new ArrayList<>(items);
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
        String ing = items.get(position);
        holder.text.setText(ing);
        holder.itemView.setOnLongClickListener(v -> {
            listener.onRemove(ing);
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
