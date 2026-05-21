package ntu.quy65132908.smartgym_ai.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;

import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.databinding.ItemWorkoutDayBinding;

public class WeeklyPlanAdapter extends ListAdapter<Workout, WeeklyPlanAdapter.ViewHolder> {

    private static final String[] DAY_NAMES = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"};

    public WeeklyPlanAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Workout> DIFF_CALLBACK = new DiffUtil.ItemCallback<Workout>() {
        @Override
        public boolean areItemsTheSame(@NonNull Workout a, @NonNull Workout b) {
            return a.getId() != null && a.getId().equals(b.getId());
        }
        @Override
        public boolean areContentsTheSame(@NonNull Workout a, @NonNull Workout b) {
            return a.getTitle().equals(b.getTitle()) && a.isCompleted() == b.isCompleted();
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
        String dayName = (dayIndex >= 0 && dayIndex < DAY_NAMES.length) ? DAY_NAMES[dayIndex] : "Ngày " + workout.getDayOfWeek();

        holder.binding.tvWorkoutDay.setText(dayName);
        holder.binding.tvWorkoutName.setText(workout.getTitle());

        int todayDow = getTodayDayOfWeek();
        holder.binding.tvBadge.setVisibility(workout.getDayOfWeek() == todayDow ? View.VISIBLE : View.GONE);
    }

    private int getTodayDayOfWeek() {
        int calDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return calDay == Calendar.SUNDAY ? 7 : calDay - 1;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemWorkoutDayBinding binding;
        ViewHolder(ItemWorkoutDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
