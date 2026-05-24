package ntu.quy65132908.smartgym_ai.data.model;

public class Challenge {
    private String id;
    private String title;
    private String description;
    private int targetDays;
    private int dailyMinutes;

    public Challenge() {}

    public Challenge(String id, String title, String description, int targetDays, int dailyMinutes) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.targetDays = targetDays;
        this.dailyMinutes = dailyMinutes;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getTargetDays() { return targetDays; }
    public int getDailyMinutes() { return dailyMinutes; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setTargetDays(int targetDays) { this.targetDays = targetDays; }
    public void setDailyMinutes(int dailyMinutes) { this.dailyMinutes = dailyMinutes; }
}
