package ntu.quy65132908.smartgym_ai.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.Uri;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
public class AuthRepositoryTest {

    @Test
    public void buildGoogleUserProfile_usesFirebaseUserFields() {
        FirebaseUser firebaseUser = googleUser("uid-1", "Quy", "quy@example.com", "https://example.com/avatar.png");

        Map<String, Object> profile = AuthRepository.buildGoogleUserProfile(firebaseUser);

        assertEquals("uid-1", profile.get("uid"));
        assertEquals("Quy", profile.get("displayName"));
        assertEquals("quy@example.com", profile.get("email"));
        assertEquals("https://example.com/avatar.png", profile.get("photoUrl"));
    }

    @Test
    public void buildGoogleUserProfile_emptyName_usesDefaultName() {
        FirebaseUser firebaseUser = googleUser("uid-1", "", "quy@example.com", null);

        Map<String, Object> profile = AuthRepository.buildGoogleUserProfile(firebaseUser);

        assertEquals("Người dùng", profile.get("displayName"));
    }

    @Test
    public void signInWithGoogle_missingProfile_createsMergedProfile() {
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference users = mock(CollectionReference.class);
        DocumentReference userDocument = mock(DocumentReference.class);
        DocumentSnapshot missingSnapshot = mock(DocumentSnapshot.class);
        AuthResult authResult = mock(AuthResult.class);
        FirebaseUser firebaseUser = googleUser("uid-1", "Quy", "quy@example.com", null);

        when(auth.signInWithCredential(any())).thenReturn(Tasks.forResult(authResult));
        when(authResult.getUser()).thenReturn(firebaseUser);
        when(firestore.collection("users")).thenReturn(users);
        when(users.document("uid-1")).thenReturn(userDocument);
        when(userDocument.get()).thenReturn(Tasks.forResult(missingSnapshot));
        when(missingSnapshot.exists()).thenReturn(false);
        when(userDocument.set(anyMap(), any(SetOptions.class))).thenReturn(Tasks.forResult(null));

        AuthRepository repository = new AuthRepository(auth, firestore);
        AtomicReference<FirebaseUser> success = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();

        repository.signInWithGoogle("id-token", callback(success, error));
        drainMainLooper();

        ArgumentCaptor<Map<String, Object>> profileCaptor = ArgumentCaptor.forClass(Map.class);
        verify(userDocument).set(profileCaptor.capture(), any(SetOptions.class));
        assertEquals("uid-1", profileCaptor.getValue().get("uid"));
        assertSame(firebaseUser, success.get());
        assertNull(error.get());
    }

    @Test
    public void signInWithGoogle_existingProfile_doesNotOverwrite() {
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference users = mock(CollectionReference.class);
        DocumentReference userDocument = mock(DocumentReference.class);
        DocumentSnapshot existingSnapshot = mock(DocumentSnapshot.class);
        AuthResult authResult = mock(AuthResult.class);
        FirebaseUser firebaseUser = googleUser("uid-1", "Google Name", "quy@example.com", null);

        when(auth.signInWithCredential(any())).thenReturn(Tasks.forResult(authResult));
        when(authResult.getUser()).thenReturn(firebaseUser);
        when(firestore.collection("users")).thenReturn(users);
        when(users.document("uid-1")).thenReturn(userDocument);
        when(userDocument.get()).thenReturn(Tasks.forResult(existingSnapshot));
        when(existingSnapshot.exists()).thenReturn(true);

        AuthRepository repository = new AuthRepository(auth, firestore);
        AtomicReference<FirebaseUser> success = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();

        repository.signInWithGoogle("id-token", callback(success, error));
        drainMainLooper();

        verify(userDocument, never()).set(any(), any(SetOptions.class));
        assertSame(firebaseUser, success.get());
        assertNull(error.get());
    }

    private static AuthRepository.AuthCallback callback(
            AtomicReference<FirebaseUser> success,
            AtomicReference<Exception> error) {
        return new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                success.set(user);
            }

            @Override
            public void onError(Exception e) {
                error.set(e);
            }
        };
    }

    private static void drainMainLooper() {
        ShadowLooper.idleMainLooper();
        ShadowLooper.idleMainLooper();
        ShadowLooper.idleMainLooper();
    }

    private static FirebaseUser googleUser(String uid, String name, String email, String photoUrl) {
        FirebaseUser firebaseUser = mock(FirebaseUser.class);
        when(firebaseUser.getUid()).thenReturn(uid);
        when(firebaseUser.getDisplayName()).thenReturn(name);
        when(firebaseUser.getEmail()).thenReturn(email);
        when(firebaseUser.getPhotoUrl()).thenReturn(photoUrl != null ? Uri.parse(photoUrl) : null);
        return firebaseUser;
    }
}
