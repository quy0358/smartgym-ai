package ntu.quy65132908.smartgym_ai.ui.pose;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PoseSignalSmootherTest {

    @Test
    public void update_usesHysteresisToIgnoreSingleNoisyFrame() {
        PoseSignalSmoother smoother = new PoseSignalSmoother(0.5f, 0.75f, 0.25f);

        assertFalse(smoother.update(false));
        assertFalse(smoother.update(true));
        assertFalse(smoother.update(false));

        assertFalse(smoother.update(true));
        assertTrue(smoother.update(true));
        assertTrue(smoother.update(false));
        assertFalse(smoother.update(false));
    }
}
