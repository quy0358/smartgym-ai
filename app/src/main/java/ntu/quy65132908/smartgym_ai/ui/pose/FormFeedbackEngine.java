package ntu.quy65132908.smartgym_ai.ui.pose;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FormFeedbackEngine {
    private static final String NO_PERSON = "ÄÆ°a toÃ n thÃ¢n vÃ o khung hÃ¬nh Ä‘á»ƒ báº¯t Ä‘áº§u nháº­n diá»‡n.";
    private static final String READY = "Sáºµn sÃ ng. Giá»¯ toÃ n thÃ¢n trong khung hÃ¬nh.";

    private ExerciseType exerciseType = ExerciseType.PUSH_UP;
    private final PoseRepCounter pushUpCounter = new PoseRepCounter(2);
    private final PoseRepCounter squatCounter = new PoseRepCounter(2);
    private long accumulatedPlankHoldMs;
    private long activePlankStartedAtMs;

    public void setExerciseType(@NonNull ExerciseType exerciseType) {
        this.exerciseType = exerciseType;
        reset();
    }

    public void reset() {
        pushUpCounter.reset();
        squatCounter.reset();
        accumulatedPlankHoldMs = 0L;
        activePlankStartedAtMs = 0L;
    }

    public PoseFeedback evaluate(@Nullable PoseFrame frame) {
        if (frame == null || frame.getPoints().isEmpty() || frame.visibilityScore() < 0.25f) {
            int holdSeconds = exerciseType == ExerciseType.PLANK ? pausePlankHoldAndGetSeconds() : 0;
            return new PoseFeedback(NO_PERSON, currentReps(), 0, false, holdSeconds);
        }

        switch (exerciseType) {
            case SQUAT:
                return evaluateSquat(frame);
            case PLANK:
                return evaluatePlank(frame);
            case PUSH_UP:
            default:
                return evaluatePushUp(frame);
        }
    }

    private PoseFeedback evaluatePushUp(PoseFrame frame) {
        Float leftElbow = PoseAngleCalculator.angle(
                frame,
                BodyLandmark.LEFT_SHOULDER,
                BodyLandmark.LEFT_ELBOW,
                BodyLandmark.LEFT_WRIST);
        Float rightElbow = PoseAngleCalculator.angle(
                frame,
                BodyLandmark.RIGHT_SHOULDER,
                BodyLandmark.RIGHT_ELBOW,
                BodyLandmark.RIGHT_WRIST);
        float elbow = PoseAngleCalculator.averageAvailable(leftElbow, rightElbow);

        Float leftBody = PoseAngleCalculator.angle(
                frame,
                BodyLandmark.LEFT_SHOULDER,
                BodyLandmark.LEFT_HIP,
                BodyLandmark.LEFT_ANKLE);
        Float rightBody = PoseAngleCalculator.angle(
                frame,
                BodyLandmark.RIGHT_SHOULDER,
                BodyLandmark.RIGHT_HIP,
                BodyLandmark.RIGHT_ANKLE);
        float bodyLine = PoseAngleCalculator.averageAvailable(leftBody, rightBody);

        if (Float.isNaN(elbow)) {
            return new PoseFeedback("Giá»¯ vai, khuá»·u tay vÃ  cá»• tay rÃµ trong khung hÃ¬nh.", pushUpCounter.getReps(), quality(frame), true);
        }

        if (!Float.isNaN(bodyLine) && bodyLine < 150f) {
            return new PoseFeedback("Giá»¯ thÃ¢n ngÆ°á»i tháº³ng, siáº¿t bá»¥ng vÃ  trÃ¡nh vÃµng lÆ°ng.", pushUpCounter.getReps(), quality(frame) - 15, true);
        }

        if (elbow < 95f) {
            pushUpCounter.update(true);
            return new PoseFeedback("Tá»‘t. Äáº©y ngÆ°á»i lÃªn, giá»¯ khuá»·u tay kiá»ƒm soÃ¡t.", pushUpCounter.getReps(), quality(frame), true);
        }

        if (elbow > 155f) {
            int before = pushUpCounter.getReps();
            int after = pushUpCounter.update(false);
            if (after > before) {
                return new PoseFeedback("HoÃ n thÃ nh má»™t láº§n chá»‘ng Ä‘áº©y. Tiáº¿p tá»¥c giá»¯ thÃ¢n ngÆ°á»i tháº³ng.", after, quality(frame), true);
            }
            return new PoseFeedback("Háº¡ ngÆ°á»i cháº­m xuá»‘ng, ngá»±c hÆ°á»›ng gáº§n sÃ n.", after, quality(frame), true);
        }

        return new PoseFeedback("Äi sÃ¢u thÃªm má»™t chÃºt Ä‘á»ƒ biÃªn Ä‘á»™ chá»‘ng Ä‘áº©y Ä‘á»§ tá»‘t.", pushUpCounter.getReps(), quality(frame) - 5, true);
    }

    private PoseFeedback evaluateSquat(PoseFrame frame) {
        Float leftKnee = PoseAngleCalculator.angle(
                frame,
                BodyLandmark.LEFT_HIP,
                BodyLandmark.LEFT_KNEE,
                BodyLandmark.LEFT_ANKLE);
        Float rightKnee = PoseAngleCalculator.angle(
                frame,
                BodyLandmark.RIGHT_HIP,
                BodyLandmark.RIGHT_KNEE,
                BodyLandmark.RIGHT_ANKLE);
        float knee = PoseAngleCalculator.averageAvailable(leftKnee, rightKnee);

        Float leftHip = PoseAngleCalculator.angle(
                frame,
                BodyLandmark.LEFT_SHOULDER,
                BodyLandmark.LEFT_HIP,
                BodyLandmark.LEFT_KNEE);
        Float rightHip = PoseAngleCalculator.angle(
                frame,
                BodyLandmark.RIGHT_SHOULDER,
                BodyLandmark.RIGHT_HIP,
                BodyLandmark.RIGHT_KNEE);
        float hip = PoseAngleCalculator.averageAvailable(leftHip, rightHip);

        if (Float.isNaN(knee)) {
            return new PoseFeedback("Giá»¯ hÃ´ng, gá»‘i vÃ  cá»• chÃ¢n rÃµ trong khung hÃ¬nh.", squatCounter.getReps(), quality(frame), true);
        }

        if (!Float.isNaN(hip) && hip < 55f) {
            return new PoseFeedback("Giá»¯ ngá»±c má»Ÿ hÆ¡n, khÃ´ng gáº­p ngÆ°á»i quÃ¡ sÃ¢u vá» trÆ°á»›c.", squatCounter.getReps(), quality(frame) - 10, true);
        }

        if (knee < 95f) {
            squatCounter.update(true);
            return new PoseFeedback("Äá»™ sÃ¢u squat tá»‘t. Äáº©y gá»‘i theo hÆ°á»›ng mÅ©i chÃ¢n.", squatCounter.getReps(), quality(frame), true);
        }

        if (knee > 160f) {
            int before = squatCounter.getReps();
            int after = squatCounter.update(false);
            if (after > before) {
                return new PoseFeedback("HoÃ n thÃ nh má»™t láº§n squat. Chuáº©n bá»‹ láº§n tiáº¿p theo.", after, quality(frame), true);
            }
            return new PoseFeedback("Háº¡ hÃ´ng xuá»‘ng cháº­m, giá»¯ trá»ng tÃ¢m á»Ÿ giá»¯a bÃ n chÃ¢n.", after, quality(frame), true);
        }

        return new PoseFeedback("Xuá»‘ng thÃªm má»™t chÃºt Ä‘á»ƒ Ä‘Ã¹i gáº§n song song vá»›i sÃ n.", squatCounter.getReps(), quality(frame) - 5, true);
    }

    private PoseFeedback evaluatePlank(PoseFrame frame) {
        Float leftBody = PoseAngleCalculator.angle(
                frame,
                BodyLandmark.LEFT_SHOULDER,
                BodyLandmark.LEFT_HIP,
                BodyLandmark.LEFT_ANKLE);
        Float rightBody = PoseAngleCalculator.angle(
                frame,
                BodyLandmark.RIGHT_SHOULDER,
                BodyLandmark.RIGHT_HIP,
                BodyLandmark.RIGHT_ANKLE);
        float bodyLine = PoseAngleCalculator.averageAvailable(leftBody, rightBody);

        if (Float.isNaN(bodyLine)) {
            return new PoseFeedback("Giá»¯ vai, hÃ´ng vÃ  cá»• chÃ¢n rÃµ trong khung hÃ¬nh.", currentReps(), quality(frame), true, pausePlankHoldAndGetSeconds());
        }

        if (bodyLine < 150f) {
            return new PoseFeedback("NÃ¢ng hÃ´ng vÃ  siáº¿t bá»¥ng Ä‘á»ƒ thÃ¢n ngÆ°á»i tháº³ng hÆ¡n.", currentReps(), quality(frame) - 15, true, pausePlankHoldAndGetSeconds());
        }

        if (bodyLine > 175f) {
            return new PoseFeedback("Háº¡ hÃ´ng nháº¹ Ä‘á»ƒ thÃ¢n ngÆ°á»i tháº³ng tá»± nhiÃªn hÆ¡n.", currentReps(), quality(frame) - 5, true, pausePlankHoldAndGetSeconds());
        }

        return new PoseFeedback(
                "Form plank á»•n. Giá»¯ nhá»‹p thá»Ÿ Ä‘á»u vÃ  siáº¿t bá»¥ng.",
                currentReps(),
                quality(frame),
                true,
                currentPlankHoldSeconds());
    }

    private int currentPlankHoldSeconds() {
        long now = System.currentTimeMillis();
        if (activePlankStartedAtMs <= 0L) {
            activePlankStartedAtMs = now;
        }
        return (int) Math.max(0L, (accumulatedPlankHoldMs + now - activePlankStartedAtMs) / 1000L);
    }

    private int pausePlankHoldAndGetSeconds() {
        long now = System.currentTimeMillis();
        if (activePlankStartedAtMs > 0L) {
            accumulatedPlankHoldMs += Math.max(0L, now - activePlankStartedAtMs);
            activePlankStartedAtMs = 0L;
        }
        return (int) Math.max(0L, accumulatedPlankHoldMs / 1000L);
    }

    private int quality(PoseFrame frame) {
        int score = Math.round(frame.visibilityScore() * 100f);
        if (score == 0) {
            return 35;
        }
        return score;
    }

    public String getReadyMessage() {
        return READY;
    }

    private int currentReps() {
        switch (exerciseType) {
            case SQUAT:
                return squatCounter.getReps();
            case PUSH_UP:
            case PLANK:
            default:
                return pushUpCounter.getReps();
        }
    }
}
