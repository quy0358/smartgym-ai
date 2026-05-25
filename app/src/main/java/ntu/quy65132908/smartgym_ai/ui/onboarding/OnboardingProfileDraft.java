package ntu.quy65132908.smartgym_ai.ui.onboarding;

import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.ui.profile.ProfileMetrics;

public class OnboardingProfileDraft {
    private static final float MIN_WEIGHT_KG = 20f;
    private static final float MAX_WEIGHT_KG = 350f;
    private static final float MIN_HEIGHT_CM = 80f;
    private static final float MAX_HEIGHT_CM = 250f;

    private String gender = "";
    private String birthDate = "";
    private Float heightCm;
    private Float weightKg;
    private Float targetWeightKg;
    private String goal = "";
    private String fitnessLevel = "";

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = sanitize(gender);
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = sanitize(birthDate);
    }

    public Float getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(Float heightCm) {
        this.heightCm = heightCm;
    }

    public Float getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(Float weightKg) {
        this.weightKg = weightKg;
    }

    public Float getTargetWeightKg() {
        return targetWeightKg;
    }

    public void setTargetWeightKg(Float targetWeightKg) {
        this.targetWeightKg = targetWeightKg;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = sanitize(goal);
    }

    public String getFitnessLevel() {
        return fitnessLevel;
    }

    public void setFitnessLevel(String fitnessLevel) {
        this.fitnessLevel = sanitize(fitnessLevel);
    }

    public boolean isComplete() {
        return !isBlank(gender)
                && !isBlank(birthDate)
                && inRange(heightCm, MIN_HEIGHT_CM, MAX_HEIGHT_CM)
                && inRange(weightKg, MIN_WEIGHT_KG, MAX_WEIGHT_KG)
                && inRange(targetWeightKg, MIN_WEIGHT_KG, MAX_WEIGHT_KG)
                && !isBlank(goal)
                && !isBlank(fitnessLevel);
    }

    public Float calculateBmiOrNull() {
        return ProfileMetrics.calculateBmiOrNull(weightKg, heightCm);
    }

    public String bmiCategoryOrEmpty() {
        Float bmi = calculateBmiOrNull();
        return bmi != null ? ProfileMetrics.categoryForBmi(bmi) : "";
    }

    public void applyToUser(User user) {
        if (user == null) {
            return;
        }
        if (!isComplete()) {
            throw new IllegalStateException("Onboarding profile is incomplete");
        }

        user.setGender(gender);
        user.setBirthDate(birthDate);
        user.setHeight(heightCm);
        user.setWeight(weightKg);
        user.setTargetWeight(targetWeightKg);
        user.setGoal(goal);
        user.setFitnessLevel(fitnessLevel);

        Float bmi = calculateBmiOrNull();
        user.setBmi(bmi);
        user.setBmiCategory(bmi != null ? ProfileMetrics.categoryForBmi(bmi) : null);
        user.setOnboardingCompleted(true);
        user.setOnboardingCompletedAt(System.currentTimeMillis());
    }

    private static boolean inRange(Float value, float min, float max) {
        return value != null && value >= min && value <= max;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String sanitize(String value) {
        return value != null ? value.trim().replaceAll("<[^>]*>", "") : "";
    }
}
