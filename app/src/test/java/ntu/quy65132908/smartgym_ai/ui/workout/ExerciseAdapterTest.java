package ntu.quy65132908.smartgym_ai.ui.workout;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ntu.quy65132908.smartgym_ai.data.model.Exercise;

public class ExerciseAdapterTest {

    @Test
    public void zeroReps_displaysAsFreeForm() {
        Exercise exercise = new Exercise("e1", "Stretch", 1, 0, null, false);

        assertEquals("1 hiệp • Tự do", ExerciseAdapter.formatExerciseDetail(exercise));
    }

    @Test
    public void normalExercise_displaysVietnameseSetsAndReps() {
        Exercise exercise = new Exercise("e1", "Push up", 3, 12, null, false);

        assertEquals("3 hiệp × 12 lần", ExerciseAdapter.formatExerciseDetail(exercise));
    }

    @Test
    public void exerciseWithWeight_appendsKg() {
        Exercise exercise = new Exercise("e1", "Squat", 3, 12, 10f, false);

        assertEquals("3 hiệp × 12 lần • 10kg", ExerciseAdapter.formatExerciseDetail(exercise));
    }

    @Test
    public void zeroWeight_doesNotAppendKg() {
        Exercise exercise = new Exercise("e1", "Squat", 3, 12, 0f, false);

        assertEquals("3 hiệp × 12 lần", ExerciseAdapter.formatExerciseDetail(exercise));
    }
}
