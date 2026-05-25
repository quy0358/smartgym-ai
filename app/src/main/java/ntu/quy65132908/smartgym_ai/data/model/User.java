package ntu.quy65132908.smartgym_ai.data.model;

import java.util.HashMap;
import java.util.Map;

public class User {
    private String uid;
    private String displayName;
    private String email;
    private String photoUrl;
    private Float weight;
    private Float height;
    private Float bmi;
    private String bmiCategory;
    private String goal;
    private Float targetWeight;
    private String gender;
    private String birthDate;
    private String fitnessLevel;
    private boolean onboardingCompleted;
    private long onboardingCompletedAt;
    private long createdAt;

    public User() {} // Firestore requires empty constructor

    public User(String uid, String displayName, String email) {
        this.uid = uid;
        this.displayName = displayName;
        this.email = email;
        this.createdAt = System.currentTimeMillis();
        this.bmiCategory = "Bình thường";
    }

    // Getters
    public String getUid() { return uid; }
    public String getDisplayName() { return displayName != null ? displayName : ""; }
    public String getEmail() { return email != null ? email : ""; }
    public String getPhotoUrl() { return photoUrl; }
    public Float getWeight() { return weight; }
    public Float getHeight() { return height; }
    public Float getBmi() { return bmi; }
    public String getBmiCategory() { return bmiCategory != null ? bmiCategory : "Bình thường"; }
    public String getGoal() { return goal; }
    public Float getTargetWeight() { return targetWeight; }
    public String getGender() { return gender != null ? gender : ""; }
    public String getBirthDate() { return birthDate != null ? birthDate : ""; }
    public String getFitnessLevel() { return fitnessLevel != null ? fitnessLevel : ""; }
    public boolean isOnboardingCompleted() { return onboardingCompleted; }
    public long getOnboardingCompletedAt() { return onboardingCompletedAt; }
    public long getCreatedAt() { return createdAt; }

    // Setters
    public void setUid(String uid) { this.uid = uid; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public void setWeight(Float weight) { this.weight = weight; }
    public void setHeight(Float height) { this.height = height; }
    public void setBmi(Float bmi) { this.bmi = bmi; }
    public void setBmiCategory(String bmiCategory) { this.bmiCategory = bmiCategory; }
    public void setGoal(String goal) { this.goal = goal; }
    public void setTargetWeight(Float targetWeight) { this.targetWeight = targetWeight; }
    public void setGender(String gender) { this.gender = gender; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
    public void setFitnessLevel(String fitnessLevel) { this.fitnessLevel = fitnessLevel; }
    public void setOnboardingCompleted(boolean onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; }
    public void setOnboardingCompletedAt(long onboardingCompletedAt) { this.onboardingCompletedAt = onboardingCompletedAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (uid != null) map.put("uid", uid);
        if (displayName != null) map.put("displayName", displayName);
        if (email != null) map.put("email", email);
        if (photoUrl != null) map.put("photoUrl", photoUrl);
        if (weight != null) map.put("weight", weight);
        if (height != null) map.put("height", height);
        if (bmi != null) map.put("bmi", bmi);
        if (bmiCategory != null) map.put("bmiCategory", bmiCategory);
        if (goal != null) map.put("goal", goal);
        if (targetWeight != null) map.put("targetWeight", targetWeight);
        if (gender != null) map.put("gender", gender);
        if (birthDate != null) map.put("birthDate", birthDate);
        if (fitnessLevel != null) map.put("fitnessLevel", fitnessLevel);
        map.put("onboardingCompleted", onboardingCompleted);
        if (onboardingCompletedAt > 0L) map.put("onboardingCompletedAt", onboardingCompletedAt);
        map.put("createdAt", createdAt);
        return map;
    }
}
