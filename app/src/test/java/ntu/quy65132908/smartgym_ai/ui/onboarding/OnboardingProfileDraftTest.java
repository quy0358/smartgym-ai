package ntu.quy65132908.smartgym_ai.ui.onboarding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ntu.quy65132908.smartgym_ai.data.model.User;

public class OnboardingProfileDraftTest {

    @Test
    public void isComplete_requiresAllPersonalizationFields() {
        OnboardingProfileDraft draft = new OnboardingProfileDraft();
        draft.setGender("male");
        draft.setBirthDate("2000-05-21");
        draft.setHeightCm(170f);
        draft.setWeightKg(78f);
        draft.setTargetWeightKg(72f);
        draft.setGoal("Giảm cân");

        assertFalse(draft.isComplete());

        draft.setFitnessLevel("Người mới");

        assertTrue(draft.isComplete());
    }

    @Test
    public void applyToUser_setsBmiAndCompletionFields() {
        User user = new User("uid-1", "Quy", "quy@example.com");
        OnboardingProfileDraft draft = new OnboardingProfileDraft();
        draft.setGender("female");
        draft.setBirthDate("2000-05-21");
        draft.setHeightCm(170f);
        draft.setWeightKg(78f);
        draft.setTargetWeightKg(58f);
        draft.setGoal("Tăng cơ");
        draft.setFitnessLevel("Trung cấp");

        draft.applyToUser(user);

        assertEquals("female", user.getGender());
        assertEquals("2000-05-21", user.getBirthDate());
        assertEquals(170f, user.getHeight(), 0.001f);
        assertEquals(78f, user.getWeight(), 0.001f);
        assertEquals(58f, user.getTargetWeight(), 0.001f);
        assertEquals("Tăng cơ", user.getGoal());
        assertEquals("Trung cấp", user.getFitnessLevel());
        assertEquals(26.99f, user.getBmi(), 0.05f);
        assertTrue(user.isOnboardingCompleted());
        assertNotNull(user.getBmiCategory());
        assertTrue(user.getOnboardingCompletedAt() > 0L);
    }
}
