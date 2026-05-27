package ntu.quy65132908.smartgym_ai.ui.workout;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import ntu.quy65132908.smartgym_ai.data.model.ExerciseCatalogItem;
import ntu.quy65132908.smartgym_ai.databinding.ItemExerciseCatalogBinding;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.ui.media.UiImageResolver;

public class ExerciseCatalogAdapter extends RecyclerView.Adapter<ExerciseCatalogAdapter.ViewHolder> {
    public interface Listener {
        void onToggle(ExerciseCatalogItem item);
    }

    private final Listener listener;
    private final List<ExerciseCatalogItem> items = new ArrayList<>();
    private final Set<String> selectedIds = new HashSet<>();

    public ExerciseCatalogAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<ExerciseCatalogItem> newItems) {
        List<ExerciseCatalogItem> oldItems = new ArrayList<>(items);
        List<ExerciseCatalogItem> updatedItems = newItems != null ? new ArrayList<>(newItems) : new ArrayList<>();
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ExerciseDiffCallback(oldItems, updatedItems));
        items.clear();
        items.addAll(updatedItems);
        diffResult.dispatchUpdatesTo(this);
    }

    public void setSelectedIds(Set<String> ids) {
        Set<String> oldIds = new HashSet<>(selectedIds);
        selectedIds.clear();
        if (ids != null) {
            selectedIds.addAll(ids);
        }
        for (int i = 0; i < items.size(); i++) {
            String id = items.get(i).getId();
            if (oldIds.contains(id) != selectedIds.contains(id)) {
                notifyItemChanged(i);
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemExerciseCatalogBinding binding = ItemExerciseCatalogBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExerciseCatalogItem item = items.get(position);
        holder.binding.ivExerciseImage.setImageResource(UiImageResolver.exerciseImageFor(
                item.getId(),
                item.getName(),
                item.getPrimaryMuscle()));
        holder.binding.tvExerciseName.setText(item.getName());
        holder.binding.tvExerciseMeta.setText(holder.itemView.getContext().getString(
                R.string.exercise_meta_format,
                item.getPrimaryMuscle(),
                item.getEquipment(),
                item.getDifficulty()));
        boolean timedExercise = "plank".equalsIgnoreCase(item.getPoseTypeKey());
        holder.binding.tvExercisePrescription.setText(holder.itemView.getContext().getString(
                timedExercise
                        ? R.string.exercise_timed_prescription_format
                        : R.string.exercise_prescription_format,
                item.getDefaultSets(),
                item.getDefaultReps()));
        holder.binding.tvExerciseSafety.setText(item.getSafetyNote());
        holder.binding.btnToggleExercise.setText(selectedIds.contains(item.getId())
                ? holder.itemView.getContext().getString(R.string.exercise_chosen)
                : holder.itemView.getContext().getString(R.string.exercise_choose));
        holder.binding.getRoot().setOnClickListener(v -> listener.onToggle(item));
        holder.binding.btnToggleExercise.setOnClickListener(v -> listener.onToggle(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemExerciseCatalogBinding binding;

        ViewHolder(ItemExerciseCatalogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static class ExerciseDiffCallback extends DiffUtil.Callback {
        private final List<ExerciseCatalogItem> oldItems;
        private final List<ExerciseCatalogItem> newItems;

        ExerciseDiffCallback(List<ExerciseCatalogItem> oldItems, List<ExerciseCatalogItem> newItems) {
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
            ExerciseCatalogItem oldItem = oldItems.get(oldItemPosition);
            ExerciseCatalogItem newItem = newItems.get(newItemPosition);
            if (oldItem.getId() != null && newItem.getId() != null) {
                return oldItem.getId().equals(newItem.getId());
            }
            return Objects.equals(oldItem.getName(), newItem.getName())
                    && Objects.equals(oldItem.getPrimaryMuscle(), newItem.getPrimaryMuscle())
                    && Objects.equals(oldItem.getEquipment(), newItem.getEquipment());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            ExerciseCatalogItem oldItem = oldItems.get(oldItemPosition);
            ExerciseCatalogItem newItem = newItems.get(newItemPosition);
            return Objects.equals(oldItem.getName(), newItem.getName())
                    && Objects.equals(oldItem.getPrimaryMuscle(), newItem.getPrimaryMuscle())
                    && Objects.equals(oldItem.getEquipment(), newItem.getEquipment())
                    && Objects.equals(oldItem.getDifficulty(), newItem.getDifficulty())
                    && oldItem.getDefaultSets() == newItem.getDefaultSets()
                    && oldItem.getDefaultReps() == newItem.getDefaultReps()
                    && oldItem.getRestSeconds() == newItem.getRestSeconds()
                    && Objects.equals(oldItem.getPoseTypeKey(), newItem.getPoseTypeKey())
                    && Objects.equals(oldItem.getSafetyNote(), newItem.getSafetyNote());
        }
    }
}
