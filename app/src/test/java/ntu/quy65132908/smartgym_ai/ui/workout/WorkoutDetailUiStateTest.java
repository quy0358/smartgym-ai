package ntu.quy65132908.smartgym_ai.ui.workout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
}
