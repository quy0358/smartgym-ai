package ntu.quy65132908.smartgym_ai.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

import ntu.quy65132908.smartgym_ai.data.model.Reminder;

public class ReminderSchedulerTest {

    @Test
    public void nextDelayMillis_sameDayFutureUsesRemainingTime() {
        ReminderScheduler scheduler = new ReminderScheduler();
        Calendar now = calendar(2024, Calendar.MAY, 24, 8, 0, 0); // Friday
        Reminder reminder = new Reminder("r1", "Tập", 9, 0, true, Arrays.asList(5));

        long delay = scheduler.nextDelayMillis(reminder, now);

        assertEquals(60L * 60L * 1000L, delay);
    }

    @Test
    public void nextDelayMillis_sameDayPastRollsForwardOneWeek() {
        ReminderScheduler scheduler = new ReminderScheduler();
        Calendar now = calendar(2024, Calendar.MAY, 24, 10, 0, 0); // Friday
        Reminder reminder = new Reminder("r1", "Tập", 9, 0, true, Arrays.asList(5));

        long delay = scheduler.nextDelayMillis(reminder, now);

        assertTrue(delay > 6L * 24L * 60L * 60L * 1000L);
        assertTrue(delay < 7L * 24L * 60L * 60L * 1000L);
    }

    @Test
    public void nextDelayMillis_invalidDaysFallsBackToOneMinute() {
        ReminderScheduler scheduler = new ReminderScheduler();
        Calendar now = calendar(2024, Calendar.MAY, 24, 10, 0, 0);
        Reminder reminder = new Reminder("r1", "Tập", 9, 0, true, Arrays.asList(0, 8));

        long delay = scheduler.nextDelayMillis(reminder, now);

        assertEquals(60_000L, delay);
    }

    private Calendar calendar(int year, int month, int dayOfMonth, int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(year, month, dayOfMonth, hour, minute, second);
        return calendar;
    }
}
