package ntu.quy65132908.smartgym_ai.data.repository;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import ntu.quy65132908.smartgym_ai.data.model.Reminder;

public class ReminderRepositoryTest {

    @Test
    public void validateReminder_acceptsEnabledReminderWithValidTimeAndDays() {
        Reminder reminder = new Reminder("r1", "Tập buổi sáng", 6, 30, true, Arrays.asList(1, 3, 5));

        assertNull(ReminderRepository.validateReminder(reminder));
    }

    @Test
    public void validateReminder_rejectsInvalidHourAndEmptyDays() {
        Reminder badHour = new Reminder("r1", "Tập", 25, 0, true, Arrays.asList(1));
        Reminder noDays = new Reminder("r2", "Tập", 7, 15, true, Collections.emptyList());

        assertTrue(ReminderRepository.validateReminder(badHour).contains("giờ"));
        assertTrue(ReminderRepository.validateReminder(noDays).contains("ngày"));
    }

    @Test
    public void validateReminder_rejectsInvalidDayValues() {
        Reminder invalidDay = new Reminder("r1", "Tập", 7, 15, true, Arrays.asList(0, 8));

        assertTrue(ReminderRepository.validateReminder(invalidDay).contains("Ngày"));
    }
}
