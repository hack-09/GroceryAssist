package com.example.smartgroceryassistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class RecommendationsAdapter extends RecyclerView.Adapter<RecommendationsAdapter.RecommendationViewHolder> {

    private List<MainActivity.Recommendation> recommendations;

    public RecommendationsAdapter(List<MainActivity.Recommendation> recommendations) {
        this.recommendations = recommendations;
    }

    @NonNull
    @Override
    public RecommendationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recommendation, parent, false);
        return new RecommendationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecommendationViewHolder holder, int position) {
        MainActivity.Recommendation recommendation = recommendations.get(position);
        holder.nameTextView.setText(recommendation.getName());
        holder.priceTextView.setText(recommendation.getPriceInfo().getLinePriceDisplay());
        Glide.with(holder.itemView.getContext()).load(recommendation.getImageUrl()).into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return recommendations.size();
    }

    public void updateRecommendations(List<MainActivity.Recommendation> newRecommendations) {
        this.recommendations = newRecommendations;
        notifyDataSetChanged();
    }

    static class RecommendationViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView priceTextView;
        ImageView imageView;

        public RecommendationViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.recommendation_name);
            priceTextView = itemView.findViewById(R.id.recommendation_price);
            imageView = itemView.findViewById(R.id.recommendation_image);
        }
    }
}
