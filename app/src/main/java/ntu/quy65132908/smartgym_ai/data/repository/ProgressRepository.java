package ntu.quy65132908.smartgym_ai.data.repository;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import ntu.quy65132908.smartgym_ai.data.model.ProgressEntry;

@Singleton
public class ProgressRepository {
    private final FirebaseFirestore firestore;

    @Inject
    public ProgressRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public void getHistory(String uid, ProgressCallback cb) {
        firestore.collection("users").document(uid).collection("progress")
                .orderBy("date", Query.Direction.DESCENDING).limit(30).get()
                .addOnSuccessListener(snap -> {
                    List<ProgressEntry> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        ProgressEntry e = doc.toObject(ProgressEntry.class);
                        e.setId(doc.getId());
                        list.add(e);
                    }
                    cb.onSuccess(list);
                }).addOnFailureListener(cb::onError);
    }

    public void addEntry(String uid, ProgressEntry entry, SimpleCallback cb) {
        if (uid == null || uid.trim().isEmpty()) {
            cb.onError(new IllegalArgumentException("uid is required"));
            return;
        }
        if (entry == null) {
            cb.onError(new IllegalArgumentException("progress entry is required"));
            return;
        }

        DocumentReference userRef = firestore.collection("users").document(uid);
        userRef.get(Source.DEFAULT)
                .addOnSuccessListener(userDoc -> saveEntryAndProfileMetrics(uid, userRef, userDoc, entry, cb))
                .addOnFailureListener(error -> saveEntryAndProfileMetrics(uid, userRef, null, entry, cb));
    }

    private void saveEntryAndProfileMetrics(String uid,
                                            DocumentReference userRef,
                                            DocumentSnapshot userDoc,
                                            ProgressEntry entry,
                                            SimpleCallback cb) {
        Map<String, Object> data = new HashMap<>();
        data.put("weight", entry.getWeight());
        data.put("date", entry.getDate());
        data.put("userId", uid);
        if (entry.getBodyFat() != null) {
            data.put("bodyFat", entry.getBodyFat());
        }
        if (entry.getLeanMass() != null) {
            data.put("leanMass", entry.getLeanMass());
        }
        if (entry.getNote() != null && !entry.getNote().trim().isEmpty()) {
            data.put("note", entry.getNote());
        }

        Map<String, Object> profileMetrics = new HashMap<>();
        profileMetrics.put("weight", entry.getWeight());
        Float height = readFloat(userDoc, "height");
        Float bmi = calculateBmiOrNull(entry.getWeight(), height);
        if (bmi != null) {
            profileMetrics.put("bmi", bmi);
            profileMetrics.put("bmiCategory", categoryForBmi(bmi));
        }

        DocumentReference progressRef = userRef.collection("progress").document();
        progressRef.set(data)
                .addOnSuccessListener(r -> {
                    cb.onSuccess();
                    userRef.set(profileMetrics, SetOptions.merge());
                })
                .addOnFailureListener(cb::onError);
    }

    static Float calculateBmiOrNull(float weightKg, Float heightCm) {
        if (heightCm == null || heightCm <= 0f || weightKg <= 0f) {
            return null;
        }
        float heightM = heightCm / 100f;
        return weightKg / (heightM * heightM);
    }

    static String categoryForBmi(float bmi) {
        if (bmi < 18.5f) {
            return "Thiếu cân";
        }
        if (bmi < 25f) {
            return "Bình thường";
        }
        if (bmi < 30f) {
            return "Thừa cân";
        }
        return "Béo phì";
    }

    private static Float readFloat(DocumentSnapshot document, String field) {
        if (document == null || !document.exists()) {
            return null;
        }
        Double value = document.getDouble(field);
        return value != null ? value.floatValue() : null;
    }

    public interface ProgressCallback {
        void onSuccess(List<ProgressEntry> e);
        void onError(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(Exception e);
    }
}
