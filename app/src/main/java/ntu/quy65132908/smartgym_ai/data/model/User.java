package ntu.quy65132908.smartgym_ai.data.model;

import java.util.HashMap;
import java.util.Map;

public class User {
    private String uid;
    private String displayName;
    private String email;
    private String photoUrl;
    private String beforePhotoUrl;
    private String afterPhotoUrl;
    private Float weight;
    private Float height;
    private Float bmi;
    private String bmiCategory;
    private String goal;
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
    public String getBeforePhotoUrl() { return beforePhotoUrl; }
    public String getAfterPhotoUrl() { return afterPhotoUrl; }
    public Float getWeight() { return weight; }
    public Float getHeight() { return height; }
    public Float getBmi() { return bmi; }
    public String getBmiCategory() { return bmiCategory != null ? bmiCategory : "Bình thường"; }
    public String getGoal() { return goal; }
    public long getCreatedAt() { return createdAt; }

    // Setters
    public void setUid(String uid) { this.uid = uid; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public void setBeforePhotoUrl(String beforePhotoUrl) { this.beforePhotoUrl = beforePhotoUrl; }
    public void setAfterPhotoUrl(String afterPhotoUrl) { this.afterPhotoUrl = afterPhotoUrl; }
    public void setWeight(Float weight) { this.weight = weight; }
    public void setHeight(Float height) { this.height = height; }
    public void setBmi(Float bmi) { this.bmi = bmi; }
    public void setBmiCategory(String bmiCategory) { this.bmiCategory = bmiCategory; }
    public void setGoal(String goal) { this.goal = goal; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (uid != null) map.put("uid", uid);
        if (displayName != null) map.put("displayName", displayName);
        if (email != null) map.put("email", email);
        if (photoUrl != null) map.put("photoUrl", photoUrl);
        if (beforePhotoUrl != null) map.put("beforePhotoUrl", beforePhotoUrl);
        if (afterPhotoUrl != null) map.put("afterPhotoUrl", afterPhotoUrl);
        if (weight != null) map.put("weight", weight);
        if (height != null) map.put("height", height);
        if (bmi != null) map.put("bmi", bmi);
        if (bmiCategory != null) map.put("bmiCategory", bmiCategory);
        if (goal != null) map.put("goal", goal);
        map.put("createdAt", createdAt);
        return map;
    }
}
