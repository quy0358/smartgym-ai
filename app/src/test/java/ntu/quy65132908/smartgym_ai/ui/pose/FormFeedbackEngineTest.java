package ntu.quy65132908.smartgym_ai.ui.pose;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        assertTrue(top.getMessage().length() > 0);
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
    public void evaluatePlank_givesBodyLineFeedbackAndHoldMetric() {
        FormFeedbackEngine engine = new FormFeedbackEngine();
        engine.setExerciseType(ExerciseType.PLANK);

        PoseFeedback feedback = engine.evaluate(PoseTestFactory.plankAligned());

        assertTrue(feedback.getMessage().length() > 0);
        assertTrue(feedback.getQualityPercent() > 0);
        assertEquals(0, feedback.getReps());
        assertTrue(feedback.getHoldSeconds() >= 0);
    }

    @Test
    public void evaluatePlank_keepsAccumulatedHoldAfterInvalidForm() throws Exception {
        FormFeedbackEngine engine = new FormFeedbackEngine();
        engine.setExerciseType(ExerciseType.PLANK);

        engine.evaluate(PoseTestFactory.plankAligned());
        Thread.sleep(1200L);
        PoseFeedback firstSegment = engine.evaluate(PoseTestFactory.plankAligned());

        PoseFeedback invalidSegment = engine.evaluate(PoseFrame.empty());
        Thread.sleep(1200L);
        engine.evaluate(PoseTestFactory.plankAligned());
        Thread.sleep(1200L);
        PoseFeedback resumedSegment = engine.evaluate(PoseTestFactory.plankAligned());

        assertTrue(firstSegment.getHoldSeconds() >= 1);
        assertEquals(firstSegment.getHoldSeconds(), invalidSegment.getHoldSeconds());
        assertTrue(resumedSegment.getHoldSeconds() >= firstSegment.getHoldSeconds() + 1);
    }

    @Test
    public void evaluateEmptyFrame_requestsFullBodyAndMarksNoPerson() {
        FormFeedbackEngine engine = new FormFeedbackEngine();

        PoseFeedback feedback = engine.evaluate(PoseFrame.empty());

        assertEquals(0, feedback.getReps());
        assertFalse(feedback.isPersonDetected());
        assertEquals("Đưa toàn thân vào khung hình để bắt đầu nhận diện.", feedback.getMessage());
        assertNoMojibake(feedback.getMessage());
    }

    @Test
    public void readyAndExerciseFeedback_useReadableVietnamese() {
        FormFeedbackEngine engine = new FormFeedbackEngine();

        assertEquals("Sẵn sàng. Giữ toàn thân trong khung hình.", engine.getReadyMessage());
        assertNoMojibake(engine.getReadyMessage());

        PoseFeedback feedback = engine.evaluate(PoseTestFactory.pushUpTop());

        assertNoMojibake(feedback.getMessage());
        assertTrue(feedback.getMessage().contains("chống đẩy"));
    }

    private static void assertNoMojibake(String value) {
        String[] markers = {"Ã", "Ä", "á»", "áº", "Æ", "â€¢", "â€", "â€¦", "â†", "â", "ðŸ", "ï¸", "Å"};
        for (String marker : markers) {
            assertFalse("Unexpected mojibake marker " + marker + " in: " + value,
                    value.contains(marker));
        }
    }
}
