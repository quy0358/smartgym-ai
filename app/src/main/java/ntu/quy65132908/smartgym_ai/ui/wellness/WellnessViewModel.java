package ntu.quy65132908.smartgym_ai.ui.wellness;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Challenge;
import ntu.quy65132908.smartgym_ai.data.model.ChallengeProgress;
import ntu.quy65132908.smartgym_ai.data.model.InjuryProfile;
import ntu.quy65132908.smartgym_ai.data.model.Reminder;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ChallengeRepository;
import ntu.quy65132908.smartgym_ai.data.repository.InjuryProfileRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ReminderRepository;
import ntu.quy65132908.smartgym_ai.util.ReminderScheduler;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

@HiltViewModel
public class WellnessViewModel extends ViewModel {
    private final Context appContext;
    private final AuthRepository authRepository;
    private final ReminderRepository reminderRepository;
    private final InjuryProfileRepository injuryProfileRepository;
    private final ChallengeRepository challengeRepository;
    private final ReminderScheduler reminderScheduler;

    private final List<Challenge> defaultChallenges = ChallengeRepository.defaultChallenges();
    private final MutableLiveData<WellnessUiState> uiState = new MutableLiveData<>();
    private final MutableLiveData<WellnessFormErrors> formErrors = new MutableLiveData<>(WellnessFormErrors.none());
    private final SingleLiveEvent<String> message = new SingleLiveEvent<>();
    private final SingleLiveEvent<Reminder> reminderReadyToScheduleEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Reminder> notificationPermissionRequestEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> injuryProfileSavedEvent = new SingleLiveEvent<>();

    private Reminder currentReminder;
    private InjuryProfile currentInjuryProfile = new InjuryProfile();
    private List<ChallengeProgress> currentChallengeProgress = new ArrayList<>();
    private boolean loadingReminder;
    private boolean loadingInjury;
    private boolean loadingChallenges;
    private String savingTarget = WellnessUiState.SAVING_NONE;

    @Inject
    public WellnessViewModel(@ApplicationContext Context appContext,
                             AuthRepository authRepository,
                             ReminderRepository reminderRepository,
                             InjuryProfileRepository injuryProfileRepository,
                             ChallengeRepository challengeRepository,
                             ReminderScheduler reminderScheduler) {
        this.appContext = appContext;
        this.authRepository = authRepository;
        this.reminderRepository = reminderRepository;
        this.injuryProfileRepository = injuryProfileRepository;
        this.challengeRepository = challengeRepository;
        this.reminderScheduler = reminderScheduler;
        this.currentReminder = defaultReminder();
        publishState();
        loadSavedState();
    }

    public LiveData<WellnessUiState> getUiState() { return uiState; }
    public LiveData<WellnessFormErrors> getFormErrors() { return formErrors; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<Reminder> getReminderReadyToScheduleEvent() { return reminderReadyToScheduleEvent; }
    public LiveData<Reminder> getNotificationPermissionRequestEvent() { return notificationPermissionRequestEvent; }
    public LiveData<Boolean> getInjuryProfileSavedEvent() { return injuryProfileSavedEvent; }

    public void refresh() {
        loadSavedState();
    }

    public void saveReminder(String title, String time, boolean enabled, List<Integer> daysOfWeek) {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            message.setValue(appContext.getString(R.string.wellness_reminder_login_required));
            return;
        }
        int[] parsed = parseTime(time);
        WellnessFormErrors errors = buildReminderErrors(title, parsed, enabled, daysOfWeek);
        formErrors.setValue(errors);
        if (errors.hasReminderErrors()) {
            return;
        }
        Reminder reminder = new Reminder(
                "training",
                title.trim(),
                parsed[0],
                parsed[1],
                enabled,
                enabled ? sanitizeDays(daysOfWeek) : new ArrayList<>()
        );
        setSavingTarget(WellnessUiState.SAVING_REMINDER);
        reminderRepository.upsertReminder(user.getUid(), reminder, new ReminderRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                currentReminder = reminder;
                setSavingTarget(WellnessUiState.SAVING_NONE);
                reminderReadyToScheduleEvent.postValue(reminder);
                message.postValue(appContext.getString(R.string.wellness_reminder_saved));
            }

            @Override
            public void onError(Exception e) {
                setSavingTarget(WellnessUiState.SAVING_NONE);
                message.postValue(e.getMessage() != null ? e.getMessage() : appContext.getString(R.string.wellness_reminder_save_error));
            }
        });
    }

    public void requestNotificationPermissionFor(Reminder reminder) {
        if (reminder != null) {
            notificationPermissionRequestEvent.setValue(reminder);
        }
    }

    public void scheduleReminder(Reminder reminder) {
        if (reminder == null || !reminder.isEnabled()) {
            reminderScheduler.cancel(appContext, reminder != null ? reminder.getId() : "training");
            return;
        }
        reminderScheduler.schedule(appContext, reminder);
    }

    public void saveInjuryProfile(boolean knee, boolean shoulder, boolean back, String notes) {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            message.setValue(appContext.getString(R.string.wellness_injury_login_required));
            return;
        }
        WellnessFormErrors errors = buildInjuryErrors(notes);
        formErrors.setValue(errors);
        if (errors.hasInjuryErrors()) {
            return;
        }
        InjuryProfile profile = new InjuryProfile();
        profile.setKneeSensitive(knee);
        profile.setShoulderSensitive(shoulder);
        profile.setLowerBackSensitive(back);
        profile.setNotes(notes != null ? notes.trim() : "");
        setSavingTarget(WellnessUiState.SAVING_INJURY);
        injuryProfileRepository.saveInjuryProfile(user.getUid(), profile, new InjuryProfileRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                currentInjuryProfile = profile;
                setSavingTarget(WellnessUiState.SAVING_NONE);
                injuryProfileSavedEvent.postValue(true);
            }

            @Override
            public void onError(Exception e) {
                setSavingTarget(WellnessUiState.SAVING_NONE);
                message.postValue(appContext.getString(R.string.wellness_injury_save_error));
            }
        });
    }

    public void joinChallenge(Challenge challenge) {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            message.setValue(appContext.getString(R.string.wellness_challenge_login_required));
            return;
        }
        if (challenge == null) {
            return;
        }
        setSavingTarget(WellnessUiState.SAVING_CHALLENGE);
        challengeRepository.joinChallenge(user.getUid(), challenge, new ChallengeRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                setSavingTarget(WellnessUiState.SAVING_NONE);
                loadChallenges(user.getUid());
                message.postValue(appContext.getString(R.string.wellness_challenge_joined_format, challenge.getTitle()));
            }

            @Override
            public void onError(Exception e) {
                setSavingTarget(WellnessUiState.SAVING_NONE);
                message.postValue(appContext.getString(R.string.wellness_challenge_join_error));
            }
        });
    }

    private int[] parseTime(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.trim().split(":");
        if (parts.length != 2) {
            return null;
        }
        try {
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return null;
            }
            return new int[]{hour, minute};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void loadSavedState() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            currentReminder = defaultReminder();
            currentInjuryProfile = new InjuryProfile();
            currentChallengeProgress = new ArrayList<>();
            publishState();
            return;
        }
        String uid = user.getUid();
        loadReminder(uid);
        loadInjuryProfile(uid);
        loadChallenges(uid);
    }

    private void loadReminder(String uid) {
        loadingReminder = true;
        publishState();
        reminderRepository.getReminder(uid, "training", new ReminderRepository.ReminderCallback() {
            @Override
            public void onSuccess(Reminder reminder) {
                currentReminder = reminder != null ? reminder : defaultReminder();
                loadingReminder = false;
                publishState();
            }

            @Override
            public void onError(Exception e) {
                loadingReminder = false;
                message.postValue(appContext.getString(R.string.wellness_reminder_load_error));
                publishState();
            }
        });
    }

    private void loadInjuryProfile(String uid) {
        loadingInjury = true;
        publishState();
        injuryProfileRepository.getInjuryProfile(uid, new InjuryProfileRepository.InjuryProfileCallback() {
            @Override
            public void onSuccess(InjuryProfile profile) {
                currentInjuryProfile = profile != null ? profile : new InjuryProfile();
                loadingInjury = false;
                publishState();
            }

            @Override
            public void onError(Exception e) {
                loadingInjury = false;
                message.postValue(appContext.getString(R.string.wellness_injury_load_error));
                publishState();
            }
        });
    }

    private void loadChallenges(String uid) {
        loadingChallenges = true;
        publishState();
        challengeRepository.getChallengeProgressList(uid, new ChallengeRepository.ChallengeProgressCallback() {
            @Override
            public void onSuccess(List<ChallengeProgress> progressList) {
                currentChallengeProgress = progressList != null ? progressList : new ArrayList<>();
                loadingChallenges = false;
                publishState();
            }

            @Override
            public void onError(Exception e) {
                loadingChallenges = false;
                message.postValue(appContext.getString(R.string.wellness_challenge_load_error));
                publishState();
            }
        });
    }

    private void publishState() {
        uiState.postValue(new WellnessUiState(
                currentReminder,
                currentInjuryProfile,
                buildChallengeItems(),
                loadingReminder || loadingInjury || loadingChallenges,
                authRepository.getCurrentUser() == null,
                savingTarget
        ));
    }

    private List<ChallengeDisplayItem> buildChallengeItems() {
        Map<String, ChallengeProgress> progressByChallenge = new HashMap<>();
        for (ChallengeProgress progress : currentChallengeProgress) {
            if (progress != null && progress.getChallengeId() != null) {
                progressByChallenge.put(progress.getChallengeId(), progress);
            }
        }
        List<ChallengeDisplayItem> items = new ArrayList<>();
        for (Challenge challenge : defaultChallenges) {
            items.add(new ChallengeDisplayItem(challenge, progressByChallenge.get(challenge.getId())));
        }
        return items;
    }

    private void setSavingTarget(String target) {
        savingTarget = target != null ? target : WellnessUiState.SAVING_NONE;
        publishState();
    }

    private Reminder defaultReminder() {
        return new Reminder(
                "training",
                appContext.getString(R.string.wellness_default_reminder),
                6,
                30,
                true,
                Arrays.asList(1, 2, 3, 4, 5, 6, 7)
        );
    }

    private WellnessFormErrors buildReminderErrors(String title, int[] parsedTime, boolean enabled, List<Integer> daysOfWeek) {
        String titleError = null;
        String timeError = null;
        String daysError = null;
        String safeTitle = title != null ? title.trim() : "";
        if (safeTitle.isEmpty()) {
            titleError = appContext.getString(R.string.wellness_reminder_title_error);
        } else if (safeTitle.length() > 80) {
            titleError = appContext.getString(R.string.wellness_reminder_title_length_error);
        }
        if (parsedTime == null) {
            timeError = appContext.getString(R.string.wellness_reminder_time_error);
        }
        if (enabled && sanitizeDays(daysOfWeek).isEmpty()) {
            daysError = appContext.getString(R.string.wellness_reminder_days_error);
        }
        return new WellnessFormErrors(titleError, timeError, daysError, formErrors.getValue() != null
                ? formErrors.getValue().getInjuryNotesError()
                : null);
    }

    private WellnessFormErrors buildInjuryErrors(String notes) {
        String notesError = null;
        if (notes != null && notes.trim().length() > 500) {
            notesError = appContext.getString(R.string.wellness_injury_notes_length_error);
        }
        WellnessFormErrors current = formErrors.getValue();
        return new WellnessFormErrors(
                current != null ? current.getReminderTitleError() : null,
                current != null ? current.getReminderTimeError() : null,
                current != null ? current.getReminderDaysError() : null,
                notesError
        );
    }

    private List<Integer> sanitizeDays(List<Integer> daysOfWeek) {
        List<Integer> sanitized = new ArrayList<>();
        if (daysOfWeek == null) {
            return sanitized;
        }
        for (Integer day : daysOfWeek) {
            if (day != null && day >= 1 && day <= 7 && !sanitized.contains(day)) {
                sanitized.add(day);
            }
        }
        return sanitized;
    }
}
