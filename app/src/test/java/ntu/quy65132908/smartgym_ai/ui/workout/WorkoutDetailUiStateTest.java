package ntu.quy65132908.smartgym_ai.ui.workout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

import ntu.quy65132908.smartgym_ai.data.model.Exercise;

public class WorkoutDetailUiStateTest {

    @Test
    public void restDayState_hidesProgressListAndPoseAction() {
        WorkoutDetailUiState state = WorkoutDetailUiState.rest("Nghỉ ngơi hoàn toàn");

        assertTrue(state.isRestDay());
        assertFalse(state.hasExercises());
        assertFalse(state.shouldShowProgress());
        assertFalse(state.shouldShowPoseAction());
        assertEquals(0, state.getProgressPercent());
    }

    @Test
    public void success_copiesExerciseMetadataAndCalculatesProgress() {
        Exercise plank = new Exercise("e1", "Plank", 3, 0, null, true);
        plank.setPoseTypeKey("plank");
        plank.setPrimaryMuscle("Core");
        plank.setDurationSeconds(30);
        plank.setOrderIndex(2);
        Exercise squat = new Exercise("e2", "Squat", 3, 12, null, false);

        WorkoutDetailUiState state = WorkoutDetailUiState.success(Arrays.asList(plank, squat), "Core");

        assertEquals(50, state.getProgressPercent());
        Exercise copied = state.getExercises().get(0);
        assertEquals("plank", copied.getPoseTypeKey());
        assertEquals("Core", copied.getPrimaryMuscle());
        assertEquals(30, copied.getDurationSeconds());
        assertEquals(2, copied.getOrderIndex());
    }
}
