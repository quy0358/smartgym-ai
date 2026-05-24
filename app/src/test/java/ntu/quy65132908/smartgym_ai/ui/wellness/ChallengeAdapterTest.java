package ntu.quy65132908.smartgym_ai.ui.wellness;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import ntu.quy65132908.smartgym_ai.data.model.Challenge;
import ntu.quy65132908.smartgym_ai.data.model.ChallengeProgress;

@RunWith(RobolectricTestRunner.class)
public class ChallengeAdapterTest {

    @Test
    public void formatChallengeStatus_inProgress_usesProgressText() {
        Context context = RuntimeEnvironment.getApplication();
        ChallengeProgress progress = ChallengeProgress.forChallenge(
                new Challenge("c1", "7 ngày vận động", "Tập 20 phút", 7, 20),
                1716400000000L);
        progress.setCompletedDays(3);

        assertEquals("Đã hoàn thành 3/7 ngày", ChallengeAdapter.formatChallengeStatus(context, progress));
    }

    @Test
    public void formatChallengeStatus_completed_usesCompletedText() {
        Context context = RuntimeEnvironment.getApplication();
        ChallengeProgress progress = ChallengeProgress.forChallenge(
                new Challenge("c2", "30 ngày bền bỉ", "Tập đều", 30, 20),
                1716400000000L);
        progress.setCompletedDays(30);
        progress.setCompleted(true);

        assertEquals("Đã hoàn thành thử thách", ChallengeAdapter.formatChallengeStatus(context, progress));
    }
}
