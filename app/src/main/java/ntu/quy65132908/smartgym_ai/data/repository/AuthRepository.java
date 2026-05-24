package ntu.quy65132908.smartgym_ai.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import javax.inject.Inject;
import javax.inject.Singleton;

import ntu.quy65132908.smartgym_ai.data.model.User;

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
                        // Create user profile in Firestore
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
                        // Check if new user and create Firestore profile
                        if (result.getAdditionalUserInfo() != null && result.getAdditionalUserInfo().isNewUser()) {
                            String name = user.getDisplayName() != null ? user.getDisplayName() : "Người dùng";
                            String email = user.getEmail() != null ? user.getEmail() : "";
                            User newUser = new User(user.getUid(), name, email);
                            firestore.collection("users").document(user.getUid())
                                    .set(newUser.toMap())
                                    .addOnSuccessListener(v -> callback.onSuccess(user))
                                    .addOnFailureListener(callback::onError);
                        } else {
                            callback.onSuccess(user);
                        }
                    } else {
                        callback.onError(new IllegalStateException("Google sign-in returned no user"));
                    }
                })
                .addOnFailureListener(callback::onError);
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
}
