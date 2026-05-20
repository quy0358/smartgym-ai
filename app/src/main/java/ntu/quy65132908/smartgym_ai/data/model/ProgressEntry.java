package ntu.quy65132908.smartgym_ai.data.model;

public class ProgressEntry {
    private String id;
    private String userId;
    private float weight;
    private Float bodyFat;
    private Float leanMass;
    private long date;
    private String note;

    public ProgressEntry() {}

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public float getWeight() { return weight; }
    public Float getBodyFat() { return bodyFat; }
    public Float getLeanMass() { return leanMass; }
    public long getDate() { return date; }
    public String getNote() { return note; }

    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setWeight(float weight) { this.weight = weight; }
    public void setBodyFat(Float bodyFat) { this.bodyFat = bodyFat; }
    public void setLeanMass(Float leanMass) { this.leanMass = leanMass; }
    public void setDate(long date) { this.date = date; }
    public void setNote(String note) { this.note = note; }
}
