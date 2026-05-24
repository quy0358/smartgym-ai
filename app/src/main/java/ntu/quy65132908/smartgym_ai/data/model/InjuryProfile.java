package ntu.quy65132908.smartgym_ai.data.model;

public class InjuryProfile {
    private boolean kneeSensitive;
    private boolean shoulderSensitive;
    private boolean lowerBackSensitive;
    private String notes;

    public InjuryProfile() {}

    public boolean isKneeSensitive() { return kneeSensitive; }
    public boolean isShoulderSensitive() { return shoulderSensitive; }
    public boolean isLowerBackSensitive() { return lowerBackSensitive; }
    public String getNotes() { return notes; }

    public void setKneeSensitive(boolean kneeSensitive) { this.kneeSensitive = kneeSensitive; }
    public void setShoulderSensitive(boolean shoulderSensitive) { this.shoulderSensitive = shoulderSensitive; }
    public void setLowerBackSensitive(boolean lowerBackSensitive) { this.lowerBackSensitive = lowerBackSensitive; }
    public void setNotes(String notes) { this.notes = notes; }
}
