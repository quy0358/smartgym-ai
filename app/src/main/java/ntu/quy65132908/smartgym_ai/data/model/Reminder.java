package ntu.quy65132908.smartgym_ai.data.model;

import java.util.ArrayList;
import java.util.List;

public class Reminder {
    private String id;
    private String title;
    private int hour;
    private int minute;
    private boolean enabled;
    private List<Integer> daysOfWeek = new ArrayList<>();

    public Reminder() {}

    public Reminder(String id, String title, int hour, int minute, boolean enabled, List<Integer> daysOfWeek) {
        this.id = id;
        this.title = title;
        this.hour = hour;
        this.minute = minute;
        this.enabled = enabled;
        this.daysOfWeek = daysOfWeek != null ? daysOfWeek : new ArrayList<>();
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public int getHour() { return hour; }
    public int getMinute() { return minute; }
    public boolean isEnabled() { return enabled; }
    public List<Integer> getDaysOfWeek() { return daysOfWeek; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setHour(int hour) { this.hour = hour; }
    public void setMinute(int minute) { this.minute = minute; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setDaysOfWeek(List<Integer> daysOfWeek) { this.daysOfWeek = daysOfWeek != null ? daysOfWeek : new ArrayList<>(); }
}
