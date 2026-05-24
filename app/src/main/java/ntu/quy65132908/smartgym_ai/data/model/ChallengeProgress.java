package ntu.quy65132908.smartgym_ai.data.model;

import java.util.ArrayList;
import java.util.List;

public class ChallengeProgress {
    private String id;
    private String challengeId;
    private String title;
    private int targetDays;
    private int completedDays;
    private int dailyMinutes;
    private boolean completed;
    private long startedAt;
    private long updatedAt;
    private List<String> completedDateKeys = new ArrayList<>();

    public ChallengeProgress() {}

    public static ChallengeProgress forChallenge(Challenge challenge, long startedAt) {
        ChallengeProgress progress = new ChallengeProgress();
        progress.challengeId = challenge.getId();
        progress.title = challenge.getTitle();
        progress.targetDays = challenge.getTargetDays();
        progress.dailyMinutes = challenge.getDailyMinutes();
        progress.startedAt = startedAt;
        progress.updatedAt = startedAt;
        return progress;
    }

    public String getId() { return id; }
    public String getChallengeId() { return challengeId; }
    public String getTitle() { return title; }
    public int getTargetDays() { return targetDays; }
    public int getCompletedDays() { return completedDays; }
    public int getDailyMinutes() { return dailyMinutes; }
    public boolean isCompleted() { return completed; }
    public long getStartedAt() { return startedAt; }
    public long getUpdatedAt() { return updatedAt; }
    public List<String> getCompletedDateKeys() { return completedDateKeys; }

    public void setId(String id) { this.id = id; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
    public void setTitle(String title) { this.title = title; }
    public void setTargetDays(int targetDays) { this.targetDays = targetDays; }
    public void setCompletedDays(int completedDays) { this.completedDays = completedDays; }
    public void setDailyMinutes(int dailyMinutes) { this.dailyMinutes = dailyMinutes; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public void setCompletedDateKeys(List<String> completedDateKeys) { this.completedDateKeys = completedDateKeys != null ? completedDateKeys : new ArrayList<>(); }

    public int getProgressPercent() {
        if (targetDays <= 0) {
            return 0;
        }
        return Math.min(100, Math.round((completedDays * 100f) / targetDays));
    }
}
