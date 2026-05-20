package ntu.quy65132908.smartgym_ai.ui.workout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import ntu.quy65132908.smartgym_ai.R;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ViewHolder> {

    private final String[][] exercises = {
            {"Bench Press", "4 sets × 10 reps • 60kg"},
            {"Incline Dumbbell Press", "3 sets × 12 reps • 20kg"},
            {"Overhead Press", "4 sets × 8 reps • 40kg"},
            {"Lateral Raises", "3 sets × 15 reps • 10kg"},
            {"Tricep Dips", "3 sets × 12 reps"},
            {"Cable Flyes", "3 sets × 15 reps • 15kg"}
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exercise, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvName.setText(exercises[position][0]);
        holder.tvDetail.setText(exercises[position][1]);
    }

    @Override
    public int getItemCount() { return exercises.length; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvDetail;
        final CheckBox cbDone;

        ViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.tv_exercise_name);
            tvDetail = view.findViewById(R.id.tv_exercise_detail);
            cbDone = view.findViewById(R.id.cb_done);
        }
    }
}
