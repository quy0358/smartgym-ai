package ntu.quy65132908.smartgym_ai.ui.profile;

public class ProfileUiState {
    private final String displayName;
    private final String email;
    private final String photoUrl;
    private final int totalWorkouts;
    private final float totalHours;
    private final int streakDays;
    private final boolean loading;
    private final boolean loggedOut;
    private final boolean profileLoadFailed;
    private final boolean statsLoadFailed;

    public ProfileUiState(String displayName,
                          String email,
                          String photoUrl,
                          int totalWorkouts,
                          float totalHours,
                          int streakDays,
                          boolean loading,
                          boolean loggedOut,
                          boolean profileLoadFailed,
                          boolean statsLoadFailed) {
        this.displayName = displayName != null ? displayName : "";
        this.email = email != null ? email : "";
        this.photoUrl = photoUrl != null ? photoUrl : "";
        this.totalWorkouts = totalWorkouts;
        this.totalHours = totalHours;
        this.streakDays = streakDays;
        this.loading = loading;
        this.loggedOut = loggedOut;
        this.profileLoadFailed = profileLoadFailed;
        this.statsLoadFailed = statsLoadFailed;
    }

    public static ProfileUiState initial() {
        return new ProfileUiState("", "", "", 0, 0f, 0, true, false, false, false);
    }

    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getPhotoUrl() { return photoUrl; }
    public int getTotalWorkouts() { return totalWorkouts; }
    public float getTotalHours() { return totalHours; }
    public int getStreakDays() { return streakDays; }
    public boolean isLoading() { return loading; }
    public boolean isLoggedOut() { return loggedOut; }
    public boolean isProfileLoadFailed() { return profileLoadFailed; }
    public boolean isStatsLoadFailed() { return statsLoadFailed; }
}
