package ntu.quy65132908.smartgym_ai.ui.wellness;

public class WellnessFormErrors {
    private final String reminderTitleError;
    private final String reminderTimeError;
    private final String reminderDaysError;
    private final String injuryNotesError;

    public WellnessFormErrors(String reminderTitleError,
                              String reminderTimeError,
                              String reminderDaysError,
                              String injuryNotesError) {
        this.reminderTitleError = reminderTitleError;
        this.reminderTimeError = reminderTimeError;
        this.reminderDaysError = reminderDaysError;
        this.injuryNotesError = injuryNotesError;
    }

    public String getReminderTitleError() { return reminderTitleError; }
    public String getReminderTimeError() { return reminderTimeError; }
    public String getReminderDaysError() { return reminderDaysError; }
    public String getInjuryNotesError() { return injuryNotesError; }

    public boolean hasReminderErrors() {
        return reminderTitleError != null || reminderTimeError != null || reminderDaysError != null;
    }

    public boolean hasInjuryErrors() {
        return injuryNotesError != null;
    }

    public static WellnessFormErrors none() {
        return new WellnessFormErrors(null, null, null, null);
    }
}
