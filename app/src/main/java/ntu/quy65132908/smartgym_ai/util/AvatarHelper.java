package ntu.quy65132908.smartgym_ai.util;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;

import java.util.Locale;

/**
 * H5: Utility to load avatar images with Glide, falling back to letter initial.
 */
public class AvatarHelper {

    /**
     * Load avatar photo from URL. Shows letter fallback if URL is null/empty.
     *
     * @param context   Context
     * @param photoUrl  Firebase Storage URL or null
     * @param imageView ImageView for photo (hidden when no URL)
     * @param textView  TextView for letter fallback (shown when no URL)
     * @param name      User name to derive initial letter
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
