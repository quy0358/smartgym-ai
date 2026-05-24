package ntu.quy65132908.smartgym_ai.ui.workout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.Objects;

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
            return Objects.equals(a.getId(), b.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Exercise a, @NonNull Exercise b) {
            return a.isCompleted() == b.isCompleted()
                    && a.getSets() == b.getSets()
                    && a.getReps() == b.getReps()
                    && Objects.equals(a.getName(), b.getName())
                    && Objects.equals(a.getWeight(), b.getWeight())
                    && Objects.equals(a.getNotes(), b.getNotes());
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
        String detail = formatExerciseDetail(ex);
        holder.tvDetail.setText(detail);

        holder.cbDone.setOnCheckedChangeListener(null);
        holder.cbDone.setChecked(ex.isCompleted());
        holder.itemView.setContentDescription(holder.itemView.getContext().getString(
                R.string.exercise_item_a11y,
                ex.getName(),
                detail,
                ex.isCompleted()
                        ? holder.itemView.getContext().getString(R.string.workout_status_completed)
                        : holder.itemView.getContext().getString(R.string.workout_status_pending)));
        holder.cbDone.setOnCheckedChangeListener((btn, checked) -> {
            if (listener != null) listener.onChecked(ex, checked);
        });
    }

    static String formatExerciseDetail(Exercise ex) {
        String detail;
        if (ex.getReps() == 0) {
            detail = ex.getSets() + " hiệp • Tự do";
        } else {
            detail = ex.getSets() + " hiệp × " + ex.getReps() + " lần";
        }

        if (ex.getWeight() != null && ex.getWeight() > 0) {
            detail += " • " + ex.getWeight().intValue() + "kg";
        }
        return detail;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvDetail;
        final MaterialCheckBox cbDone;

        ViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.tv_exercise_name);
            tvDetail = view.findViewById(R.id.tv_exercise_detail);
            cbDone = view.findViewById(R.id.cb_done);
        }
    }
}
