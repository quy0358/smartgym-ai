package ntu.quy65132908.smartgym_ai.ui.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Post;

public class PostAdapter extends ListAdapter<Post, PostAdapter.ViewHolder> {

    public interface OnLikeClickListener {
        void onLike(Post post, boolean isCurrentlyLiked);
    }

    private final OnLikeClickListener likeListener;
    private String currentUserId;

    public PostAdapter(OnLikeClickListener listener) {
        super(DIFF_CALLBACK);
        this.likeListener = listener;
    }

    public void setCurrentUserId(String uid) { this.currentUserId = uid; }

    private static final DiffUtil.ItemCallback<Post> DIFF_CALLBACK = new DiffUtil.ItemCallback<Post>() {
        @Override
        public boolean areItemsTheSame(@NonNull Post a, @NonNull Post b) {
            return a.getId() != null && a.getId().equals(b.getId());
        }
        @Override
        public boolean areContentsTheSame(@NonNull Post a, @NonNull Post b) {
            return a.getLikes() == b.getLikes()
                    && a.getCreatedAt() == b.getCreatedAt()
                    && Objects.equals(a.getAuthorName(), b.getAuthorName())
                    && Objects.equals(a.getContent(), b.getContent())
                    && Objects.equals(a.getLikedBy(), b.getLikedBy());
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = getItem(position);
        holder.tvAuthor.setText(post.getAuthorName());
        holder.tvContent.setText(post.getContent());
        holder.tvLikes.setText(holder.itemView.getContext().getString(R.string.post_likes_format, post.getLikes()));
        holder.tvAvatar.setText(post.getAuthorName().isEmpty()
                ? holder.itemView.getContext().getString(R.string.default_avatar_letter)
                : String.valueOf(post.getAuthorName().charAt(0)).toUpperCase(Locale.getDefault()));
        holder.tvTime.setText(formatRelativeTime(holder, post.getCreatedAt()));

        boolean isLiked = currentUserId != null && post.getLikedBy().contains(currentUserId);
        holder.btnLike.setImageResource(isLiked ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        holder.btnLike.setOnClickListener(v -> {
            if (likeListener != null) likeListener.onLike(post, isLiked);
        });
    }

    private static String formatRelativeTime(ViewHolder holder, long createdAt) {
        if (createdAt <= 0) return "";
        long diffMs = Math.max(0, System.currentTimeMillis() - createdAt);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs);
        if (minutes < 1) return holder.itemView.getContext().getString(R.string.time_just_now);
        if (minutes < 60) {
            return holder.itemView.getContext().getString(R.string.time_minutes_ago_format, minutes);
        }
        long hours = TimeUnit.MILLISECONDS.toHours(diffMs);
        if (hours < 24) {
            return holder.itemView.getContext().getString(R.string.time_hours_ago_format, hours);
        }
        long days = TimeUnit.MILLISECONDS.toDays(diffMs);
        if (days < 7) {
            return holder.itemView.getContext().getString(R.string.time_days_ago_format, days);
        }
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(createdAt));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvAuthor, tvContent, tvLikes, tvTime, tvAvatar;
        final ImageButton btnLike;
        ViewHolder(View view) {
            super(view);
            tvAuthor = view.findViewById(R.id.tv_post_author);
            tvContent = view.findViewById(R.id.tv_post_content);
            tvLikes = view.findViewById(R.id.tv_post_likes);
            tvTime = view.findViewById(R.id.tv_post_time);
            tvAvatar = view.findViewById(R.id.tv_post_avatar);
            btnLike = view.findViewById(R.id.btn_like);
        }
    }
}
