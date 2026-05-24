package ntu.quy65132908.smartgym_ai.ui.pose;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PoseRepCounterTest {

    @Test
    public void update_requiresStableDownAndTopFramesBeforeCountingRep() {
        PoseRepCounter counter = new PoseRepCounter(2);

        assertEquals(0, counter.update(true));
        assertEquals(0, counter.update(false));
        assertEquals(0, counter.update(true));
        assertEquals(0, counter.update(true));
        assertEquals(0, counter.update(false));
        assertEquals(1, counter.update(false));
    }

    @Test
    public void update_doesNotDoubleCountWhenTopFramesContinue() {
        PoseRepCounter counter = new PoseRepCounter(1);

        assertEquals(0, counter.update(true));
        assertEquals(1, counter.update(false));
        assertEquals(1, counter.update(false));
        assertEquals(1, counter.update(false));
    }
}
