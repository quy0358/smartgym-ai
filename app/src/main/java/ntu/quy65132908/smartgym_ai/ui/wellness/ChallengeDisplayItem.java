package ntu.quy65132908.smartgym_ai.ui.wellness;

import ntu.quy65132908.smartgym_ai.data.model.Challenge;
import ntu.quy65132908.smartgym_ai.data.model.ChallengeProgress;

public class ChallengeDisplayItem {
    private final Challenge challenge;
    private final ChallengeProgress progress;

    public ChallengeDisplayItem(Challenge challenge, ChallengeProgress progress) {
        this.challenge = challenge;
        this.progress = progress;
    }

    public Challenge getChallenge() { return challenge; }
    public ChallengeProgress getProgress() { return progress; }

    public boolean isJoined() {
        return progress != null;
    }

    public boolean isCompleted() {
        return progress != null && progress.isCompleted();
    }
}
