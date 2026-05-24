package ntu.quy65132908.smartgym_ai.data.model;

import java.util.ArrayList;
import java.util.List;

public class MealPlanDay {
    private int dayOfWeek;
    private String dayLabel;
    private int targetCalories;
    private List<Meal> meals = new ArrayList<>();

    public MealPlanDay() {}

    public int getDayOfWeek() { return dayOfWeek; }
    public String getDayLabel() { return dayLabel; }
    public int getTargetCalories() { return targetCalories; }
    public List<Meal> getMeals() { return meals; }

    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public void setDayLabel(String dayLabel) { this.dayLabel = dayLabel; }
    public void setTargetCalories(int targetCalories) { this.targetCalories = targetCalories; }
    public void setMeals(List<Meal> meals) { this.meals = meals != null ? meals : new ArrayList<>(); }
}
