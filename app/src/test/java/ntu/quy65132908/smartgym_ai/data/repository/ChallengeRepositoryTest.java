package ntu.quy65132908.smartgym_ai.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ntu.quy65132908.smartgym_ai.data.model.Challenge;
import ntu.quy65132908.smartgym_ai.data.model.ChallengeProgress;

public class ChallengeRepositoryTest {

    @Test
    public void applyWorkoutCompletion_countsOneProgressPerCalendarDay() {
        Challenge challenge = new Challenge("c7", "7 ngày vận động", "Tập 7 ngày bất kỳ", 7, 20);
        ChallengeProgress progress = ChallengeProgress.forChallenge(challenge, 1716400000000L);

        ChallengeProgress dayOne = ChallengeRepository.applyWorkoutCompletion(progress, 1716400000000L);
        ChallengeProgress duplicateDayOne = ChallengeRepository.applyWorkoutCompletion(dayOne, 1716440000000L);
        ChallengeProgress dayTwo = ChallengeRepository.applyWorkoutCompletion(duplicateDayOne, 1716486400000L);

        assertEquals(1, dayOne.getCompletedDays());
        assertEquals(1, duplicateDayOne.getCompletedDays());
        assertEquals(2, dayTwo.getCompletedDays());
        assertEquals(29, dayTwo.getProgressPercent());
    }

    @Test
    public void applyWorkoutCompletion_capsProgressAtTargetAndMarksComplete() {
        Challenge challenge = new Challenge("c2", "2 ngày", "Tập 2 ngày", 2, 20);
        ChallengeProgress progress = ChallengeProgress.forChallenge(challenge, 1716400000000L);

        progress = ChallengeRepository.applyWorkoutCompletion(progress, 1716400000000L);
        progress = ChallengeRepository.applyWorkoutCompletion(progress, 1716486400000L);
        progress = ChallengeRepository.applyWorkoutCompletion(progress, 1716572800000L);

        assertEquals(2, progress.getCompletedDays());
        assertEquals(100, progress.getProgressPercent());
        assertTrue(progress.isCompleted());
    }
}
