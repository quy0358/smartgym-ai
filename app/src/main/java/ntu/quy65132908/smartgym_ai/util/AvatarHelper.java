package ntu.quy65132908.smartgym_ai.util;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;

import java.util.Locale;

/**
 * H5: Tiện ích tải ảnh đại diện bằng Glide, fallback về chữ cái đầu.
 */
public class AvatarHelper {

    /**
     * Tải ảnh đại diện từ URL. Hiển thị chữ cái fallback nếu URL null hoặc rỗng.
     *
     * @param context   Context của Android
     * @param photoUrl  URL Firebase Storage hoặc null
     * @param imageView ImageView dùng cho ảnh, ẩn khi không có URL
     * @param textView  TextView dùng cho chữ cái fallback, hiện khi không có URL
     * @param name      Tên người dùng để lấy chữ cái đầu
     */
    public static void loadAvatar(Context context, String photoUrl,
                                  ImageView imageView, TextView textView, String name) {
        if (photoUrl != null && !photoUrl.isEmpty()) {
            imageView.setVisibility(android.view.View.VISIBLE);
            textView.setVisibility(android.view.View.GONE);

            Glide.with(context)
                    .load(photoUrl)
                    .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                    .placeholder(android.R.color.transparent)
                    .into(imageView);
        } else {
            imageView.setVisibility(android.view.View.GONE);
            textView.setVisibility(android.view.View.VISIBLE);
            String initial = (name != null && !name.isEmpty())
                    ? String.valueOf(name.charAt(0)).toUpperCase(Locale.getDefault())
                    : "U";
            textView.setText(initial);
        }
    }
}
