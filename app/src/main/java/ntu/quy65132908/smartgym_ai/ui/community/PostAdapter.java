package ntu.quy65132908.smartgym_ai.ui.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import ntu.quy65132908.smartgym_ai.R;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {

    private final String[][] posts = {
            {"Minh Anh", "Hôm nay tập Leg Day xong rồi! Squat 100kg PR mới 🎉💪", "24", "2 giờ trước"},
            {"Hoàng Nam", "Ai có tips tập vai cho đều không ạ? Vai phải to hơn vai trái 😅", "8", "5 giờ trước"},
            {"Thanh Hoa", "3 tháng kiên trì, giảm được 8kg! Cảm ơn FitAI đã giúp mình theo dõi ❤️", "56", "1 ngày trước"},
            {"Đức Phú", "Form Deadlift đã cải thiện nhiều nhờ AI phân tích. Recommend mọi người thử!", "15", "2 ngày trước"}
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvAuthor.setText(posts[position][0]);
        holder.tvContent.setText(posts[position][1]);
        holder.tvLikes.setText(posts[position][2] + " ❤️");
        holder.tvTime.setText(posts[position][3]);

        // Avatar initial
        holder.tvAvatar.setText(String.valueOf(posts[position][0].charAt(0)));

        holder.btnLike.setOnClickListener(v -> {
            int likes = Integer.parseInt(posts[position][2]) + 1;
            holder.tvLikes.setText(likes + " ❤️");
        });
    }

    @Override
    public int getItemCount() { return posts.length; }

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
