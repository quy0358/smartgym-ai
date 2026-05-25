package ntu.quy65132908.smartgym_ai.ui.workout;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void setSelectedIds(Set<String> ids) {
        selectedIds.clear();
        if (ids != null) {
            selectedIds.addAll(ids);
        }
        notifyDataSetChanged();
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
        holder.binding.tvExerciseMeta.setText(item.getPrimaryMuscle() + " • " + item.getEquipment() + " • " + item.getDifficulty());
        holder.binding.tvExercisePrescription.setText(holder.itemView.getContext().getString(
                R.string.exercise_prescription_format,
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
}
