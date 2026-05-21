# Dashboard Empty States & Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix 7 remaining visual/UX issues on the Dashboard Home tab — empty states, BMI color coding, goal stat clarity, and spacing improvements.

**Architecture:** All changes fit within the existing ViewModel-driven pattern. DashboardViewModel gets two new LiveData fields (`bmiColorRes`, `goalDisplay`) and a helper method. DashboardFragment adds visibility toggling for empty states and color binding. XML layouts gain empty state views and spacing adjustments.

**Tech Stack:** Android (Java), ViewBinding, LiveData, Material Design Components, Firebase Firestore (existing)

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `app/src/main/res/values/strings.xml` | Modify | Add 5 new string resources |
| `app/src/main/res/values/colors.xml` | Modify | Add `warning` color |
| `app/src/main/res/layout/item_stat_card.xml` | Modify | Increase padding and margins |
| `app/src/main/res/layout/fragment_dashboard.xml` | Modify | Add empty plan layout + rest day card |
| `app/src/main/java/.../ui/dashboard/DashboardViewModel.java` | Modify | Add `bmiColorRes`, `goalDisplay`, `computeBmiColor()` |
| `app/src/main/java/.../ui/dashboard/DashboardFragment.java` | Modify | Add empty state logic, BMI color binding, goal display binding |
| `app/src/test/.../ui/dashboard/DashboardViewModelTest.java` | Modify | Add tests for new methods |

---

### Task 1: Add String and Color Resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values/colors.xml`

- [ ] **Step 1: Add empty state strings to strings.xml**

Open `app/src/main/res/values/strings.xml` and add the following entries before the closing `</resources>` tag:

```xml
    <!-- Dashboard empty states -->
    <string name="empty_plan_title">Chưa có kế hoạch tuần</string>
    <string name="empty_plan_subtitle">Hãy tạo kế hoạch tập luyện để bắt đầu hành trình</string>
    <string name="btn_create_plan">Tạo kế hoạch</string>
    <string name="rest_day_title">Hôm nay là ngày nghỉ!</string>
    <string name="rest_day_subtitle">Hãy nghỉ ngơi để cơ thể phục hồi 💪</string>
```

- [ ] **Step 2: Add warning color to colors.xml**

Open `app/src/main/res/values/colors.xml` and add after the `<color name="error">` line:

```xml
    <!-- Warning (Orange) -->
    <color name="warning">#FFFFB74D</color>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values/colors.xml
git commit -m "res: add empty state strings and warning color"
```

---

### Task 2: Update Stat Card Spacing

**Files:**
- Modify: `app/src/main/res/layout/item_stat_card.xml`

- [ ] **Step 1: Update item_stat_card.xml with improved spacing**

Replace the entire content of `app/src/main/res/layout/item_stat_card.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/bg_stat_card_rounded"
    android:gravity="center_horizontal"
    android:orientation="vertical"
    android:padding="@dimen/spacing_lg">

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
        android:layout_marginTop="@dimen/spacing_sm"
        android:text="0"
        android:textAppearance="@style/TextStyle.HeadlineMd"
        android:textColor="@color/primary" />

    <TextView
        android:id="@+id/tv_stat_unit"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="@dimen/spacing_xs"
        android:text="unit"
        android:textAppearance="@style/TextStyle.LabelCaps" />
</LinearLayout>
```

Key changes from current:
- Root padding: `spacing_md` (12dp) → `spacing_lg` (16dp)
- Value marginTop: `spacing_xs` (4dp) → `spacing_sm` (8dp)
- Unit marginTop: 0 → `spacing_xs` (4dp)

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/layout/item_stat_card.xml
git commit -m "ui: improve stat card spacing"
```

---

### Task 3: Add Empty State and Rest Day Card to Dashboard Layout

**Files:**
- Modify: `app/src/main/res/layout/fragment_dashboard.xml`

- [ ] **Step 1: Add rest day card after AI workout card**

In `app/src/main/res/layout/fragment_dashboard.xml`, find the closing `</androidx.cardview.widget.CardView>` tag of `card_ai_workout` (around line 168). Add the following immediately after it:

```xml
                <!-- Rest Day Card -->
                <androidx.cardview.widget.CardView
                    android:id="@+id/card_rest_day"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/spacing_lg"
                    android:visibility="gone"
                    app:cardBackgroundColor="@color/surface_container"
                    app:cardCornerRadius="@dimen/radius_lg"
                    app:cardElevation="0dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:gravity="center_vertical"
                        android:orientation="horizontal"
                        android:padding="@dimen/spacing_lg">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="🧘"
                            android:textSize="36sp" />

                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_marginStart="@dimen/spacing_md"
                            android:layout_weight="1"
                            android:orientation="vertical">

                            <TextView
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:text="@string/rest_day_title"
                                android:textAppearance="@style/TextStyle.BodyBold" />

                            <TextView
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:layout_marginTop="@dimen/spacing_xs"
                                android:text="@string/rest_day_subtitle"
                                android:textAppearance="@style/TextStyle.BodyBase" />
                        </LinearLayout>
                    </LinearLayout>
                </androidx.cardview.widget.CardView>
```

- [ ] **Step 2: Add empty plan layout after the RecyclerView**

Find the `rv_weekly_plan` RecyclerView (around line 197-202). Add the following immediately after its closing tag:

```xml
                <!-- Empty State for Weekly Plan -->
                <LinearLayout
                    android:id="@+id/layout_empty_plan"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/spacing_xl"
                    android:gravity="center"
                    android:orientation="vertical"
                    android:padding="@dimen/spacing_xl"
                    android:visibility="gone">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="📋"
                        android:textSize="48sp" />

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="@dimen/spacing_md"
                        android:text="@string/empty_plan_title"
                        android:textAppearance="@style/TextStyle.BodyBold" />

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="@dimen/spacing_xs"
                        android:gravity="center"
                        android:text="@string/empty_plan_subtitle"
                        android:textAppearance="@style/TextStyle.BodyBase" />

                    <com.google.android.material.button.MaterialButton
                        android:id="@+id/btn_create_plan"
                        style="@style/Widget.SmartGym.Button.Outline"
                        android:layout_width="wrap_content"
                        android:layout_height="48dp"
                        android:layout_marginTop="@dimen/spacing_lg"
                        android:text="@string/btn_create_plan" />
                </LinearLayout>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/fragment_dashboard.xml
git commit -m "ui: add empty plan state and rest day card to dashboard layout"
```

---

### Task 4: Add BMI Color and Goal Display to DashboardViewModel

**Files:**
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModel.java`
- Test: `app/src/test/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModelTest.java`

- [ ] **Step 1: Write failing tests for computeBmiColor and goal display**

Open `app/src/test/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModelTest.java` and add the following test methods:

```java
    @Test
    public void computeBmiColor_underweight_returnsTertiary() {
        int color = viewModel.computeBmiColor(17.5f);
        assertEquals(R.color.tertiary, color);
    }

    @Test
    public void computeBmiColor_normal_returnsPrimary() {
        int color = viewModel.computeBmiColor(22.0f);
        assertEquals(R.color.primary, color);
    }

    @Test
    public void computeBmiColor_overweight_returnsWarning() {
        int color = viewModel.computeBmiColor(27.0f);
        assertEquals(R.color.warning, color);
    }

    @Test
    public void computeBmiColor_obese_returnsError() {
        int color = viewModel.computeBmiColor(32.0f);
        assertEquals(R.color.error, color);
    }

    @Test
    public void computeBmiColor_boundaryNormal_returnsPrimary() {
        int color = viewModel.computeBmiColor(18.5f);
        assertEquals(R.color.primary, color);
    }

    @Test
    public void computeBmiColor_boundaryOverweight_returnsWarning() {
        int color = viewModel.computeBmiColor(25.0f);
        assertEquals(R.color.warning, color);
    }

    @Test
    public void computeBmiColor_boundaryObese_returnsError() {
        int color = viewModel.computeBmiColor(30.0f);
        assertEquals(R.color.error, color);
    }

    @Test
    public void formatGoalDisplay_positiveGoal_showsMinusPrefix() {
        assertEquals("−6", viewModel.formatGoalDisplay(6));
    }

    @Test
    public void formatGoalDisplay_zeroGoal_showsZero() {
        assertEquals("0", viewModel.formatGoalDisplay(0));
    }

    @Test
    public void formatGoalDisplay_negativeGoal_showsMinusAbsolute() {
        assertEquals("−3", viewModel.formatGoalDisplay(-3));
    }
```

Also add the import at the top:
```java
import static org.junit.Assert.assertEquals;
import ntu.quy65132908.smartgym_ai.R;
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "*DashboardViewModelTest*" --info`
Expected: FAIL — methods `computeBmiColor` and `formatGoalDisplay` do not exist

- [ ] **Step 3: Add bmiColorRes and goalDisplay LiveData + methods to DashboardViewModel**

Open `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModel.java`.

Add new fields after the existing `goalWeight` field (around line 37):

```java
    private final MutableLiveData<Integer> bmiColorRes = new MutableLiveData<>(R.color.on_surface_variant);
    private final MutableLiveData<String> goalDisplay = new MutableLiveData<>("0");
```

Add public getters after existing getters (around line 57):

```java
    public LiveData<Integer> getBmiColorRes() { return bmiColorRes; }
    public LiveData<String> getGoalDisplay() { return goalDisplay; }
```

Add the `computeBmiColor` method (package-private for testing) after `parseGoalWeight`:

```java
    int computeBmiColor(float bmiValue) {
        if (bmiValue < 18.5f) return R.color.tertiary;
        if (bmiValue < 25.0f) return R.color.primary;
        if (bmiValue < 30.0f) return R.color.warning;
        return R.color.error;
    }
```

Add the `formatGoalDisplay` method (package-private for testing):

```java
    String formatGoalDisplay(int goal) {
        if (goal == 0) return "0";
        return "\u2212" + Math.abs(goal);  // U+2212 minus sign
    }
```

Update the `onSuccess` callback inside `loadUserData()`. After the existing lines that set `goalWeight` (around line 97), add:

```java
                goalDisplay.postValue(formatGoalDisplay(parseGoalWeight(user.getGoal())));
                bmiColorRes.postValue(computeBmiColor(user.getBmi() != null ? user.getBmi() : 0f));
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "*DashboardViewModelTest*" --info`
Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModel.java
git add app/src/test/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardViewModelTest.java
git commit -m "feat: add BMI color computation and goal display formatting to DashboardViewModel"
```

---

### Task 5: Update DashboardFragment with Empty State Logic and Bindings

**Files:**
- Modify: `app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardFragment.java`

- [ ] **Step 1: Add imports**

Add these imports at the top of `DashboardFragment.java`:

```java
import java.util.List;

import androidx.core.content.ContextCompat;

import ntu.quy65132908.smartgym_ai.data.model.Workout;
```

Note: `List` and `Workout` may already be imported — only add if missing.

- [ ] **Step 2: Update setupClickListeners to add create plan button**

In the `setupClickListeners()` method, add after the existing `binding.tvViewAll.setOnClickListener(...)` block:

```java
        binding.btnCreatePlan.setOnClickListener(v -> {
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_nav);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_workout);
            }
        });
```

- [ ] **Step 3: Update weekly plan observer to toggle empty state**

In the `observeViewModel()` method, replace the existing weekly plan observer:

```java
        viewModel.getWeeklyPlan().observe(getViewLifecycleOwner(), plan ->
                weeklyPlanAdapter.submitList(plan));
```

With:

```java
        viewModel.getWeeklyPlan().observe(getViewLifecycleOwner(), plan -> {
            boolean isEmpty = plan == null || plan.isEmpty();
            binding.rvWeeklyPlan.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.layoutEmptyPlan.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            weeklyPlanAdapter.submitList(plan);
        });
```

- [ ] **Step 4: Update AI recommendation observer to show rest day card**

Replace the existing AI recommendation observer:

```java
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
```

With:

```java
        viewModel.getAiRecommendation().observe(getViewLifecycleOwner(), workout -> {
            if (workout != null) {
                binding.cardAiWorkout.setVisibility(View.VISIBLE);
                binding.cardRestDay.setVisibility(View.GONE);
                binding.tvWorkoutTitle.setText(workout.getTitle());
                int exerciseCount = workout.getExercises() != null ? workout.getExercises().size() : 0;
                binding.tvWorkoutSubtitle.setText(getString(
                        R.string.workout_subtitle_format,
                        exerciseCount,
                        workout.getDurationMinutes(),
                        workout.getIntensity() != null ? workout.getIntensity() : ""));
            } else {
                binding.cardAiWorkout.setVisibility(View.GONE);
                List<Workout> plan = viewModel.getWeeklyPlan().getValue();
                boolean hasPlan = plan != null && !plan.isEmpty();
                binding.cardRestDay.setVisibility(hasPlan ? View.VISIBLE : View.GONE);
            }
        });
```

- [ ] **Step 5: Add BMI color observer**

Add after the existing `getBmiCategory()` observer in `observeViewModel()`:

```java
        viewModel.getBmiColorRes().observe(getViewLifecycleOwner(), colorRes -> {
            int color = ContextCompat.getColor(requireContext(), colorRes);
            binding.statBmi.tvStatValue.setTextColor(color);
            binding.statBmi.tvStatUnit.setTextColor(color);
        });
```

- [ ] **Step 6: Replace goal weight observer with goal display observer**

Replace:

```java
        viewModel.getGoalWeight().observe(getViewLifecycleOwner(), goal ->
                binding.statGoal.tvStatValue.setText(String.valueOf(goal)));
```

With:

```java
        viewModel.getGoalDisplay().observe(getViewLifecycleOwner(), display ->
                binding.statGoal.tvStatValue.setText(display));
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/ntu/quy65132908/smartgym_ai/ui/dashboard/DashboardFragment.java
git commit -m "feat: add empty state logic, BMI color binding, and goal display to DashboardFragment"
```

---

### Task 6: Build Verification

**Files:** None (verification only)

- [ ] **Step 1: Run full test suite**

Run: `./gradlew testDebugUnitTest --info`
Expected: All tests PASS

- [ ] **Step 2: Verify project compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — no compilation errors from layout binding mismatches

- [ ] **Step 3: Final commit (if any fixes needed)**

If any fixes were required:
```bash
git add -A
git commit -m "fix: resolve build issues from dashboard polish changes"
```

---

## Summary

| Task | Description | Files |
|------|-------------|-------|
| 1 | String + color resources | strings.xml, colors.xml |
| 2 | Stat card spacing | item_stat_card.xml |
| 3 | Empty state + rest day layouts | fragment_dashboard.xml |
| 4 | ViewModel: BMI color + goal display | DashboardViewModel.java, DashboardViewModelTest.java |
| 5 | Fragment: empty state logic + bindings | DashboardFragment.java |
| 6 | Build verification | — |
