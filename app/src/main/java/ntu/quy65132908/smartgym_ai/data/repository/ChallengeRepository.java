package ntu.quy65132908.smartgym_ai.data.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import ntu.quy65132908.smartgym_ai.data.model.Challenge;
import ntu.quy65132908.smartgym_ai.data.model.ChallengeProgress;

@Singleton
public class ChallengeRepository {
    private static final SimpleDateFormat DATE_KEY_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private final FirebaseFirestore firestore;

    @Inject
    public ChallengeRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public void joinChallenge(String uid, Challenge challenge, SimpleCallback callback) {
        if (challenge == null || challenge.getId() == null || challenge.getId().trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Thử thách không hợp lệ."));
            return;
        }
        firestore.collection("users").document(uid).collection("challenges")
                .document(challenge.getId())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        callback.onSuccess();
                        return;
                    }
                    ChallengeProgress progress = ChallengeProgress.forChallenge(challenge, System.currentTimeMillis());
                    progress.setId(challenge.getId());
                    firestore.collection("users").document(uid)
                            .collection("challenges").document(challenge.getId())
                            .set(progress)
                            .addOnSuccessListener(v -> callback.onSuccess())
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getChallengeProgressList(String uid, ChallengeProgressCallback callback) {
        firestore.collection("users").document(uid)
                .collection("challenges")
                .get()
                .addOnSuccessListener(snap -> {
                    List<ChallengeProgress> progressList = new ArrayList<>();
                    if (snap != null) {
                        for (QueryDocumentSnapshot doc : snap) {
                            ChallengeProgress progress = doc.toObject(ChallengeProgress.class);
                            progress.setId(doc.getId());
                            if (progress.getChallengeId() == null || progress.getChallengeId().trim().isEmpty()) {
                                progress.setChallengeId(doc.getId());
                            }
                            progressList.add(progress);
                        }
                    }
                    callback.onSuccess(progressList);
                })
                .addOnFailureListener(callback::onError);
    }

    public void recordWorkoutCompletion(String uid, SimpleCallback callback) {
        firestore.collection("users").document(uid)
                .collection("challenges")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap == null || snap.isEmpty()) {
                        callback.onSuccess();
                        return;
                    }
                    WriteBatch batch = firestore.batch();
                    boolean hasUpdates = false;
                    long completedAt = System.currentTimeMillis();
                    for (QueryDocumentSnapshot doc : snap) {
                        ChallengeProgress current = doc.toObject(ChallengeProgress.class);
                        current.setId(doc.getId());
                        if (current.getChallengeId() == null || current.getChallengeId().trim().isEmpty()) {
                            current.setChallengeId(doc.getId());
                        }
                        ChallengeProgress updated = applyWorkoutCompletion(current, completedAt);
                        if (updated != null && updated.getCompletedDays() != current.getCompletedDays()) {
                            batch.set(doc.getReference(), updated);
                            hasUpdates = true;
                        }
                    }
                    if (!hasUpdates) {
                        callback.onSuccess();
                        return;
                    }
                    batch.commit()
                            .addOnSuccessListener(v -> callback.onSuccess())
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public static ChallengeProgress applyWorkoutCompletion(ChallengeProgress progress, long completedAt) {
        if (progress == null) {
            return null;
        }
        ChallengeProgress updated = copyProgress(progress);
        String dateKey = DATE_KEY_FORMAT.format(new Date(completedAt));
        List<String> dates = updated.getCompletedDateKeys() != null
                ? new ArrayList<>(updated.getCompletedDateKeys())
                : new ArrayList<>();
        if (!dates.contains(dateKey) && updated.getCompletedDays() < updated.getTargetDays()) {
            dates.add(dateKey);
            updated.setCompletedDays(Math.min(updated.getTargetDays(), updated.getCompletedDays() + 1));
        }
        updated.setCompletedDateKeys(dates);
        updated.setUpdatedAt(completedAt);
        updated.setCompleted(updated.getTargetDays() > 0 && updated.getCompletedDays() >= updated.getTargetDays());
        return updated;
    }

    public static List<Challenge> defaultChallenges() {
        List<Challenge> challenges = new ArrayList<>();
        challenges.add(new Challenge("move_7", "7 ngày vận động", "Tập ít nhất 20 phút mỗi ngày.", 7, 20));
        challenges.add(new Challenge("strength_14", "14 ngày khỏe hơn", "Hoàn thành bài sức mạnh nhẹ và trung bình.", 14, 25));
        challenges.add(new Challenge("habit_30", "30 ngày bền bỉ", "Duy trì thói quen tập luyện và phục hồi.", 30, 20));
        return challenges;
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public interface ChallengeProgressCallback {
        void onSuccess(List<ChallengeProgress> progressList);
        void onError(Exception e);
    }

    private static ChallengeProgress copyProgress(ChallengeProgress progress) {
        ChallengeProgress copy = new ChallengeProgress();
        copy.setId(progress.getId());
        copy.setChallengeId(progress.getChallengeId());
        copy.setTitle(progress.getTitle());
        copy.setTargetDays(progress.getTargetDays());
        copy.setCompletedDays(progress.getCompletedDays());
        copy.setDailyMinutes(progress.getDailyMinutes());
        copy.setCompleted(progress.isCompleted());
        copy.setStartedAt(progress.getStartedAt());
        copy.setUpdatedAt(progress.getUpdatedAt());
        copy.setCompletedDateKeys(progress.getCompletedDateKeys() != null
                ? new ArrayList<>(progress.getCompletedDateKeys())
                : new ArrayList<>());
        return copy;
    }
}
