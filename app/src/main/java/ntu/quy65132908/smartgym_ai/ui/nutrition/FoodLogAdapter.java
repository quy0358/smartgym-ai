package ntu.quy65132908.smartgym_ai.ui.nutrition;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ntu.quy65132908.smartgym_ai.data.model.FoodLogEntry;
import ntu.quy65132908.smartgym_ai.databinding.ItemFoodLogBinding;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.ui.media.UiImageResolver;

public class FoodLogAdapter extends RecyclerView.Adapter<FoodLogAdapter.ViewHolder> {
    private final List<FoodLogEntry> items = new ArrayList<>();

    public void submitList(List<FoodLogEntry> logs) {
        items.clear();
        if (logs != null) {
            items.addAll(logs);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFoodLogBinding binding = ItemFoodLogBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodLogEntry entry = items.get(position);
        holder.binding.ivFoodImage.setImageResource(UiImageResolver.mealImageFor(
                entry.getMealType(),
                entry.getName()));
        holder.binding.tvFoodName.setText(entry.getName());
        holder.binding.tvFoodMeal.setText(entry.getMealType());
        holder.binding.tvFoodCalories.setText(holder.itemView.getContext().getString(
                R.string.nutrition_food_calories_format,
                entry.getCalories()));
        holder.binding.tvFoodMacros.setText(holder.itemView.getContext().getString(
                R.string.nutrition_macros_format,
                entry.getProteinGrams(),
                entry.getCarbsGrams(),
                entry.getFatGrams()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemFoodLogBinding binding;

        ViewHolder(ItemFoodLogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
