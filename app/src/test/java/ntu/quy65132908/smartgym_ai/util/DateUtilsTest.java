package ntu.quy65132908.smartgym_ai.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class DateUtilsTest {

    @Test
    public void getTodayDayOfWeek_returnsValidRange() {
        int today = DateUtils.getTodayDayOfWeek();
        assertTrue("Day of week must be >= 1", today >= 1);
        assertTrue("Day of week must be <= 7", today <= 7);
    }

    @Test
    public void isRestDayWorkout_nullTitle_returnsFalse() {
        assertFalse(DateUtils.isRestDayWorkout(null, 30));
    }

    @Test
    public void isRestDayWorkout_restInTitle_returnsTrue() {
        assertTrue(DateUtils.isRestDayWorkout("Rest day/ Recover", 0));
    }

    @Test
    public void isRestDayWorkout_nghiInTitle_returnsTrue() {
        assertTrue(DateUtils.isRestDayWorkout("Nghỉ ngơi", 0));
    }

    @Test
    public void isRestDayWorkout_zeroDurationNoExercises_returnsTrue() {
        assertTrue(DateUtils.isRestDayWorkout("Something", 0));
    }

    @Test
    public void isRestDayWorkout_normalWorkout_returnsFalse() {
        assertFalse(DateUtils.isRestDayWorkout("Tập nặng cơ ngực", 45));
    }

    @Test
    public void isRestDayWorkout_recoverInTitle_returnsTrue() {
        assertTrue(DateUtils.isRestDayWorkout("Recovery day", 0));
    }
}
