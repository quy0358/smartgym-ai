package ntu.quy65132908.smartgym_ai.ui.dashboard;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.databinding.ItemWorkoutDayBinding;
import ntu.quy65132908.smartgym_ai.ui.media.UiImageResolver;
import ntu.quy65132908.smartgym_ai.util.DateUtils;

public class WeeklyPlanAdapter extends ListAdapter<Workout, WeeklyPlanAdapter.ViewHolder> {

    private final OnWorkoutClickListener listener;

    public WeeklyPlanAdapter() {
        this(null);
    }

    public WeeklyPlanAdapter(OnWorkoutClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public interface OnWorkoutClickListener {
        void onWorkoutClick(Workout workout);
    }

    private static final DiffUtil.ItemCallback<Workout> DIFF_CALLBACK = new DiffUtil.ItemCallback<Workout>() {
        @Override
        public boolean areItemsTheSame(@NonNull Workout a, @NonNull Workout b) {
            if (a.getId() != null && b.getId() != null) {
                return a.getId().equals(b.getId());
            }
            return a.getDayOfWeek() == b.getDayOfWeek()
                    && Objects.equals(a.getTitle(), b.getTitle());
        }
        @Override
        public boolean areContentsTheSame(@NonNull Workout a, @NonNull Workout b) {
            return Objects.equals(a.getTitle(), b.getTitle())
                    && Objects.equals(a.getSubtitle(), b.getSubtitle())
                    && Objects.equals(a.getIntensity(), b.getIntensity())
                    && a.getDurationMinutes() == b.getDurationMinutes()
                    && a.getDayOfWeek() == b.getDayOfWeek()
                    && a.isCompleted() == b.isCompleted();
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWorkoutDayBinding binding = ItemWorkoutDayBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Workout workout = getItem(position);
        int dayIndex = workout.getDayOfWeek() - 1;
        String[] dayNames = holder.itemView.getResources().getStringArray(R.array.weekly_plan_day_names);
        String dayName = (dayIndex >= 0 && dayIndex < dayNames.length)
                ? dayNames[dayIndex]
                : holder.itemView.getContext().getString(R.string.workout_day_fallback, workout.getDayOfWeek());
        String title = workout.getTitle() != null && !workout.getTitle().trim().isEmpty()
                ? workout.getTitle()
                : holder.itemView.getContext().getString(R.string.workout_untitled);

        holder.binding.tvWorkoutDay.setText(dayName);
        holder.binding.tvWorkoutName.setText(title);
        holder.binding.ivWorkoutImage.setImageResource(UiImageResolver.workoutImageFor(workout));
        String metaText = buildMetaText(holder, workout);
        holder.binding.tvWorkoutMeta.setText(metaText);

        int todayDow = DateUtils.getTodayDayOfWeek();
        holder.binding.tvBadge.setVisibility(workout.getDayOfWeek() == todayDow ? View.VISIBLE : View.GONE);

        String statusText;
        // Hiển thị trạng thái đã hoàn thành.
        if (workout.isCompleted()) {
            holder.binding.ivStatus.setImageResource(R.drawable.ic_check_circle);
            holder.binding.ivStatus.setImageTintList(null); // Dùng màu nền có sẵn của tài nguyên vẽ.
            holder.itemView.setAlpha(0.7f);
            statusText = holder.itemView.getContext().getString(R.string.workout_status_completed);
        } else {
            holder.binding.ivStatus.setImageResource(R.drawable.ic_dumbbell);
            holder.binding.ivStatus.setImageTintList(
                    ColorStateList.valueOf(
                            holder.itemView.getContext().getColor(R.color.on_surface_variant)));
            holder.itemView.setAlpha(1f);
            statusText = holder.itemView.getContext().getString(R.string.workout_status_pending);
        }
        bindDayType(holder, workout);

        boolean hasValidId = workout.getId() != null && !workout.getId().isEmpty();
        holder.itemView.setEnabled(hasValidId);
        if (!hasValidId) {
            holder.itemView.setAlpha(0.55f);
            statusText = holder.itemView.getContext().getString(R.string.workout_status_unavailable);
        }
        holder.itemView.setContentDescription(holder.itemView.getContext().getString(
                R.string.weekly_plan_item_a11y,
                dayName,
                title,
                metaText,
                statusText));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && hasValidId) {
                listener.onWorkoutClick(workout);
            }
        });
    }

    private void bindDayType(@NonNull ViewHolder holder, Workout workout) {
        if (workout.isRestDay()) {
            holder.binding.ivStatus.setImageResource(R.drawable.ic_check_circle);
            holder.binding.ivStatus.setImageTintList(null);
            holder.binding.tvTypeBadge.setVisibility(View.VISIBLE);
            holder.binding.tvTypeBadge.setText(R.string.workout_type_rest);
        } else if (workout.isRecoveryDay()) {
            holder.binding.ivStatus.setImageResource(R.drawable.ic_dumbbell);
            holder.binding.tvTypeBadge.setVisibility(View.VISIBLE);
            holder.binding.tvTypeBadge.setText(R.string.workout_type_recovery);
        } else {
            holder.binding.tvTypeBadge.setVisibility(View.GONE);
        }
    }

    private String buildMetaText(@NonNull ViewHolder holder, Workout workout) {
        if (workout.isRestDay()) {
            return holder.itemView.getContext().getString(R.string.workout_meta_rest);
        }
        String intensity = workout.getIntensity() != null && !workout.getIntensity().trim().isEmpty()
                ? workout.getIntensity().trim()
                : holder.itemView.getContext().getString(R.string.value_unavailable);
        if (workout.getExerciseCount() > 0) {
            return holder.itemView.getContext().getString(
                    R.string.workout_meta_with_count,
                    workout.getDurationMinutes(),
                    intensity,
                    workout.getExerciseCount());
        }
        return holder.itemView.getContext().getString(
                R.string.workout_meta_without_count,
                workout.getDurationMinutes(),
                intensity);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemWorkoutDayBinding binding;
        ViewHolder(ItemWorkoutDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
