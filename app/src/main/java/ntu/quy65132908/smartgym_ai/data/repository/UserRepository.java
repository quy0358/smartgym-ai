package ntu.quy65132908.smartgym_ai.data.repository;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import javax.inject.Inject;
import javax.inject.Singleton;

import ntu.quy65132908.smartgym_ai.data.model.User;

@Singleton
public class UserRepository {

    private final FirebaseFirestore firestore;

    @Inject
    public UserRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public void getUser(String uid, UserCallback callback) {
        if (isBlank(uid)) {
            callback.onError(new IllegalArgumentException("uid is required"));
            return;
        }
        firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setUid(uid);
                            callback.onSuccess(user);
                        } else {
                            callback.onError(new Exception("User data is null"));
                        }
                    } else {
                        callback.onError(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void updateUser(String uid, User user, SimpleCallback callback) {
        if (isBlank(uid)) {
            callback.onError(new IllegalArgumentException("uid is required"));
            return;
        }
        if (user == null) {
            callback.onError(new IllegalArgumentException("user is required"));
            return;
        }
        firestore.collection("users").document(uid)
                .set(user.toMap(), SetOptions.merge())
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public interface UserCallback {
        void onSuccess(User user);
        void onError(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(Exception e);
    }
}
