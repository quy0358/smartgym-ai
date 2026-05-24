package ntu.quy65132908.smartgym_ai.data.repository;

import com.google.firebase.firestore.FirebaseFirestore;

import javax.inject.Inject;
import javax.inject.Singleton;

import ntu.quy65132908.smartgym_ai.data.model.InjuryProfile;

@Singleton
public class InjuryProfileRepository {
    private final FirebaseFirestore firestore;

    @Inject
    public InjuryProfileRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public void saveInjuryProfile(String uid, InjuryProfile profile, SimpleCallback callback) {
        firestore.collection("users").document(uid)
                .collection("injuryProfile").document("current")
                .set(profile)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void getInjuryProfile(String uid, InjuryProfileCallback callback) {
        firestore.collection("users").document(uid)
                .collection("injuryProfile").document("current")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        callback.onSuccess(doc.toObject(InjuryProfile.class));
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public interface InjuryProfileCallback {
        void onSuccess(InjuryProfile profile);
        void onError(Exception e);
    }
}
