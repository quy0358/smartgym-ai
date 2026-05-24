# Dashboard Deep Audit Remediation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix 25 identified issues in the Dashboard Home screen across Visual/UI, Logic/Data, Architecture, and Polish categories.

**Architecture:** Android MVVM with Hilt DI. DashboardViewModel is the single source of truth; DashboardFragment is a pure observer. Fixes are applied in 4 phases: Visual → Logic → Architecture → Polish. Each phase is independently testable.

**Tech Stack:** Java 17, Android SDK 35, Hilt 2.59.2, Firebase Firestore, ViewBinding, Navigation Component, JUnit/Mockito.

---

## File Structure

### Files to Create

| File | Responsibility |
|------|---------------|
| `app/src/main/res/drawable/ic_dumbbell.xml` | Custom fitness icon for weekly plan items |
| `app/src/main/res/drawable/ic_check_circle.xml` | Green checkmark for completed workouts |
| `app/src/main/res/drawable/bg_workout_item_rounded.xml` | Rounded background for weekly plan items |
| `app/src/main/java/ntu/quy65132908/smartgym_ai/util/DateUtils.java` | Shared date utility (getTodayDayOfWeek) |
| `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/TodayState.java` | Tri-state enum for today's workout status |
| `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardError.java` | Error code enum replacing hardcoded strings |
| `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/navigation/BottomNavHost.java` | Interface for tab switching |
| `app/src/test/java/ntu/quy65132908/smartgym_ai/util/DateUtilsTest.java` | Tests for DateUtils |
| `app/src/test/java/ntu/quy65132908/smartgym_ai/ui/dashboard/TodayStateTest.java` | Tests for rest day detection |

### Files to Modify

| File | Changes |
|------|---------|
| `app/src/main/res/layout/item_workout_day.xml` | Replace icon, add rounded background, add completed state |
| `app/src/main/res/layout/item_stat_card.xml` | Remove hardcoded text color |
| `app/src/main/res/layout/fragment_dashboard.xml` | Add loading text, rename tv_view_all, add comment |
| `app/src/main/res/values/strings.xml` | Add new strings for loading, refresh, accessibility |
| `app/src/main/res/navigation/nav_graph.xml` | Add missing argument declarations |
| `app/src/main/java/ntu/quy65132908/smartgym_ai/data/model/Workout.java` | Add exerciseCount field |
| `app/src/main/java/ntu/quy65132908/smartgym_ai/data/repository/WorkoutRepository.java` | Set exerciseCount on save, add cache metadata |
| `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModel.java` | Major: TodayState, throttle, error codes, MediatorLiveData, refresh success |
| `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardFragment.java` | Observer updates, BottomNavHost, accessibility |
| `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/WeeklyPlanAdapter.java` | Use DateUtils, show completed state |
| `app/src/main/java/ntu/quy65132908/smartgym_ai/MainActivity.java` | Implement BottomNavHost |
| `app/src/test/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModelTest.java` | Add tests for new logic |

---

## Phase 1: Visual/UI Fixes

### Task 1: Create drawable resources

**Files:**
- Create: `app/src/main/res/drawable/ic_dumbbell.xml`
- Create: `app/src/main/res/drawable/ic_check_circle.xml`
- Create: `app/src/main/res/drawable/bg_workout_item_rounded.xml`

- [ ] **Step 1: Create ic_dumbbell.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/on_surface_variant"
        android:pathData="M20.57,14.86L22,13.43L20.57,12L17,15.57L8.43,7L12,3.43L10.57,2L9.14,3.43L7.71,2L5.57,4.14L4.14,2.71L2.71,4.14L4.14,5.57L2,7.71L3.43,9.14L2,10.57L3.43,12L7,8.43L15.57,17L12,20.57L13.43,22L14.86,20.57L16.29,22L18.43,19.86L19.86,21.29L21.29,19.86L19.86,18.43L22,16.29L20.57,14.86Z" />
</vector>
```

- [ ] **Step 2: Create ic_check_circle.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/primary"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM10,17l-5,-5 1.41,-1.41L10,14.17l7.59,-7.59L19,8l-9,9z" />
</vector>
```

- [ ] **Step 3: Create bg_workout_item_rounded.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/surface_container" />
    <corners android:radius="@dimen/radius_md" />
</shape>
```

- [ ] **Step 4: Verify build compiles**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/drawable/ic_dumbbell.xml app/src/main/res/drawable/ic_check_circle.xml app/src/main/res/drawable/bg_workout_item_rounded.xml
git commit -m "feat(dashboard): add drawable resources for workout items"
```

---

### Task 2: Fix item_workout_day.xml — icon and rounded corners

**Files:**
- Modify: `app/src/main/res/layout/item_workout_day.xml`

- [ ] **Step 1: Replace system icon and add rounded background**

Replace the entire file content with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="@dimen/spacing_sm"
    android:background="@drawable/bg_workout_item_rounded"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:padding="@dimen/spacing_md"
    tools:ignore="Overdraw">

    <!-- Status Icon -->
    <FrameLayout
        android:layout_width="@dimen/avatar_sm"
        android:layout_height="@dimen/avatar_sm"
        android:background="@drawable/bg_rounded_12">

        <ImageView
            android:id="@+id/iv_status"
            android:layout_width="@dimen/icon_md"
            android:layout_height="@dimen/icon_md"
            android:layout_gravity="center"
            android:importantForAccessibility="no"
            android:src="@drawable/ic_dumbbell"
            app:tint="@color/on_surface_variant" />
    </FrameLayout>

    <!-- Info -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="@dimen/spacing_md"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:id="@+id/tv_workout_name"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAppearance="@style/TextStyle.BodyBold"
            tools:text="Upper Body Strength" />

        <TextView
            android:id="@+id/tv_workout_day"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAppearance="@style/TextStyle.LabelCaps"
            tools:text="Thứ 2" />
    </LinearLayout>

    <!-- Badge -->
    <TextView
        android:id="@+id/tv_badge"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:background="@drawable/bg_badge_today"
        android:paddingHorizontal="@dimen/spacing_sm"
        android:paddingVertical="2dp"
        android:text="@string/today_badge"
        android:textAppearance="@style/TextStyle.LabelCaps"
        android:textColor="@color/primary"
        android:textSize="11sp"
        android:visibility="gone" />
</LinearLayout>
```

- [ ] **Step 2: Verify build compiles**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/item_workout_day.xml
git commit -m "fix(dashboard): replace system icon with custom dumbbell, add rounded corners"
```

---

### Task 3: Fix item_stat_card.xml — remove hardcoded text color

**Files:**
- Modify: `app/src/main/res/layout/item_stat_card.xml`

- [ ] **Step 1: Change tv_stat_value textColor from primary to on_surface**

In `item_stat_card.xml`, change line 29 from:
```xml
android:textColor="@color/primary"
```
to:
```xml
android:textColor="@color/on_surface"
```

- [ ] **Step 2: Verify build compiles**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/item_stat_card.xml
git commit -m "fix(dashboard): use neutral text color for stat values, allow dynamic override"
```

---

### Task 4: Update fragment_dashboard.xml — loading text, rename btn, add comment

**Files:**
- Modify: `app/src/main/res/layout/fragment_dashboard.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add loading message string**

Add to `strings.xml` before the closing `</resources>` tag:

```xml
    <string name="loading_dashboard">Đang tải dữ liệu...</string>
    <string name="refresh_success">Đã cập nhật!</string>
    <string name="data_offline">Dữ liệu offline</string>
    <string name="stat_weight_a11y">Cân nặng: %1$s kg</string>
    <string name="stat_bmi_a11y">BMI: %1$s, %2$s</string>
    <string name="stat_goal_a11y">Mục tiêu: %1$s kg</string>
```

- [ ] **Step 2: Add loading text below ProgressBar in fragment_dashboard.xml**

After the `ProgressBar` (line 20), add:

```xml
        <TextView
            android:id="@+id/tv_loading"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:layout_marginTop="80dp"
            android:text="@string/loading_dashboard"
            android:textAppearance="@style/TextStyle.BodyBase"
            android:visibility="gone" />
```

- [ ] **Step 3: Rename tv_view_all to btn_view_all**

In `fragment_dashboard.xml`, change the id of the MaterialButton "Xem tất cả" from:
```xml
android:id="@+id/tv_view_all"
```
to:
```xml
android:id="@+id/btn_view_all"
```

- [ ] **Step 4: Add documentation comment to RecyclerView**

Before the RecyclerView for weekly plan, add XML comment:
```xml
                <!-- nestedScrollingEnabled=false required inside NestedScrollView;
                     max 7 items so recycling loss is negligible -->
```

- [ ] **Step 5: Verify build compiles**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD FAILURE — `binding.tvViewAll` no longer exists (renamed to `btnViewAll`)

This is expected. We'll fix the Fragment reference in a later task. For now, temporarily comment out the line in `DashboardFragment.java` that references `binding.tvViewAll` (line 87):

```java
        // binding.tvViewAll.setOnClickListener(v -> navigateToWorkoutTab());
        binding.btnViewAll.setOnClickListener(v -> navigateToWorkoutTab());
```

- [ ] **Step 6: Verify build compiles after fix**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/res/layout/fragment_dashboard.xml app/src/main/res/values/strings.xml app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardFragment.java
git commit -m "feat(dashboard): add loading text, rename btn_view_all, add new strings"
```

---

### Task 5: Update WeeklyPlanAdapter — show completed state visually

**Files:**
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/WeeklyPlanAdapter.java`

- [ ] **Step 1: Add completed visual logic to onBindViewHolder**

In `WeeklyPlanAdapter.java`, replace the `onBindViewHolder` method (lines 60-79) with:

```java
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Workout workout = getItem(position);
        int dayIndex = workout.getDayOfWeek() - 1;
        String dayName = (dayIndex >= 0 && dayIndex < DAY_NAMES.length) ? DAY_NAMES[dayIndex] : "Ngày " + workout.getDayOfWeek();

        holder.binding.tvWorkoutDay.setText(dayName);
        holder.binding.tvWorkoutName.setText(workout.getTitle());

        int todayDow = getTodayDayOfWeek();
        holder.binding.tvBadge.setVisibility(workout.getDayOfWeek() == todayDow ? View.VISIBLE : View.GONE);

        // Show completed state
        if (workout.isCompleted()) {
            holder.binding.ivStatus.setImageResource(R.drawable.ic_check_circle);
            holder.binding.ivStatus.setImageTintList(null); // Use drawable's own fill color
            holder.itemView.setAlpha(0.7f);
        } else {
            holder.binding.ivStatus.setImageResource(R.drawable.ic_dumbbell);
            holder.binding.ivStatus.setImageTintList(
                    android.content.res.ColorStateList.valueOf(
                            holder.itemView.getContext().getColor(R.color.on_surface_variant)));
            holder.itemView.setAlpha(1f);
        }

        boolean hasValidId = workout.getId() != null && !workout.getId().isEmpty();
        holder.itemView.setEnabled(hasValidId);
        if (!hasValidId) {
            holder.itemView.setAlpha(0.55f);
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && hasValidId) {
                listener.onWorkoutClick(workout);
            }
        });
    }
```

- [ ] **Step 2: Add missing import**

Add at the top of the file:
```java
import ntu.quy65132908.smartgym_ai.R;
```

- [ ] **Step 3: Verify build compiles**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/WeeklyPlanAdapter.java
git commit -m "feat(dashboard): show checkmark icon for completed workouts"
```

---

## Phase 2: Logic/Data Fixes

### Task 6: Create utility classes — DateUtils, TodayState, DashboardError

**Files:**
- Create: `app/src/main/java/ntu/quy65132908/smartgym_ai/util/DateUtils.java`
- Create: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/TodayState.java`
- Create: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardError.java`
- Create: `app/src/test/java/ntu/quy65132908/smartgym_ai/util/DateUtilsTest.java`

- [ ] **Step 1: Write DateUtils test**

Create `app/src/test/java/ntu/quy65132908/smartgym_ai/util/DateUtilsTest.java`:

```java
package ntu.quy65132908.smartgym_ai.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class DateUtilsTest {

    @Test
    public void getTodayDayOfWeek_returnsValidRange() {
        int today = DateUtils.getTodayDayOfWeek();
        assertTrue("Day of week must be >= 1", today >= 1);
        assertTrue("Day of week must be <= 7", today <= 7);
    }

    @Test
    public void isRestDayWorkout_nullTitle_returnsFalse() {
        assertFalse(DateUtils.isRestDayWorkout(null, 30));
    }

    @Test
    public void isRestDayWorkout_restInTitle_returnsTrue() {
        assertTrue(DateUtils.isRestDayWorkout("Rest day/ Recover", 0));
    }

    @Test
    public void isRestDayWorkout_nghiInTitle_returnsTrue() {
        assertTrue(DateUtils.isRestDayWorkout("Nghỉ ngơi", 0));
    }

    @Test
    public void isRestDayWorkout_zeroDurationNoExercises_returnsTrue() {
        assertTrue(DateUtils.isRestDayWorkout("Something", 0));
    }

    @Test
    public void isRestDayWorkout_normalWorkout_returnsFalse() {
        assertFalse(DateUtils.isRestDayWorkout("Tập nặng cơ ngực", 45));
    }

    @Test
    public void isRestDayWorkout_recoverInTitle_returnsTrue() {
        assertTrue(DateUtils.isRestDayWorkout("Recovery day", 0));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "ntu.quy65132908.smartgym_ai.util.DateUtilsTest"`
Expected: FAIL — class DateUtils does not exist

- [ ] **Step 3: Create DateUtils.java**

Create `app/src/main/java/ntu/quy65132908/smartgym_ai/util/DateUtils.java`:

```java
package ntu.quy65132908.smartgym_ai.util;

import java.util.Calendar;
import java.util.Locale;

/**
 * Shared date/time utilities for the Dashboard and adapters.
 */
public final class DateUtils {

    private DateUtils() {} // Utility class

    /**
     * Returns today's day of week as 1=Monday ... 7=Sunday.
     */
    public static int getTodayDayOfWeek() {
        int calDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return calDay == Calendar.SUNDAY ? 7 : calDay - 1;
    }

    /**
     * Determines if a workout represents a rest/recovery day.
     * Returns true if title contains rest/recovery keywords OR duration is 0.
     */
    public static boolean isRestDayWorkout(String title, int durationMinutes) {
        if (durationMinutes == 0) {
            return true;
        }
        if (title == null || title.trim().isEmpty()) {
            return false;
        }
        String lower = title.toLowerCase(Locale.ROOT);
        return lower.contains("rest") || lower.contains("nghỉ") || lower.contains("recover");
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "ntu.quy65132908.smartgym_ai.util.DateUtilsTest"`
Expected: PASS — all 7 tests pass

- [ ] **Step 5: Create TodayState.java**

Create `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/TodayState.java`:

```java
package ntu.quy65132908.smartgym_ai.ui.dashboard;

/**
 * Represents the state of today's workout on the Dashboard.
 */
public enum TodayState {
    /** Today has a real workout to perform */
    WORKOUT,
    /** Today is a rest/recovery day (deliberate or implicit) */
    REST_DAY,
    /** User has no weekly plan at all */
    NO_PLAN
}
```

- [ ] **Step 6: Create DashboardError.java**

Create `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardError.java`:

```java
package ntu.quy65132908.smartgym_ai.ui.dashboard;

/**
 * Error codes for Dashboard data loading.
 * Fragment maps these to user-visible string resources.
 */
public enum DashboardError {
    USER_LOAD_FAILED,
    WEEKLY_PLAN_LOAD_FAILED
}
```

- [ ] **Step 7: Verify build compiles**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/ntu/quy65132908/smartgym_ai/util/DateUtils.java app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/TodayState.java app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardError.java app/src/test/java/ntu/quy65132908/smartgym_ai/util/DateUtilsTest.java
git commit -m "feat(dashboard): add DateUtils, TodayState, DashboardError utilities"
```

---

### Task 7: Add exerciseCount field to Workout model

**Files:**
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/data/model/Workout.java`

- [ ] **Step 1: Add exerciseCount field**

In `Workout.java`, after the `dayOfWeek` field declaration (line 13), add:

```java
    private int exerciseCount; // Denormalized count for dashboard display
```

And add getter/setter after the `setDayOfWeek` method:

```java
    public int getExerciseCount() { return exerciseCount; }
    public void setExerciseCount(int exerciseCount) { this.exerciseCount = exerciseCount; }
```

- [ ] **Step 2: Verify build compiles**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/ntu/quy65132908/smartgym_ai/data/model/Workout.java
git commit -m "feat(model): add exerciseCount field to Workout for denormalized display"
```

---

### Task 8: Update WorkoutRepository — set exerciseCount on save

**Files:**
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/data/repository/WorkoutRepository.java`

- [ ] **Step 1: Set exerciseCount before batch write in saveWeeklyPlan**

In `WorkoutRepository.java`, inside the `saveWeeklyPlan` method, after `workout.setId(workoutRef.getId());` (line 105), add:

```java
            // Denormalize exercise count for efficient dashboard reads
            if (workout.getExercises() != null) {
                workout.setExerciseCount(workout.getExercises().size());
            }
```

- [ ] **Step 2: Verify build compiles**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/ntu/quy65132908/smartgym_ai/data/repository/WorkoutRepository.java
git commit -m "fix(repository): denormalize exerciseCount when saving weekly plan"
```

---

### Task 9: Rewrite DashboardViewModel — TodayState, throttle, error codes, MediatorLiveData

**Files:**
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModel.java`
- Modify: `app/src/test/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModelTest.java`

- [ ] **Step 1: Write tests for new TodayState logic**

Add the following tests to `DashboardViewModelTest.java`:

```java
    @Test
    public void todayState_noPlan_returnsNoPlan() {
        User user = new User("test-uid", "Test", "test@email.com");
        DashboardViewModel viewModel = createViewModelWithUser(user, Collections.emptyList());

        assertEquals(TodayState.NO_PLAN, viewModel.getTodayState().getValue());
    }

    @Test
    public void todayState_restDayWorkout_returnsRestDay() {
        User user = new User("test-uid", "Test", "test@email.com");
        int today = ntu.quy65132908.smartgym_ai.util.DateUtils.getTodayDayOfWeek();
        Workout restWorkout = new Workout("1", "Rest day/ Recover", "", "", 0);
        restWorkout.setDayOfWeek(today);

        DashboardViewModel viewModel = createViewModelWithUser(user, Collections.singletonList(restWorkout));

        assertEquals(TodayState.REST_DAY, viewModel.getTodayState().getValue());
    }

    @Test
    public void todayState_normalWorkout_returnsWorkout() {
        User user = new User("test-uid", "Test", "test@email.com");
        int today = ntu.quy65132908.smartgym_ai.util.DateUtils.getTodayDayOfWeek();
        Workout workout = new Workout("1", "Tập nặng cơ ngực", "Chest", "High", 45);
        workout.setDayOfWeek(today);

        DashboardViewModel viewModel = createViewModelWithUser(user, Collections.singletonList(workout));

        assertEquals(TodayState.WORKOUT, viewModel.getTodayState().getValue());
    }

    @Test
    public void todayState_planExistsButNoWorkoutToday_returnsRestDay() {
        User user = new User("test-uid", "Test", "test@email.com");
        int today = ntu.quy65132908.smartgym_ai.util.DateUtils.getTodayDayOfWeek();
        int otherDay = today == 1 ? 2 : 1;
        Workout workout = new Workout("1", "Tập nặng cơ ngực", "Chest", "High", 45);
        workout.setDayOfWeek(otherDay);

        DashboardViewModel viewModel = createViewModelWithUser(user, Collections.singletonList(workout));

        assertEquals(TodayState.REST_DAY, viewModel.getTodayState().getValue());
    }

    @Test
    public void refresh_withinCooldown_doesNotReload() {
        User user = new User("test-uid", "Test", "test@email.com");
        DashboardViewModel viewModel = createViewModelWithUser(user, Collections.emptyList());

        // First refresh should work
        viewModel.refresh();
        // Immediate second refresh should be ignored (within 5s cooldown)
        viewModel.refresh();

        // Verify userRepository was called exactly 2 times total (constructor + first refresh)
        verify(userRepository, times(2)).getUser(eq("test-uid"), any());
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat testDebugUnitTest --tests "ntu.quy65132908.smartgym_ai.ui.dashboard.DashboardViewModelTest"`
Expected: FAIL — `getTodayState()` method doesn't exist

- [ ] **Step 3: Update DashboardViewModel with all new logic**

Replace the full content of `DashboardViewModel.java` with:

```java
package ntu.quy65132908.smartgym_ai.ui.dashboard;

import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;
import ntu.quy65132908.smartgym_ai.util.DateUtils;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

/**
 * ViewModel for the Dashboard Home tab.
 * 
 * Data flow:
 * loadUserData() → [UserRepository.getUser]
 *   → onSuccess → loadWeeklyPlan()
 *   → [WorkoutRepository.getWeeklyPlan]
 *   → onSuccess → update all LiveData, set isLoading=false
 * Error at any step → post error event, set isLoading=false
 */
@HiltViewModel
public class DashboardViewModel extends ViewModel {

    private static final Pattern GOAL_NUMBER_PATTERN = Pattern.compile("[-+]?\\d+");
    private static final String PLACEHOLDER = "--";
    private static final String UNICODE_MINUS = "\u2212";
    private static final long REFRESH_COOLDOWN_MS = 5000; // 5 seconds

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final WorkoutRepository workoutRepository;

    // User profile data
    private final MutableLiveData<String> userName = new MutableLiveData<>("Bạn");
    private final MutableLiveData<String> avatarLetter = new MutableLiveData<>("U");
    private final MutableLiveData<String> photoUrl = new MutableLiveData<>("");
    private final MutableLiveData<Integer> weight = new MutableLiveData<>();
    private final MutableLiveData<Float> bmi = new MutableLiveData<>();
    private final MutableLiveData<String> bmiCategory = new MutableLiveData<>("");
    private final MutableLiveData<Integer> goalWeight = new MutableLiveData<>();
    private final MutableLiveData<Integer> bmiColorRes = new MutableLiveData<>(R.color.on_surface_variant);
    private final MutableLiveData<String> goalDisplay = new MutableLiveData<>(PLACEHOLDER);

    // AI Recommendation & Today State
    private final MutableLiveData<Workout> aiRecommendation = new MutableLiveData<>(null);
    private final MutableLiveData<TodayState> todayState = new MutableLiveData<>(TodayState.NO_PLAN);

    // Weekly Plan
    private final MutableLiveData<List<Workout>> weeklyPlan = new MutableLiveData<>(Collections.emptyList());

    // Combined avatar data to avoid race conditions
    private final MediatorLiveData<Pair<String, String>> avatarData = new MediatorLiveData<>();

    // UI State
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);
    private final SingleLiveEvent<DashboardError> errorEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> requireLoginEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> refreshSuccessEvent = new SingleLiveEvent<>();

    // Throttle tracking
    private long lastRefreshTime = 0;
    private boolean isLoadInProgress = false;

    // Public getters
    public LiveData<String> getUserName() { return userName; }
    public LiveData<String> getAvatarLetter() { return avatarLetter; }
    public LiveData<String> getPhotoUrl() { return photoUrl; }
    public LiveData<Pair<String, String>> getAvatarData() { return avatarData; }
    public LiveData<Integer> getWeight() { return weight; }
    public LiveData<Float> getBmi() { return bmi; }
    public LiveData<String> getBmiCategory() { return bmiCategory; }
    public LiveData<Integer> getGoalWeight() { return goalWeight; }
    public LiveData<Integer> getBmiColorRes() { return bmiColorRes; }
    public LiveData<String> getGoalDisplay() { return goalDisplay; }
    public LiveData<Workout> getAiRecommendation() { return aiRecommendation; }
    public LiveData<TodayState> getTodayState() { return todayState; }
    public LiveData<List<Workout>> getWeeklyPlan() { return weeklyPlan; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsRefreshing() { return isRefreshing; }
    public LiveData<DashboardError> getErrorEvent() { return errorEvent; }
    public LiveData<Boolean> getRequireLoginEvent() { return requireLoginEvent; }
    public LiveData<Boolean> getRefreshSuccessEvent() { return refreshSuccessEvent; }

    @Inject
    public DashboardViewModel(AuthRepository authRepository,
                              UserRepository userRepository,
                              WorkoutRepository workoutRepository) {
        this.authRepository = authRepository;
        this.userRepository = userRepository;
        this.workoutRepository = workoutRepository;

        // Setup MediatorLiveData for avatar (prevents race condition)
        avatarData.addSource(userName, name ->
                avatarData.setValue(new Pair<>(name, photoUrl.getValue())));
        avatarData.addSource(photoUrl, url ->
                avatarData.setValue(new Pair<>(userName.getValue(), url)));

        loadUserData();
    }

    public void refresh() {
        long now = System.currentTimeMillis();
        if (now - lastRefreshTime < REFRESH_COOLDOWN_MS) {
            isRefreshing.setValue(false);
            return;
        }
        if (isLoadInProgress) {
            isRefreshing.setValue(false);
            return;
        }
        lastRefreshTime = now;
        isRefreshing.setValue(true);
        loadUserData();
    }

    private void loadUserData() {
        isLoadInProgress = true;
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            isLoading.setValue(false);
            isRefreshing.setValue(false);
            isLoadInProgress = false;
            requireLoginEvent.setValue(true);
            return;
        }

        String uid = currentUser.getUid();

        userRepository.getUser(uid, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(ntu.quy65132908.smartgym_ai.data.model.User user) {
                String name = user.getDisplayName();
                Float userBmi = user.getBmi();
                userName.postValue(name != null && !name.isEmpty() ? name : "Bạn");
                avatarLetter.postValue(computeAvatarLetter(name));
                photoUrl.postValue(user.getPhotoUrl() != null ? user.getPhotoUrl() : "");
                weight.postValue(user.getWeight() != null ? user.getWeight().intValue() : null);
                bmi.postValue(userBmi);
                bmiCategory.postValue(userBmi != null ? user.getBmiCategory() : "");
                goalWeight.postValue(parseGoalWeightOrNull(user.getGoal()));
                goalDisplay.postValue(formatGoalDisplay(user.getGoal()));
                bmiColorRes.postValue(userBmi != null
                        ? computeBmiColor(userBmi)
                        : R.color.on_surface_variant);

                loadWeeklyPlan(uid);
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                isRefreshing.postValue(false);
                isLoadInProgress = false;
                errorEvent.postValue(DashboardError.USER_LOAD_FAILED);
            }
        });
    }

    private void loadWeeklyPlan(String uid) {
        workoutRepository.getWeeklyPlan(uid, new WorkoutRepository.WorkoutListCallback() {
            @Override
            public void onSuccess(List<Workout> workouts) {
                weeklyPlan.postValue(workouts);

                // Determine today's state
                TodayState state = determineTodayState(workouts);
                todayState.postValue(state);

                // Set AI recommendation only for WORKOUT state
                if (state == TodayState.WORKOUT) {
                    aiRecommendation.postValue(findTodayWorkout(workouts));
                } else {
                    aiRecommendation.postValue(null);
                }

                isLoading.postValue(false);
                boolean wasRefreshing = Boolean.TRUE.equals(isRefreshing.getValue());
                isRefreshing.postValue(false);
                isLoadInProgress = false;

                if (wasRefreshing) {
                    refreshSuccessEvent.postValue(true);
                }
            }

            @Override
            public void onError(Exception e) {
                weeklyPlan.postValue(Collections.emptyList());
                todayState.postValue(TodayState.NO_PLAN);
                aiRecommendation.postValue(null);
                isLoading.postValue(false);
                isRefreshing.postValue(false);
                isLoadInProgress = false;
                errorEvent.postValue(DashboardError.WEEKLY_PLAN_LOAD_FAILED);
            }
        });
    }

    TodayState determineTodayState(List<Workout> workouts) {
        if (workouts == null || workouts.isEmpty()) {
            return TodayState.NO_PLAN;
        }

        Workout todayWorkout = findTodayWorkout(workouts);
        if (todayWorkout == null) {
            // Plan exists but no entry for today → implicit rest day
            return TodayState.REST_DAY;
        }

        if (DateUtils.isRestDayWorkout(todayWorkout.getTitle(), todayWorkout.getDurationMinutes())) {
            return TodayState.REST_DAY;
        }

        return TodayState.WORKOUT;
    }

    String computeAvatarLetter(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return "U";
        }
        return String.valueOf(displayName.trim().charAt(0)).toUpperCase(Locale.ROOT);
    }

    int computeBmiColor(float bmiValue) {
        if (bmiValue < 18.5f) return R.color.tertiary;
        if (bmiValue < 25.0f) return R.color.primary;
        if (bmiValue < 30.0f) return R.color.warning;
        return R.color.error;
    }

    String formatGoalDisplay(String goal) {
        GoalValue parsed = parseGoal(goal);
        if (parsed == null) {
            return PLACEHOLDER;
        }
        if (parsed.isDelta) {
            return formatSignedGoal(parsed.value);
        }
        return String.valueOf(Math.abs(parsed.value));
    }

    int parseGoalWeight(String goal) {
        Integer parsed = parseGoalWeightOrNull(goal);
        return parsed != null ? parsed : 0;
    }

    private Integer parseGoalWeightOrNull(String goal) {
        GoalValue parsed = parseGoal(goal);
        return parsed != null ? parsed.value : null;
    }

    private GoalValue parseGoal(String goal) {
        if (goal == null || goal.trim().isEmpty()) {
            return null;
        }

        Matcher matcher = GOAL_NUMBER_PATTERN.matcher(goal.trim());
        if (!matcher.find()) {
            return null;
        }

        String numberToken = matcher.group();
        int value;
        try {
            value = Integer.parseInt(numberToken);
        } catch (NumberFormatException ignored) {
            return null;
        }

        boolean explicitSign = numberToken.startsWith("+") || numberToken.startsWith("-");
        String normalized = normalizeGoal(goal);
        boolean targetGoal = hasTargetKeyword(normalized);
        boolean deltaGoal = explicitSign || (hasDeltaKeyword(normalized) && !targetGoal);

        if (deltaGoal && !explicitSign && containsWord(normalized, "giam")) {
            value = -Math.abs(value);
        }

        return new GoalValue(value, deltaGoal);
    }

    private String formatSignedGoal(int value) {
        if (value < 0) {
            return UNICODE_MINUS + Math.abs(value);
        }
        if (value > 0) {
            return "+" + value;
        }
        return "0";
    }

    private String normalizeGoal(String goal) {
        String decomposed = Normalizer.normalize(goal, Normalizer.Form.NFD);
        return decomposed.replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private boolean hasDeltaKeyword(String normalizedGoal) {
        return containsWord(normalizedGoal, "giam") || containsWord(normalizedGoal, "tang");
    }

    private boolean hasTargetKeyword(String normalizedGoal) {
        return containsWord(normalizedGoal, "ve")
                || containsWord(normalizedGoal, "xuong")
                || containsWord(normalizedGoal, "len")
                || containsWord(normalizedGoal, "den")
                || normalizedGoal.contains("muc tieu")
                || normalizedGoal.contains("target");
    }

    private boolean containsWord(String normalizedGoal, String word) {
        Pattern pattern = Pattern.compile("(^|\\W)" + Pattern.quote(word) + "(\\W|$)");
        return pattern.matcher(normalizedGoal).find();
    }

    private static class GoalValue {
        final int value;
        final boolean isDelta;

        GoalValue(int value, boolean isDelta) {
            this.value = value;
            this.isDelta = isDelta;
        }
    }

    private Workout findTodayWorkout(List<Workout> workouts) {
        if (workouts == null || workouts.isEmpty()) {
            return null;
        }
        int todayDow = DateUtils.getTodayDayOfWeek();
        for (Workout w : workouts) {
            if (w.getDayOfWeek() == todayDow) {
                return w;
            }
        }
        return null;
    }
}
```

- [ ] **Step 4: Add missing import in test file**

Add to `DashboardViewModelTest.java`:
```java
import ntu.quy65132908.smartgym_ai.ui.dashboard.TodayState;
import android.util.Pair;
```

- [ ] **Step 5: Run all Dashboard tests**

Run: `.\gradlew.bat testDebugUnitTest --tests "ntu.quy65132908.smartgym_ai.ui.dashboard.DashboardViewModelTest"`
Expected: ALL PASS (existing tests + new tests)

- [ ] **Step 6: Run full test suite**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: ALL PASS

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModel.java app/src/test/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModelTest.java
git commit -m "feat(dashboard): add TodayState, refresh throttle, error codes, MediatorLiveData"
```

---

### Task 10: Update WeeklyPlanAdapter to use DateUtils

**Files:**
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/WeeklyPlanAdapter.java`

- [ ] **Step 1: Replace getTodayDayOfWeek with DateUtils call**

In `WeeklyPlanAdapter.java`:

1. Add import: `import ntu.quy65132908.smartgym_ai.util.DateUtils;`
2. Replace the private method `getTodayDayOfWeek()` body with:
```java
    private int getTodayDayOfWeek() {
        return DateUtils.getTodayDayOfWeek();
    }
```

Or simply replace all usages of `getTodayDayOfWeek()` with `DateUtils.getTodayDayOfWeek()` and delete the private method.

3. Remove the `import java.util.Calendar;` since it's no longer needed.

- [ ] **Step 2: Verify build compiles**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/WeeklyPlanAdapter.java
git commit -m "refactor(dashboard): use DateUtils in WeeklyPlanAdapter, remove duplication"
```

---

### Task 11: Fix nav_graph.xml — add missing argument declarations

**Files:**
- Modify: `app/src/main/res/navigation/nav_graph.xml`

- [ ] **Step 1: Add workoutTitle and workoutDuration arguments to nav_workout_detail**

In `nav_graph.xml`, inside the `nav_workout_detail` fragment (after the existing `workoutId` argument), add:

```xml
        <argument
            android:name="workoutTitle"
            app:argType="string"
            android:defaultValue="" />
        <argument
            android:name="workoutDuration"
            app:argType="integer"
            android:defaultValue="0" />
```

- [ ] **Step 2: Verify build compiles**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/navigation/nav_graph.xml
git commit -m "fix(nav): add workoutTitle and workoutDuration args to workout detail"
```

---

### Task 12: Rewrite DashboardFragment — TodayState observer, error mapping, accessibility

**Files:**
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardFragment.java`

- [ ] **Step 1: Replace the full DashboardFragment.java**

```java
package ntu.quy65132908.smartgym_ai.ui.dashboard;

import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.databinding.FragmentDashboardBinding;
import ntu.quy65132908.smartgym_ai.ui.navigation.BottomNavHost;
import ntu.quy65132908.smartgym_ai.util.AvatarHelper;

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
        binding.statWeight.tvStatLabel.setText(R.string.stat_weight);
        binding.statWeight.tvStatUnit.setText(R.string.unit_kg);
        binding.statBmi.tvStatLabel.setText(R.string.stat_bmi);
        binding.statGoal.tvStatLabel.setText(R.string.stat_goal);
        binding.statGoal.tvStatUnit.setText(R.string.unit_kg);
    }

    private void setupRecyclerView() {
        weeklyPlanAdapter = new WeeklyPlanAdapter(this::navigateToWorkoutDetail);
        binding.rvWeeklyPlan.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvWeeklyPlan.setAdapter(weeklyPlanAdapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.primary);
        binding.swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.surface_container);
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
    }

    private void setupClickListeners() {
        binding.btnNotification.setVisibility(View.VISIBLE);
        binding.btnNotification.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), R.string.notification_coming_soon, Snackbar.LENGTH_SHORT).show());

        binding.btnStartWorkout.setOnClickListener(v -> {
            TodayState state = viewModel.getTodayState().getValue();
            if (state == TodayState.WORKOUT) {
                Workout recommendation = viewModel.getAiRecommendation().getValue();
                if (recommendation != null) {
                    navigateToWorkoutDetail(recommendation);
                }
            } else {
                navigateToWorkoutTab();
            }
        });

        binding.btnViewAll.setOnClickListener(v -> navigateToWorkoutTab());

        binding.btnCreatePlan.setOnClickListener(v -> navigateToWorkoutTab());
    }

    private void navigateToWorkoutDetail(Workout workout) {
        if (workout == null || workout.getId() == null || workout.getId().isEmpty()) {
            Snackbar.make(binding.getRoot(), R.string.error_open_workout, Snackbar.LENGTH_LONG).show();
            return;
        }
        Bundle args = new Bundle();
        args.putString("workoutId", workout.getId());
        args.putString("workoutTitle", workout.getTitle() != null ? workout.getTitle() : "");
        args.putInt("workoutDuration", workout.getDurationMinutes());
        Navigation.findNavController(binding.getRoot()).navigate(
                R.id.action_dashboard_to_workout_detail, args);
    }

    private void observeViewModel() {
        // Combined avatar observer — prevents race condition between name and photoUrl
        viewModel.getAvatarData().observe(getViewLifecycleOwner(), pair -> {
            if (pair != null) {
                AvatarHelper.loadAvatar(
                        requireContext(),
                        pair.second,
                        binding.ivAvatar,
                        binding.tvAvatar,
                        pair.first
                );
            }
        });

        viewModel.getUserName().observe(getViewLifecycleOwner(), name ->
                binding.tvGreeting.setText(getString(R.string.greeting_format, name)));

        viewModel.getWeight().observe(getViewLifecycleOwner(), w -> {
            boolean hasWeight = w != null;
            String weightText = hasWeight ? String.valueOf(w) : getString(R.string.value_unavailable);
            binding.statWeight.tvStatValue.setText(weightText);
            binding.statWeight.tvStatUnit.setVisibility(hasWeight ? View.VISIBLE : View.INVISIBLE);
            // Accessibility
            binding.statWeight.getRoot().setContentDescription(
                    hasWeight ? getString(R.string.stat_weight_a11y, String.valueOf(w)) : null);
        });

        viewModel.getBmi().observe(getViewLifecycleOwner(), bmiVal -> {
            String bmiText = bmiVal != null
                    ? String.format(Locale.getDefault(), "%.1f", bmiVal)
                    : getString(R.string.value_unavailable);
            binding.statBmi.tvStatValue.setText(bmiText);
        });

        viewModel.getBmiCategory().observe(getViewLifecycleOwner(), category -> {
            binding.statBmi.tvStatUnit.setText(category != null && !category.isEmpty()
                    ? category
                    : getString(R.string.value_unavailable));
            // Accessibility
            Float bmiVal = viewModel.getBmi().getValue();
            if (bmiVal != null) {
                binding.statBmi.getRoot().setContentDescription(
                        getString(R.string.stat_bmi_a11y,
                                String.format(Locale.getDefault(), "%.1f", bmiVal),
                                category != null ? category : ""));
            }
        });

        viewModel.getBmiColorRes().observe(getViewLifecycleOwner(), colorRes -> {
            int color = ContextCompat.getColor(requireContext(),
                    colorRes != null ? colorRes : R.color.on_surface_variant);
            binding.statBmi.tvStatValue.setTextColor(color);
            binding.statBmi.tvStatUnit.setTextColor(color);
        });

        viewModel.getGoalDisplay().observe(getViewLifecycleOwner(), display -> {
            boolean hasGoal = display != null && !display.equals(getString(R.string.value_unavailable))
                    && !display.equals("--");
            binding.statGoal.tvStatValue.setText(hasGoal ? display : getString(R.string.value_unavailable));
            binding.statGoal.tvStatUnit.setVisibility(hasGoal ? View.VISIBLE : View.INVISIBLE);
            // Accessibility
            binding.statGoal.getRoot().setContentDescription(
                    hasGoal ? getString(R.string.stat_goal_a11y, display) : null);
        });

        // Today State observer — controls which card is shown
        viewModel.getTodayState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case WORKOUT:
                    binding.cardAiWorkout.setVisibility(View.VISIBLE);
                    binding.cardRestDay.setVisibility(View.GONE);
                    break;
                case REST_DAY:
                    binding.cardAiWorkout.setVisibility(View.GONE);
                    binding.cardRestDay.setVisibility(View.VISIBLE);
                    break;
                case NO_PLAN:
                    binding.cardAiWorkout.setVisibility(View.VISIBLE);
                    binding.cardRestDay.setVisibility(View.GONE);
                    binding.tvWorkoutTitle.setText(R.string.ai_empty_workout_title);
                    binding.tvWorkoutSubtitle.setText(R.string.ai_empty_workout_subtitle);
                    binding.btnStartWorkout.setText(R.string.btn_create_plan);
                    break;
            }
        });

        viewModel.getAiRecommendation().observe(getViewLifecycleOwner(), workout -> {
            if (workout != null) {
                binding.tvWorkoutTitle.setText(workout.getTitle() != null && !workout.getTitle().isEmpty()
                        ? workout.getTitle()
                        : getString(R.string.workout_untitled));
                int exerciseCount = workout.getExerciseCount();
                String intensity = workout.getIntensity() != null ? workout.getIntensity() : "";
                if (exerciseCount > 0) {
                    binding.tvWorkoutSubtitle.setText(getString(
                            R.string.workout_subtitle_format,
                            exerciseCount,
                            workout.getDurationMinutes(),
                            intensity));
                } else {
                    binding.tvWorkoutSubtitle.setText(getString(
                            R.string.workout_subtitle_without_count_format,
                            workout.getDurationMinutes(),
                            intensity));
                }
                binding.btnStartWorkout.setText(R.string.btn_start);
            }
        });

        viewModel.getWeeklyPlan().observe(getViewLifecycleOwner(), plan -> {
            boolean isEmpty = plan == null || plan.isEmpty();
            binding.rvWeeklyPlan.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.layoutEmptyPlan.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            weeklyPlanAdapter.submitList(plan);
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.tvLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.scrollView.setVisibility(loading ? View.GONE : View.VISIBLE);
        });

        viewModel.getIsRefreshing().observe(getViewLifecycleOwner(), refreshing ->
                binding.swipeRefresh.setRefreshing(refreshing));

        // Error event observer — maps error codes to strings
        viewModel.getErrorEvent().observe(getViewLifecycleOwner(), error -> {
            int msgRes;
            switch (error) {
                case WEEKLY_PLAN_LOAD_FAILED:
                    msgRes = R.string.error_load_weekly_plan;
                    break;
                case USER_LOAD_FAILED:
                default:
                    msgRes = R.string.error_load_dashboard;
                    break;
            }
            Snackbar.make(binding.getRoot(), msgRes, Snackbar.LENGTH_LONG).show();
        });

        viewModel.getRefreshSuccessEvent().observe(getViewLifecycleOwner(), success ->
                Snackbar.make(binding.getRoot(), R.string.refresh_success, Snackbar.LENGTH_SHORT).show());

        viewModel.getRequireLoginEvent().observe(getViewLifecycleOwner(), requireLogin -> {
            if (Boolean.TRUE.equals(requireLogin)) {
                Navigation.findNavController(binding.getRoot()).navigate(R.id.action_global_to_login);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void navigateToWorkoutTab() {
        if (getActivity() instanceof BottomNavHost) {
            ((BottomNavHost) getActivity()).selectTab(R.id.nav_workout);
        }
    }
}
```

- [ ] **Step 2: Create BottomNavHost interface**

Create `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/navigation/BottomNavHost.java`:

```java
package ntu.quy65132908.smartgym_ai.ui.navigation;

import androidx.annotation.IdRes;

/**
 * Interface for Activities that host a BottomNavigationView.
 * Decouples Fragments from direct Activity layout manipulation.
 */
public interface BottomNavHost {
    void selectTab(@IdRes int menuItemId);
}
```

- [ ] **Step 3: Add BottomNavHost implementation to MainActivity**

In `MainActivity.java`, add `implements BottomNavHost` and the method:

```java
import ntu.quy65132908.smartgym_ai.ui.navigation.BottomNavHost;
import com.google.android.material.bottomnavigation.BottomNavigationView;

// Class declaration becomes:
// public class MainActivity extends AppCompatActivity implements BottomNavHost {

@Override
public void selectTab(@IdRes int menuItemId) {
    BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
    if (bottomNav != null) {
        bottomNav.setSelectedItemId(menuItemId);
    }
}
```

- [ ] **Step 4: Verify build compiles**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Run all tests**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: ALL PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardFragment.java app/src/main/java/ntu/quy65132908/smartgym_ai/ui/navigation/BottomNavHost.java app/src/main/java/ntu/quy65132908/smartgym_ai/MainActivity.java
git commit -m "feat(dashboard): rewrite Fragment with TodayState, error mapping, BottomNavHost, a11y"
```

---

## Phase 3: Architecture Improvements

### Task 13: Add offline data staleness indicator

**Files:**
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/data/repository/WorkoutRepository.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModel.java`
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardFragment.java`

- [ ] **Step 1: Add metadata callback to WorkoutRepository**

Add to `WorkoutRepository.java` after the existing `getWeeklyPlan` method:

```java
    public void getWeeklyPlanWithMetadata(String uid, WorkoutListWithMetadataCallback cb) {
        firestore.collection("users").document(uid).collection("workouts")
                .orderBy("dayOfWeek").get()
                .addOnSuccessListener(snap -> {
                    List<Workout> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Workout w = doc.toObject(Workout.class);
                        w.setId(doc.getId());
                        list.add(w);
                    }
                    boolean fromCache = snap.getMetadata().isFromCache();
                    cb.onSuccess(list, fromCache);
                }).addOnFailureListener(cb::onError);
    }

    public interface WorkoutListWithMetadataCallback {
        void onSuccess(List<Workout> workouts, boolean fromCache);
        void onError(Exception e);
    }
```

- [ ] **Step 2: Add isDataStale LiveData to ViewModel**

In `DashboardViewModel.java`, add field:
```java
    private final MutableLiveData<Boolean> isDataStale = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsDataStale() { return isDataStale; }
```

Update `loadWeeklyPlan` to use the new method and post `isDataStale`:
```java
    private void loadWeeklyPlan(String uid) {
        workoutRepository.getWeeklyPlanWithMetadata(uid, new WorkoutRepository.WorkoutListWithMetadataCallback() {
            @Override
            public void onSuccess(List<Workout> workouts, boolean fromCache) {
                weeklyPlan.postValue(workouts);
                isDataStale.postValue(fromCache);
                TodayState state = determineTodayState(workouts);
                todayState.postValue(state);
                if (state == TodayState.WORKOUT) {
                    aiRecommendation.postValue(findTodayWorkout(workouts));
                } else {
                    aiRecommendation.postValue(null);
                }
                isLoading.postValue(false);
                boolean wasRefreshing = Boolean.TRUE.equals(isRefreshing.getValue());
                isRefreshing.postValue(false);
                isLoadInProgress = false;
                if (wasRefreshing) {
                    refreshSuccessEvent.postValue(true);
                }
            }

            @Override
            public void onError(Exception e) {
                weeklyPlan.postValue(Collections.emptyList());
                todayState.postValue(TodayState.NO_PLAN);
                aiRecommendation.postValue(null);
                isLoading.postValue(false);
                isRefreshing.postValue(false);
                isLoadInProgress = false;
                errorEvent.postValue(DashboardError.WEEKLY_PLAN_LOAD_FAILED);
            }
        });
    }
```

- [ ] **Step 3: Observe isDataStale in Fragment**

Add to `DashboardFragment.observeViewModel()`:
```java
        viewModel.getIsDataStale().observe(getViewLifecycleOwner(), stale -> {
            if (Boolean.TRUE.equals(stale)) {
                Snackbar.make(binding.getRoot(), R.string.data_offline, Snackbar.LENGTH_SHORT).show();
            }
        });
```

- [ ] **Step 4: Verify build compiles**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Run tests**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: ALL PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/ntu/quy65132908/smartgym_ai/data/repository/WorkoutRepository.java app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModel.java app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardFragment.java
git commit -m "feat(dashboard): add offline data staleness indicator"
```

---

### Task 14: Add architecture documentation comments

**Files:**
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModel.java`

- [ ] **Step 1: Verify class-level Javadoc is complete**

Ensure the ViewModel has this class comment (should exist from Task 9):
```java
/**
 * ViewModel for the Dashboard Home tab.
 *
 * Data flow:
 * loadUserData() → [UserRepository.getUser]
 *   → onSuccess → loadWeeklyPlan()
 *   → [WorkoutRepository.getWeeklyPlan]
 *   → onSuccess → update all LiveData, set isLoading=false
 * Error at any step → post error event, set isLoading=false
 *
 * Design decisions:
 * - Constructor-time loading: intentional, ViewModel survives config changes
 * - Multiple LiveData vs sealed state: Java-friendly approach, works correctly
 * - SingleLiveEvent for errors: acceptable loss if Fragment is backgrounded
 * - MediatorLiveData for avatar: prevents race condition between name and photoUrl
 * - Refresh cooldown: 5s minimum between refreshes to prevent Firestore quota waste
 */
```

If missing, add it above the `@HiltViewModel` annotation.

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModel.java
git commit -m "docs(dashboard): add architecture decision documentation"
```

---

## Phase 4: Polish & Accessibility

### Task 15: Fix greeting format string

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Change %s to %1$s**

In `strings.xml`, change:
```xml
<string name="greeting_format">Chào %s! 💪</string>
```
to:
```xml
<string name="greeting_format">Chào %1$s! 💪</string>
```

- [ ] **Step 2: Verify build and lint**

Run: `.\gradlew.bat assembleDebug && .\gradlew.bat lintDebug`
Expected: BUILD SUCCESSFUL, LINT PASS

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "fix(strings): use positional format arg in greeting_format"
```

---

### Task 16: Final verification

**Files:** None — verification only

- [ ] **Step 1: Run full build**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run all unit tests**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: ALL PASS

- [ ] **Step 3: Run lint**

Run: `.\gradlew.bat lintDebug`
Expected: PASS

- [ ] **Step 4: Review git log**

Run: `git log --oneline -15`
Expected: ~15 focused commits covering all 4 phases

- [ ] **Step 5: Final commit if needed**

```bash
git status
# If clean: done
# If dirty: git add -A && git commit -m "chore: final cleanup after dashboard audit remediation"
```

---

## Summary

| Phase | Tasks | Commits | Key Changes |
|-------|-------|---------|-------------|
| 1: Visual | Tasks 1-5 | 5 | Drawables, rounded corners, icons, loading text, completed indicator |
| 2: Logic | Tasks 6-12 | 7 | DateUtils, TodayState, DashboardError, ViewModel rewrite, Fragment rewrite, BottomNavHost |
| 3: Architecture | Tasks 13-14 | 2 | Offline indicator, documentation |
| 4: Polish | Tasks 15-16 | 2 | String format fix, final verification |

**Total: 16 tasks, ~16 commits, 25 issues resolved**