package com.cookmatch.app.ui.main.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cookmatch.app.R;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.ViewHolder> {

    private final List<Map<String, Object>> items = new ArrayList<>();

    public void submitList(List<Map<String, Object>> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_post, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> item = items.get(position);
        holder.title.setText(String.valueOf(item.getOrDefault("title", "Untitled")));

        String author = String.valueOf(item.getOrDefault("sharedByName", "CookMatcher"));
        holder.author.setText("by " + author);

        Object likes = item.get("likes");
        holder.likes.setText("❤ " + (likes != null ? likes.toString() : "0"));

        String timeText = "Just now";
        Object sharedAt = item.get("sharedAt");
        if (sharedAt instanceof Timestamp) {
            Date date = ((Timestamp) sharedAt).toDate();
            timeText = DateUtils.getRelativeTimeSpanString(
                    date.getTime(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
            ).toString();
        }
        holder.time.setText(timeText);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, author, likes, time;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.communityTitle);
            author = itemView.findViewById(R.id.communityAuthor);
            likes = itemView.findViewById(R.id.communityLikes);
            time = itemView.findViewById(R.id.communityTime);
        }
    }
}
