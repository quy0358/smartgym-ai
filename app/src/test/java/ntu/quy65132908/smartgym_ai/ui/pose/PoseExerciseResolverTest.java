package ntu.quy65132908.smartgym_ai.ui.pose;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import ntu.quy65132908.smartgym_ai.data.model.Exercise;

public class PoseExerciseResolverTest {

    @Test
    public void resolve_prefersPoseTypeKeyOverName() {
        Exercise exercise = new Exercise("e1", "Bai tap tuy chinh", 3, 12, null, false);
        exercise.setPoseTypeKey("squat");

        assertEquals(ExerciseType.SQUAT, PoseExerciseResolver.resolve(exercise));
    }

    @Test
    public void resolve_infersSupportedVietnameseNames() {
        assertEquals(ExerciseType.PUSH_UP, PoseExerciseResolver.resolve(null, "Chong day"));
        assertEquals(ExerciseType.PUSH_UP, PoseExerciseResolver.resolve(null, "Chống đẩy"));
        assertEquals(ExerciseType.SQUAT, PoseExerciseResolver.resolve(null, "Ngồi xổm"));
        assertEquals(ExerciseType.SQUAT, PoseExerciseResolver.resolve(null, "Ganh ta"));
        assertEquals(ExerciseType.PLANK, PoseExerciseResolver.resolve(null, "Plank giữ thân"));
    }

    @Test
    public void resolve_returnsNullForUnsupportedExercises() {
        assertNull(PoseExerciseResolver.resolve(null, "Dumbbell row"));
        assertNull(PoseExerciseResolver.resolve("unknown", "Bicep curl"));
    }
}
