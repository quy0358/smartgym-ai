package ntu.quy65132908.smartgym_ai.ui.pose;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ExerciseTypeTest {

    @Test
    public void fromKey_defaultsToPushUp() {
        assertEquals(ExerciseType.PUSH_UP, ExerciseType.fromKey(null));
        assertEquals(ExerciseType.PUSH_UP, ExerciseType.fromKey("unknown"));
    }

    @Test
    public void fromKey_acceptsCommonSeparators() {
        assertEquals(ExerciseType.PUSH_UP, ExerciseType.fromKey("push-up"));
        assertEquals(ExerciseType.SQUAT, ExerciseType.fromKey("squat"));
        assertEquals(ExerciseType.PLANK, ExerciseType.fromKey("plank"));
    }
}
