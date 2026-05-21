# Dashboard Home Tab Overhaul Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all 15 identified issues in the Dashboard Home tab through a ViewModel-driven refactor that makes the dashboard display live data, proper icons, loading/error states, and pull-to-refresh.

**Architecture:** DashboardViewModel becomes the single source of truth exposing LiveData for all UI state. DashboardFragment is a pure observer that binds data and sets up click listeners. Layout XML gains SwipeRefreshLayout, ProgressBar, proper Material icons, and rounded stat cards.

**Tech Stack:** Android (Java), Hilt DI, LiveData, ViewBinding, Material Design 3, Firebase Firestore

---

## File Map

| Path | Action | Responsibility |
|------|--------|---------------|
| `app/src/main/res/drawable/bg_stat_card_rounded.xml` | CREATE | Rounded background shape for stat cards |
| `app/src/main/res/drawable/ic_notification_bell.xml` | CREATE | Material notification bell vector icon |
| `app/src/main/res/drawable/ic_play_circle.xml` | CREATE | Play arrow vector icon for button |
| `app/src/main/res/values/strings.xml` | MODIFY | Add 10 new Vietnamese string resources |
| `app/src/main/res/layout/item_stat_card.xml` | MODIFY | Apply rounded background drawable |
| `app/src/main/res/layout/fragment_dashboard.xml` | MODIFY | Add SwipeRefreshLayout, ProgressBar, fix icons, add ids |
| `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModel.java` | REWRITE | Full rewrite with all LiveData, WorkoutRepository, refresh logic |
| `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardFragment.java` | REWRITE | Pure observer pattern with all observers and click handlers |
| `app/src/test/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModelTest.java` | CREATE | Unit tests for ViewModel logic |

---

## Task 1: Create Drawable Resources

**Files:**
- Create: `app/src/main/res/drawable/bg_stat_card_rounded.xml`
- Create: `app/src/main/res/drawable/ic_notification_bell.xml`
- Create: `app/src/main/res/drawable/ic_play_circle.xml`

- [ ] **Step 1: Create rounded stat card background**

Create `app/src/main/res/drawable/bg_stat_card_rounded.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/surface_container" />
    <corners android:radius="12dp" />
</shape>
```

- [ ] **Step 2: Create notification bell icon**

Create `app/src/main/res/drawable/ic_notification_bell.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/on_surface_variant"
        android:pathData="M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.9,2 2,2zM18,16v-5c0,-3.07 -1.63,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68C7.64,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2z" />
</vector>
```

- [ ] **Step 3: Create play circle icon**

Create `app/src/main/res/drawable/ic_play_circle.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="20dp"
    android:height="20dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/on_primary"
        android:pathData="M8,5v14l11,-7z" />
</vector>
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/drawable/bg_stat_card_rounded.xml app/src/main/res/drawable/ic_notification_bell.xml app/src/main/res/drawable/ic_play_circle.xml
git commit -m "feat(dashboard): add drawable resources for stat cards and icons"
```

---

## Task 2: Add String Resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add dashboard strings to strings.xml**

Add the following entries inside the `<resources>` tag of `app/src/main/res/values/strings.xml`:

```xml
<!-- Dashboard -->
<string name="greeting_format">Chào %1$s! 💪</string>
<string name="stat_label_weight">CÂN NẶNG</string>
<string name="stat_label_bmi">BMI</string>
<string name="stat_label_goal">MỤC TIÊU</string>
<string name="stat_unit_kg">kg</string>
<string name="ai_suggestion_label">⚡ AI ĐỀ XUẤT</string>
<string name="btn_start">Bắt đầu</string>
<string name="workout_subtitle_format">%1$d bài • %2$d phút • %3$s</string>
<string name="error_load_dashboard">Không thể tải dữ liệu. Kéo xuống để thử lại.</string>
<string name="no_workout_today">Hôm nay không có bài tập. Hãy nghỉ ngơi! 🧘</string>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat(dashboard): add Vietnamese string resources for dashboard"
```

---

## Task 3: Update item_stat_card.xml with Rounded Background

**Files:**
- Modify: `app/src/main/res/layout/item_stat_card.xml`

- [ ] **Step 1: Replace flat background with rounded drawable**

Replace the root `LinearLayout` background attribute in `app/src/main/res/layout/item_stat_card.xml`:

Change line 5 from:
```xml
android:background="@color/surface_container"
```
to:
```xml
android:background="@drawable/bg_stat_card_rounded"
```

The full file should be:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/bg_stat_card_rounded"
    android:gravity="center_horizontal"
    android:orientation="vertical"
    android:padding="@dimen/spacing_md">

    <TextView
        android:id="@+id/tv_stat_label"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Label"
        android:textAppearance="@style/TextStyle.LabelCaps" />

    <TextView
        android:id="@+id/tv_stat_value"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="@dimen/spacing_xs"
        android:text="0"
        android:textAppearance="@style/TextStyle.HeadlineMd"
        android:textColor="@color/primary" />

    <TextView
        android:id="@+id/tv_stat_unit"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="unit"
        android:textAppearance="@style/TextStyle.LabelCaps" />
</LinearLayout>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/layout/item_stat_card.xml
git commit -m "feat(dashboard): apply rounded corners to stat cards"
```

---

## Task 4: Rewrite fragment_dashboard.xml

**Files:**
- Modify: `app/src/main/res/layout/fragment_dashboard.xml`

- [ ] **Step 1: Rewrite the full layout**

Replace the entire content of `app/src/main/res/layout/fragment_dashboard.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.swiperefreshlayout.widget.SwipeRefreshLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/swipe_refresh"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background">

    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <!-- Loading indicator -->
        <ProgressBar
            android:id="@+id/progress_bar"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:visibility="gone"
            android:indeterminateTint="@color/primary" />

        <androidx.core.widget.NestedScrollView
            android:id="@+id/scroll_view"
            android:layout_width="match_parent"
            android:layout_height="match_parent">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="@dimen/spacing_lg">

                <!-- Top Bar -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/spacing_lg"
                    android:gravity="center_vertical"
                    android:orientation="horizontal">

                    <TextView
                        android:id="@+id/tv_avatar"
                        android:layout_width="@dimen/avatar_sm"
                        android:layout_height="@dimen/avatar_sm"
                        android:background="@drawable/bg_avatar_circle"
                        android:gravity="center"
                        android:text="U"
                        android:textColor="@color/primary"
                        android:fontFamily="@font/inter_semibold"
                        android:textSize="16sp" />

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_marginStart="@dimen/spacing_md"
                        android:layout_weight="1"
                        android:text="FitAI"
                        android:textColor="@color/primary"
                        android:fontFamily="@font/montserrat_bold"
                        android:textSize="22sp" />

                    <ImageButton
                        android:id="@+id/btn_notification"
                        android:layout_width="@dimen/avatar_sm"
                        android:layout_height="@dimen/avatar_sm"
                        android:background="?attr/selectableItemBackgroundBorderless"
                        android:contentDescription="@string/notification"
                        android:src="@drawable/ic_notification_bell"
                        app:tint="@color/on_surface_variant" />
                </LinearLayout>

                <!-- Greeting -->
                <TextView
                    android:id="@+id/tv_greeting"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/spacing_lg"
                    android:textAppearance="@style/TextStyle.HeadlineMd" />

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/spacing_xs"
                    android:text="@string/break_limits"
                    android:textAppearance="@style/TextStyle.BodyBase" />

                <!-- Stats Grid -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/spacing_lg"
                    android:orientation="horizontal"
                    android:weightSum="3">

                    <include
                        android:id="@+id/stat_weight"
                        layout="@layout/item_stat_card"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:layout_marginEnd="@dimen/spacing_xs" />

                    <include
                        android:id="@+id/stat_bmi"
                        layout="@layout/item_stat_card"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:layout_marginStart="@dimen/spacing_xs"
                        android:layout_marginEnd="@dimen/spacing_xs" />

                    <include
                        android:id="@+id/stat_goal"
                        layout="@layout/item_stat_card"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:layout_marginStart="@dimen/spacing_xs" />
                </LinearLayout>

                <!-- AI Workout Card -->
                <androidx.cardview.widget.CardView
                    android:id="@+id/card_ai_workout"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/spacing_lg"
                    app:cardBackgroundColor="@color/surface_container"
                    app:cardCornerRadius="@dimen/radius_lg"
                    app:cardElevation="0dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:padding="@dimen/spacing_lg">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="@string/ai_suggestion_label"
                            android:textAppearance="@style/TextStyle.LabelCaps"
                            android:textColor="@color/primary" />

                        <TextView
                            android:id="@+id/tv_workout_title"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="@dimen/spacing_sm"
                            android:textAppearance="@style/TextStyle.HeadlineMd" />

                        <TextView
                            android:id="@+id/tv_workout_subtitle"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="@dimen/spacing_xs"
                            android:textAppearance="@style/TextStyle.BodyBase" />

                        <com.google.android.material.button.MaterialButton
                            android:id="@+id/btn_start_workout"
                            style="@style/Widget.SmartGym.Button.Primary"
                            android:layout_width="wrap_content"
                            android:layout_height="48dp"
                            android:layout_marginTop="@dimen/spacing_md"
                            android:text="@string/btn_start"
                            app:icon="@drawable/ic_play_circle"
                            app:iconGravity="textStart" />
                    </LinearLayout>
                </androidx.cardview.widget.CardView>

                <!-- Weekly Plan Header -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/spacing_xl"
                    android:gravity="center_vertical"
                    android:orientation="horizontal">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="@string/weekly_plan"
                        android:textAppearance="@style/TextStyle.BodyBold" />

                    <TextView
                        android:id="@+id/tv_view_all"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/view_all"
                        android:textAppearance="@style/TextStyle.LabelCaps"
                        android:textColor="@color/primary"
                        android:background="?attr/selectableItemBackgroundBorderless"
                        android:padding="@dimen/spacing_xs" />
                </LinearLayout>

                <!-- Weekly Plan List -->
                <androidx.recyclerview.widget.RecyclerView
                    android:id="@+id/rv_weekly_plan"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/spacing_md"
                    android:nestedScrollingEnabled="false" />

            </LinearLayout>
        </androidx.core.widget.NestedScrollView>

    </FrameLayout>
</androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
```

- [ ] **Step 2: Add "notification" string if not present**

Check if `@string/notification` exists in strings.xml. If not, add:

```xml
<string name="notification">Thông báo</string>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/fragment_dashboard.xml app/src/main/res/values/strings.xml
git commit -m "feat(dashboard): rewrite layout with SwipeRefresh, ProgressBar, proper icons"
```

---

## Task 5: Rewrite DashboardViewModel

**Files:**
- Rewrite: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModel.java`
- Test: `app/src/test/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModelTest.java`

- [ ] **Step 1: Write unit tests for DashboardViewModel**

Create `app/src/test/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModelTest.java`:

```java
package ntu.quy65132908.smartgym_ai.ui.dashboard;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;

public class DashboardViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private AuthRepository authRepository;
    @Mock private UserRepository userRepository;
    @Mock private WorkoutRepository workoutRepository;
    @Mock private FirebaseUser firebaseUser;

    private DashboardViewModel viewModel;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(firebaseUser.getUid()).thenReturn("test-uid");
        when(authRepository.getCurrentUser()).thenReturn(firebaseUser);
    }

    private void createViewModelWithUser(User user, List<Workout> workouts) {
        doAnswer(invocation -> {
            UserRepository.UserCallback cb = invocation.getArgument(1);
            cb.onSuccess(user);
            return null;
        }).when(userRepository).getUser(eq("test-uid"), any());

        doAnswer(invocation -> {
            WorkoutRepository.WorkoutListCallback cb = invocation.getArgument(1);
            cb.onSuccess(workouts);
            return null;
        }).when(workoutRepository).getWeeklyPlan(eq("test-uid"), any());

        viewModel = new DashboardViewModel(authRepository, userRepository, workoutRepository);
    }

    @Test
    public void avatarLetter_fromDisplayName_takesFirstCharUppercase() {
        User user = new User("test-uid", "Nguyen", "test@email.com");
        createViewModelWithUser(user, Collections.emptyList());

        assertEquals("N", viewModel.getAvatarLetter().getValue());
    }

    @Test
    public void avatarLetter_nullDisplayName_defaultsToU() {
        User user = new User("test-uid", null, "test@email.com");
        createViewModelWithUser(user, Collections.emptyList());

        assertEquals("U", viewModel.getAvatarLetter().getValue());
    }

    @Test
    public void avatarLetter_emptyDisplayName_defaultsToU() {
        User user = new User("test-uid", "", "test@email.com");
        createViewModelWithUser(user, Collections.emptyList());

        assertEquals("U", viewModel.getAvatarLetter().getValue());
    }

    @Test
    public void weight_fromUser_convertsToInteger() {
        User user = new User("test-uid", "Test", "test@email.com");
        user.setWeight(72.5f);
        createViewModelWithUser(user, Collections.emptyList());

        assertEquals(Integer.valueOf(72), viewModel.getWeight().getValue());
    }

    @Test
    public void weight_nullFromUser_defaultsToZero() {
        User user = new User("test-uid", "Test", "test@email.com");
        user.setWeight(null);
        createViewModelWithUser(user, Collections.emptyList());

        assertEquals(Integer.valueOf(0), viewModel.getWeight().getValue());
    }

    @Test
    public void bmi_fromUser_exposedCorrectly() {
        User user = new User("test-uid", "Test", "test@email.com");
        user.setBmi(22.5f);
        user.setBmiCategory("Bình thường");
        createViewModelWithUser(user, Collections.emptyList());

        assertEquals(Float.valueOf(22.5f), viewModel.getBmi().getValue());
        assertEquals("Bình thường", viewModel.getBmiCategory().getValue());
    }

    @Test
    public void goalWeight_parsedFromGoalString() {
        User user = new User("test-uid", "Test", "test@email.com");
        user.setGoal("65");
        createViewModelWithUser(user, Collections.emptyList());

        assertEquals(Integer.valueOf(65), viewModel.getGoalWeight().getValue());
    }

    @Test
    public void goalWeight_parsesNumberFromText() {
        User user = new User("test-uid", "Test", "test@email.com");
        user.setGoal("Giảm cân về 60kg");
        createViewModelWithUser(user, Collections.emptyList());

        assertEquals(Integer.valueOf(60), viewModel.getGoalWeight().getValue());
    }

    @Test
    public void goalWeight_nullGoal_defaultsToZero() {
        User user = new User("test-uid", "Test", "test@email.com");
        user.setGoal(null);
        createViewModelWithUser(user, Collections.emptyList());

        assertEquals(Integer.valueOf(0), viewModel.getGoalWeight().getValue());
    }

    @Test
    public void weeklyPlan_exposedFromRepository() {
        User user = new User("test-uid", "Test", "test@email.com");
        Workout w1 = new Workout("1", "Upper Body", "Chest focus", "High", 45);
        w1.setDayOfWeek(1);
        Workout w2 = new Workout("2", "Lower Body", "Leg focus", "Medium", 40);
        w2.setDayOfWeek(2);

        createViewModelWithUser(user, Arrays.asList(w1, w2));

        List<Workout> plan = viewModel.getWeeklyPlan().getValue();
        assertNotNull(plan);
        assertEquals(2, plan.size());
        assertEquals("Upper Body", plan.get(0).getTitle());
    }

    @Test
    public void isLoading_falseAfterDataLoads() {
        User user = new User("test-uid", "Test", "test@email.com");
        createViewModelWithUser(user, Collections.emptyList());

        assertEquals(Boolean.FALSE, viewModel.getIsLoading().getValue());
    }

    @Test
    public void noCurrentUser_setsLoadingFalse() {
        when(authRepository.getCurrentUser()).thenReturn(null);
        viewModel = new DashboardViewModel(authRepository, userRepository, workoutRepository);

        assertEquals(Boolean.FALSE, viewModel.getIsLoading().getValue());
    }

    @Test
    public void userLoadError_postsErrorMessage() {
        doAnswer(invocation -> {
            UserRepository.UserCallback cb = invocation.getArgument(1);
            cb.onError(new Exception("Network error"));
            return null;
        }).when(userRepository).getUser(eq("test-uid"), any());

        viewModel = new DashboardViewModel(authRepository, userRepository, workoutRepository);

        // isLoading should be false even on error
        assertEquals(Boolean.FALSE, viewModel.getIsLoading().getValue());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "ntu.quy65132908.smartgym_ai.ui.dashboard.DashboardViewModelTest" --info`

Expected: FAIL — the current DashboardViewModel doesn't have `getAvatarLetter()`, `getGoalWeight()`, `getWeeklyPlan()`, etc.

- [ ] **Step 3: Rewrite DashboardViewModel implementation**

Replace the entire content of `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModel.java`:

```java
package ntu.quy65132908.smartgym_ai.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

@HiltViewModel
public class DashboardViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final WorkoutRepository workoutRepository;

    // User profile data
    private final MutableLiveData<String> userName = new MutableLiveData<>("Bạn");
    private final MutableLiveData<String> avatarLetter = new MutableLiveData<>("U");
    private final MutableLiveData<Integer> weight = new MutableLiveData<>(0);
    private final MutableLiveData<Float> bmi = new MutableLiveData<>(0f);
    private final MutableLiveData<String> bmiCategory = new MutableLiveData<>("");
    private final MutableLiveData<Integer> goalWeight = new MutableLiveData<>(0);

    // AI Recommendation
    private final MutableLiveData<Workout> aiRecommendation = new MutableLiveData<>(null);

    // Weekly Plan
    private final MutableLiveData<List<Workout>> weeklyPlan = new MutableLiveData<>(Collections.emptyList());

    // UI State
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);
    private final SingleLiveEvent<String> errorMessage = new SingleLiveEvent<>();

    // Public getters
    public LiveData<String> getUserName() { return userName; }
    public LiveData<String> getAvatarLetter() { return avatarLetter; }
    public LiveData<Integer> getWeight() { return weight; }
    public LiveData<Float> getBmi() { return bmi; }
    public LiveData<String> getBmiCategory() { return bmiCategory; }
    public LiveData<Integer> getGoalWeight() { return goalWeight; }
    public LiveData<Workout> getAiRecommendation() { return aiRecommendation; }
    public LiveData<List<Workout>> getWeeklyPlan() { return weeklyPlan; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsRefreshing() { return isRefreshing; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    @Inject
    public DashboardViewModel(AuthRepository authRepository,
                              UserRepository userRepository,
                              WorkoutRepository workoutRepository) {
        this.authRepository = authRepository;
        this.userRepository = userRepository;
        this.workoutRepository = workoutRepository;
        loadUserData();
    }

    public void refresh() {
        isRefreshing.setValue(true);
        loadUserData();
    }

    private void loadUserData() {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            isLoading.setValue(false);
            isRefreshing.setValue(false);
            return;
        }

        String uid = currentUser.getUid();

        userRepository.getUser(uid, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(ntu.quy65132908.smartgym_ai.data.model.User user) {
                // Update user profile fields
                String name = user.getDisplayName();
                userName.postValue(name != null && !name.isEmpty() ? name : "Bạn");
                avatarLetter.postValue(computeAvatarLetter(name));
                weight.postValue(user.getWeight() != null ? user.getWeight().intValue() : 0);
                bmi.postValue(user.getBmi() != null ? user.getBmi() : 0f);
                bmiCategory.postValue(user.getBmiCategory() != null ? user.getBmiCategory() : "");
                goalWeight.postValue(parseGoalWeight(user.getGoal()));

                // Now load weekly plan
                loadWeeklyPlan(uid);
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                isRefreshing.postValue(false);
                errorMessage.postValue("Không thể tải dữ liệu. Kéo xuống để thử lại.");
            }
        });
    }

    private void loadWeeklyPlan(String uid) {
        workoutRepository.getWeeklyPlan(uid, new WorkoutRepository.WorkoutListCallback() {
            @Override
            public void onSuccess(List<Workout> workouts) {
                weeklyPlan.postValue(workouts);
                aiRecommendation.postValue(findTodayWorkout(workouts));
                isLoading.postValue(false);
                isRefreshing.postValue(false);
            }

            @Override
            public void onError(Exception e) {
                weeklyPlan.postValue(Collections.emptyList());
                aiRecommendation.postValue(null);
                isLoading.postValue(false);
                isRefreshing.postValue(false);
                errorMessage.postValue("Không thể tải kế hoạch tuần.");
            }
        });
    }

    private String computeAvatarLetter(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return "U";
        }
        return String.valueOf(displayName.trim().charAt(0)).toUpperCase();
    }

    private int parseGoalWeight(String goal) {
        if (goal == null || goal.isEmpty()) {
            return 0;
        }
        // Try parsing as pure number first
        try {
            return Integer.parseInt(goal.trim());
        } catch (NumberFormatException ignored) {}

        // Extract first numeric value from text like "Giảm cân về 60kg"
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(goal);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group());
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private Workout findTodayWorkout(List<Workout> workouts) {
        if (workouts == null || workouts.isEmpty()) {
            return null;
        }
        int todayDow = getTodayDayOfWeek();
        for (Workout w : workouts) {
            if (w.getDayOfWeek() == todayDow) {
                return w;
            }
        }
        return null;
    }

    private int getTodayDayOfWeek() {
        int calDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        // Convert from Calendar (SUN=1, MON=2...) to our format (MON=1...SUN=7)
        return calDay == Calendar.SUNDAY ? 7 : calDay - 1;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "ntu.quy65132908.smartgym_ai.ui.dashboard.DashboardViewModelTest" --info`

Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModel.java app/src/test/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModelTest.java
git commit -m "feat(dashboard): rewrite DashboardViewModel with full LiveData state management"
```

---

## Task 6: Rewrite DashboardFragment

**Files:**
- Rewrite: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardFragment.java`

- [ ] **Step 1: Rewrite DashboardFragment as pure observer**

Replace the entire content of `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardFragment.java`:

```java
package ntu.quy65132908.smartgym_ai.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.databinding.FragmentDashboardBinding;

@AndroidEntryPoint
public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;
    private WeeklyPlanAdapter weeklyPlanAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        setupStatLabels();
        setupRecyclerView();
        setupSwipeRefresh();
        setupClickListeners();
        observeViewModel();
    }

    private void setupStatLabels() {
        // Set static labels from string resources
        binding.statWeight.tvStatLabel.setText(R.string.stat_label_weight);
        binding.statWeight.tvStatUnit.setText(R.string.stat_unit_kg);
        binding.statBmi.tvStatLabel.setText(R.string.stat_label_bmi);
        binding.statGoal.tvStatLabel.setText(R.string.stat_label_goal);
        binding.statGoal.tvStatUnit.setText(R.string.stat_unit_kg);
    }

    private void setupRecyclerView() {
        weeklyPlanAdapter = new WeeklyPlanAdapter();
        binding.rvWeeklyPlan.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvWeeklyPlan.setAdapter(weeklyPlanAdapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.primary);
        binding.swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.surface_container);
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
    }

    private void setupClickListeners() {
        binding.btnStartWorkout.setOnClickListener(v -> {
            Workout recommendation = viewModel.getAiRecommendation().getValue();
            if (recommendation != null && recommendation.getId() != null) {
                Bundle args = new Bundle();
                args.putString("workoutId", recommendation.getId());
                Navigation.findNavController(v).navigate(
                        R.id.action_dashboard_to_workout_detail, args);
            }
        });

        binding.tvViewAll.setOnClickListener(v -> {
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_nav);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_workout);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getUserName().observe(getViewLifecycleOwner(), name ->
                binding.tvGreeting.setText(getString(R.string.greeting_format, name)));

        viewModel.getAvatarLetter().observe(getViewLifecycleOwner(), letter ->
                binding.tvAvatar.setText(letter));

        viewModel.getWeight().observe(getViewLifecycleOwner(), w ->
                binding.statWeight.tvStatValue.setText(String.valueOf(w)));

        viewModel.getBmi().observe(getViewLifecycleOwner(), bmiVal ->
                binding.statBmi.tvStatValue.setText(String.format("%.1f", bmiVal)));

        viewModel.getBmiCategory().observe(getViewLifecycleOwner(), category ->
                binding.statBmi.tvStatUnit.setText(category));

        viewModel.getGoalWeight().observe(getViewLifecycleOwner(), goal ->
                binding.statGoal.tvStatValue.setText(String.valueOf(goal)));

        viewModel.getAiRecommendation().observe(getViewLifecycleOwner(), workout -> {
            if (workout != null) {
                binding.cardAiWorkout.setVisibility(View.VISIBLE);
                binding.tvWorkoutTitle.setText(workout.getTitle());
                int exerciseCount = workout.getExercises() != null ? workout.getExercises().size() : 0;
                binding.tvWorkoutSubtitle.setText(getString(
                        R.string.workout_subtitle_format,
                        exerciseCount,
                        workout.getDurationMinutes(),
                        workout.getIntensity() != null ? workout.getIntensity() : ""));
            } else {
                binding.cardAiWorkout.setVisibility(View.GONE);
            }
        });

        viewModel.getWeeklyPlan().observe(getViewLifecycleOwner(), plan ->
                weeklyPlanAdapter.submitList(plan));

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.scrollView.setVisibility(loading ? View.GONE : View.VISIBLE);
        });

        viewModel.getIsRefreshing().observe(getViewLifecycleOwner(), refreshing ->
                binding.swipeRefresh.setRefreshing(refreshing));

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg ->
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
```

- [ ] **Step 2: Verify the app compiles**

Run: `./gradlew :app:assembleDebug`

Expected: BUILD SUCCESSFUL. If there are binding errors (e.g., `scrollView` not found), verify the XML ids match: `scroll_view` → `scrollView` in ViewBinding (underscore converts to camelCase automatically).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardFragment.java
git commit -m "feat(dashboard): rewrite Fragment as pure observer with all state bindings"
```

---

## Task 7: Final Integration Test and Cleanup

**Files:**
- Verify: All modified/created files
- Test: Manual verification on device/emulator

- [ ] **Step 1: Run full test suite**

Run: `./gradlew :app:testDebugUnitTest --info`

Expected: All tests pass including the new `DashboardViewModelTest`.

- [ ] **Step 2: Run lint check**

Run: `./gradlew :app:lintDebug`

Expected: No new errors introduced. Warnings are acceptable.

- [ ] **Step 3: Build debug APK**

Run: `./gradlew :app:assembleDebug`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit final state**

```bash
git add -A
git commit -m "feat(dashboard): complete home tab overhaul - all 15 issues fixed"
```

---

## Verification Checklist

After completing all tasks, verify against the original 15 issues:

| # | Issue | Verification |
|---|-------|--------------|
| 1 | Weekly plan empty | RecyclerView shows workout items from Firestore |
| 2 | BMI never updates | BMI stat card shows live value from user profile |
| 3 | Goal hardcoded | Goal stat shows parsed value from User.goal |
| 4 | Avatar always "U" | Shows first letter of user's display name |
| 5 | Silent errors | Snackbar appears on network/load failure |
| 6 | No rounded corners | Stat cards have 12dp rounded background |
| 7 | System notification icon | Custom Material bell icon displayed |
| 8 | Text emoji play | Proper vector icon on button |
| 9 | No loading state | ProgressBar shown during initial load |
| 10 | Conflicting clicks | Only button navigates, card is not clickable |
| 11 | "XEM TẤT CẢ" dead | Tapping switches to Workout tab |
| 12 | No pull-to-refresh | SwipeRefreshLayout triggers viewModel.refresh() |
| 13 | AI recommendation static | Shows today's workout from weekly plan |
| 14 | Hardcoded Vietnamese | All strings from strings.xml |
| 15 | BMI category not shown | Displayed as stat_bmi unit text |
