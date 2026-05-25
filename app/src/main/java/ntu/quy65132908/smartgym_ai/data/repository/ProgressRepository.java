package ntu.quy65132908.smartgym_ai.data.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
        firestore.collection("users").document(uid).collection("progress").add(data)
                .addOnSuccessListener(r -> cb.onSuccess()).addOnFailureListener(cb::onError);
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
