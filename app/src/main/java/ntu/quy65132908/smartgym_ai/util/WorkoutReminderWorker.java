package ntu.quy65132908.smartgym_ai.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;
import java.util.List;

import ntu.quy65132908.smartgym_ai.data.model.Reminder;
import ntu.quy65132908.smartgym_ai.R;

public class WorkoutReminderWorker extends Worker {
    public static final String CHANNEL_ID = "smartgym_reminders";
    public static final String KEY_REMINDER_ID = "reminder_id";
    public static final String KEY_TITLE = "title";
    public static final String KEY_BODY = "body";
    public static final String KEY_HOUR = "hour";
    public static final String KEY_MINUTE = "minute";
    public static final String KEY_DAYS_OF_WEEK = "days_of_week";

    public WorkoutReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        createChannel(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            scheduleNext(context);
            return Result.success();
        }

        String title = getInputData().getString(KEY_TITLE);
        String body = getInputData().getString(KEY_BODY);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title != null ? title : context.getString(R.string.wellness_default_reminder))
                .setContentText(body != null ? body : context.getString(R.string.wellness_notification_default_body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        PendingIntent launchIntent = createLaunchIntent(context);
        if (launchIntent != null) {
            builder.setContentIntent(launchIntent);
        }
        try {
            NotificationManagerCompat.from(context).notify(6513, builder.build());
        } catch (SecurityException ignored) {
            // Permission can be revoked after WorkManager starts the job.
        }
        scheduleNext(context);
        return Result.success();
    }

    private PendingIntent createLaunchIntent(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (intent == null) {
            return null;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(context, 6513, intent, flags);
    }

    private void scheduleNext(Context context) {
        String id = getInputData().getString(KEY_REMINDER_ID);
        int hour = getInputData().getInt(KEY_HOUR, -1);
        int minute = getInputData().getInt(KEY_MINUTE, -1);
        int[] days = getInputData().getIntArray(KEY_DAYS_OF_WEEK);
        if (hour < 0 || minute < 0 || days == null || days.length == 0) {
            return;
        }
        List<Integer> daysOfWeek = new ArrayList<>();
        for (int day : days) {
            if (day >= 1 && day <= 7) {
                daysOfWeek.add(day);
            }
        }
        if (daysOfWeek.isEmpty()) {
            return;
        }
        Reminder reminder = new Reminder(
                id != null ? id : "training",
                getInputData().getString(KEY_TITLE),
                hour,
                minute,
                true,
                daysOfWeek
        );
        new ReminderScheduler().schedule(context, reminder);
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.wellness_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
