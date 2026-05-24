# Workout Detail Screen — Complete Overhaul Design

**Date:** 2026-05-22  
**Scope:** Fix all 20 identified issues across Data, Logic, UI, and Testing layers  
**Approach:** Layered Bottom-Up (Data → Logic → UI → Tests)

---

## Problem Statement

The Workout Detail screen has 20 potential issues spanning UI/UX bugs (no back nav, broken progress ring, checkbox renders as solid square), logic errors (duplicate progress calc, race conditions, full reload on toggle), performance problems (no ordering, no caching), and data quality issues (Firestore serialization bug with `isCompleted` field).

---

## Issues Catalog

| # | Category | Issue | Severity |
|---|----------|-------|----------|
| 1 | UI/UX | No back navigation — no toolbar/back arrow | High |
| 2 | UI/UX | CheckBox renders as yellow square due to primary buttonTint | High |
| 3 | UI/UX | Progress "ring" uses progressBarStyleHorizontal — not circular | High |
| 4 | UI/UX | "0 REPS" displayed for stretching exercises | Medium |
| 5 | UI/UX | No "Start Workout" CTA button | Medium |
| 6 | UI/UX | Exercise items lack rounded corners | Low |
| 7 | UI/UX | No loading skeleton — only "..." text | Low |
| 8 | Logic | Duplicate progress calculation in Fragment AND ViewModel | High |
| 9 | Logic | Race condition — exercises observer and progress observer fight | High |
| 10 | Logic | workoutId extracted 3 times without caching — DRY violation | Medium |
| 11 | Logic | SafeArgs not used despite nav graph typed arguments | Medium |
| 12 | Logic | No retry on error — Snackbar has no action | Medium |
| 13 | Logic | Full Firestore reload after every toggle — performance & flicker | High |
| 14 | Perf | RecyclerView inside NestedScrollView disables recycling | Medium |
| 15 | Perf | No offline cache — Firestore .get() without Source | Medium |
| 16 | Perf | No exercise ordering — items appear randomly | Medium |
| 17 | Perf | No progress animation — value jumps | Low |
| 18 | Data | Subtitle format fragile — may double info | Low |
| 19 | Data | No empty state for exercises | Medium |
| 20 | Data | Exercise isCompleted naming conflict with JavaBean convention | High |

---

## Architecture

### Layer 1: Data Model & Repository

#### 1.1 Exercise.java — Firestore Annotation Fix (#20)

Add `@PropertyName("isCompleted")` to getter and setter:

```java
import com.google.firebase.firestore.PropertyName;

@PropertyName("isCompleted")
public boolean isCompleted() { return isCompleted; }

@PropertyName("isCompleted")
public void setCompleted(boolean completed) { isCompleted = completed; }
```

**Rationale:** Firestore's JavaBean serializer strips "is" prefix, mapping `isCompleted()` to field name "completed". But Firestore documents use "isCompleted". The annotation forces correct mapping.

#### 1.2 WorkoutRepository.java — Ordering & Caching (#15, #16)

```java
public void getExercises(String uid, String workoutId, ExerciseListCallback cb) {
    firestore.collection("users").document(uid)
        .collection("workouts").document(workoutId)
        .collection("exercises")
        .orderBy("name")
        .get(Source.DEFAULT)  // Uses local cache when offline
        .addOnSuccessListener(snap -> {
            List<Exercise> list = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snap) {
                Exercise e = doc.toObject(Exercise.class);
                e.setId(doc.getId());
                list.add(e);
            }
            cb.onSuccess(list);
        })
        .addOnFailureListener(cb::onError);
}
```

---

### Layer 2: ViewModel Logic

#### 2.1 WorkoutDetailUiState.java — New Unified State (#8, #9)

Single class that holds ALL screen state, eliminating race conditions:

```java
public class WorkoutDetailUiState {
    private final List<Exercise> exercises;  // null during loading
    private final int progressPercent;
    private final boolean isLoading;
    private final String errorMessage;       // null = no error
    private final String subtitle;

    // Private constructor — use factory methods
    private WorkoutDetailUiState(List<Exercise> exercises, int progressPercent,
                                  boolean isLoading, String errorMessage, String subtitle) { ... }

    public static WorkoutDetailUiState loading(String subtitle) {
        return new WorkoutDetailUiState(null, 0, true, null, subtitle);
    }

    public static WorkoutDetailUiState success(List<Exercise> exercises, String subtitle) {
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

    // Getters only — immutable
    public List<Exercise> getExercises() { return exercises; }
    public int getProgressPercent() { return progressPercent; }
    public boolean isLoading() { return isLoading; }
    public String getErrorMessage() { return errorMessage; }
    public String getSubtitle() { return subtitle; }
    public boolean isEmpty() { return exercises != null && exercises.isEmpty(); }
}
```

#### 2.2 WorkoutViewModel.java — Rewrite (#8, #9, #10, #11, #12, #13)

```java
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
                @Override public void onSuccess() { /* Already reflected */ }
                @Override public void onError(Exception e) {
                    // Revert
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

---

### Layer 3: UI/Layout

#### 3.1 fragment_workout_detail.xml — Full Rewrite

Structure:
```
CoordinatorLayout
├── AppBarLayout
│   └── MaterialToolbar (back arrow + title)
└── NestedScrollView
    └── LinearLayout (vertical, padding)
        ├── TextView (subtitle)
        ├── FrameLayout (CircularProgressIndicator + % text)
        ├── CircularProgressIndicator (indeterminate loading)
        ├── LinearLayout (empty state - hidden by default)
        ├── TextView ("DANH SÁCH BÀI TẬP" label)
        ├── RecyclerView (exercises)
        └── MaterialButton ("Bắt đầu tập")
```

Key layout components:

- **MaterialToolbar** with navigation icon for back navigation (#1)
- **CircularProgressIndicator** (determinate, Material3) replacing broken ProgressBar (#3, #17)
- **Animated progress** via ObjectAnimator (#17)
- **Indeterminate loading spinner** replacing "..." text (#7)
- **Empty state** layout with icon + message (#19)
- **MaterialButton** "Bắt đầu tập" at bottom (#5)

#### 3.2 item_exercise.xml — Fix Checkbox & Rounded Corners (#2, #6)

- Replace `CheckBox` with `MaterialCheckBox` using color state list
- Add `background="@drawable/bg_workout_item_rounded"` to root
- Keep existing horizontal layout structure

#### 3.3 New Resource: res/color/checkbox_tint.xml (#2)

```xml
<selector>
    <item android:color="@color/primary" android:state_checked="true" />
    <item android:color="@color/outline" android:state_checked="false" />
</selector>
```

#### 3.4 New Resource: res/drawable/ic_arrow_back.xml (#1)

Standard Material back arrow icon (24dp).

#### 3.5 ExerciseAdapter.java — Display Logic Fix (#4)

```java
@Override
public void onBindViewHolder(ViewHolder holder, int position) {
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

#### 3.6 WorkoutDetailFragment.java — Rewrite (#1, #5, #7, #10, #11, #12, #19)

Fragment responsibilities reduced to:
1. Set up toolbar back navigation
2. Set up RecyclerView + adapter
3. Observe SINGLE `uiState` LiveData and render
4. Observe `snackbarMessage` and show with retry action
5. Handle "Bắt đầu tập" button click

```java
@Override
public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(this).get(WorkoutViewModel.class);

    binding.toolbar.setNavigationOnClickListener(v ->
        Navigation.findNavController(v).navigateUp());

    ExerciseAdapter adapter = new ExerciseAdapter((exercise, checked) ->
        viewModel.toggleExercise(exercise.getId(), checked));
    binding.rvExercises.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.rvExercises.setAdapter(adapter);

    viewModel.getUiState().observe(getViewLifecycleOwner(), state -> render(state, adapter));

    // Toggle-failure messages only (state already reverted optimistically)
    viewModel.getSnackbarMessage().observe(getViewLifecycleOwner(), msg -> {
        if (msg != null) {
            Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
        }
    });

    binding.btnStartWorkout.setOnClickListener(v ->
        Toast.makeText(requireContext(), "Bắt đầu tập luyện!", Toast.LENGTH_SHORT).show());
}

private void render(WorkoutDetailUiState state, ExerciseAdapter adapter) {
    // Subtitle
    binding.tvWorkoutSubtitle.setText(state.getSubtitle());

    // Loading
    binding.loadingIndicator.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);

    // Error — handled via snackbar, but also show retry in error state
    if (state.getErrorMessage() != null) {
        Snackbar.make(binding.getRoot(), state.getErrorMessage(), Snackbar.LENGTH_INDEFINITE)
            .setAction("Thử lại", v -> viewModel.retry())
            .show();
    }

    // Empty state
    boolean isEmpty = state.isEmpty();
    binding.layoutEmptyExercises.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    binding.rvExercises.setVisibility(
        state.getExercises() != null && !isEmpty ? View.VISIBLE : View.GONE);

    // Exercise list
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
    binding.btnStartWorkout.setVisibility(
        state.getExercises() != null && !isEmpty ? View.VISIBLE : View.GONE);
}
```

---

### Layer 4: Testing

#### 4.1 WorkoutViewModelTest.java — 9 Test Cases

| Test | What It Validates |
|------|-------------------|
| `loadExercises_success_emitsSuccessState` | Happy path load → UiState.Success with exercises and correct % |
| `loadExercises_error_emitsErrorState` | Network fail → UiState.Error with message |
| `loadExercises_empty_emitsSuccessWithEmptyList` | No exercises → UiState.Success, isEmpty=true, 0% |
| `toggleExercise_optimistic_updatesImmediately` | Toggle → immediate UiState change before callback |
| `toggleExercise_failure_revertsState` | Firestore error → previous state restored |
| `toggleExercise_updatesProgressPercent` | 1/3→33%, 2/3→66%, 3/3→100% |
| `retry_afterError_reloadsExercises` | retry() → loadExercises re-invoked |
| `init_extractsArgsFromSavedStateHandle` | workoutId/title/duration parsed correctly |
| `nullUser_doesNotCrash` | No auth → no crash, no network call |

#### 4.2 ExerciseAdapter Display Tests — 4 Cases

| Test | What It Validates |
|------|-------------------|
| `zeroReps_displaysAsFreeForm` | 0 reps → "1 set • Tự do" |
| `normalExercise_displaysSetsAndReps` | "3 sets × 12 reps" |
| `exerciseWithWeight_appendsKg` | "3 sets × 12 reps • 10kg" |
| `zeroWeight_doesNotAppendKg` | weight=0 or null → no "kg" suffix |

---

## Files Changed

| File | Action | Issues |
|------|--------|--------|
| `data/model/Exercise.java` | Modify | #20 |
| `data/repository/WorkoutRepository.java` | Modify | #15, #16 |
| `ui/workout/WorkoutDetailUiState.java` | **New** | #8, #9 |
| `ui/workout/WorkoutViewModel.java` | Rewrite | #8-13 |
| `ui/workout/WorkoutDetailFragment.java` | Rewrite | #1, #5, #7, #10-12, #19 |
| `ui/workout/ExerciseAdapter.java` | Modify | #4 |
| `res/layout/fragment_workout_detail.xml` | Rewrite | #1, #3, #5-7, #14, #17, #19 |
| `res/layout/item_exercise.xml` | Modify | #2, #6 |
| `res/color/checkbox_tint.xml` | **New** | #2 |
| `res/drawable/ic_arrow_back.xml` | **New** | #1 |
| `test/.../WorkoutViewModelTest.java` | Rewrite | All logic |

## Dependencies

No new library dependencies. All components from existing Material3 and AndroidX libraries.

---

## Out of Scope

- Timer/tracking mode for "Bắt đầu tập" button (future iteration)
- Drag-to-reorder exercises
- Exercise detail/edit screen
- Workout notes/comments
