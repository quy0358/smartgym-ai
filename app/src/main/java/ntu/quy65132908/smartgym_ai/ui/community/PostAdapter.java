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
            return a.getLikes() == b.getLikes() && a.getContent().equals(b.getContent());
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
        holder.tvLikes.setText(post.getLikes() + " ❤️");
        holder.tvAvatar.setText(post.getAuthorName().isEmpty() ? "U" : String.valueOf(post.getAuthorName().charAt(0)));

        boolean isLiked = currentUserId != null && post.getLikedBy().contains(currentUserId);
        holder.btnLike.setOnClickListener(v -> {
            if (likeListener != null) likeListener.onLike(post, isLiked);
        });
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
