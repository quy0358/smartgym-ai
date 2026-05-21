# SmartGym AI — Full Project Cleanup Design Spec

**Date:** 2026-05-21
**Status:** Approved
**Scope:** Fix 31 identified issues across 4 phases

---

## Overview

Complete systematic remediation of the SmartGym AI Android project covering crash-causing bugs, security vulnerabilities, architectural debt, missing features, and UX polish.

**Approach:** Phased Cleanup (Phase 1 → 2 → 3 → 4), each phase independently deployable.

---

## Phase 1: Critical + Security Fixes

### 1.1 Fix Set.of() API Level Crash

**File:** `app/src/main/java/ntu/quy65132908/smartgym_ai/MainActivity.java` (line 48)

**Problem:** `Set.of()` requires API 30+ but `minSdk = 24`. Crashes on Android 7-10 with `NoSuchMethodError`.

**Fix:** Replace with:
```java
Set<Integer> mainDestinations = new HashSet<>(Arrays.asList(
    R.id.nav_dashboard, R.id.nav_workout, R.id.nav_progress,
    R.id.nav_community, R.id.nav_profile
));
```

---

### 1.2 Fix LiveData Re-trigger on Configuration Change

**Files:**
- `AuthViewModel.java` — `authSuccess` field
- `ProfileViewModel.java` — `signedOut` field

**Problem:** Standard `MutableLiveData` re-delivers the last value on rotation, causing duplicate navigation.

**Fix:** Create `util/SingleLiveEvent.java`:
```java
public class SingleLiveEvent<T> extends MutableLiveData<T> {
    private final AtomicBoolean pending = new AtomicBoolean(false);

    @Override
    public void setValue(T value) {
        pending.set(true);
        super.setValue(value);
    }

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

Apply to: `authSuccess`, `signedOut`, `errorMessage`.

---

### 1.3 Fix Sign-Out Navigation Without Backstack Clear

**File:** `app/src/main/res/navigation/nav_graph.xml`

**Problem:** `ProfileFragment` navigates to `nav_login` without clearing backstack. User can press back to return to authenticated screens.

**Fix:** Add global action in nav_graph:
```xml
<action
    android:id="@+id/action_global_to_login"
    app:destination="@id/nav_login"
    app:popUpTo="@id/nav_graph"
    app:popUpToInclusive="true" />
```

Use this action in `ProfileFragment` after sign-out.

---

### 1.4 Move API Key to Safer Storage

**File:** `app/build.gradle.kts` (line 31)

**Problem:** GEMINI_API_KEY compiled into APK, easily decompiled.

**Fix:**
1. Keep `local.properties` approach for development (already .gitignored)
2. For release: use Firebase Remote Config to deliver key at runtime
   - **Prerequisite:** Enable Remote Config in Firebase Console, create parameter `gemini_api_key`
3. Create `util/GeminiKeyProvider.java` abstraction:
   - Debug: reads from `BuildConfig.GEMINI_API_KEY`
   - Release: fetches from Firebase Remote Config with 12h cache
4. `GeminiRepository` consumes the provider, never accesses BuildConfig directly

---

### 1.5 Add Input Sanitization

**File:** New `util/InputValidator.java`

**Problem:** User-provided name stored directly in Firestore without validation.

**Fix:** Create utility with methods:
- `sanitizeName(String input)` — trim, limit 50 chars, strip HTML tags
- `sanitizeContent(String input)` — trim, limit 500 chars for posts
- `isValidName(String input)` — returns false if empty after trim
- `isValidEmail(String email)` — delegates to `Patterns.EMAIL_ADDRESS`

Apply in `AuthViewModel.signUp()` and community post creation.

---

## Phase 2: Architecture Foundation

### 2.1 Create Missing Repositories

**New files:**

| File | Firestore Path | Responsibilities |
|------|---------------|------------------|
| `data/repository/WorkoutRepository.java` | `users/{uid}/workouts` | CRUD workout plans, fetch weekly plan, mark exercises complete |
| `data/repository/ProgressRepository.java` | `users/{uid}/progress` | Add progress entries, fetch history sorted by date |
| `data/repository/CommunityRepository.java` | `posts` | Fetch posts paginated, create post, like/unlike |
| `data/repository/GeminiRepository.java` | N/A (API) | Workout plan generation, form analysis |

**Pattern for each:**
- `@Singleton` annotation
- `@Inject` constructor with Firebase/SDK dependencies
- Callback interface for async results
- Provided via `AppModule`

---

### 2.2 Create Missing ViewModels

**New files:**

| ViewModel | Dependencies | LiveData |
|-----------|-------------|----------|
| `ui/workout/WorkoutViewModel.java` | WorkoutRepository, AuthRepository | exercises, workoutTitle, isLoading |
| `ui/progress/ProgressViewModel.java` | ProgressRepository, AuthRepository | entries, stats |
| `ui/community/CommunityViewModel.java` | CommunityRepository, AuthRepository | posts, isRefreshing |
| `ui/analysis/AIAnalysisViewModel.java` | GeminiRepository, UserRepository, AuthRepository | aiResponse, userMetrics, isLoading |

Each ViewModel:
- Annotated with `@HiltViewModel`
- Uses `SingleLiveEvent` for one-shot events
- Loads data in constructor or on explicit refresh

---

### 2.3 Replace Hardcoded Adapters

**Refactor pattern:** Convert from static `String[][]` to `ListAdapter<T, VH>` with `DiffUtil.ItemCallback<T>`:

- `PostAdapter` — accepts `List<Post>`, uses `DiffUtil` on `Post.id`
- `ExerciseAdapter` — accepts `List<Exercise>`, includes completion callback via interface
- `WeeklyPlanAdapter` — accepts `List<Workout>`, computes "today" from `Calendar.DAY_OF_WEEK`

Each adapter:
- Extends `ListAdapter<T, ViewHolder>` instead of bare `RecyclerView.Adapter`
- Receives data via `submitList()`
- Never holds mutable state internally

---

### 2.4 Firestore Data Structure

```
firestore-root/
├── users/{uid}
│   ├── displayName: String
│   ├── email: String
│   ├── photoUrl: String?
│   ├── weight: Float?
│   ├── height: Float?
│   ├── bmi: Float?
│   ├── bmiCategory: String?
│   ├── goal: String?
│   ├── createdAt: Long
│   │
│   ├── workouts/{workoutId}
│   │   ├── title: String
│   │   ├── subtitle: String
│   │   ├── intensity: String
│   │   ├── durationMinutes: Int
│   │   ├── dayOfWeek: Int (1=Mon...7=Sun)
│   │   ├── isCompleted: Boolean
│   │   └── exercises/{exerciseId}
│   │       ├── name: String
│   │       ├── sets: Int
│   │       ├── reps: Int
│   │       ├── weight: Float?
│   │       ├── isCompleted: Boolean
│   │       └── notes: String?
│   │
│   └── progress/{entryId}
│       ├── weight: Float
│       ├── bodyFat: Float?
│       ├── leanMass: Float?
│       ├── date: Long
│       └── note: String?
│
└── posts/{postId}
    ├── authorId: String
    ├── authorName: String
    ├── content: String
    ├── likes: Int
    ├── likedBy: List<String> (UIDs)
    └── createdAt: Long
```

Key decisions:
- Workouts and progress are sub-collections under user (privacy by default)
- Posts are top-level (community-visible)
- `likedBy` array prevents double-liking

---

### 2.5 Remove Unused Dependencies

**Remove from `libs.versions.toml` and `build.gradle.kts`:**
- `firebase-database` (project uses Firestore exclusively)

**Keep and integrate:**
- `swiperefreshlayout` — use in Community and Progress fragments

---

### 2.6 Offline Caching Strategy

**Approach:** Leverage Firestore built-in offline persistence (enabled by default on Android).

**Implementation:**
1. In `AppModule`, configure Firestore settings:
   ```java
   FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
       .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
       .build();
   firestore.setFirestoreSettings(settings);
   ```
2. Show cached data immediately, update from network when available
3. Add `NetworkMonitor` utility using `ConnectivityManager` to show offline indicator in UI

---

## Phase 3: Feature Implementation

### 3.1 Gemini AI Integration

**New file:** `data/repository/GeminiRepository.java`

**Capabilities:**
1. **Generate Workout Plan** — personalized based on user profile
2. **Analyze Exercise Form** — corrections from text description

**Implementation:**
```java
@Singleton
public class GeminiRepository {
    private final GenerativeModel model;

    @Inject
    public GeminiRepository(GeminiKeyProvider keyProvider) {
        // Use latest available model; check SDK docs for current model ID
        this.model = new GenerativeModel("gemini-2.0-flash", keyProvider.getApiKey());
    }

    public void generateWorkoutPlan(User userProfile, String goal, AiCallback callback) {
        String prompt = buildWorkoutPrompt(userProfile, goal);
        // Call model.generateContent() async
    }

    public void analyzeForm(String exerciseName, String formDescription, AiCallback callback) {
        String prompt = buildFormAnalysisPrompt(exerciseName, formDescription);
        // Call model.generateContent() async
    }
}
```

**Prompt strategy:**
- System context: certified fitness coach, responds in Vietnamese
- Workout prompt includes: weight, height, BMI, goal, experience level
- Form prompt includes: exercise name, user's description of their technique
- Output formatted as Markdown for UI rendering
- 10-second timeout, disable button during request

**UI updates to `AIAnalysisFragment`:**
- Input field for exercise form description
- Button to generate workout plan (uses user profile data)
- Scrollable response area (Markdown-rendered)
- Loading animation during API call
- Error state with retry

---

### 3.2 Google Sign-In via Credential Manager

**New file:** `util/GoogleSignInHelper.java`

**Flow:**
1. User taps Google button in `LoginFragment`
2. `GoogleSignInHelper` creates `GetGoogleIdOption` with web client ID from `google-services.json`
3. `CredentialManager.getCredential()` shows account picker
4. On success, extract `GoogleIdTokenCredential`
5. Exchange for Firebase credential: `GoogleAuthProvider.getCredential(idToken, null)`
6. Call `FirebaseAuth.signInWithCredential()`
7. If new user (first login), create Firestore profile from Google account data (name, email, photoUrl)
8. Navigate to dashboard

**Integration point:** Add `signInWithGoogle()` method to `AuthRepository`.

---

### 3.3 Create Post Feature

**New files:**
- `ui/community/CreatePostBottomSheet.java` — BottomSheetDialogFragment
- Update `fragment_community.xml` — ensure FAB wired correctly

**UI:**
- Multi-line EditText (max 500 chars)
- Character counter
- Post button (disabled when empty)
- Author info auto-filled from current user

**Backend:** `CommunityRepository.createPost(Post post, SimpleCallback callback)`
- Sets `authorId` and `authorName` from current user
- Sets `createdAt = System.currentTimeMillis()`
- Initializes `likes = 0`, `likedBy = empty list`
- Writes to `posts` collection

**Community list improvements:**
- Real-time listener via `addSnapshotListener()` for live updates
- Pull-to-refresh with SwipeRefreshLayout
- Like/unlike: `FieldValue.arrayUnion(uid)` / `FieldValue.arrayRemove(uid)` on `likedBy`, increment/decrement `likes`

---

### 3.4 Edit Profile Feature

**New files:**
- `ui/profile/EditProfileFragment.java`
- `ui/profile/EditProfileViewModel.java`
- `res/layout/fragment_edit_profile.xml`

**Navigation:** Add to nav_graph:
```xml
<fragment
    android:id="@+id/nav_edit_profile"
    android:name="ntu.quy65132908.smartgym_ai.ui.profile.EditProfileFragment"
    android:label="Edit Profile" />

<action
    android:id="@+id/action_profile_to_edit_profile"
    app:destination="@id/nav_edit_profile" />
```

**Editable fields:** Display Name, Weight (kg), Height (cm), Goal
**Auto-calculated:** BMI = weight / (height/100)^2, BMI Category based on value
**Save:** `UserRepository.updateUser()` with validated data
**Cancel:** Pop backstack

---

## Phase 4: Polish

### 4.1 Extract Hardcoded Strings

Move all Vietnamese strings from Java files to `res/values/strings.xml`:
- Error messages from `AuthViewModel`
- Stat labels from `DashboardFragment`, `ProfileFragment`, `ProgressFragment`, `AIAnalysisFragment`
- Greeting format from `DashboardFragment`

For ViewModel error messages: expose `@StringRes int` resource IDs instead of raw strings. Fragment resolves to actual string.

---

### 4.2 Custom Bottom Navigation Icons

Replace `@android:drawable/ic_menu_*` with custom Material vector drawables:

| Menu item | New icon file | Icon description |
|-----------|--------------|-----------------|
| Home | `ic_nav_home.xml` | Home outline |
| Workout | `ic_nav_workout.xml` | Dumbbell |
| Progress | `ic_nav_progress.xml` | Trending up chart |
| Community | `ic_nav_community.xml` | People group |
| Profile | `ic_nav_profile.xml` | Person circle |

Source vectors from Material Design Icons set.

---

### 4.3 Loading, Empty, and Error States

**Pattern for every data screen:**

States: `LOADING` → `CONTENT` | `EMPTY` | `ERROR`

**Implementation:**
- Create `util/UiState.java` — enum or sealed-like class with `LOADING`, `SUCCESS`, `EMPTY`, `ERROR`
- Each ViewModel exposes `LiveData<UiState<T>>` instead of raw data
- Fragment layout contains `ViewFlipper` with child views for each state
- Fragment observes state and calls `viewFlipper.setDisplayedChild(index)`

**Affected screens:** Dashboard, Workout, Progress, Community, AI Analysis

**SwipeRefreshLayout:** Wrap content in Community and Progress for pull-to-refresh.

---

### 4.4 Enable R8 for Release Builds

**File:** `app/build.gradle.kts`

```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
```

**ProGuard rules** (`proguard-rules.pro`):
```
# Firestore model classes (reflection-based deserialization)
-keep class ntu.quy65132908.smartgym_ai.data.model.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }

# Gemini AI SDK
-keep class com.google.ai.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
```

---

### 4.5 Unit and UI Tests

**Unit tests** (`app/src/test/`):

| Test class | Scope |
|-----------|-------|
| `AuthViewModelTest` | Validation, error mapping, sign-in/up with mocked repo |
| `DashboardViewModelTest` | Data loading, default values on error |
| `ProfileViewModelTest` | Sign-out event, profile loading |
| `InputValidatorTest` | Sanitization rules, edge cases |
| `WorkoutViewModelTest` | Exercise loading, completion toggle |
| `CommunityViewModelTest` | Post creation, like/unlike |

**UI tests** (`app/src/androidTest/`):

| Test class | Scope |
|-----------|-------|
| `LoginFlowTest` | Email validation, error display, navigation |
| `NavigationTest` | Bottom nav destination switching |
| `SignOutTest` | Backstack cleared, returns to login |

**Test dependencies to add:**
```kotlin
testImplementation("org.mockito:mockito-core:5.12.0")
testImplementation("androidx.arch.core:core-testing:2.2.0")
androidTestImplementation("androidx.navigation:navigation-testing:2.9.0")
```

---

## New Files Summary

| Path | Purpose |
|------|---------|
| `util/SingleLiveEvent.java` | One-shot LiveData for navigation events |
| `util/InputValidator.java` | Input sanitization utility |
| `util/GeminiKeyProvider.java` | API key abstraction (debug/release) |
| `util/GoogleSignInHelper.java` | Credential Manager wrapper |
| `util/UiState.java` | Loading/Success/Empty/Error state enum |
| `util/NetworkMonitor.java` | Connectivity state observer |
| `data/repository/WorkoutRepository.java` | Workout CRUD operations |
| `data/repository/ProgressRepository.java` | Progress tracking operations |
| `data/repository/CommunityRepository.java` | Community posts operations |
| `data/repository/GeminiRepository.java` | AI interactions |
| `ui/workout/WorkoutViewModel.java` | Workout screen logic |
| `ui/progress/ProgressViewModel.java` | Progress screen logic |
| `ui/community/CommunityViewModel.java` | Community screen logic |
| `ui/community/CreatePostBottomSheet.java` | Post creation dialog |
| `ui/analysis/AIAnalysisViewModel.java` | AI analysis logic |
| `ui/profile/EditProfileFragment.java` | Edit profile screen |
| `ui/profile/EditProfileViewModel.java` | Edit profile logic |
| `res/layout/fragment_edit_profile.xml` | Edit profile layout |
| `res/drawable/ic_nav_*.xml` | 5 custom nav icons |
| Tests: 6 unit test + 3 UI test classes | Verification |

---

## Modified Files Summary

| Path | Changes |
|------|---------|
| `MainActivity.java` | Fix Set.of() to HashSet |
| `AuthViewModel.java` | Use SingleLiveEvent, @StringRes errors |
| `ProfileViewModel.java` | Use SingleLiveEvent for signedOut |
| `ProfileFragment.java` | Use global action for sign-out nav |
| `nav_graph.xml` | Add global_to_login action, edit_profile destination |
| `AppModule.java` | Provide new repositories, Firestore settings |
| `build.gradle.kts` | Enable R8, add test deps |
| `libs.versions.toml` | Remove firebase-database, add test versions |
| `proguard-rules.pro` | Add keep rules |
| `PostAdapter.java` | Convert to ListAdapter with DiffUtil |
| `ExerciseAdapter.java` | Convert to ListAdapter with DiffUtil |
| `WeeklyPlanAdapter.java` | Convert to ListAdapter, fix today logic |
| `DashboardFragment.java` | Use ViewModel data, add UiState |
| `CommunityFragment.java` | Wire ViewModel, SwipeRefresh |
| `ProgressFragment.java` | Wire ViewModel, add UiState |
| `WorkoutDetailFragment.java` | Wire ViewModel |
| `AIAnalysisFragment.java` | Full AI integration UI |
| `LoginFragment.java` | Google Sign-In integration |
| `bottom_nav_menu.xml` | Custom icons |
| `strings.xml` | All extracted strings |
| `User.java` | Fix toMap() null handling |
| `fragment_community.xml` | Add SwipeRefreshLayout |
| `fragment_progress.xml` | Add SwipeRefreshLayout |

---

## Implementation Order

1. Phase 1: Critical + Security (5 tasks)
2. Phase 2: Architecture Foundation (6 tasks)
3. Phase 3: Feature Implementation (4 tasks)
4. Phase 4: Polish (5 tasks)

Total: 20 implementation tasks
