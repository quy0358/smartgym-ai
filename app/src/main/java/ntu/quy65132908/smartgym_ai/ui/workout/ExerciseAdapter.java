package ntu.quy65132908.smartgym_ai.ui.workout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Exercise;

public class ExerciseAdapter extends ListAdapter<Exercise, ExerciseAdapter.ViewHolder> {

    public interface OnExerciseCheckedListener {
        void onChecked(Exercise exercise, boolean isChecked);
    }

    private final OnExerciseCheckedListener listener;

    public ExerciseAdapter(OnExerciseCheckedListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Exercise> DIFF_CALLBACK = new DiffUtil.ItemCallback<Exercise>() {
        @Override
        public boolean areItemsTheSame(@NonNull Exercise a, @NonNull Exercise b) {
            return a.getId() != null && a.getId().equals(b.getId());
        }
        @Override
        public boolean areContentsTheSame(@NonNull Exercise a, @NonNull Exercise b) {
            return a.isCompleted() == b.isCompleted() && a.getName().equals(b.getName());
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exercise ex = getItem(position);
        holder.tvName.setText(ex.getName());
        String detail = ex.getSets() + " sets × " + ex.getReps() + " reps";
        if (ex.getWeight() != null) detail += " • " + ex.getWeight().intValue() + "kg";
        holder.tvDetail.setText(detail);
        holder.cbDone.setOnCheckedChangeListener(null);
        holder.cbDone.setChecked(ex.isCompleted());
        holder.cbDone.setOnCheckedChangeListener((btn, checked) -> {
            if (listener != null) listener.onChecked(ex, checked);
        });
    }

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
