package ntu.quy65132908.smartgym_ai.ui.pose;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PoseAngleCalculatorTest {

    @Test
    public void angle_returnsStraightLineForExtendedElbow() {
        Float angle = PoseAngleCalculator.angle(
                PoseTestFactory.pushUpTop(),
                BodyLandmark.LEFT_SHOULDER,
                BodyLandmark.LEFT_ELBOW,
                BodyLandmark.LEFT_WRIST);

        assertEquals(180f, angle, 0.1f);
    }

    @Test
    public void angle_returnsRightAngleForBentElbow() {
        Float angle = PoseAngleCalculator.angle(
                PoseTestFactory.pushUpDown(),
                BodyLandmark.LEFT_SHOULDER,
                BodyLandmark.LEFT_ELBOW,
                BodyLandmark.LEFT_WRIST);

        assertEquals(90f, angle, 0.1f);
    }

    @Test
    public void angle_returnsNullWhenLandmarkMissing() {
        PoseFrame frame = PoseTestFactory.frame(
                BodyLandmark.LEFT_SHOULDER, 100, 100,
                BodyLandmark.LEFT_ELBOW, 150, 100);

        Float angle = PoseAngleCalculator.angle(
                frame,
                BodyLandmark.LEFT_SHOULDER,
                BodyLandmark.LEFT_ELBOW,
                BodyLandmark.LEFT_WRIST);

        assertNull(angle);
    }

    @Test
    public void averageAvailable_usesOnlyAvailableSide() {
        float value = PoseAngleCalculator.averageAvailable(null, 93f);

        assertEquals(93f, value, 0.1f);
    }

    @Test
    public void averageAvailable_returnsNaNWhenBothMissing() {
        assertTrue(Float.isNaN(PoseAngleCalculator.averageAvailable(null, null)));
    }
}
