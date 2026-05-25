package ntu.quy65132908.smartgym_ai.ui.profile;

public class ProfileFormErrors {
    private final String displayNameError;
    private final String weightError;
    private final String heightError;
    private final String goalError;

    public ProfileFormErrors(String displayNameError,
                             String weightError,
                             String heightError,
                             String goalError) {
        this.displayNameError = displayNameError;
        this.weightError = weightError;
        this.heightError = heightError;
        this.goalError = goalError;
    }

    public String getDisplayNameError() { return displayNameError; }
    public String getWeightError() { return weightError; }
    public String getHeightError() { return heightError; }
    public String getGoalError() { return goalError; }

    public boolean hasErrors() {
        return displayNameError != null
                || weightError != null
                || heightError != null
                || goalError != null;
    }

    public static ProfileFormErrors none() {
        return new ProfileFormErrors(null, null, null, null);
    }
}
