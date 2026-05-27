package ntu.quy65132908.smartgym_ai.ui.nutrition;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import ntu.quy65132908.smartgym_ai.data.model.FoodLogEntry;
import ntu.quy65132908.smartgym_ai.databinding.ItemFoodLogBinding;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.ui.media.UiImageResolver;

public class FoodLogAdapter extends RecyclerView.Adapter<FoodLogAdapter.ViewHolder> {
    private final List<FoodLogEntry> items = new ArrayList<>();
    private final OnFoodLogActionListener actionListener;

    public FoodLogAdapter() {
        this(null);
    }

    public FoodLogAdapter(OnFoodLogActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void submitList(List<FoodLogEntry> logs) {
        List<FoodLogEntry> oldItems = new ArrayList<>(items);
        List<FoodLogEntry> newItems = logs != null ? new ArrayList<>(logs) : new ArrayList<>();
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new FoodLogDiffCallback(oldItems, newItems));
        items.clear();
        items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
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
        boolean startsGroup = position == 0
                || !Objects.equals(normalize(items.get(position - 1).getMealType()), normalize(entry.getMealType()));
        holder.binding.tvFoodGroup.setVisibility(startsGroup ? android.view.View.VISIBLE : android.view.View.GONE);
        holder.binding.tvFoodGroup.setText(entry.getMealType());
        holder.binding.ivFoodImage.setImageResource(UiImageResolver.mealIconFor(
                entry.getCategory(),
                entry.getMealType()));
        holder.binding.tvFoodName.setText(entry.getName());
        holder.binding.tvFoodMeal.setText(entry.getMealType());
        if (entry.getServingText() != null && !entry.getServingText().trim().isEmpty()) {
            holder.binding.tvFoodServing.setVisibility(android.view.View.VISIBLE);
            holder.binding.tvFoodServing.setText(entry.getServingText().trim());
        } else {
            holder.binding.tvFoodServing.setVisibility(android.view.View.GONE);
        }
        holder.binding.tvFoodCalories.setText(holder.itemView.getContext().getString(
                R.string.nutrition_food_calories_format,
                entry.getCalories()));
        holder.binding.tvFoodMacros.setText(holder.itemView.getContext().getString(
                R.string.nutrition_macros_format,
                entry.getProteinGrams(),
                entry.getCarbsGrams(),
                entry.getFatGrams()));
        holder.binding.tvFoodSource.setText(sourceLabel(holder, entry));
        boolean editable = actionListener != null;
        holder.binding.btnDeleteFood.setVisibility(editable ? android.view.View.VISIBLE : android.view.View.GONE);
        holder.binding.btnDeleteFood.setOnClickListener(v -> {
            if (editable) {
                actionListener.onDeleteFood(entry);
            }
        });
        holder.itemView.setOnClickListener(v -> {
            if (editable) {
                actionListener.onEditFood(entry);
            }
        });
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

    private static class FoodLogDiffCallback extends DiffUtil.Callback {
        private final List<FoodLogEntry> oldItems;
        private final List<FoodLogEntry> newItems;

        FoodLogDiffCallback(List<FoodLogEntry> oldItems, List<FoodLogEntry> newItems) {
            this.oldItems = oldItems;
            this.newItems = newItems;
        }

        @Override
        public int getOldListSize() {
            return oldItems.size();
        }

        @Override
        public int getNewListSize() {
            return newItems.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            FoodLogEntry oldItem = oldItems.get(oldItemPosition);
            FoodLogEntry newItem = newItems.get(newItemPosition);
            if (oldItem.getId() != null && newItem.getId() != null) {
                return oldItem.getId().equals(newItem.getId());
            }
            return oldItem.getEatenAt() == newItem.getEatenAt()
                    && Objects.equals(oldItem.getName(), newItem.getName());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            FoodLogEntry oldItem = oldItems.get(oldItemPosition);
            FoodLogEntry newItem = newItems.get(newItemPosition);
            return Objects.equals(oldItem.getName(), newItem.getName())
                    && Objects.equals(oldItem.getMealType(), newItem.getMealType())
                    && oldItem.getCalories() == newItem.getCalories()
                    && oldItem.getProteinGrams() == newItem.getProteinGrams()
                    && oldItem.getCarbsGrams() == newItem.getCarbsGrams()
                    && oldItem.getFatGrams() == newItem.getFatGrams()
                    && oldItem.getEatenAt() == newItem.getEatenAt()
                    && Objects.equals(oldItem.getServingText(), newItem.getServingText())
                    && Objects.equals(oldItem.getCategory(), newItem.getCategory())
                    && Objects.equals(oldItem.getSource(), newItem.getSource())
                    && Objects.equals(oldItem.getPlanImportKey(), newItem.getPlanImportKey())
                    && Objects.equals(oldItem.getAiConfidence(), newItem.getAiConfidence())
                    && Objects.equals(oldItem.getNotes(), newItem.getNotes());
        }
    }

    private static String sourceLabel(ViewHolder holder, FoodLogEntry entry) {
        String source = entry.getSource();
        if (FoodLogEntry.SOURCE_AI.equals(source)) {
            String label = holder.itemView.getContext().getString(R.string.nutrition_source_ai);
            if (entry.getAiConfidence() != null) {
                return label + " " + Math.round(entry.getAiConfidence() * 100f) + "%";
            }
            return label;
        }
        if (FoodLogEntry.SOURCE_PLAN.equals(source)) {
            return holder.itemView.getContext().getString(R.string.nutrition_source_plan);
        }
        return holder.itemView.getContext().getString(R.string.nutrition_source_manual);
    }

    private static String normalize(String value) {
        return value != null ? value.trim().toLowerCase(Locale.ROOT) : "";
    }

    public interface OnFoodLogActionListener {
        void onEditFood(FoodLogEntry entry);
        void onDeleteFood(FoodLogEntry entry);
    }
}
