package ntu.quy65132908.smartgym_ai.ui.onboarding;

import ntu.quy65132908.smartgym_ai.data.model.User;

public final class OnboardingDestinationResolver {
    private OnboardingDestinationResolver() {
    }

    public static boolean requiresOnboarding(User user) {
        if (user == null || !user.isOnboardingCompleted()) {
            return true;
        }
        return isBlank(user.getGender())
                || isBlank(user.getBirthDate())
                || user.getHeight() == null
                || user.getWeight() == null
                || user.getTargetWeight() == null
                || isBlank(user.getGoal())
                || isBlank(user.getFitnessLevel());
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
