# Workout Detail Screen Overhaul Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all 20 identified issues in the Workout Detail screen — covering data serialization, logic race conditions, UI/UX, and test coverage.

**Architecture:** Layered bottom-up approach (Data → Logic → UI → Tests). Each layer builds on the previous. Single unified `WorkoutDetailUiState` replaces multiple LiveData fields. Optimistic UI updates for checkbox toggling. Material3 components for proper rendering.

**Tech Stack:** Java, Android XML, Hilt, Firebase Firestore, Material Design 3, JUnit 4/Mockito

---

**Status 2026-05-22:** Implementation completed in the working tree and verified with `testDebugUnitTest` plus `assembleDebug`. Commit steps were intentionally not executed because the repository already has unrelated uncommitted changes and no explicit commit request was made. The detail ViewModel now consumes nav arguments through `SavedStateHandle`; enabling generated Safe Args was attempted but reverted because the `androidx.navigation.safeargs` plugin fails against the current AGP setup with `safeargs plugin must be used with android plugin`.

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `app/src/main/java/.../data/model/Exercise.java` | Modify | Add `@PropertyName` annotation for Firestore |
| `app/src/main/java/.../data/repository/WorkoutRepository.java` | Modify | Add ordering + cache source |
| `app/src/main/java/.../ui/workout/WorkoutDetailUiState.java` | Create | Unified screen state (immutable) |
| `app/src/main/java/.../ui/workout/WorkoutViewModel.java` | Rewrite | Single UiState, optimistic toggle, SavedStateHandle |
| `app/src/main/java/.../ui/workout/WorkoutDetailFragment.java` | Rewrite | Single observer, toolbar, render function |
| `app/src/main/java/.../ui/workout/ExerciseAdapter.java` | Modify | 0-reps display logic, MaterialCheckBox |
| `app/src/main/res/layout/fragment_workout_detail.xml` | Rewrite | CoordinatorLayout, CircularProgressIndicator, empty state |
| `app/src/main/res/layout/item_exercise.xml` | Modify | MaterialCheckBox, rounded background |
| `app/src/main/res/color/checkbox_tint.xml` | Create | Color state list for checkbox |
| `app/src/main/res/drawable/ic_arrow_back.xml` | Create | Back navigation icon |
| `app/src/test/java/.../ui/workout/WorkoutViewModelTest.java` | Rewrite | 9 test cases for new ViewModel |

---

### Task 1: Fix Exercise Model Firestore Serialization

**Files:**
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/data/model/Exercise.java`

- [ ] **Step 1: Add PropertyName import and annotations to Exercise.java**

```java
package ntu.quy65132908.smartgym_ai.data.model;

import com.google.firebase.firestore.PropertyName;

public class Exercise {
    private String id;
    private String name;
    private int sets;
    private int reps;
    private Float weight;
    private boolean isCompleted;
    private String notes;

    public Exercise() {}

    public Exercise(String id, String name, int sets, int reps, Float weight, boolean isCompleted) {
        this.id = id;
        this.name = name;
        this.sets = sets;
        this.reps = reps;
        this.weight = weight;
        this.isCompleted = isCompleted;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getSets() { return sets; }
    public int getReps() { return reps; }
    public Float getWeight() { return weight; }

    @PropertyName("isCompleted")
    public boolean isCompleted() { return isCompleted; }

    public String getNotes() { return notes; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setSets(int sets) { this.sets = sets; }
    public void setReps(int reps) { this.reps = reps; }
    public void setWeight(Float weight) { this.weight = weight; }

    @PropertyName("isCompleted")
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public void setNotes(String notes) { this.notes = notes; }
}
```

- [ ] **Step 2: Verify project compiles**

Run: `cd e:/Subject/LTTBDD/smartgym-ai && gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/ntu/quy65132908/smartgym_ai/data/model/Exercise.java
git commit -m "fix: add @PropertyName annotation to Exercise for correct Firestore serialization"
```

---

### Task 2: Fix WorkoutRepository — Ordering & Caching

**Files:**
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/data/repository/WorkoutRepository.java`

- [ ] **Step 1: Add Source import and update getExercises method**

Add `import com.google.firebase.firestore.Source;` to imports.

Replace the `getExercises` method body with:

```java
public void getExercises(String uid, String workoutId, ExerciseListCallback cb) {
    firestore.collection("users").document(uid).collection("workouts").document(workoutId)
            .collection("exercises")
            .orderBy("name")
            .get(Source.DEFAULT)
            .addOnSuccessListener(snap -> {
                List<Exercise> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snap) {
                    Exercise e = doc.toObject(Exercise.class);
                    e.setId(doc.getId());
                    list.add(e);
                }
                cb.onSuccess(list);
            }).addOnFailureListener(cb::onError);
}
```

- [ ] **Step 2: Verify project compiles**

Run: `cd e:/Subject/LTTBDD/smartgym-ai && gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/ntu/quy65132908/smartgym_ai/data/repository/WorkoutRepository.java
git commit -m "fix: add exercise ordering by name and cache-first strategy"
```

---

### Task 3: Create WorkoutDetailUiState

**Files:**
- Create: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/workout/WorkoutDetailUiState.java`

- [ ] **Step 1: Create the unified state class**

```java
package ntu.quy65132908.smartgym_ai.ui.workout;

import java.util.Collections;
import java.util.List;

import ntu.quy65132908.smartgym_ai.data.model.Exercise;

/**
 * Immutable UI state for the Workout Detail screen.
 * Single source of truth — Fragment observes one LiveData of this type.
 */
public class WorkoutDetailUiState {
    private final List<Exercise> exercises;
    private final int progressPercent;
    private final boolean isLoading;
    private final String errorMessage;
    private final String subtitle;

    private WorkoutDetailUiState(List<Exercise> exercises, int progressPercent,
                                  boolean isLoading, String errorMessage, String subtitle) {
        this.exercises = exercises;
        this.progressPercent = progressPercent;
        this.isLoading = isLoading;
        this.errorMessage = errorMessage;
        this.subtitle = subtitle != null ? subtitle : "";
    }

    public static WorkoutDetailUiState loading(String subtitle) {
        return new WorkoutDetailUiState(null, 0, true, null, subtitle);
    }

    public static WorkoutDetailUiState success(List<Exercise> exercises, String subtitle) {
        if (exercises == null) exercises = Collections.emptyList();
        int completed = 0;
        for (Exercise ex : exercises) {
            if (ex.isCompleted()) completed++;
        }
        int percent = exercises.isEmpty() ? 0 : (completed * 100) / exercises.size();
        return new WorkoutDetailUiState(exercises, percent, false, null, subtitle);
    }

    public static WorkoutDetailUiState error(String message, String subtitle) {
        return new WorkoutDetailUiState(null, 0, false, message, subtitle);
    }

    public List<Exercise> getExercises() { return exercises; }
    public int getProgressPercent() { return progressPercent; }
    public boolean isLoading() { return isLoading; }
    public String getErrorMessage() { return errorMessage; }
    public String getSubtitle() { return subtitle; }

    public boolean isEmpty() {
        return exercises != null && exercises.isEmpty();
    }

    public boolean hasExercises() {
        return exercises != null && !exercises.isEmpty();
    }
}
```

- [ ] **Step 2: Verify project compiles**

Run: `cd e:/Subject/LTTBDD/smartgym-ai && gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/ntu/quy65132908/smartgym_ai/ui/workout/WorkoutDetailUiState.java
git commit -m "feat: add WorkoutDetailUiState as single source of truth for detail screen"
```

---

### Task 4: Write WorkoutViewModel Tests (RED phase)

**Files:**
- Rewrite: `app/src/test/java/ntu/quy65132908/smartgym_ai/ui/workout/WorkoutViewModelTest.java`

- [ ] **Step 1: Write all 9 test cases**

```java
package ntu.quy65132908.smartgym_ai.ui.workout;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ntu.quy65132908.smartgym_ai.data.model.Exercise;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;

public class WorkoutViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantRule = new InstantTaskExecutorRule();

    @Mock private WorkoutRepository workoutRepo;
    @Mock private AuthRepository authRepo;
    @Mock private FirebaseUser firebaseUser;

    private SavedStateHandle savedState;
    private WorkoutViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(firebaseUser.getUid()).thenReturn("uid123");
        when(authRepo.getCurrentUser()).thenReturn(firebaseUser);

        savedState = new SavedStateHandle();
        savedState.set("workoutId", "workout1");
        savedState.set("workoutTitle", "Thứ 6 - Tập nhẹ cơ tay");
        savedState.set("workoutDuration", 30);
    }

    private void createViewModel() {
        viewModel = new WorkoutViewModel(workoutRepo, authRepo, savedState);
    }

    @Test
    public void init_extractsArgsFromSavedStateHandle() {
        createViewModel();
        WorkoutDetailUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertTrue(state.getSubtitle().contains("Thứ 6 - Tập nhẹ cơ tay"));
        assertTrue(state.getSubtitle().contains("30 phút"));
    }

    @Test
    public void loadExercises_success_emitsSuccessState() {
        List<Exercise> exercises = Arrays.asList(
                new Exercise("e1", "Push up", 3, 12, null, false),
                new Exercise("e2", "Squat", 3, 10, 20f, true)
        );

        doAnswer(invocation -> {
            WorkoutRepository.ExerciseListCallback cb = invocation.getArgument(2);
            cb.onSuccess(exercises);
            return null;
        }).when(workoutRepo).getExercises(eq("uid123"), eq("workout1"), any());

        createViewModel();

        WorkoutDetailUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertFalse(state.isLoading());
        assertNull(state.getErrorMessage());
        assertEquals(2, state.getExercises().size());
        assertEquals(50, state.getProgressPercent()); // 1/2 = 50%
    }

    @Test
    public void loadExercises_error_emitsErrorState() {
        doAnswer(invocation -> {
            WorkoutRepository.ExerciseListCallback cb = invocation.getArgument(2);
            cb.onError(new Exception("Network error"));
            return null;
        }).when(workoutRepo).getExercises(eq("uid123"), eq("workout1"), any());

        createViewModel();

        WorkoutDetailUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertFalse(state.isLoading());
        assertNotNull(state.getErrorMessage());
        assertNull(state.getExercises());
    }

    @Test
    public void loadExercises_empty_emitsSuccessWithEmptyList() {
        doAnswer(invocation -> {
            WorkoutRepository.ExerciseListCallback cb = invocation.getArgument(2);
            cb.onSuccess(Collections.emptyList());
            return null;
        }).when(workoutRepo).getExercises(eq("uid123"), eq("workout1"), any());

        createViewModel();

        WorkoutDetailUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertTrue(state.isEmpty());
        assertEquals(0, state.getProgressPercent());
    }

    @Test
    public void toggleExercise_optimistic_updatesImmediately() {
        List<Exercise> exercises = Arrays.asList(
                new Exercise("e1", "Push up", 3, 12, null, false),
                new Exercise("e2", "Squat", 3, 10, null, false)
        );

        doAnswer(invocation -> {
            WorkoutRepository.ExerciseListCallback cb = invocation.getArgument(2);
            cb.onSuccess(exercises);
            return null;
        }).when(workoutRepo).getExercises(eq("uid123"), eq("workout1"), any());

        createViewModel();

        // Do NOT call the Firestore callback yet — just verify immediate UI update
        doNothing().when(workoutRepo).markExerciseCompleteAndSyncWorkout(
                anyString(), anyString(), anyString(), anyBoolean(), any());

        viewModel.toggleExercise("e1", true);

        WorkoutDetailUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertTrue(state.getExercises().get(0).isCompleted());
        assertEquals(50, state.getProgressPercent()); // 1/2 = 50%
    }

    @Test
    public void toggleExercise_failure_revertsState() {
        List<Exercise> exercises = Arrays.asList(
                new Exercise("e1", "Push up", 3, 12, null, false)
        );

        doAnswer(invocation -> {
            WorkoutRepository.ExerciseListCallback cb = invocation.getArgument(2);
            cb.onSuccess(exercises);
            return null;
        }).when(workoutRepo).getExercises(eq("uid123"), eq("workout1"), any());

        createViewModel();

        // Simulate Firestore failure on toggle
        doAnswer(invocation -> {
            WorkoutRepository.SimpleCallback cb = invocation.getArgument(4);
            cb.onError(new Exception("Write failed"));
            return null;
        }).when(workoutRepo).markExerciseCompleteAndSyncWorkout(
                anyString(), anyString(), anyString(), anyBoolean(), any());

        viewModel.toggleExercise("e1", true);

        WorkoutDetailUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertFalse(state.getExercises().get(0).isCompleted()); // Reverted
        assertEquals(0, state.getProgressPercent());
    }

    @Test
    public void toggleExercise_updatesProgressPercent() {
        List<Exercise> exercises = Arrays.asList(
                new Exercise("e1", "A", 3, 12, null, false),
                new Exercise("e2", "B", 3, 10, null, false),
                new Exercise("e3", "C", 3, 8, null, false)
        );

        doAnswer(invocation -> {
            WorkoutRepository.ExerciseListCallback cb = invocation.getArgument(2);
            cb.onSuccess(exercises);
            return null;
        }).when(workoutRepo).getExercises(eq("uid123"), eq("workout1"), any());

        createViewModel();
        assertEquals(0, viewModel.getUiState().getValue().getProgressPercent());

        // Toggle first -> 33%
        doNothing().when(workoutRepo).markExerciseCompleteAndSyncWorkout(
                anyString(), anyString(), anyString(), anyBoolean(), any());
        viewModel.toggleExercise("e1", true);
        assertEquals(33, viewModel.getUiState().getValue().getProgressPercent());

        // Toggle second -> 66%
        viewModel.toggleExercise("e2", true);
        assertEquals(66, viewModel.getUiState().getValue().getProgressPercent());
    }

    @Test
    public void retry_afterError_reloadsExercises() {
        // First call fails
        doAnswer(invocation -> {
            WorkoutRepository.ExerciseListCallback cb = invocation.getArgument(2);
            cb.onError(new Exception("fail"));
            return null;
        }).when(workoutRepo).getExercises(eq("uid123"), eq("workout1"), any());

        createViewModel();
        assertNotNull(viewModel.getUiState().getValue().getErrorMessage());

        // Second call succeeds
        List<Exercise> exercises = Arrays.asList(
                new Exercise("e1", "Push up", 3, 12, null, false)
        );
        doAnswer(invocation -> {
            WorkoutRepository.ExerciseListCallback cb = invocation.getArgument(2);
            cb.onSuccess(exercises);
            return null;
        }).when(workoutRepo).getExercises(eq("uid123"), eq("workout1"), any());

        viewModel.retry();

        WorkoutDetailUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertNull(state.getErrorMessage());
        assertEquals(1, state.getExercises().size());
    }

    @Test
    public void nullUser_doesNotCrash() {
        when(authRepo.getCurrentUser()).thenReturn(null);
        createViewModel();
        // Should not throw, verify no Firestore calls
        verify(workoutRepo, never()).getExercises(anyString(), anyString(), any());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail (RED phase)**

Run: `cd e:/Subject/LTTBDD/smartgym-ai && gradlew.bat testDebugUnitTest --tests "ntu.quy65132908.smartgym_ai.ui.workout.WorkoutViewModelTest" --info`
Expected: FAIL — WorkoutViewModel doesn't accept SavedStateHandle yet, and doesn't expose `getUiState()`

- [ ] **Step 3: Commit failing tests**

```bash
git add app/src/test/java/ntu/quy65132908/smartgym_ai/ui/workout/WorkoutViewModelTest.java
git commit -m "test(red): add 9 WorkoutViewModel tests for new UiState architecture"
```

---

### Task 5: Rewrite WorkoutViewModel (GREEN phase)

**Files:**
- Rewrite: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/workout/WorkoutViewModel.java`

- [ ] **Step 1: Rewrite WorkoutViewModel with unified UiState**

```java
package ntu.quy65132908.smartgym_ai.ui.workout;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.model.Exercise;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

@HiltViewModel
public class WorkoutViewModel extends ViewModel {
    private final WorkoutRepository workoutRepo;
    private final AuthRepository authRepo;

    private final MutableLiveData<WorkoutDetailUiState> uiState = new MutableLiveData<>();
    private final SingleLiveEvent<String> snackbarMessage = new SingleLiveEvent<>();

    private final String workoutId;
    private final String subtitle;
    private List<Exercise> currentExercises = new ArrayList<>();

    public LiveData<WorkoutDetailUiState> getUiState() { return uiState; }
    public LiveData<String> getSnackbarMessage() { return snackbarMessage; }

    @Inject
    public WorkoutViewModel(WorkoutRepository workoutRepo, AuthRepository authRepo,
                            SavedStateHandle savedState) {
        this.workoutRepo = workoutRepo;
        this.authRepo = authRepo;

        this.workoutId = savedState.get("workoutId");
        String title = savedState.get("workoutTitle");
        Integer duration = savedState.get("workoutDuration");
        this.subtitle = (title != null ? title : "") + " • " +
                        (duration != null ? duration : 0) + " phút";

        loadExercises();
    }

    public void loadExercises() {
        FirebaseUser u = authRepo.getCurrentUser();
        if (u == null || workoutId == null || workoutId.isEmpty()) return;

        uiState.setValue(WorkoutDetailUiState.loading(subtitle));

        workoutRepo.getExercises(u.getUid(), workoutId, new WorkoutRepository.ExerciseListCallback() {
            @Override
            public void onSuccess(List<Exercise> list) {
                currentExercises = new ArrayList<>(list);
                uiState.postValue(WorkoutDetailUiState.success(currentExercises, subtitle));
            }

            @Override
            public void onError(Exception e) {
                uiState.postValue(WorkoutDetailUiState.error("Không thể tải bài tập.", subtitle));
            }
        });
    }

    public void toggleExercise(String exerciseId, boolean done) {
        FirebaseUser u = authRepo.getCurrentUser();
        if (u == null || workoutId == null) return;

        // Optimistic local update
        for (Exercise ex : currentExercises) {
            if (ex.getId().equals(exerciseId)) {
                ex.setCompleted(done);
                break;
            }
        }
        uiState.setValue(WorkoutDetailUiState.success(new ArrayList<>(currentExercises), subtitle));

        // Sync to Firestore
        workoutRepo.markExerciseCompleteAndSyncWorkout(u.getUid(), workoutId, exerciseId, done,
                new WorkoutRepository.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        // Already reflected in UI
                    }

                    @Override
                    public void onError(Exception e) {
                        // Revert optimistic update
                        for (Exercise ex : currentExercises) {
                            if (ex.getId().equals(exerciseId)) {
                                ex.setCompleted(!done);
                                break;
                            }
                        }
                        uiState.postValue(WorkoutDetailUiState.success(
                                new ArrayList<>(currentExercises), subtitle));
                        snackbarMessage.postValue("Không thể cập nhật. Thử lại.");
                    }
                });
    }

    public void retry() {
        loadExercises();
    }
}
```

- [ ] **Step 2: Run tests to verify they pass (GREEN phase)**

Run: `cd e:/Subject/LTTBDD/smartgym-ai && gradlew.bat testDebugUnitTest --tests "ntu.quy65132908.smartgym_ai.ui.workout.WorkoutViewModelTest" --info`
Expected: All 9 tests PASS

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/ntu/quy65132908/smartgym_ai/ui/workout/WorkoutViewModel.java
git commit -m "feat: rewrite WorkoutViewModel with unified UiState and optimistic toggle"
```

---

### Task 6: Create UI Resources (checkbox tint, back icon)

**Files:**
- Create: `app/src/main/res/color/checkbox_tint.xml`
- Create: `app/src/main/res/drawable/ic_arrow_back.xml`

- [ ] **Step 1: Create checkbox color state list**

File: `app/src/main/res/color/checkbox_tint.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="@color/primary" android:state_checked="true" />
    <item android:color="@color/outline" android:state_checked="false" />
</selector>
```

- [ ] **Step 2: Create back arrow icon**

File: `app/src/main/res/drawable/ic_arrow_back.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="@color/on_background">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z" />
</vector>
```

- [ ] **Step 3: Verify project compiles**

Run: `cd e:/Subject/LTTBDD/smartgym-ai && gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/color/checkbox_tint.xml app/src/main/res/drawable/ic_arrow_back.xml
git commit -m "feat: add checkbox color state list and back arrow icon"
```

---

### Task 7: Rewrite fragment_workout_detail.xml Layout

**Files:**
- Rewrite: `app/src/main/res/layout/fragment_workout_detail.xml`

- [ ] **Step 1: Replace entire layout with CoordinatorLayout + Material3 components**

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@color/background"
        app:elevation="0dp">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            app:navigationIcon="@drawable/ic_arrow_back"
            app:title="Chi tiết bài tập"
            app:titleTextColor="@color/on_background" />
    </com.google.android.material.appbar.AppBarLayout>

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="@dimen/spacing_lg">

            <!-- Subtitle -->
            <TextView
                android:id="@+id/tv_workout_subtitle"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text=""
                android:textAppearance="@style/TextStyle.BodyBase" />

            <!-- Progress Ring -->
            <FrameLayout
                android:layout_width="match_parent"
                android:layout_height="120dp"
                android:layout_marginTop="@dimen/spacing_xl">

                <com.google.android.material.progressindicator.CircularProgressIndicator
                    android:id="@+id/progress_ring"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_gravity="center"
                    android:max="100"
                    android:progress="0"
                    app:indicatorSize="80dp"
                    app:trackThickness="8dp"
                    app:indicatorColor="@color/primary"
                    app:trackColor="@color/surface_container" />

                <TextView
                    android:id="@+id/tv_progress_text"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_gravity="center"
                    android:text="0%"
                    android:textAppearance="@style/TextStyle.HeadlineMd"
                    android:textColor="@color/primary" />
            </FrameLayout>

            <!-- Loading indicator (indeterminate) -->
            <com.google.android.material.progressindicator.CircularProgressIndicator
                android:id="@+id/loading_indicator"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center"
                android:layout_marginTop="@dimen/spacing_xl"
                android:indeterminate="true"
                android:visibility="gone"
                app:indicatorColor="@color/primary" />

            <!-- Empty state -->
            <LinearLayout
                android:id="@+id/layout_empty_exercises"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:orientation="vertical"
                android:padding="@dimen/spacing_xxl"
                android:visibility="gone">

                <ImageView
                    android:layout_width="48dp"
                    android:layout_height="48dp"
                    android:src="@drawable/ic_dumbbell"
                    android:contentDescription="Empty"
                    app:tint="@color/outline" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/spacing_md"
                    android:text="Chưa có bài tập nào"
                    android:textAppearance="@style/TextStyle.BodyBase"
                    android:textColor="@color/outline" />
            </LinearLayout>

            <!-- Exercise List Header -->
            <TextView
                android:id="@+id/tv_exercise_header"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/spacing_xl"
                android:text="DANH SÁCH BÀI TẬP"
                android:textAppearance="@style/TextStyle.LabelCaps"
                android:visibility="gone" />

            <!-- Exercise RecyclerView -->
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rv_exercises"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/spacing_md"
                android:nestedScrollingEnabled="false"
                android:visibility="gone" />

            <!-- Start Workout Button -->
            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_start_workout"
                android:layout_width="match_parent"
                android:layout_height="@dimen/button_height"
                android:layout_marginTop="@dimen/spacing_xl"
                android:text="Bắt đầu tập"
                android:visibility="gone"
                app:icon="@drawable/ic_play_circle"
                app:iconGravity="textStart" />

        </LinearLayout>
    </androidx.core.widget.NestedScrollView>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

- [ ] **Step 2: Verify project compiles**

Run: `cd e:/Subject/LTTBDD/smartgym-ai && gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL (or fail on Fragment — expected, fixed in Task 9)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/fragment_workout_detail.xml
git commit -m "feat: rewrite workout detail layout with Material3 components"
```

---

### Task 8: Update item_exercise.xml — MaterialCheckBox + Rounded Corners

**Files:**
- Modify: `app/src/main/res/layout/item_exercise.xml`

- [ ] **Step 1: Replace item layout with MaterialCheckBox and rounded background**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="@dimen/spacing_sm"
    android:background="@drawable/bg_workout_item_rounded"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:padding="@dimen/spacing_md">

    <com.google.android.material.checkbox.MaterialCheckBox
        android:id="@+id/cb_done"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:buttonTint="@color/checkbox_tint" />

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="@dimen/spacing_sm"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:id="@+id/tv_exercise_name"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Exercise"
            android:textAppearance="@style/TextStyle.BodyBold" />

        <TextView
            android:id="@+id/tv_exercise_detail"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Sets × Reps"
            android:textAppearance="@style/TextStyle.LabelCaps" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 2: Verify project compiles**

Run: `cd e:/Subject/LTTBDD/smartgym-ai && gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/item_exercise.xml
git commit -m "feat: update exercise item with MaterialCheckBox and rounded corners"
```

---

### Task 9: Update ExerciseAdapter — 0 Reps Display Logic

**Files:**
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/workout/ExerciseAdapter.java`

- [ ] **Step 1: Update onBindViewHolder with conditional reps display**

Replace the `onBindViewHolder` method in `ExerciseAdapter.java`:

```java
@Override
public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    Exercise ex = getItem(position);
    holder.tvName.setText(ex.getName());

    String detail;
    if (ex.getReps() == 0) {
        detail = ex.getSets() + " set • Tự do";
    } else {
        detail = ex.getSets() + " sets × " + ex.getReps() + " reps";
    }
    if (ex.getWeight() != null && ex.getWeight() > 0) {
        detail += " • " + ex.getWeight().intValue() + "kg";
    }
    holder.tvDetail.setText(detail);

    holder.cbDone.setOnCheckedChangeListener(null);
    holder.cbDone.setChecked(ex.isCompleted());
    holder.cbDone.setOnCheckedChangeListener((btn, checked) -> {
        if (listener != null) listener.onChecked(ex, checked);
    });
}
```

- [ ] **Step 2: Verify project compiles**

Run: `cd e:/Subject/LTTBDD/smartgym-ai && gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/ntu/quy65132908/smartgym_ai/ui/workout/ExerciseAdapter.java
git commit -m "fix: display '1 set • Tự do' for exercises with 0 reps"
```

---

### Task 10: Rewrite WorkoutDetailFragment

**Files:**
- Rewrite: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/workout/WorkoutDetailFragment.java`

- [ ] **Step 1: Replace entire Fragment with new implementation**

```java
package ntu.quy65132908.smartgym_ai.ui.workout;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.databinding.FragmentWorkoutDetailBinding;

@AndroidEntryPoint
public class WorkoutDetailFragment extends Fragment {

    private FragmentWorkoutDetailBinding binding;
    private WorkoutViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWorkoutDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(WorkoutViewModel.class);

        // Back navigation
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        // Exercise list
        ExerciseAdapter adapter = new ExerciseAdapter((exercise, checked) ->
                viewModel.toggleExercise(exercise.getId(), checked));
        binding.rvExercises.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvExercises.setAdapter(adapter);

        // Single UiState observer
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> render(state, adapter));

        // Toggle-failure messages (state already reverted optimistically)
        viewModel.getSnackbarMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
            }
        });

        // Start workout button
        binding.btnStartWorkout.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Bắt đầu tập luyện!", Toast.LENGTH_SHORT).show());
    }

    private void render(WorkoutDetailUiState state, ExerciseAdapter adapter) {
        if (state == null) return;

        // Subtitle
        binding.tvWorkoutSubtitle.setText(state.getSubtitle());

        // Loading
        binding.loadingIndicator.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);

        // Error — show persistent Snackbar with retry
        if (state.getErrorMessage() != null) {
            Snackbar.make(binding.getRoot(), state.getErrorMessage(), Snackbar.LENGTH_INDEFINITE)
                    .setAction("Thử lại", v -> viewModel.retry())
                    .show();
        }

        // Empty state
        boolean isEmpty = state.isEmpty();
        binding.layoutEmptyExercises.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

        // Exercise list + header
        boolean hasExercises = state.hasExercises();
        binding.tvExerciseHeader.setVisibility(hasExercises ? View.VISIBLE : View.GONE);
        binding.rvExercises.setVisibility(hasExercises ? View.VISIBLE : View.GONE);

        if (state.getExercises() != null) {
            adapter.submitList(new ArrayList<>(state.getExercises()));
        }

        // Progress ring (animated)
        int newProgress = state.getProgressPercent();
        int oldProgress = binding.progressRing.getProgress();
        if (newProgress != oldProgress) {
            ObjectAnimator.ofInt(binding.progressRing, "progress", oldProgress, newProgress)
                    .setDuration(400)
                    .start();
        }
        binding.tvProgressText.setText(newProgress + "%");

        // Start button visibility
        binding.btnStartWorkout.setVisibility(hasExercises ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
```

- [ ] **Step 2: Verify project compiles**

Run: `cd e:/Subject/LTTBDD/smartgym-ai && gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/ntu/quy65132908/smartgym_ai/ui/workout/WorkoutDetailFragment.java
git commit -m "feat: rewrite WorkoutDetailFragment with single UiState observer and Material3 UI"
```

---

### Task 11: Run Full Test Suite & Final Verification

**Files:**
- None (verification only)

- [ ] **Step 1: Run all unit tests**

Run: `cd e:/Subject/LTTBDD/smartgym-ai && gradlew.bat testDebugUnitTest --info`
Expected: All tests PASS including the 9 new WorkoutViewModelTest cases

- [ ] **Step 2: Run full build to verify no compilation errors**

Run: `cd e:/Subject/LTTBDD/smartgym-ai && gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Final commit (if any test fixes needed)**

```bash
git add -A
git commit -m "chore: workout detail overhaul complete — all 20 issues fixed"
```
