package ntu.quy65132908.smartgym_ai.data.repository;

import android.net.Uri;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.util.InputValidator;

@Singleton
public class AuthRepository {

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    @Inject
    public AuthRepository(FirebaseAuth auth, FirebaseFirestore firestore) {
        this.auth = auth;
        this.firestore = firestore;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public void getCurrentUserDisplayName(DisplayNameCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onSuccess("");
            return;
        }

        String fallbackName = InputValidator.sanitizeName(user.getDisplayName());
        firestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String profileName = snapshot.exists() ? snapshot.getString("displayName") : "";
                    String sanitizedProfileName = InputValidator.sanitizeName(profileName);
                    callback.onSuccess(!sanitizedProfileName.isEmpty() ? sanitizedProfileName : fallbackName);
                })
                .addOnFailureListener(e -> {
                    if (!fallbackName.isEmpty()) {
                        callback.onSuccess(fallbackName);
                    } else {
                        callback.onError(e);
                    }
                });
    }

    public void signIn(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user != null) {
                        callback.onSuccess(user);
                    } else {
                        callback.onError(new IllegalStateException("Firebase sign-in returned no user"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void signUp(String name, String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser != null) {
                        // Tạo hồ sơ người dùng trong Firestore.
                        User user = new User(firebaseUser.getUid(), name, email);
                        firestore.collection("users")
                                .document(firebaseUser.getUid())
                                .set(user.toMap())
                                .addOnSuccessListener(v -> callback.onSuccess(firebaseUser))
                                .addOnFailureListener(callback::onError);
                    } else {
                        callback.onError(new IllegalStateException("Firebase sign-up returned no user"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void signInWithGoogle(String idToken, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user != null) {
                        ensureGoogleProfile(user, callback);
                    } else {
                        callback.onError(new IllegalStateException("Google sign-in returned no user"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    private void ensureGoogleProfile(FirebaseUser user, AuthCallback callback) {
        firestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        callback.onSuccess(user);
                        return;
                    }

                    firestore.collection("users")
                            .document(user.getUid())
                            .set(buildGoogleUserProfile(user), SetOptions.merge())
                            .addOnSuccessListener(v -> callback.onSuccess(user))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    static Map<String, Object> buildGoogleUserProfile(FirebaseUser firebaseUser) {
        String name = InputValidator.sanitizeName(firebaseUser.getDisplayName());
        if (name.isEmpty()) {
            name = "Người dùng";
        }
        String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
        User user = new User(firebaseUser.getUid(), name, email);
        Uri photoUri = firebaseUser.getPhotoUrl();
        if (photoUri != null) {
            user.setPhotoUrl(photoUri.toString());
        }
        return user.toMap();
    }

    public void sendPasswordResetEmail(String email, SimpleCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void signOut() {
        auth.signOut();
    }

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public interface DisplayNameCallback {
        void onSuccess(String displayName);
        void onError(Exception e);
    }
}
