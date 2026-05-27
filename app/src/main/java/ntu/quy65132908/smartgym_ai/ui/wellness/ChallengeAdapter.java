package ntu.quy65132908.smartgym_ai.ui.wellness;

import android.content.Context;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        List<ChallengeDisplayItem> oldItems = new ArrayList<>(items);
        List<ChallengeDisplayItem> newItems = challenges != null ? new ArrayList<>(challenges) : new ArrayList<>();
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ChallengeDiffCallback(oldItems, newItems));
        items.clear();
        items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
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

    private static class ChallengeDiffCallback extends DiffUtil.Callback {
        private final List<ChallengeDisplayItem> oldItems;
        private final List<ChallengeDisplayItem> newItems;

        ChallengeDiffCallback(List<ChallengeDisplayItem> oldItems, List<ChallengeDisplayItem> newItems) {
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
            Challenge oldChallenge = oldItems.get(oldItemPosition).getChallenge();
            Challenge newChallenge = newItems.get(newItemPosition).getChallenge();
            if (oldChallenge == null || newChallenge == null) {
                return oldChallenge == newChallenge;
            }
            String oldId = oldChallenge.getId();
            String newId = newChallenge.getId();
            if (oldId != null && newId != null) {
                return oldId.equals(newId);
            }
            return Objects.equals(oldChallenge.getTitle(), newChallenge.getTitle())
                    && oldChallenge.getTargetDays() == newChallenge.getTargetDays()
                    && oldChallenge.getDailyMinutes() == newChallenge.getDailyMinutes();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            ChallengeDisplayItem oldItem = oldItems.get(oldItemPosition);
            ChallengeDisplayItem newItem = newItems.get(newItemPosition);
            return sameChallenge(oldItem.getChallenge(), newItem.getChallenge())
                    && sameProgress(oldItem.getProgress(), newItem.getProgress());
        }

        private static boolean sameChallenge(Challenge oldChallenge, Challenge newChallenge) {
            if (oldChallenge == newChallenge) {
                return true;
            }
            if (oldChallenge == null || newChallenge == null) {
                return false;
            }
            return Objects.equals(oldChallenge.getId(), newChallenge.getId())
                    && Objects.equals(oldChallenge.getTitle(), newChallenge.getTitle())
                    && Objects.equals(oldChallenge.getDescription(), newChallenge.getDescription())
                    && oldChallenge.getTargetDays() == newChallenge.getTargetDays()
                    && oldChallenge.getDailyMinutes() == newChallenge.getDailyMinutes();
        }

        private static boolean sameProgress(ChallengeProgress oldProgress, ChallengeProgress newProgress) {
            if (oldProgress == newProgress) {
                return true;
            }
            if (oldProgress == null || newProgress == null) {
                return false;
            }
            return Objects.equals(oldProgress.getId(), newProgress.getId())
                    && Objects.equals(oldProgress.getChallengeId(), newProgress.getChallengeId())
                    && oldProgress.getTargetDays() == newProgress.getTargetDays()
                    && oldProgress.getCompletedDays() == newProgress.getCompletedDays()
                    && oldProgress.getDailyMinutes() == newProgress.getDailyMinutes()
                    && oldProgress.isCompleted() == newProgress.isCompleted();
        }
    }
}
