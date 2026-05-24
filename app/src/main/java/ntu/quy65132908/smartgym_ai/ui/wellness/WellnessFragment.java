package ntu.quy65132908.smartgym_ai.ui.wellness;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.InjuryProfile;
import ntu.quy65132908.smartgym_ai.data.model.Reminder;
import ntu.quy65132908.smartgym_ai.databinding.FragmentWellnessBinding;

@AndroidEntryPoint
public class WellnessFragment extends Fragment {
    private FragmentWellnessBinding binding;
    private WellnessViewModel viewModel;
    private ChallengeAdapter challengeAdapter;
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private Reminder pendingReminderForPermission;
    private boolean bindingState;
    private String lastReminderSignature = "";
    private String lastInjurySignature = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    Reminder pending = pendingReminderForPermission;
                    pendingReminderForPermission = null;
                    if (pending == null || viewModel == null) {
                        return;
                    }
                    if (Boolean.TRUE.equals(granted)) {
                        viewModel.scheduleReminder(pending);
                    } else {
                        showSnackbar(getString(R.string.wellness_notification_permission_denied));
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWellnessBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(WellnessViewModel.class);
        challengeAdapter = new ChallengeAdapter(challenge -> viewModel.joinChallenge(challenge));
        binding.rvChallenges.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvChallenges.setAdapter(challengeAdapter);
        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        binding.etReminderTime.setOnClickListener(v -> showTimePicker());
        binding.tilReminderTime.setEndIconOnClickListener(v -> showTimePicker());
        binding.switchReminderEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!bindingState) {
                setReminderDayControlsEnabled(isChecked);
            }
        });

        binding.btnSaveReminder.setOnClickListener(v ->
                viewModel.saveReminder(
                        text(binding.etReminderTitle),
                        text(binding.etReminderTime),
                        binding.switchReminderEnabled.isChecked(),
                        selectedReminderDays()));
        binding.btnSaveInjuryProfile.setOnClickListener(v -> viewModel.saveInjuryProfile(
                binding.cbKnee.isChecked(),
                binding.cbShoulder.isChecked(),
                binding.cbBack.isChecked(),
                text(binding.etInjuryNotes)
        ));
        observe();
    }

    private void observe() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getFormErrors().observe(getViewLifecycleOwner(), this::renderErrors);
        viewModel.getReminderReadyToScheduleEvent().observe(getViewLifecycleOwner(), this::handleReminderScheduleRequest);
        viewModel.getNotificationPermissionRequestEvent().observe(getViewLifecycleOwner(), reminder -> {
            if (notificationPermissionLauncher != null) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        });
        viewModel.getMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                showSnackbar(msg);
            }
        });
    }

    private void render(WellnessUiState state) {
        if (state == null || binding == null) {
            return;
        }
        binding.progressBar.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
        binding.btnSaveReminder.setEnabled(!state.isSavingReminder());
        binding.btnSaveInjuryProfile.setEnabled(!state.isSavingInjury());
        binding.btnSaveReminder.setText(state.isSavingReminder()
                ? R.string.wellness_saving
                : R.string.wellness_save_reminder);
        binding.btnSaveInjuryProfile.setText(state.isSavingInjury()
                ? R.string.wellness_saving
                : R.string.wellness_save_injury);

        String reminderSignature = signature(state.getReminder());
        if (!state.isSavingReminder() && !reminderSignature.equals(lastReminderSignature)) {
            bindReminder(state.getReminder());
            lastReminderSignature = reminderSignature;
        }
        String injurySignature = signature(state.getInjuryProfile());
        if (!state.isSavingInjury() && !injurySignature.equals(lastInjurySignature)) {
            bindInjuryProfile(state.getInjuryProfile());
            lastInjurySignature = injurySignature;
        }
        challengeAdapter.submitList(state.getChallengeItems());
    }

    private void bindReminder(Reminder reminder) {
        if (reminder == null || binding == null) {
            return;
        }
        bindingState = true;
        binding.etReminderTitle.setText(reminder.getTitle() != null ? reminder.getTitle() : "");
        binding.etReminderTime.setText(formatTime(reminder.getHour(), reminder.getMinute()));
        binding.switchReminderEnabled.setChecked(reminder.isEnabled());
        setSelectedReminderDays(reminder.getDaysOfWeek());
        setReminderDayControlsEnabled(reminder.isEnabled());
        bindingState = false;
    }

    private void bindInjuryProfile(InjuryProfile profile) {
        if (profile == null || binding == null) {
            return;
        }
        bindingState = true;
        binding.cbKnee.setChecked(profile.isKneeSensitive());
        binding.cbShoulder.setChecked(profile.isShoulderSensitive());
        binding.cbBack.setChecked(profile.isLowerBackSensitive());
        binding.etInjuryNotes.setText(profile.getNotes() != null ? profile.getNotes() : "");
        bindingState = false;
    }

    private void renderErrors(WellnessFormErrors errors) {
        if (errors == null || binding == null) {
            return;
        }
        binding.tilReminderTitle.setError(errors.getReminderTitleError());
        binding.tilReminderTime.setError(errors.getReminderTimeError());
        binding.tilInjuryNotes.setError(errors.getInjuryNotesError());

        String daysError = errors.getReminderDaysError();
        binding.tvReminderDaysError.setText(daysError);
        binding.tvReminderDaysError.setVisibility(daysError != null ? View.VISIBLE : View.GONE);
    }

    private void handleReminderScheduleRequest(Reminder reminder) {
        if (reminder == null) {
            return;
        }
        if (!reminder.isEnabled()) {
            viewModel.scheduleReminder(reminder);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            pendingReminderForPermission = reminder;
            viewModel.requestNotificationPermissionFor(reminder);
            return;
        }
        viewModel.scheduleReminder(reminder);
    }

    private void showTimePicker() {
        int[] current = parseTime(text(binding.etReminderTime));
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(current[0])
                .setMinute(current[1])
                .setTitleText(getString(R.string.wellness_time_picker_title))
                .build();
        picker.addOnPositiveButtonClickListener(v ->
                binding.etReminderTime.setText(formatTime(picker.getHour(), picker.getMinute())));
        picker.show(getParentFragmentManager(), "wellness_time_picker");
    }

    private int[] parseTime(String value) {
        if (value == null) {
            return new int[]{6, 30};
        }
        String[] parts = value.trim().split(":");
        if (parts.length != 2) {
            return new int[]{6, 30};
        }
        try {
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return new int[]{6, 30};
            }
            return new int[]{hour, minute};
        } catch (NumberFormatException e) {
            return new int[]{6, 30};
        }
    }

    private List<Integer> selectedReminderDays() {
        List<Integer> days = new ArrayList<>();
        if (binding == null) {
            return days;
        }
        for (Integer buttonId : binding.toggleReminderDays.getCheckedButtonIds()) {
            int day = dayForButtonId(buttonId);
            if (day > 0 && !days.contains(day)) {
                days.add(day);
            }
        }
        return days;
    }

    private void setSelectedReminderDays(List<Integer> days) {
        if (binding == null) {
            return;
        }
        binding.toggleReminderDays.clearChecked();
        if (days == null) {
            return;
        }
        for (Integer day : days) {
            int buttonId = buttonIdForDay(day != null ? day : -1);
            if (buttonId != View.NO_ID) {
                binding.toggleReminderDays.check(buttonId);
            }
        }
    }

    private int dayForButtonId(int buttonId) {
        if (buttonId == R.id.btn_day_mon) return 1;
        if (buttonId == R.id.btn_day_tue) return 2;
        if (buttonId == R.id.btn_day_wed) return 3;
        if (buttonId == R.id.btn_day_thu) return 4;
        if (buttonId == R.id.btn_day_fri) return 5;
        if (buttonId == R.id.btn_day_sat) return 6;
        if (buttonId == R.id.btn_day_sun) return 7;
        return -1;
    }

    private int buttonIdForDay(int day) {
        switch (day) {
            case 1: return R.id.btn_day_mon;
            case 2: return R.id.btn_day_tue;
            case 3: return R.id.btn_day_wed;
            case 4: return R.id.btn_day_thu;
            case 5: return R.id.btn_day_fri;
            case 6: return R.id.btn_day_sat;
            case 7: return R.id.btn_day_sun;
            default: return View.NO_ID;
        }
    }

    private void setReminderDayControlsEnabled(boolean enabled) {
        binding.tvReminderDaysLabel.setEnabled(enabled);
        binding.toggleReminderDays.setEnabled(enabled);
        binding.btnDayMon.setEnabled(enabled);
        binding.btnDayTue.setEnabled(enabled);
        binding.btnDayWed.setEnabled(enabled);
        binding.btnDayThu.setEnabled(enabled);
        binding.btnDayFri.setEnabled(enabled);
        binding.btnDaySat.setEnabled(enabled);
        binding.btnDaySun.setEnabled(enabled);
    }

    private String formatTime(int hour, int minute) {
        return String.format(Locale.US, "%02d:%02d", hour, minute);
    }

    private String signature(Reminder reminder) {
        if (reminder == null) {
            return "";
        }
        return reminder.getTitle() + "|"
                + reminder.getHour() + "|"
                + reminder.getMinute() + "|"
                + reminder.isEnabled() + "|"
                + reminder.getDaysOfWeek();
    }

    private String signature(InjuryProfile profile) {
        if (profile == null) {
            return "";
        }
        return profile.isKneeSensitive() + "|"
                + profile.isShoulderSensitive() + "|"
                + profile.isLowerBackSensitive() + "|"
                + profile.getNotes();
    }

    private void showSnackbar(String message) {
        if (binding != null) {
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    private String text(com.google.android.material.textfield.TextInputEditText input) {
        return input.getText() != null ? input.getText().toString() : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
