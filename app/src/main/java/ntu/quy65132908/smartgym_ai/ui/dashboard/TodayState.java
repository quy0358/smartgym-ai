package ntu.quy65132908.smartgym_ai.ui.dashboard;

/**
 * Represents the state of today's workout on the Dashboard.
 */
public enum TodayState {
    /** Today has a real workout to perform */
    WORKOUT,
    /** Today is a rest/recovery day (deliberate or implicit) */
    REST_DAY,
    /** User has no weekly plan at all */
    NO_PLAN
}
