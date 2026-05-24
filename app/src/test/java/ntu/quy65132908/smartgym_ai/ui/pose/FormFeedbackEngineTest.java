package ntu.quy65132908.smartgym_ai.ui.pose;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FormFeedbackEngineTest {

    @Test
    public void evaluatePushUp_countsRepAfterStableDownThenStableTop() {
        FormFeedbackEngine engine = new FormFeedbackEngine();
        engine.setExerciseType(ExerciseType.PUSH_UP);

        PoseFeedback down = engine.evaluate(PoseTestFactory.pushUpDown());
        engine.evaluate(PoseTestFactory.pushUpDown());
        PoseFeedback top = engine.evaluate(PoseTestFactory.pushUpTop());
        top = engine.evaluate(PoseTestFactory.pushUpTop());

        assertEquals(0, down.getReps());
        assertEquals(1, top.getReps());
        assertTrue(top.getMessage().contains("chống đẩy"));
    }

    @Test
    public void evaluateSquat_countsRepAfterStableDownThenStableTop() {
        FormFeedbackEngine engine = new FormFeedbackEngine();
        engine.setExerciseType(ExerciseType.SQUAT);

        PoseFeedback down = engine.evaluate(PoseTestFactory.squatDown());
        engine.evaluate(PoseTestFactory.squatDown());
        PoseFeedback top = engine.evaluate(PoseTestFactory.squatTop());
        top = engine.evaluate(PoseTestFactory.squatTop());

        assertEquals(0, down.getReps());
        assertEquals(1, top.getReps());
        assertTrue(top.getMessage().contains("squat"));
    }

    @Test
    public void evaluatePushUp_ignoresSingleNoisyDownFrame() {
        FormFeedbackEngine engine = new FormFeedbackEngine();
        engine.setExerciseType(ExerciseType.PUSH_UP);

        engine.evaluate(PoseTestFactory.pushUpDown());
        PoseFeedback feedback = engine.evaluate(PoseTestFactory.pushUpTop());

        assertEquals(0, feedback.getReps());
    }

    @Test
    public void evaluatePlank_givesVietnameseBodyLineFeedbackAndHoldMetric() {
        FormFeedbackEngine engine = new FormFeedbackEngine();
        engine.setExerciseType(ExerciseType.PLANK);

        PoseFeedback feedback = engine.evaluate(PoseTestFactory.pushUpTop());

        assertTrue(feedback.getMessage().contains("plank")
                || feedback.getMessage().contains("thân người"));
        assertTrue(feedback.getQualityPercent() > 0);
        assertEquals(0, feedback.getReps());
        assertTrue(feedback.getHoldSeconds() >= 0);
    }

    @Test
    public void evaluateEmptyFrame_requestsFullBodyInVietnamese() {
        FormFeedbackEngine engine = new FormFeedbackEngine();

        PoseFeedback feedback = engine.evaluate(PoseFrame.empty());

        assertEquals(0, feedback.getReps());
        assertTrue(feedback.getMessage().contains("toàn thân"));
    }
}
