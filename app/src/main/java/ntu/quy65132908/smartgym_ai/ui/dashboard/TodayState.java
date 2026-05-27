package ntu.quy65132908.smartgym_ai.ui.dashboard;

/**
 * Biểu diễn trạng thái buổi tập hôm nay trên Dashboard.
 */
public enum TodayState {
    /** Hôm nay có buổi tập thực sự cần hoàn thành. */
    WORKOUT,
    /** Hôm nay là ngày nghỉ hoặc phục hồi, có chủ đích hoặc ngầm định. */
    REST_DAY,
    /** Người dùng chưa có kế hoạch tuần. */
    NO_PLAN
}
