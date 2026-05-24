package ntu.quy65132908.smartgym_ai.ui.workout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.ExerciseCatalogItem;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ExerciseCatalogRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;

public class ExerciseLibraryViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private Context appContext;
    @Mock private ExerciseCatalogRepository catalogRepository;
    @Mock private WorkoutRepository workoutRepository;
    @Mock private AuthRepository authRepository;
    @Mock private FirebaseUser firebaseUser;

    private ExerciseCatalogItem pushUp;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(firebaseUser.getUid()).thenReturn("uid-1");
        when(authRepository.getCurrentUser()).thenReturn(firebaseUser);
        when(appContext.getString(anyInt())).thenAnswer(invocation -> {
            int resId = invocation.getArgument(0);
            if (resId == R.string.exercise_custom_title) return "Bài tập tùy chỉnh";
            if (resId == R.string.exercise_custom_goal) return "Tập luyện cá nhân";
            if (resId == R.string.exercise_custom_intensity) return "Tùy chỉnh";
            if (resId == R.string.exercise_custom_saved) return "Đã lưu";
            if (resId == R.string.exercise_select_required) return "Chọn ít nhất một bài tập.";
            if (resId == R.string.exercise_login_required) return "Cần đăng nhập";
            if (resId == R.string.exercise_custom_save_error) return "Không thể lưu";
            if (resId == R.string.exercise_library_load_error) return "Không thể tải";
            return "string-" + resId;
        });

        pushUp = new ExerciseCatalogItem(
                "push_up",
                "Chống đẩy",
                "Ngực",
                "Không dụng cụ",
                "Cơ bản",
                3,
                10,
                60,
                "push_up",
                "Giữ thân người thẳng");

        doAnswer(invocation -> {
            ExerciseCatalogRepository.CatalogCallback callback = invocation.getArgument(2);
            callback.onSuccess(Collections.singletonList(pushUp));
            return null;
        }).when(catalogRepository).search(any(), any(), any());
    }

    @Test
    public void initialState_cannotSaveWithoutSelection() {
        ExerciseLibraryViewModel viewModel = createViewModel();

        assertEquals(Integer.valueOf(0), viewModel.getSelectedCount().getValue());
        assertEquals(Boolean.FALSE, viewModel.getCanSave().getValue());
    }

    @Test
    public void toggleSelection_updatesCanSaveState() {
        ExerciseLibraryViewModel viewModel = createViewModel();

        viewModel.toggle(pushUp);

        assertEquals(Integer.valueOf(1), viewModel.getSelectedCount().getValue());
        assertEquals(Boolean.TRUE, viewModel.getCanSave().getValue());

        viewModel.toggle(pushUp);

        assertEquals(Integer.valueOf(0), viewModel.getSelectedCount().getValue());
        assertEquals(Boolean.FALSE, viewModel.getCanSave().getValue());
    }

    @Test
    public void saveSelectedWorkout_savesOpenableWorkoutDocument() {
        doAnswer(invocation -> {
            WorkoutRepository.SimpleCallback callback = invocation.getArgument(2);
            callback.onSuccess();
            return null;
        }).when(workoutRepository).saveWorkout(eq("uid-1"), any(Workout.class), any());
        ExerciseLibraryViewModel viewModel = createViewModel();
        viewModel.toggle(pushUp);

        viewModel.saveSelectedWorkout();

        ArgumentCaptor<Workout> workoutCaptor = ArgumentCaptor.forClass(Workout.class);
        verify(workoutRepository).saveWorkout(eq("uid-1"), workoutCaptor.capture(), any());
        verify(workoutRepository, never()).saveCustomWorkoutTemplate(any(), any(), any());
        Workout saved = workoutCaptor.getValue();
        assertEquals(Workout.DAY_TYPE_TRAINING, saved.getDayType());
        assertEquals("Bài tập tùy chỉnh", saved.getTitle());
        assertEquals(1, saved.getExerciseCount());
        assertNotNull(saved.getExercises());
        assertEquals(1, saved.getExercises().size());
        assertFalse(Boolean.TRUE.equals(viewModel.getIsSaving().getValue()));
        assertEquals(Boolean.FALSE, viewModel.getCanSave().getValue());
    }

    @Test
    public void saveSelectedWorkout_withoutSelectionDoesNotSave() {
        ExerciseLibraryViewModel viewModel = createViewModel();

        viewModel.saveSelectedWorkout();

        verify(workoutRepository, never()).saveWorkout(any(), any(), any());
        assertTrue(viewModel.getMessage().getValue().contains("Chọn"));
    }

    private ExerciseLibraryViewModel createViewModel() {
        return new ExerciseLibraryViewModel(
                appContext,
                catalogRepository,
                workoutRepository,
                authRepository);
    }
}
