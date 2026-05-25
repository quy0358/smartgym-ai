package ntu.quy65132908.smartgym_ai.ui.progress;

public class ProgressFormErrors {
    private final String weightError;
    private final String bodyFatError;
    private final String leanMassError;
    private final String noteError;

    public ProgressFormErrors(String weightError,
                              String bodyFatError,
                              String leanMassError,
                              String noteError) {
        this.weightError = weightError;
        this.bodyFatError = bodyFatError;
        this.leanMassError = leanMassError;
        this.noteError = noteError;
    }

    public String getWeightError() { return weightError; }
    public String getBodyFatError() { return bodyFatError; }
    public String getLeanMassError() { return leanMassError; }
    public String getNoteError() { return noteError; }

    public boolean hasErrors() {
        return weightError != null
                || bodyFatError != null
                || leanMassError != null
                || noteError != null;
    }

    public static ProgressFormErrors none() {
        return new ProgressFormErrors(null, null, null, null);
    }
}
