package ntu.quy65132908.smartgym_ai.ui.wellness;

import java.util.ArrayList;
import java.util.List;

import ntu.quy65132908.smartgym_ai.data.model.ChallengeProgress;
import ntu.quy65132908.smartgym_ai.data.model.InjuryProfile;
import ntu.quy65132908.smartgym_ai.data.model.Reminder;

public class WellnessUiState {
    public static final String SAVING_NONE = "none";
    public static final String SAVING_REMINDER = "reminder";
    public static final String SAVING_INJURY = "injury";
    public static final String SAVING_CHALLENGE = "challenge";

    private final Reminder reminder;
    private final InjuryProfile injuryProfile;
    private final List<ChallengeDisplayItem> challengeItems;
    private final boolean loading;
    private final boolean loggedOut;
    private final String savingTarget;

    public WellnessUiState(Reminder reminder,
                           InjuryProfile injuryProfile,
                           List<ChallengeDisplayItem> challengeItems,
                           boolean loading,
                           boolean loggedOut,
                           String savingTarget) {
        this.reminder = reminder;
        this.injuryProfile = injuryProfile;
        this.challengeItems = challengeItems != null ? new ArrayList<>(challengeItems) : new ArrayList<>();
        this.loading = loading;
        this.loggedOut = loggedOut;
        this.savingTarget = savingTarget != null ? savingTarget : SAVING_NONE;
    }

    public Reminder getReminder() { return reminder; }
    public InjuryProfile getInjuryProfile() { return injuryProfile; }
    public List<ChallengeDisplayItem> getChallengeItems() { return new ArrayList<>(challengeItems); }
    public boolean isLoading() { return loading; }
    public boolean isLoggedOut() { return loggedOut; }
    public String getSavingTarget() { return savingTarget; }

    public boolean isSavingReminder() { return SAVING_REMINDER.equals(savingTarget); }
    public boolean isSavingInjury() { return SAVING_INJURY.equals(savingTarget); }
    public boolean isSavingChallenge() { return SAVING_CHALLENGE.equals(savingTarget); }

    public ChallengeProgress findProgress(String challengeId) {
        if (challengeId == null) {
            return null;
        }
        for (ChallengeDisplayItem item : challengeItems) {
            if (item != null && item.getProgress() != null
                    && challengeId.equals(item.getProgress().getChallengeId())) {
                return item.getProgress();
            }
        }
        return null;
    }
}
