package ntu.quy65132908.smartgym_ai.util;

import android.content.Context;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import ntu.quy65132908.smartgym_ai.data.model.Reminder;
import ntu.quy65132908.smartgym_ai.R;

public class ReminderScheduler {
    private static final String WORK_NAME_PREFIX = "smartgym_reminder_";

    /**
     * WorkManager is intentionally used for battery-friendly reminders. Delivery can be delayed by
     * Doze or OEM background limits; product flows that require exact alarms should use a separate
     * AlarmManager implementation with the required Android permissions and user education.
     */
    public void schedule(Context context, Reminder reminder) {
        if (context == null || reminder == null || !reminder.isEnabled()
                || reminder.getDaysOfWeek() == null || reminder.getDaysOfWeek().isEmpty()
                || !isValidTime(reminder.getHour(), reminder.getMinute())
                || !hasValidDay(reminder.getDaysOfWeek())) {
            return;
        }
        Data input = new Data.Builder()
                .putString(WorkoutReminderWorker.KEY_REMINDER_ID, reminder.getId())
                .putString(WorkoutReminderWorker.KEY_TITLE, reminder.getTitle())
                .putString(WorkoutReminderWorker.KEY_BODY, context.getString(R.string.wellness_notification_body))
                .putInt(WorkoutReminderWorker.KEY_HOUR, reminder.getHour())
                .putInt(WorkoutReminderWorker.KEY_MINUTE, reminder.getMinute())
                .putIntArray(WorkoutReminderWorker.KEY_DAYS_OF_WEEK, toIntArray(reminder.getDaysOfWeek()))
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(WorkoutReminderWorker.class)
                .setInitialDelay(nextDelayMillis(reminder), TimeUnit.MILLISECONDS)
                .setInputData(input)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_PREFIX + (reminder.getId() != null ? reminder.getId() : "default"),
                ExistingWorkPolicy.REPLACE,
                request
        );
    }

    public void cancel(Context context, String reminderId) {
        if (context == null) {
            return;
        }
        WorkManager.getInstance(context).cancelUniqueWork(
                WORK_NAME_PREFIX + (reminderId != null ? reminderId : "default"));
    }

    long nextDelayMillis(Reminder reminder) {
        return nextDelayMillis(reminder, Calendar.getInstance());
    }

    long nextDelayMillis(Reminder reminder, Calendar now) {
        if (reminder == null || reminder.getDaysOfWeek() == null || reminder.getDaysOfWeek().isEmpty()
                || !isValidTime(reminder.getHour(), reminder.getMinute())) {
            return 60_000L;
        }
        long bestDelay = Long.MAX_VALUE;
        for (Integer day : reminder.getDaysOfWeek()) {
            if (day == null || day < 1 || day > 7) {
                continue;
            }
            Calendar candidate = nextCandidate(now, day, reminder.getHour(), reminder.getMinute());
            long delay = candidate.getTimeInMillis() - now.getTimeInMillis();
            if (delay > 0 && delay < bestDelay) {
                bestDelay = delay;
            }
        }
        if (bestDelay == Long.MAX_VALUE) {
            return 60_000L;
        }
        return Math.max(60_000L, bestDelay);
    }

    private Calendar nextCandidate(Calendar now, int targetDayOfWeek, int hour, int minute) {
        Calendar next = (Calendar) now.clone();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        int today = toReminderDayOfWeek(now.get(Calendar.DAY_OF_WEEK));
        int daysUntil = (targetDayOfWeek - today + 7) % 7;
        next.add(Calendar.DAY_OF_YEAR, daysUntil);
        if (!next.after(now)) {
            next.add(Calendar.DAY_OF_YEAR, 7);
        }
        return next;
    }

    private int toReminderDayOfWeek(int calendarDay) {
        return calendarDay == Calendar.SUNDAY ? 7 : calendarDay - 1;
    }

    private int[] toIntArray(java.util.List<Integer> days) {
        int[] values = new int[days.size()];
        for (int i = 0; i < days.size(); i++) {
            values[i] = days.get(i) != null ? days.get(i) : 0;
        }
        return values;
    }

    private boolean isValidTime(int hour, int minute) {
        return hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59;
    }

    private boolean hasValidDay(java.util.List<Integer> days) {
        if (days == null) {
            return false;
        }
        for (Integer day : days) {
            if (day != null && day >= 1 && day <= 7) {
                return true;
            }
        }
        return false;
    }
}
