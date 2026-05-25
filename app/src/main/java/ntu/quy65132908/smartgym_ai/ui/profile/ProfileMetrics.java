package ntu.quy65132908.smartgym_ai.ui.profile;

public final class ProfileMetrics {
    static final int MAX_NAME_LENGTH = 50;
    static final int MAX_GOAL_LENGTH = 120;
    static final float MIN_WEIGHT_KG = 20f;
    static final float MAX_WEIGHT_KG = 350f;
    static final float MIN_HEIGHT_CM = 80f;
    static final float MAX_HEIGHT_CM = 250f;

    private static final String BMI_UNDERWEIGHT = "Thiếu cân";
    private static final String BMI_NORMAL = "Bình thường";
    private static final String BMI_OVERWEIGHT = "Thừa cân";
    private static final String BMI_OBESE = "Béo phì";

    private ProfileMetrics() {}

    public static Float parseOptionalFloat(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            float value = Float.parseFloat(raw.trim().replace(',', '.'));
            if (Float.isNaN(value) || Float.isInfinite(value)) {
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static float calculateBmi(float weightKg, float heightCm) {
        float heightM = heightCm / 100f;
        return weightKg / (heightM * heightM);
    }

    public static String categoryForBmi(float bmi) {
        if (bmi < 18.5f) {
            return BMI_UNDERWEIGHT;
        }
        if (bmi < 25f) {
            return BMI_NORMAL;
        }
        if (bmi < 30f) {
            return BMI_OVERWEIGHT;
        }
        return BMI_OBESE;
    }

    public static Float calculateBmiOrNull(Float weightKg, Float heightCm) {
        if (weightKg == null || heightCm == null || heightCm <= 0f) {
            return null;
        }
        return calculateBmi(weightKg, heightCm);
    }
}
