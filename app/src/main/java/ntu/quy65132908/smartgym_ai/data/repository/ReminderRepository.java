package ntu.quy65132908.smartgym_ai.data.repository;

import com.google.firebase.firestore.FirebaseFirestore;

import javax.inject.Inject;
import javax.inject.Singleton;

import ntu.quy65132908.smartgym_ai.data.model.Reminder;

@Singleton
public class ReminderRepository {
    private final FirebaseFirestore firestore;

    @Inject
    public ReminderRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public void upsertReminder(String uid, Reminder reminder, SimpleCallback callback) {
        String validation = validateReminder(reminder);
        if (validation != null) {
            callback.onError(new IllegalArgumentException(validation));
            return;
        }
        String id = reminder.getId() != null && !reminder.getId().trim().isEmpty()
                ? reminder.getId()
                : "default";
        firestore.collection("users").document(uid)
                .collection("reminders").document(id)
                .set(reminder)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void getReminder(String uid, String reminderId, ReminderCallback callback) {
        String id = reminderId != null && !reminderId.trim().isEmpty() ? reminderId : "default";
        firestore.collection("users").document(uid)
                .collection("reminders").document(id)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        Reminder reminder = doc.toObject(Reminder.class);
                        if (reminder != null) {
                            reminder.setId(doc.getId());
                        }
                        callback.onSuccess(reminder);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public static String validateReminder(Reminder reminder) {
        if (reminder == null) {
            return "Nhắc lịch không hợp lệ.";
        }
        if (reminder.getTitle() == null || reminder.getTitle().trim().isEmpty()) {
            return "Tên nhắc lịch không được để trống.";
        }
        if (reminder.getTitle().trim().length() > 80) {
            return "Tên nhắc lịch tối đa 80 ký tự.";
        }
        if (reminder.getHour() < 0 || reminder.getHour() > 23) {
            return "giờ nhắc lịch phải từ 0 đến 23.";
        }
        if (reminder.getMinute() < 0 || reminder.getMinute() > 59) {
            return "Phút nhắc lịch phải từ 0 đến 59.";
        }
        if (reminder.isEnabled() && (reminder.getDaysOfWeek() == null || reminder.getDaysOfWeek().isEmpty())) {
            return "Chọn ít nhất một ngày để bật nhắc lịch.";
        }
        if (reminder.isEnabled()) {
            for (Integer day : reminder.getDaysOfWeek()) {
                if (day == null || day < 1 || day > 7) {
                    return "Ngày nhắc lịch phải từ 1 đến 7.";
                }
            }
        }
        return null;
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public interface ReminderCallback {
        void onSuccess(Reminder reminder);
        void onError(Exception e);
    }
}
