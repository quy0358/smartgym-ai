# SmartGym AI Full Project Cleanup — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix 31 identified issues across 4 phases — critical crashes, security holes, architecture debt, missing features, and UX polish.

**Architecture:** Android MVVM + Hilt DI + Firebase + Gemini AI SDK + Navigation Component.

**Tech Stack:** Java 17, Android SDK 35, Hilt 2.59, Firebase BOM 33.14, Gemini AI 0.9, AndroidX Navigation 2.9

**Design Spec:** `docs/superpowers/specs/2026-05-21-smartgym-full-cleanup-design.md`

---

## Phase 1: Critical + Security Fixes

### Task 1: Fix Set.of() API Level Crash

**Files:**
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/MainActivity.java:46-50`

- [ ] **Step 1: Replace Set.of() with HashSet**

Add imports at top of `MainActivity.java`:
```java
import java.util.Arrays;
import java.util.HashSet;
```

Replace line 48:
```java
// BEFORE:
Set<Integer> mainDestinations = Set.of(
        R.id.nav_dashboard, R.id.nav_workout, R.id.nav_progress,
        R.id.nav_community, R.id.nav_profile
);

// AFTER:
Set<Integer> mainDestinations = new HashSet<>(Arrays.asList(
        R.id.nav_dashboard, R.id.nav_workout, R.id.nav_progress,
        R.id.nav_community, R.id.nav_profile
));
```

- [ ] **Step 2: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/ntu/quy65132908/smartgym_ai/MainActivity.java
git commit -m "fix: replace Set.of() with HashSet for API 24 compatibility"
```

---

### Task 2: Create SingleLiveEvent + Fix LiveData Re-trigger

**Files:**
- Create: `app/src/main/java/ntu/quy65132908/smartgym_ai/util/SingleLiveEvent.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/auth/AuthViewModel.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/profile/ProfileViewModel.java`

- [ ] **Step 1: Create SingleLiveEvent**

Create `app/src/main/java/ntu/quy65132908/smartgym_ai/util/SingleLiveEvent.java`:
```java
package ntu.quy65132908.smartgym_ai.util;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

public class SingleLiveEvent<T> extends MutableLiveData<T> {
    private final AtomicBoolean pending = new AtomicBoolean(false);

    @MainThread
    @Override
    public void setValue(T value) {
        pending.set(true);
        super.setValue(value);
    }

    @Override
    public void postValue(T value) {
        pending.set(true);
        super.postValue(value);
    }

    @MainThread
    @Override
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        super.observe(owner, t -> {
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t);
            }
        });
    }
}
```

- [ ] **Step 2: Update AuthViewModel**

In `AuthViewModel.java`, add `import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;` and replace:
```java
// BEFORE:
private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
private final MutableLiveData<FirebaseUser> authSuccess = new MutableLiveData<>();

// AFTER:
private final SingleLiveEvent<String> errorMessage = new SingleLiveEvent<>();
private final SingleLiveEvent<FirebaseUser> authSuccess = new SingleLiveEvent<>();
```

- [ ] **Step 3: Update ProfileViewModel**

In `ProfileViewModel.java`, add `import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;` and replace:
```java
// BEFORE:
private final MutableLiveData<Boolean> signedOut = new MutableLiveData<>(false);

// AFTER:
private final SingleLiveEvent<Boolean> signedOut = new SingleLiveEvent<>();
```

- [ ] **Step 4: Verify build and commit**

Run: `./gradlew assembleDebug`

```bash
git add -A && git commit -m "fix: add SingleLiveEvent to prevent re-trigger on rotation"
```

---

### Task 3: Fix Sign-Out Navigation Backstack

**Files:**
- Modify: `app/src/main/res/navigation/nav_graph.xml`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/profile/ProfileFragment.java`

- [ ] **Step 1: Add global action to nav_graph.xml**

Add before closing `</navigation>`:
```xml
<action
    android:id="@+id/action_global_to_login"
    app:destination="@id/nav_login"
    app:popUpTo="@id/nav_graph"
    app:popUpToInclusive="true" />
```

- [ ] **Step 2: Update ProfileFragment**

Replace the `getSignedOut()` observer:
```java
viewModel.getSignedOut().observe(getViewLifecycleOwner(), signedOut -> {
    if (signedOut != null && signedOut) {
        Navigation.findNavController(requireView())
                .navigate(R.id.action_global_to_login);
    }
});
```

- [ ] **Step 3: Verify build and commit**

Run: `./gradlew assembleDebug`

```bash
git add -A && git commit -m "fix: clear backstack on sign-out"
```

---

### Task 4: GeminiKeyProvider + InputValidator + Security

**Files:**
- Create: `app/src/main/java/ntu/quy65132908/smartgym_ai/util/GeminiKeyProvider.java`
- Create: `app/src/main/java/ntu/quy65132908/smartgym_ai/util/InputValidator.java`
- Create: `app/src/test/java/ntu/quy65132908/smartgym_ai/util/InputValidatorTest.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/di/AppModule.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/auth/AuthViewModel.java`

- [ ] **Step 1: Create GeminiKeyProvider**

Create `app/src/main/java/ntu/quy65132908/smartgym_ai/util/GeminiKeyProvider.java`:
```java
package ntu.quy65132908.smartgym_ai.util;

import javax.inject.Inject;
import javax.inject.Singleton;

import ntu.quy65132908.smartgym_ai.BuildConfig;

@Singleton
public class GeminiKeyProvider {
    @Inject
    public GeminiKeyProvider() {}

    public String getApiKey() {
        return BuildConfig.GEMINI_API_KEY;
    }
}
```

- [ ] **Step 2: Write InputValidator test**

Create `app/src/test/java/ntu/quy65132908/smartgym_ai/util/InputValidatorTest.java`:
```java
package ntu.quy65132908.smartgym_ai.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class InputValidatorTest {
    @Test
    public void sanitizeName_trimsWhitespace() {
        assertEquals("John", InputValidator.sanitizeName("  John  "));
    }
    @Test
    public void sanitizeName_limitsTo50Chars() {
        String longName = new String(new char[100]).replace('\0', 'A');
        assertEquals(50, InputValidator.sanitizeName(longName).length());
    }
    @Test
    public void sanitizeName_stripsHtml() {
        assertEquals("alert", InputValidator.sanitizeName("<script>alert</script>"));
    }
    @Test
    public void sanitizeName_nullReturnsEmpty() {
        assertEquals("", InputValidator.sanitizeName(null));
    }
    @Test
    public void sanitizeContent_limitsTo500() {
        String s = new String(new char[600]).replace('\0', 'B');
        assertEquals(500, InputValidator.sanitizeContent(s).length());
    }
    @Test
    public void isValidName_emptyReturnsFalse() {
        assertFalse(InputValidator.isValidName(""));
        assertFalse(InputValidator.isValidName("   "));
        assertFalse(InputValidator.isValidName(null));
    }
    @Test
    public void isValidName_validReturnsTrue() {
        assertTrue(InputValidator.isValidName("Nguyen Van A"));
    }
}
```

- [ ] **Step 3: Create InputValidator**

Create `app/src/main/java/ntu/quy65132908/smartgym_ai/util/InputValidator.java`:
```java
package ntu.quy65132908.smartgym_ai.util;

import android.util.Patterns;

public final class InputValidator {
    private static final int MAX_NAME = 50;
    private static final int MAX_CONTENT = 500;

    private InputValidator() {}

    public static String sanitizeName(String input) {
        if (input == null) return "";
        String s = input.trim().replaceAll("<[^>]*>", "");
        return s.length() > MAX_NAME ? s.substring(0, MAX_NAME) : s;
    }

    public static String sanitizeContent(String input) {
        if (input == null) return "";
        String s = input.trim();
        return s.length() > MAX_CONTENT ? s.substring(0, MAX_CONTENT) : s;
    }

    public static boolean isValidName(String input) {
        return input != null && !input.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
```

- [ ] **Step 4: Run tests**

Run: `./gradlew test --tests "ntu.quy65132908.smartgym_ai.util.InputValidatorTest"`
Expected: ALL PASS

- [ ] **Step 5: Apply in AuthViewModel and AppModule**

In `AuthViewModel.signUp()`, sanitize name before use:
```java
import ntu.quy65132908.smartgym_ai.util.InputValidator;

// In signUp method, first line:
String sanitizedName = InputValidator.sanitizeName(name);
// Then check: if (!InputValidator.isValidName(sanitizedName) || ...)
// Pass sanitizedName to authRepository.signUp(sanitizedName, email, password, ...)
```

In `AppModule.java`, add provider:
```java
import ntu.quy65132908.smartgym_ai.util.GeminiKeyProvider;

@Provides @Singleton
public GeminiKeyProvider provideGeminiKeyProvider() { return new GeminiKeyProvider(); }
```

- [ ] **Step 6: Verify build and commit**

Run: `./gradlew assembleDebug`

```bash
git add -A && git commit -m "feat: add GeminiKeyProvider + InputValidator with TDD"
```

---

## Phase 2: Architecture Foundation

### Task 5: Create WorkoutRepository + ProgressRepository

**Files:**
- Create: `app/src/main/java/ntu/quy65132908/smartgym_ai/data/repository/WorkoutRepository.java`
- Create: `app/src/main/java/ntu/quy65132908/smartgym_ai/data/repository/ProgressRepository.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/data/model/Workout.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/di/AppModule.java`

- [ ] **Step 1: Add dayOfWeek to Workout model**

In `Workout.java`, add:
```java
private int dayOfWeek;
public int getDayOfWeek() { return dayOfWeek; }
public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
```

- [ ] **Step 2: Create WorkoutRepository**

Create `app/src/main/java/ntu/quy65132908/smartgym_ai/data/repository/WorkoutRepository.java`:
```java
package ntu.quy65132908.smartgym_ai.data.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import ntu.quy65132908.smartgym_ai.data.model.Exercise;
import ntu.quy65132908.smartgym_ai.data.model.Workout;

@Singleton
public class WorkoutRepository {
    private final FirebaseFirestore firestore;

    @Inject
    public WorkoutRepository(FirebaseFirestore firestore) { this.firestore = firestore; }

    public void getWeeklyPlan(String uid, WorkoutListCallback cb) {
        firestore.collection("users").document(uid).collection("workouts")
                .orderBy("dayOfWeek").get()
                .addOnSuccessListener(snap -> {
                    List<Workout> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) { Workout w = doc.toObject(Workout.class); w.setId(doc.getId()); list.add(w); }
                    cb.onSuccess(list);
                }).addOnFailureListener(cb::onError);
    }

    public void getExercises(String uid, String workoutId, ExerciseListCallback cb) {
        firestore.collection("users").document(uid).collection("workouts").document(workoutId)
                .collection("exercises").get()
                .addOnSuccessListener(snap -> {
                    List<Exercise> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) { Exercise e = doc.toObject(Exercise.class); e.setId(doc.getId()); list.add(e); }
                    cb.onSuccess(list);
                }).addOnFailureListener(cb::onError);
    }

    public void markExerciseComplete(String uid, String wId, String eId, boolean done, SimpleCallback cb) {
        firestore.collection("users").document(uid).collection("workouts").document(wId)
                .collection("exercises").document(eId).update("isCompleted", done)
                .addOnSuccessListener(v -> cb.onSuccess()).addOnFailureListener(cb::onError);
    }

    public void saveWorkout(String uid, Workout w, SimpleCallback cb) {
        firestore.collection("users").document(uid).collection("workouts").add(w)
                .addOnSuccessListener(r -> cb.onSuccess()).addOnFailureListener(cb::onError);
    }

    public interface WorkoutListCallback { void onSuccess(List<Workout> w); void onError(Exception e); }
    public interface ExerciseListCallback { void onSuccess(List<Exercise> e); void onError(Exception e); }
    public interface SimpleCallback { void onSuccess(); void onError(Exception e); }
}
```

- [ ] **Step 3: Create ProgressRepository**

Create `app/src/main/java/ntu/quy65132908/smartgym_ai/data/repository/ProgressRepository.java`:
```java
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
    public ProgressRepository(FirebaseFirestore firestore) { this.firestore = firestore; }

    public void getHistory(String uid, ProgressCallback cb) {
        firestore.collection("users").document(uid).collection("progress")
                .orderBy("date", Query.Direction.DESCENDING).limit(30).get()
                .addOnSuccessListener(snap -> {
                    List<ProgressEntry> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) { ProgressEntry e = doc.toObject(ProgressEntry.class); e.setId(doc.getId()); list.add(e); }
                    cb.onSuccess(list);
                }).addOnFailureListener(cb::onError);
    }

    public void addEntry(String uid, ProgressEntry entry, SimpleCallback cb) {
        Map<String, Object> data = new HashMap<>();
        data.put("weight", entry.getWeight()); data.put("bodyFat", entry.getBodyFat());
        data.put("leanMass", entry.getLeanMass()); data.put("date", entry.getDate());
        data.put("note", entry.getNote()); data.put("userId", uid);
        firestore.collection("users").document(uid).collection("progress").add(data)
                .addOnSuccessListener(r -> cb.onSuccess()).addOnFailureListener(cb::onError);
    }

    public interface ProgressCallback { void onSuccess(List<ProgressEntry> e); void onError(Exception e); }
    public interface SimpleCallback { void onSuccess(); void onError(Exception e); }
}
```

- [ ] **Step 4: Register in AppModule**

```java
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ProgressRepository;

@Provides @Singleton
public WorkoutRepository provideWorkoutRepository(FirebaseFirestore fs) { return new WorkoutRepository(fs); }

@Provides @Singleton
public ProgressRepository provideProgressRepository(FirebaseFirestore fs) { return new ProgressRepository(fs); }
```

- [ ] **Step 5: Verify build and commit**

Run: `./gradlew assembleDebug`

```bash
git add -A && git commit -m "feat: add WorkoutRepository + ProgressRepository"
```

---

### Task 6: Create CommunityRepository + GeminiRepository

**Files:**
- Create: `app/src/main/java/ntu/quy65132908/smartgym_ai/data/repository/CommunityRepository.java`
- Create: `app/src/main/java/ntu/quy65132908/smartgym_ai/data/repository/GeminiRepository.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/data/model/Post.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/di/AppModule.java`

- [ ] **Step 1: Add likedBy to Post model**

In `Post.java`, add:
```java
import java.util.ArrayList;
import java.util.List;

private List<String> likedBy;
public List<String> getLikedBy() { return likedBy != null ? likedBy : new ArrayList<>(); }
public void setLikedBy(List<String> likedBy) { this.likedBy = likedBy; }
```

- [ ] **Step 2: Create CommunityRepository**

Create `app/src/main/java/ntu/quy65132908/smartgym_ai/data/repository/CommunityRepository.java`:
```java
package ntu.quy65132908.smartgym_ai.data.repository;

import com.google.firebase.firestore.*;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import ntu.quy65132908.smartgym_ai.data.model.Post;

@Singleton
public class CommunityRepository {
    private final FirebaseFirestore firestore;
    private ListenerRegistration listener;

    @Inject
    public CommunityRepository(FirebaseFirestore firestore) { this.firestore = firestore; }

    public void listenToPosts(PostsCallback cb) {
        removeListener();
        listener = firestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(50)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) { cb.onError(err); return; }
                    if (snap != null) {
                        List<Post> posts = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snap) { Post p = doc.toObject(Post.class); p.setId(doc.getId()); posts.add(p); }
                        cb.onSuccess(posts);
                    }
                });
    }

    public void createPost(String authorId, String authorName, String content, SimpleCallback cb) {
        Map<String, Object> data = new HashMap<>();
        data.put("authorId", authorId); data.put("authorName", authorName);
        data.put("content", content); data.put("likes", 0);
        data.put("likedBy", new ArrayList<>()); data.put("createdAt", System.currentTimeMillis());
        firestore.collection("posts").add(data)
                .addOnSuccessListener(r -> cb.onSuccess()).addOnFailureListener(cb::onError);
    }

    public void toggleLike(String postId, String uid, boolean isLiked, SimpleCallback cb) {
        Map<String, Object> updates = new HashMap<>();
        if (isLiked) { updates.put("likedBy", FieldValue.arrayRemove(uid)); updates.put("likes", FieldValue.increment(-1)); }
        else { updates.put("likedBy", FieldValue.arrayUnion(uid)); updates.put("likes", FieldValue.increment(1)); }
        firestore.collection("posts").document(postId).update(updates)
                .addOnSuccessListener(v -> cb.onSuccess()).addOnFailureListener(cb::onError);
    }

    public void removeListener() { if (listener != null) { listener.remove(); listener = null; } }

    public interface PostsCallback { void onSuccess(List<Post> posts); void onError(Exception e); }
    public interface SimpleCallback { void onSuccess(); void onError(Exception e); }
}
```

- [ ] **Step 3: Create GeminiRepository**

Create `app/src/main/java/ntu/quy65132908/smartgym_ai/data/repository/GeminiRepository.java`:
```java
package ntu.quy65132908.smartgym_ai.data.repository;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.util.GeminiKeyProvider;

@Singleton
public class GeminiRepository {
    private final GenerativeModelFutures model;

    @Inject
    public GeminiRepository(GeminiKeyProvider keyProvider) {
        GenerativeModel gm = new GenerativeModel("gemini-2.0-flash", keyProvider.getApiKey());
        this.model = GenerativeModelFutures.from(gm);
    }

    public void generateWorkoutPlan(User user, String goal, AiCallback cb) {
        StringBuilder p = new StringBuilder("Bạn là huấn luyện viên thể hình. Tạo kế hoạch 7 ngày:\n");
        if (user.getWeight() != null) p.append("- Cân nặng: ").append(user.getWeight()).append("kg\n");
        if (user.getHeight() != null) p.append("- Chiều cao: ").append(user.getHeight()).append("cm\n");
        if (user.getBmi() != null) p.append("- BMI: ").append(user.getBmi()).append("\n");
        p.append("- Mục tiêu: ").append(goal != null ? goal : "tăng cơ").append("\nTrả lời tiếng Việt, Markdown.");
        callGemini(p.toString(), cb);
    }

    public void analyzeForm(String exercise, String description, AiCallback cb) {
        String p = "Phân tích form bài \"" + exercise + "\": \"" + description + "\"\n" +
                "Cho: 1)Điểm đúng 2)Lỗi 3)Cách sửa 4)Mẹo an toàn. Tiếng Việt, Markdown.";
        callGemini(p, cb);
    }

    private void callGemini(String prompt, AiCallback cb) {
        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> future = model.generateContent(content);
        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override public void onSuccess(GenerateContentResponse r) {
                String text = r.getText();
                cb.onSuccess(text != null ? text : "Không có phản hồi");
            }
            @Override public void onFailure(Throwable t) { cb.onError(new Exception(t)); }
        }, Executors.newSingleThreadExecutor());
    }

    public interface AiCallback { void onSuccess(String response); void onError(Exception e); }
}
```

- [ ] **Step 4: Register in AppModule**

```java
import ntu.quy65132908.smartgym_ai.data.repository.CommunityRepository;
import ntu.quy65132908.smartgym_ai.data.repository.GeminiRepository;

@Provides @Singleton
public CommunityRepository provideCommunityRepository(FirebaseFirestore fs) { return new CommunityRepository(fs); }

@Provides @Singleton
public GeminiRepository provideGeminiRepository(GeminiKeyProvider kp) { return new GeminiRepository(kp); }
```

- [ ] **Step 5: Remove firebase-database dependency**

In `gradle/libs.versions.toml`: remove `firebaseDatabase = "22.0.1"` line and `firebase-database` library entry.
In `app/build.gradle.kts`: remove `implementation(libs.firebase.database)`.

- [ ] **Step 6: Verify build and commit**

Run: `./gradlew assembleDebug`

```bash
git add -A && git commit -m "feat: add CommunityRepository + GeminiRepository, remove unused firebase-database"
```

---

### Task 7: Create ViewModels for All Screens

**Files:**
- Create: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/workout/WorkoutViewModel.java`
- Create: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/progress/ProgressViewModel.java`
- Create: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/community/CommunityViewModel.java`
- Create: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/analysis/AIAnalysisViewModel.java`

- [ ] **Step 1: Create WorkoutViewModel**

Create `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/workout/WorkoutViewModel.java`:
```java
package ntu.quy65132908.smartgym_ai.ui.workout;

import androidx.lifecycle.*;
import com.google.firebase.auth.FirebaseUser;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.model.Exercise;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;

@HiltViewModel
public class WorkoutViewModel extends ViewModel {
    private final WorkoutRepository workoutRepo;
    private final AuthRepository authRepo;
    private final MutableLiveData<List<Exercise>> exercises = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LiveData<List<Exercise>> getExercises() { return exercises; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    @Inject
    public WorkoutViewModel(WorkoutRepository workoutRepo, AuthRepository authRepo) {
        this.workoutRepo = workoutRepo;
        this.authRepo = authRepo;
    }

    public void loadExercises(String workoutId) {
        FirebaseUser u = authRepo.getCurrentUser();
        if (u == null) return;
        isLoading.setValue(true);
        workoutRepo.getExercises(u.getUid(), workoutId, new WorkoutRepository.ExerciseListCallback() {
            @Override public void onSuccess(List<Exercise> list) { isLoading.postValue(false); exercises.postValue(list); }
            @Override public void onError(Exception e) { isLoading.postValue(false); }
        });
    }
}
```

- [ ] **Step 2: Create ProgressViewModel**

Create `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/progress/ProgressViewModel.java`:
```java
package ntu.quy65132908.smartgym_ai.ui.progress;

import androidx.lifecycle.*;
import com.google.firebase.auth.FirebaseUser;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.model.ProgressEntry;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ProgressRepository;

@HiltViewModel
public class ProgressViewModel extends ViewModel {
    private final ProgressRepository progressRepo;
    private final AuthRepository authRepo;
    private final MutableLiveData<List<ProgressEntry>> entries = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LiveData<List<ProgressEntry>> getEntries() { return entries; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    @Inject
    public ProgressViewModel(ProgressRepository progressRepo, AuthRepository authRepo) {
        this.progressRepo = progressRepo; this.authRepo = authRepo;
        loadProgress();
    }

    public void loadProgress() {
        FirebaseUser u = authRepo.getCurrentUser();
        if (u == null) return;
        isLoading.setValue(true);
        progressRepo.getHistory(u.getUid(), new ProgressRepository.ProgressCallback() {
            @Override public void onSuccess(List<ProgressEntry> list) { isLoading.postValue(false); entries.postValue(list); }
            @Override public void onError(Exception e) { isLoading.postValue(false); }
        });
    }
}
```

- [ ] **Step 3: Create CommunityViewModel**

Create `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/community/CommunityViewModel.java`:
```java
package ntu.quy65132908.smartgym_ai.ui.community;

import androidx.lifecycle.*;
import com.google.firebase.auth.FirebaseUser;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.model.Post;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.CommunityRepository;
import ntu.quy65132908.smartgym_ai.util.InputValidator;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

@HiltViewModel
public class CommunityViewModel extends ViewModel {
    private final CommunityRepository communityRepo;
    private final AuthRepository authRepo;
    private final MutableLiveData<List<Post>> posts = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);
    private final SingleLiveEvent<String> error = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> postCreated = new SingleLiveEvent<>();

    public LiveData<List<Post>> getPosts() { return posts; }
    public LiveData<Boolean> getIsRefreshing() { return isRefreshing; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getPostCreated() { return postCreated; }

    @Inject
    public CommunityViewModel(CommunityRepository communityRepo, AuthRepository authRepo) {
        this.communityRepo = communityRepo; this.authRepo = authRepo;
        startListening();
    }

    private void startListening() {
        isRefreshing.setValue(true);
        communityRepo.listenToPosts(new CommunityRepository.PostsCallback() {
            @Override public void onSuccess(List<Post> list) { isRefreshing.postValue(false); posts.postValue(list); }
            @Override public void onError(Exception e) { isRefreshing.postValue(false); error.postValue(e.getMessage()); }
        });
    }

    public void createPost(String content) {
        FirebaseUser u = authRepo.getCurrentUser();
        if (u == null) return;
        String sanitized = InputValidator.sanitizeContent(content);
        if (sanitized.isEmpty()) { error.setValue("Nội dung trống"); return; }
        String name = u.getDisplayName() != null ? u.getDisplayName() : "Người dùng";
        communityRepo.createPost(u.getUid(), name, sanitized, new CommunityRepository.SimpleCallback() {
            @Override public void onSuccess() { postCreated.postValue(true); }
            @Override public void onError(Exception e) { error.postValue(e.getMessage()); }
        });
    }

    public void toggleLike(String postId, boolean isLiked) {
        FirebaseUser u = authRepo.getCurrentUser();
        if (u == null) return;
        communityRepo.toggleLike(postId, u.getUid(), isLiked, new CommunityRepository.SimpleCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) { error.postValue(e.getMessage()); }
        });
    }

    @Override protected void onCleared() { super.onCleared(); communityRepo.removeListener(); }
}
```

- [ ] **Step 4: Create AIAnalysisViewModel**

Create `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/analysis/AIAnalysisViewModel.java`:
```java
package ntu.quy65132908.smartgym_ai.ui.analysis;

import androidx.lifecycle.*;
import com.google.firebase.auth.FirebaseUser;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.repository.*;

@HiltViewModel
public class AIAnalysisViewModel extends ViewModel {
    private final GeminiRepository geminiRepo;
    private final UserRepository userRepo;
    private final AuthRepository authRepo;
    private final MutableLiveData<String> aiResponse = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMsg = new MutableLiveData<>();
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();

    public LiveData<String> getAiResponse() { return aiResponse; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMsg() { return errorMsg; }

    @Inject
    public AIAnalysisViewModel(GeminiRepository geminiRepo, UserRepository userRepo, AuthRepository authRepo) {
        this.geminiRepo = geminiRepo; this.userRepo = userRepo; this.authRepo = authRepo;
        loadUser();
    }

    private void loadUser() {
        FirebaseUser fb = authRepo.getCurrentUser();
        if (fb == null) return;
        userRepo.getUser(fb.getUid(), new UserRepository.UserCallback() {
            @Override public void onSuccess(User u) { currentUser.postValue(u); }
            @Override public void onError(Exception e) {}
        });
    }

    public void generateWorkoutPlan() {
        User u = currentUser.getValue();
        if (u == null) { errorMsg.setValue("Đang tải..."); return; }
        isLoading.setValue(true);
        geminiRepo.generateWorkoutPlan(u, u.getGoal(), new GeminiRepository.AiCallback() {
            @Override public void onSuccess(String r) { isLoading.postValue(false); aiResponse.postValue(r); }
            @Override public void onError(Exception e) { isLoading.postValue(false); errorMsg.postValue(e.getMessage()); }
        });
    }

    public void analyzeForm(String exercise, String desc) {
        if (exercise.trim().isEmpty() || desc.trim().isEmpty()) { errorMsg.setValue("Nhập đủ thông tin"); return; }
        isLoading.setValue(true);
        geminiRepo.analyzeForm(exercise, desc, new GeminiRepository.AiCallback() {
            @Override public void onSuccess(String r) { isLoading.postValue(false); aiResponse.postValue(r); }
            @Override public void onError(Exception e) { isLoading.postValue(false); errorMsg.postValue(e.getMessage()); }
        });
    }
}
```

- [ ] **Step 5: Verify build and commit**

Run: `./gradlew assembleDebug`

```bash
git add -A && git commit -m "feat: add WorkoutViewModel, ProgressViewModel, CommunityViewModel, AIAnalysisViewModel"
```

---

### Task 8: Refactor Adapters to ListAdapter + Wire Fragments

**Files:**
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/community/PostAdapter.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/workout/ExerciseAdapter.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/WeeklyPlanAdapter.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/community/CommunityFragment.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/workout/WorkoutDetailFragment.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/progress/ProgressFragment.java`

- [ ] **Step 1: Refactor PostAdapter**

Replace entire `PostAdapter.java` with ListAdapter pattern using `DiffUtil.ItemCallback<Post>`. Constructor takes `OnLikeClickListener` interface. Uses `Post` model instead of `String[][]`. See design spec section 2.3.

- [ ] **Step 2: Refactor ExerciseAdapter**

Replace entire `ExerciseAdapter.java` with ListAdapter pattern using `DiffUtil.ItemCallback<Exercise>`. Constructor takes `OnExerciseCheckedListener`. Formats detail as `sets × reps • weight kg`.

- [ ] **Step 3: Refactor WeeklyPlanAdapter**

Replace entire `WeeklyPlanAdapter.java` with ListAdapter pattern using `DiffUtil.ItemCallback<Workout>`. Computes today using `Calendar.DAY_OF_WEEK` instead of hardcoding position 0.

- [ ] **Step 4: Wire CommunityFragment to CommunityViewModel**

Update `CommunityFragment.java` to get `CommunityViewModel`, observe `posts` LiveData, and call `adapter.submitList()`.

- [ ] **Step 5: Wire WorkoutDetailFragment to WorkoutViewModel**

Update `WorkoutDetailFragment.java` to get `WorkoutViewModel`, observe `exercises`, and pass to adapter.

- [ ] **Step 6: Wire ProgressFragment to ProgressViewModel**

Update `ProgressFragment.java` to get `ProgressViewModel` and observe entries.

- [ ] **Step 7: Verify build and commit**

Run: `./gradlew assembleDebug`

```bash
git add -A && git commit -m "refactor: convert adapters to ListAdapter, wire fragments to ViewModels"
```

---

## Phase 3: Feature Implementation

### Task 9: Implement Gemini AI Chat in AIAnalysisFragment

**Files:**
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/analysis/AIAnalysisFragment.java`
- Modify: `app/src/main/res/layout/fragment_ai_analysis.xml`

- [ ] **Step 1: Update layout with AI input/output UI**

Add to `fragment_ai_analysis.xml`: input fields for exercise name and form description, buttons for "Generate Workout" and "Analyze Form", a ScrollView with TextView for AI response, and a ProgressBar for loading state.

- [ ] **Step 2: Wire AIAnalysisFragment to AIAnalysisViewModel**

Update `AIAnalysisFragment.java`:
```java
viewModel = new ViewModelProvider(this).get(AIAnalysisViewModel.class);

binding.btnGenerateWorkout.setOnClickListener(v -> viewModel.generateWorkoutPlan());
binding.btnAnalyzeForm.setOnClickListener(v -> {
    String exercise = binding.etExerciseName.getText().toString();
    String desc = binding.etFormDescription.getText().toString();
    viewModel.analyzeForm(exercise, desc);
});

viewModel.getAiResponse().observe(getViewLifecycleOwner(), response -> binding.tvAiResponse.setText(response));
viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
    binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    binding.btnGenerateWorkout.setEnabled(!loading);
    binding.btnAnalyzeForm.setEnabled(!loading);
});
viewModel.getErrorMsg().observe(getViewLifecycleOwner(), err -> { if (err != null) binding.tvAiResponse.setText(err); });
```

- [ ] **Step 3: Verify build and commit**

Run: `./gradlew assembleDebug`

```bash
git add -A && git commit -m "feat: implement Gemini AI workout generation and form analysis"
```

---

### Task 10: Implement Google Sign-In

**Files:**
- Create: `app/src/main/java/ntu/quy65132908/smartgym_ai/util/GoogleSignInHelper.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/data/repository/AuthRepository.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/auth/AuthViewModel.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/auth/LoginFragment.java`

- [ ] **Step 1: Create GoogleSignInHelper**

Create helper class that uses `CredentialManager` + `GetGoogleIdOption` to get Google ID token, then exchanges for Firebase credential via `GoogleAuthProvider.getCredential()`.

- [ ] **Step 2: Add signInWithGoogle to AuthRepository**

```java
public void signInWithGoogle(String idToken, AuthCallback callback) {
    AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
    auth.signInWithCredential(credential)
            .addOnSuccessListener(result -> {
                FirebaseUser user = result.getUser();
                if (user != null && result.getAdditionalUserInfo() != null && result.getAdditionalUserInfo().isNewUser()) {
                    User newUser = new User(user.getUid(), user.getDisplayName(), user.getEmail());
                    firestore.collection("users").document(user.getUid()).set(newUser.toMap());
                }
                callback.onSuccess(user);
            })
            .addOnFailureListener(callback::onError);
}
```

- [ ] **Step 3: Add signInWithGoogle to AuthViewModel**

- [ ] **Step 4: Wire LoginFragment Google button**

- [ ] **Step 5: Verify build and commit**

```bash
git add -A && git commit -m "feat: implement Google Sign-In via Credential Manager"
```

---

### Task 11: Implement Create Post Feature

**Files:**
- Create: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/community/CreatePostBottomSheet.java`
- Create: `app/src/main/res/layout/bottom_sheet_create_post.xml`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/community/CommunityFragment.java`

- [ ] **Step 1: Create bottom sheet layout**

`bottom_sheet_create_post.xml` with: EditText (multiline, maxLength 500), character counter, Post button.

- [ ] **Step 2: Create CreatePostBottomSheet**

BottomSheetDialogFragment that gets `CommunityViewModel` from parent fragment and calls `createPost()`.

- [ ] **Step 3: Wire FAB in CommunityFragment**

```java
binding.fabPost.setOnClickListener(v -> new CreatePostBottomSheet().show(getChildFragmentManager(), "create_post"));
```

- [ ] **Step 4: Verify build and commit**

```bash
git add -A && git commit -m "feat: implement create post bottom sheet"
```

---

### Task 12: Implement Edit Profile

**Files:**
- Create: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/profile/EditProfileFragment.java`
- Create: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/profile/EditProfileViewModel.java`
- Create: `app/src/main/res/layout/fragment_edit_profile.xml`
- Modify: `app/src/main/res/navigation/nav_graph.xml`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/profile/ProfileFragment.java`

- [ ] **Step 1: Create layout**

`fragment_edit_profile.xml` with EditText fields for: display name, weight, height, goal. Auto-calculated BMI display. Save/Cancel buttons.

- [ ] **Step 2: Create EditProfileViewModel**

Loads user data, validates input, calculates BMI (`weight / (height/100)^2`), saves via `UserRepository.updateUser()`.

- [ ] **Step 3: Create EditProfileFragment**

Wires layout to ViewModel, navigates back on save success.

- [ ] **Step 4: Add navigation destination and action**

In `nav_graph.xml`:
```xml
<fragment android:id="@+id/nav_edit_profile"
    android:name="ntu.quy65132908.smartgym_ai.ui.profile.EditProfileFragment"
    android:label="Edit Profile" />
```

Add action from profile to edit_profile.

- [ ] **Step 5: Wire ProfileFragment edit button**

```java
binding.btnEditProfile.setOnClickListener(v ->
    Navigation.findNavController(view).navigate(R.id.action_profile_to_edit_profile));
```

- [ ] **Step 6: Verify build and commit**

```bash
git add -A && git commit -m "feat: implement edit profile screen"
```

---

## Phase 4: Polish

### Task 13: Fix User.toMap() Null Values

**Files:**
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/data/model/User.java`

- [ ] **Step 1: Filter nulls in toMap()**

Replace `toMap()` method:
```java
public Map<String, Object> toMap() {
    Map<String, Object> map = new HashMap<>();
    if (uid != null) map.put("uid", uid);
    if (displayName != null) map.put("displayName", displayName);
    if (email != null) map.put("email", email);
    if (photoUrl != null) map.put("photoUrl", photoUrl);
    if (weight != null) map.put("weight", weight);
    if (height != null) map.put("height", height);
    if (bmi != null) map.put("bmi", bmi);
    if (bmiCategory != null) map.put("bmiCategory", bmiCategory);
    if (goal != null) map.put("goal", goal);
    map.put("createdAt", createdAt);
    return map;
}
```

- [ ] **Step 2: Verify build and commit**

```bash
git add -A && git commit -m "fix: filter null values in User.toMap()"
```

---

### Task 14: Custom Bottom Navigation Icons

**Files:**
- Create: `app/src/main/res/drawable/ic_nav_home.xml`
- Create: `app/src/main/res/drawable/ic_nav_workout.xml`
- Create: `app/src/main/res/drawable/ic_nav_progress.xml`
- Create: `app/src/main/res/drawable/ic_nav_community.xml`
- Create: `app/src/main/res/drawable/ic_nav_profile.xml`
- Modify: `app/src/main/res/menu/bottom_nav_menu.xml`

- [ ] **Step 1: Create 5 Material vector icons**

Create each icon as a 24dp vector drawable using Material Design icon paths (home, fitness_center, trending_up, people, person).

- [ ] **Step 2: Update bottom_nav_menu.xml**

Replace `@android:drawable/ic_menu_*` references with `@drawable/ic_nav_*`.

- [ ] **Step 3: Verify build and commit**

```bash
git add -A && git commit -m "feat: add custom Material Design bottom nav icons"
```

---

### Task 15: Extract Hardcoded Strings to Resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/auth/AuthViewModel.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardFragment.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/profile/ProfileFragment.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/progress/ProgressFragment.java`

- [ ] **Step 1: Add all Vietnamese strings to strings.xml**

Add entries like: `error_empty_fields`, `error_invalid_email`, `error_weak_password`, `stat_weight`, `stat_bmi`, `stat_goal`, `greeting_format`, etc.

- [ ] **Step 2: Replace hardcoded strings in Fragment code**

Replace `"CÂN NẶNG"` with `getString(R.string.stat_weight)`, etc.

- [ ] **Step 3: For AuthViewModel, use resource IDs**

Change error messages to use `@StringRes int` pattern or keep simple and use string resources in fragment observation layer.

- [ ] **Step 4: Verify build and commit**

```bash
git add -A && git commit -m "refactor: extract hardcoded Vietnamese strings to resources"
```

---

### Task 16: Enable R8 + ProGuard Rules

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/proguard-rules.pro`

- [ ] **Step 1: Enable R8 in build.gradle.kts**

```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
}
```

- [ ] **Step 2: Add ProGuard keep rules**

In `proguard-rules.pro`:
```
-keep class ntu.quy65132908.smartgym_ai.data.model.** { *; }
-keep class com.google.firebase.** { *; }
-keep class com.google.ai.** { *; }
-keep public class * implements com.bumptech.glide.module.GlideModule
```

- [ ] **Step 3: Verify release build**

Run: `./gradlew assembleRelease`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat: enable R8 minification with ProGuard keep rules"
```

---

### Task 17: Add Test Dependencies + AuthViewModel Unit Tests

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/test/java/ntu/quy65132908/smartgym_ai/ui/auth/AuthViewModelTest.java`

- [ ] **Step 1: Add test dependencies**

In `libs.versions.toml` add:
```toml
mockito = "5.12.0"
coreTesting = "2.2.0"
```

In `app/build.gradle.kts`:
```kotlin
testImplementation("org.mockito:mockito-core:5.12.0")
testImplementation("androidx.arch.core:core-testing:2.2.0")
```

- [ ] **Step 2: Write AuthViewModelTest**

Create test covering: empty fields validation, invalid email, password too short, successful sign-in callback, error mapping.

- [ ] **Step 3: Run tests**

Run: `./gradlew test`
Expected: ALL PASS

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "test: add AuthViewModel unit tests with Mockito"
```

---

### Task 18: Add Loading/Empty/Error States to Fragments

**Files:**
- Modify: Layout XML files for community, progress, workout fragments
- Modify: Fragment Java files to observe UiState

- [ ] **Step 1: Add ViewFlipper or visibility toggling to layouts**

Each data screen gets: a ProgressBar (loading), the content view, and a "no data" empty state text.

- [ ] **Step 2: Wire fragments to show/hide states based on ViewModel data**

- [ ] **Step 3: Verify build and commit**

```bash
git add -A && git commit -m "feat: add loading/empty/error UI states to data screens"
```

---

### Task 19: Add SwipeRefreshLayout to Community + Progress

**Files:**
- Modify: `app/src/main/res/layout/fragment_community.xml`
- Modify: `app/src/main/res/layout/fragment_progress.xml`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/community/CommunityFragment.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/progress/ProgressFragment.java`

- [ ] **Step 1: Wrap RecyclerViews in SwipeRefreshLayout**

- [ ] **Step 2: Wire refresh listener to ViewModel reload**

- [ ] **Step 3: Observe isRefreshing to stop animation**

- [ ] **Step 4: Verify build and commit**

```bash
git add -A && git commit -m "feat: add pull-to-refresh to Community and Progress screens"
```

---

### Task 20: Configure Firestore Offline Settings

**Files:**
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/di/AppModule.java`

- [ ] **Step 1: Configure Firestore settings in provider**

```java
@Provides @Singleton
public FirebaseFirestore provideFirestore() {
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build();
    firestore.setFirestoreSettings(settings);
    return firestore;
}
```

- [ ] **Step 2: Verify build and commit**

```bash
git add -A && git commit -m "feat: configure Firestore unlimited cache for offline support"
```

---

## Summary

| Phase | Tasks | What it achieves |
|-------|-------|-----------------|
| Phase 1 | Tasks 1-4 | App stops crashing, security holes patched |
| Phase 2 | Tasks 5-8 | Proper MVVM architecture with real Firestore data |
| Phase 3 | Tasks 9-12 | Gemini AI, Google Sign-In, Create Post, Edit Profile |
| Phase 4 | Tasks 13-20 | Polish: icons, strings, R8, tests, loading states, offline |

**Total: 20 tasks, ~80 steps**
