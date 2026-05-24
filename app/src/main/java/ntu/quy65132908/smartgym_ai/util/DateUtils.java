package ntu.quy65132908.smartgym_ai.util;

import java.util.Calendar;
import java.util.Locale;

/**
 * Shared date/time utilities for the Dashboard and adapters.
 */
public final class DateUtils {

    private DateUtils() {} // Utility class

    /**
     * Returns today's day of week as 1=Monday ... 7=Sunday.
     */
    public static int getTodayDayOfWeek() {
        int calDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return calDay == Calendar.SUNDAY ? 7 : calDay - 1;
    }

    /**
     * Determines if a workout represents a rest/recovery day.
     * Returns true if title contains rest/recovery keywords OR duration is 0.
     */
    public static boolean isRestDayWorkout(String title, int durationMinutes) {
        if (durationMinutes == 0) {
            return true;
        }
        if (title == null || title.trim().isEmpty()) {
            return false;
        }
        String lower = title.toLowerCase(Locale.ROOT);
        return lower.contains("rest") || lower.contains("nghỉ") || lower.contains("recover");
    }
}
