package ntu.quy65132908.smartgym_ai.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import ntu.quy65132908.smartgym_ai.databinding.ItemWorkoutDayBinding;

public class WeeklyPlanAdapter extends RecyclerView.Adapter<WeeklyPlanAdapter.ViewHolder> {

    private final String[][] weekData = {
            {"Thứ 2", "Push Day — Ngực, Vai, Tay sau"},
            {"Thứ 3", "Pull Day — Lưng, Bắp tay"},
            {"Thứ 4", "Leg Day — Đùi, Mông, Bắp chân"},
            {"Thứ 5", "Nghỉ ngơi & Cardio nhẹ"},
            {"Thứ 6", "Upper Body Blast"},
            {"Thứ 7", "Core & HIIT"},
            {"Chủ nhật", "Nghỉ ngơi hoàn toàn"}
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
        holder.binding.tvWorkoutDay.setText(weekData[position][0]);
        holder.binding.tvWorkoutName.setText(weekData[position][1]);

        // Show "HÔM NAY" badge for current day (simplified: always show for position 0)
        if (position == 0) {
            holder.binding.tvBadge.setVisibility(View.VISIBLE);
        } else {
            holder.binding.tvBadge.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return weekData.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemWorkoutDayBinding binding;

        ViewHolder(ItemWorkoutDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
