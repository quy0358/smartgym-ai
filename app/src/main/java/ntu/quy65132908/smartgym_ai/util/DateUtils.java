package ntu.quy65132908.smartgym_ai.util;

import java.util.Calendar;
import java.util.Locale;

/**
 * Tiện ích ngày giờ dùng chung cho Dashboard và adapter.
 */
public final class DateUtils {

    private DateUtils() {} // Lớp tiện ích

    /**
     * Trả về thứ trong tuần của hôm nay theo dạng 1=Thứ 2 ... 7=Chủ nhật.
     */
    public static int getTodayDayOfWeek() {
        int calDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return calDay == Calendar.SUNDAY ? 7 : calDay - 1;
    }

    /**
     * Xác định buổi tập có phải ngày nghỉ hoặc phục hồi không.
     * Trả về true nếu tiêu đề có từ khóa nghỉ/phục hồi hoặc thời lượng bằng 0.
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
