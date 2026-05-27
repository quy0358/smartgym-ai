package ntu.quy65132908.smartgym_ai.ui.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Post;

public class PostAdapter extends ListAdapter<Post, PostAdapter.ViewHolder> {
    private static final float PENDING_ALPHA = 0.5f;
    private static final float READY_ALPHA = 1f;

    public interface OnLikeClickListener {
        void onLike(Post post);
    }

    public interface OnPostActionListener {
        void onEdit(Post post);
        void onDelete(Post post);
    }

    private final OnLikeClickListener likeListener;
    private final OnPostActionListener actionListener;
    private String currentUserId;
    private Set<String> pendingLikePostIds = Collections.emptySet();
    private Set<String> pendingActionPostIds = Collections.emptySet();

    public PostAdapter(OnLikeClickListener listener) {
        this(listener, null);
    }

    public PostAdapter(OnLikeClickListener listener, OnPostActionListener actionListener) {
        super(DIFF_CALLBACK);
        this.likeListener = listener;
        this.actionListener = actionListener;
    }

    public void setCurrentUserId(String uid) {
        this.currentUserId = uid;
    }

    public void setPendingLikePostIds(Set<String> ids) {
        Set<String> oldIds = pendingLikePostIds;
        this.pendingLikePostIds = ids != null ? new HashSet<>(ids) : Collections.emptySet();
        notifyChangedPendingItems(oldIds, pendingLikePostIds);
    }

    public void setPendingActionPostIds(Set<String> ids) {
        Set<String> oldIds = pendingActionPostIds;
        this.pendingActionPostIds = ids != null ? new HashSet<>(ids) : Collections.emptySet();
        notifyChangedPendingItems(oldIds, pendingActionPostIds);
    }

    private void notifyChangedPendingItems(Set<String> oldIds, Set<String> newIds) {
        for (int i = 0; i < getItemCount(); i++) {
            Post post = getItem(i);
            String postId = post.getId();
            if (oldIds.contains(postId) != newIds.contains(postId)) {
                notifyItemChanged(i);
            }
        }
    }

    private static final DiffUtil.ItemCallback<Post> DIFF_CALLBACK = new DiffUtil.ItemCallback<Post>() {
        @Override
        public boolean areItemsTheSame(@NonNull Post a, @NonNull Post b) {
            return a.getId() != null && a.getId().equals(b.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Post a, @NonNull Post b) {
            return a.getLikes() == b.getLikes()
                    && a.getCreatedAt() == b.getCreatedAt()
                    && a.getUpdatedAt() == b.getUpdatedAt()
                    && Objects.equals(a.getAuthorId(), b.getAuthorId())
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
        bindPost(holder, getItem(position));
    }

    void bindPostForTest(ViewHolder holder, Post post) {
        bindPost(holder, post);
    }

    private void bindPost(ViewHolder holder, Post post) {
        String rawAuthorName = post.getAuthorName().trim();
        String displayName = rawAuthorName.isEmpty()
                ? holder.itemView.getContext().getString(R.string.post_user_default)
                : rawAuthorName;

        holder.tvAuthor.setText(displayName);
        holder.tvContent.setText(post.getContent());
        holder.tvLikes.setText(holder.itemView.getContext().getString(R.string.post_likes_format, post.getLikes()));
        holder.tvAvatar.setText(rawAuthorName.isEmpty()
                ? holder.itemView.getContext().getString(R.string.default_avatar_letter)
                : String.valueOf(rawAuthorName.charAt(0)).toUpperCase(Locale.getDefault()));
        String timeText = PostTimeFormatter.format(holder.itemView.getContext(), post.getCreatedAt());
        if (post.getUpdatedAt() > 0 && post.getUpdatedAt() > post.getCreatedAt()) {
            timeText = holder.itemView.getContext().getString(R.string.post_edited_time_format, timeText);
        }
        holder.tvTime.setText(timeText);

        boolean isLiked = currentUserId != null && post.getLikedBy().contains(currentUserId);
        boolean isPending = post.getId() != null && pendingLikePostIds.contains(post.getId());
        holder.btnLike.setImageResource(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        holder.btnLike.setContentDescription(holder.itemView.getContext().getString(
                isLiked ? R.string.post_unlike_a11y : R.string.post_like_a11y));
        holder.btnLike.setEnabled(!isPending);
        holder.btnLike.setAlpha(isPending ? PENDING_ALPHA : READY_ALPHA);
        holder.btnLike.setOnClickListener(v -> {
            if (!isPending && likeListener != null) {
                likeListener.onLike(post);
            }
        });

        boolean isOwner = currentUserId != null && currentUserId.equals(post.getAuthorId());
        boolean isActionPending = post.getId() != null && pendingActionPostIds.contains(post.getId());
        holder.btnPostActions.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        holder.btnPostActions.setEnabled(isOwner && !isActionPending);
        holder.btnPostActions.setAlpha(isActionPending ? PENDING_ALPHA : READY_ALPHA);
        holder.btnPostActions.setOnClickListener(v -> {
            if (isOwner && !isActionPending) {
                showPostActions(holder, post);
            }
        });
    }

    private void showPostActions(ViewHolder holder, Post post) {
        PopupMenu menu = new PopupMenu(holder.itemView.getContext(), holder.btnPostActions);
        menu.inflate(R.menu.menu_post_actions);
        menu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_edit_post) {
                if (actionListener != null) {
                    actionListener.onEdit(post);
                }
                return true;
            }
            if (itemId == R.id.action_delete_post) {
                if (actionListener != null) {
                    actionListener.onDelete(post);
                }
                return true;
            }
            return false;
        });
        menu.show();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvAuthor, tvContent, tvLikes, tvTime, tvAvatar;
        final ImageButton btnLike, btnPostActions;

        ViewHolder(View view) {
            super(view);
            tvAuthor = view.findViewById(R.id.tv_post_author);
            tvContent = view.findViewById(R.id.tv_post_content);
            tvLikes = view.findViewById(R.id.tv_post_likes);
            tvTime = view.findViewById(R.id.tv_post_time);
            tvAvatar = view.findViewById(R.id.tv_post_avatar);
            btnLike = view.findViewById(R.id.btn_like);
            btnPostActions = view.findViewById(R.id.btn_post_actions);
        }
    }
}
