package ntu.quy65132908.smartgym_ai.ui.community;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import ntu.quy65132908.smartgym_ai.R;

public final class PostTimeFormatter {
    private PostTimeFormatter() {}

    public static String format(Context context, long createdAt) {
        return format(context, createdAt, System.currentTimeMillis());
    }

    static String format(Context context, long createdAt, long now) {
        if (context == null || createdAt < 0) {
            return "";
        }

        long diffMs = Math.max(0, now - createdAt);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs);
        if (minutes < 1) {
            return context.getString(R.string.time_just_now);
        }
        if (minutes < 60) {
            return context.getString(R.string.time_minutes_ago_format, minutes);
        }

        long hours = TimeUnit.MILLISECONDS.toHours(diffMs);
        if (hours < 24) {
            return context.getString(R.string.time_hours_ago_format, hours);
        }

        long days = TimeUnit.MILLISECONDS.toDays(diffMs);
        if (days < 7) {
            return context.getString(R.string.time_days_ago_format, days);
        }

        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(createdAt));
    }
}
