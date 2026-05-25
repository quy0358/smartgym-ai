package ntu.quy65132908.smartgym_ai.ui.profile;

import ntu.quy65132908.smartgym_ai.data.model.User;

public class EditProfileUiState {
    private final User user;
    private final Float previewBmi;
    private final String previewBmiCategory;
    private final boolean loading;
    private final boolean saving;
    private final boolean loggedOut;
    private final boolean dirty;
    private final boolean valid;

    public EditProfileUiState(User user,
                              Float previewBmi,
                              String previewBmiCategory,
                              boolean loading,
                              boolean saving,
                              boolean loggedOut,
                              boolean dirty,
                              boolean valid) {
        this.user = user;
        this.previewBmi = previewBmi;
        this.previewBmiCategory = previewBmiCategory != null ? previewBmiCategory : "";
        this.loading = loading;
        this.saving = saving;
        this.loggedOut = loggedOut;
        this.dirty = dirty;
        this.valid = valid;
    }

    public static EditProfileUiState initial() {
        return new EditProfileUiState(null, null, "", true, false, false, false, false);
    }

    public User getUser() { return user; }
    public Float getPreviewBmi() { return previewBmi; }
    public String getPreviewBmiCategory() { return previewBmiCategory; }
    public boolean isLoading() { return loading; }
    public boolean isSaving() { return saving; }
    public boolean isLoggedOut() { return loggedOut; }
    public boolean isDirty() { return dirty; }
    public boolean isValid() { return valid; }
    public boolean canSave() { return dirty && valid && !loading && !saving && !loggedOut; }
}
