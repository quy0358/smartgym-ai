package ntu.quy65132908.smartgym_ai.ui.nutrition;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Meal;
import ntu.quy65132908.smartgym_ai.data.model.MealPlanDay;
import ntu.quy65132908.smartgym_ai.databinding.ItemMealPlanDayBinding;

public class MealPlanDayAdapter extends RecyclerView.Adapter<MealPlanDayAdapter.ViewHolder> {
    private final List<MealPlanDay> items = new ArrayList<>();
    private final OnMealPlanDayActionListener listener;

    public MealPlanDayAdapter(OnMealPlanDayActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<MealPlanDay> days) {
        List<MealPlanDay> oldItems = new ArrayList<>(items);
        List<MealPlanDay> newItems = days != null ? new ArrayList<>(days) : new ArrayList<>();
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DayDiffCallback(oldItems, newItems));
        items.clear();
        items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMealPlanDayBinding binding = ItemMealPlanDayBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MealPlanDay day = items.get(position);
        holder.binding.tvDayTitle.setText(nonBlank(day.getDayLabel(), "Ngày " + day.getDayOfWeek()));
        holder.binding.tvDayTarget.setText(day.getTargetCalories() > 0
                ? holder.itemView.getContext().getString(R.string.nutrition_plan_day_target_format, day.getTargetCalories())
                : holder.itemView.getContext().getString(R.string.nutrition_plan_day_target_unknown));
        holder.binding.mealContainer.removeAllViews();
        List<Meal> meals = day.getMeals();
        if (meals != null) {
            for (Meal meal : meals) {
                TextView mealView = new TextView(holder.itemView.getContext());
                mealView.setText(holder.itemView.getContext().getString(
                        R.string.nutrition_plan_meal_line_format,
                        nonBlank(meal.getMealType(), holder.itemView.getContext().getString(R.string.nutrition_fallback_meal_type)),
                        nonBlank(meal.getName(), holder.itemView.getContext().getString(R.string.nutrition_plan_food_fallback)),
                        meal.getCalories(),
                        meal.getProteinGrams(),
                        meal.getCarbsGrams(),
                        meal.getFatGrams()
                ));
                mealView.setTextAppearance(R.style.TextStyle_BodyBase);
                holder.binding.mealContainer.addView(mealView);
            }
        }
        holder.binding.btnLogDay.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLogDay(day);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemMealPlanDayBinding binding;

        ViewHolder(ItemMealPlanDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static class DayDiffCallback extends DiffUtil.Callback {
        private final List<MealPlanDay> oldItems;
        private final List<MealPlanDay> newItems;

        DayDiffCallback(List<MealPlanDay> oldItems, List<MealPlanDay> newItems) {
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
            MealPlanDay oldItem = oldItems.get(oldItemPosition);
            MealPlanDay newItem = newItems.get(newItemPosition);
            return oldItem.getDayOfWeek() == newItem.getDayOfWeek()
                    && Objects.equals(oldItem.getDayLabel(), newItem.getDayLabel());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            MealPlanDay oldItem = oldItems.get(oldItemPosition);
            MealPlanDay newItem = newItems.get(newItemPosition);
            return oldItem.getTargetCalories() == newItem.getTargetCalories()
                    && Objects.equals(oldItem.getDayLabel(), newItem.getDayLabel())
                    && mealSignature(oldItem).equals(mealSignature(newItem));
        }
    }

    private static String mealSignature(MealPlanDay day) {
        StringBuilder builder = new StringBuilder();
        if (day != null && day.getMeals() != null) {
            for (Meal meal : day.getMeals()) {
                if (meal != null) {
                    builder.append(meal.getMealType()).append('|')
                            .append(meal.getName()).append('|')
                            .append(meal.getCalories()).append(';');
                }
            }
        }
        return builder.toString();
    }

    private static String nonBlank(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value.trim() : fallback;
    }

    public interface OnMealPlanDayActionListener {
        void onLogDay(MealPlanDay day);
    }
}
