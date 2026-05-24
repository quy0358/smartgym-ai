package ntu.quy65132908.smartgym_ai.data.model;

import java.util.ArrayList;
import java.util.List;

public class MealPlan {
    private String id;
    private String title;
    private long createdAt;
    private List<MealPlanDay> days = new ArrayList<>();

    public MealPlan() {}

    public String getId() { return id; }
    public String getTitle() { return title; }
    public long getCreatedAt() { return createdAt; }
    public List<MealPlanDay> getDays() { return days; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setDays(List<MealPlanDay> days) { this.days = days != null ? days : new ArrayList<>(); }
}
