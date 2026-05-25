package ntu.quy65132908.smartgym_ai.ui.wellness;

import android.content.Context;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ntu.quy65132908.smartgym_ai.data.model.Challenge;
import ntu.quy65132908.smartgym_ai.data.model.ChallengeProgress;
import ntu.quy65132908.smartgym_ai.databinding.ItemChallengeBinding;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.ui.media.UiImageResolver;

public class ChallengeAdapter extends RecyclerView.Adapter<ChallengeAdapter.ViewHolder> {
    public interface Listener {
        void onJoin(Challenge challenge);
    }

    private final Listener listener;
    private final List<ChallengeDisplayItem> items = new ArrayList<>();

    public ChallengeAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<ChallengeDisplayItem> challenges) {
        items.clear();
        if (challenges != null) {
            items.addAll(challenges);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChallengeBinding binding = ItemChallengeBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChallengeDisplayItem item = items.get(position);
        Challenge challenge = item.getChallenge();
        ChallengeProgress progress = item.getProgress();
        Context context = holder.itemView.getContext();
        if (challenge == null) {
            return;
        }
        holder.binding.ivChallengeImage.setImageResource(UiImageResolver.challengeImageFor(challenge));
        holder.binding.tvChallengeTitle.setText(challenge.getTitle());
        holder.binding.tvChallengeDescription.setText(challenge.getDescription());
        holder.binding.tvChallengeMeta.setText(context.getString(
                R.string.wellness_challenge_meta_format,
                challenge.getTargetDays(),
                challenge.getDailyMinutes()));

        if (progress != null) {
            int percent = progress.getProgressPercent();
            holder.binding.progressChallenge.setProgress(percent);
            holder.binding.progressChallenge.setVisibility(View.VISIBLE);
            holder.binding.tvChallengeStatus.setVisibility(View.VISIBLE);
            holder.binding.tvChallengeStatus.setText(formatChallengeStatus(context, progress));
            holder.binding.btnJoinChallenge.setEnabled(false);
            holder.binding.btnJoinChallenge.setText(progress.isCompleted()
                    ? R.string.wellness_challenge_completed_button
                    : R.string.wellness_challenge_joined_button);
        } else {
            holder.binding.progressChallenge.setVisibility(View.GONE);
            holder.binding.tvChallengeStatus.setVisibility(View.GONE);
            holder.binding.btnJoinChallenge.setEnabled(true);
            holder.binding.btnJoinChallenge.setText(R.string.wellness_join_challenge);
        }
        holder.binding.btnJoinChallenge.setContentDescription(context.getString(
                R.string.wellness_challenge_button_a11y,
                holder.binding.btnJoinChallenge.getText(),
                challenge.getTitle()));
        holder.binding.btnJoinChallenge.setOnClickListener(v -> listener.onJoin(challenge));
    }

    static String formatChallengeStatus(Context context, ChallengeProgress progress) {
        if (progress == null) {
            return "";
        }
        if (progress.isCompleted()) {
            return context.getString(R.string.wellness_challenge_completed_status);
        }
        return context.getString(
                R.string.wellness_challenge_progress_format,
                progress.getCompletedDays(),
                progress.getTargetDays());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemChallengeBinding binding;

        ViewHolder(ItemChallengeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
