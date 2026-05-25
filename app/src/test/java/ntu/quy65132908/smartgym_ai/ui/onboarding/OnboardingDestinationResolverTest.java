package ntu.quy65132908.smartgym_ai.ui.onboarding;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ntu.quy65132908.smartgym_ai.data.model.User;

public class OnboardingDestinationResolverTest {

    @Test
    public void requiresOnboarding_whenUserIsMissingRequiredProfileFields() {
        User user = new User("uid-1", "Quy", "quy@example.com");
        user.setOnboardingCompleted(true);
        user.setHeight(170f);
        user.setWeight(78f);
        user.setTargetWeight(72f);
        user.setGoal("Giảm cân");
        user.setFitnessLevel("Người mới");

        assertTrue(OnboardingDestinationResolver.requiresOnboarding(user));
    }

    @Test
    public void skipsOnboarding_whenCompletedProfileHasAllRequiredFields() {
        User user = new User("uid-1", "Quy", "quy@example.com");
        user.setOnboardingCompleted(true);
        user.setGender("male");
        user.setBirthDate("2000-05-21");
        user.setHeight(170f);
        user.setWeight(78f);
        user.setTargetWeight(72f);
        user.setGoal("Giảm cân");
        user.setFitnessLevel("Người mới");

        assertFalse(OnboardingDestinationResolver.requiresOnboarding(user));
    }
}
