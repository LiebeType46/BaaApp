package com.example.baapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baapp.R;
import com.example.baapp.common.CategoryLabelResolver;
import com.example.baapp.common.MainCategoryConverter;
import com.example.baapp.data.LocationEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.TimelineViewHolder> {

    private final List<LocationEntity> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public TimelineAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(LocationEntity item);
    }

    public void setItems(List<LocationEntity> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimelineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timeline_post, parent, false);
        return new TimelineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineViewHolder holder, int position) {
        LocationEntity item = items.get(position);
        String categoryLabel = CategoryLabelResolver.getLabel(
                holder.itemView.getContext(),
                MainCategoryConverter.toCategory(item.getCategory())
        );
        String subCategory = item.getSubCategory() != null ? item.getSubCategory() : "";

        holder.tvCategorySubCategory.setText(
                categoryLabel + " / " + subCategory
        );

        holder.tvLocationTimestamp.setText(String.format(
                Locale.getDefault(),
                "%.6f, %.6f / %s",
                item.getLatitude(),
                item.getLongitude(),
                item.getTimestamp()
        ));

        holder.tvMemo.setText(item.getMemo() != null ? item.getMemo() : "");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TimelineViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategorySubCategory;
        TextView tvLocationTimestamp;
        TextView tvMemo;

        public TimelineViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategorySubCategory = itemView.findViewById(R.id.tvCategorySubCategory);
            tvLocationTimestamp = itemView.findViewById(R.id.tvLocationTimestamp);
            tvMemo = itemView.findViewById(R.id.tvMemo);
        }
    }
}
