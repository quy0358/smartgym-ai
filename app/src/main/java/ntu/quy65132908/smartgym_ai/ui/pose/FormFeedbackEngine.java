package ntu.quy65132908.smartgym_ai.ui.pose;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FormFeedbackEngine {
    private static final String NO_PERSON = "Đưa toàn thân vào khung hình để bắt đầu nhận diện.";
    private static final String READY = "Sẵn sàng. Giữ toàn thân trong khung hình.";

    private ExerciseType exerciseType = ExerciseType.PUSH_UP;
    private final PoseRepCounter pushUpCounter = new PoseRepCounter(2);
    private final PoseRepCounter squatCounter = new PoseRepCounter(2);
    private final PoseSignalSmoother pushUpDownSmoother = new PoseSignalSmoother(0.8f, 0.65f, 0.35f);
    private final PoseSignalSmoother squatDownSmoother = new PoseSignalSmoother(0.8f, 0.65f, 0.35f);
    private long accumulatedPlankHoldMs;
    private long activePlankStartedAtMs;

    public void setExerciseType(@NonNull ExerciseType exerciseType) {
        this.exerciseType = exerciseType;
        reset();
    }

    public void reset() {
        pushUpCounter.reset();
        squatCounter.reset();
        pushUpDownSmoother.reset();
        squatDownSmoother.reset();
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
            return new PoseFeedback("Giữ vai, khuỷu tay và cổ tay rõ trong khung hình.", pushUpCounter.getReps(), quality(frame), true);
        }

        if (!Float.isNaN(bodyLine) && bodyLine < 150f) {
            return new PoseFeedback("Giữ thân người thẳng, siết bụng và tránh võng lưng.", pushUpCounter.getReps(), quality(frame) - 15, true);
        }

        boolean smoothedDown = pushUpDownSmoother.update(elbow < 95f);
        if (smoothedDown) {
            pushUpCounter.update(true);
            return new PoseFeedback("Tốt. Đẩy người lên, giữ khuỷu tay kiểm soát.", pushUpCounter.getReps(), quality(frame), true);
        }

        if (elbow > 155f && !smoothedDown) {
            int before = pushUpCounter.getReps();
            int after = pushUpCounter.update(false);
            if (after > before) {
                return new PoseFeedback("Hoàn thành một lần chống đẩy. Tiếp tục giữ thân người thẳng.", after, quality(frame), true);
            }
            return new PoseFeedback("Hạ người chống đẩy chậm xuống, ngực hướng gần sàn.", after, quality(frame), true);
        }

        return new PoseFeedback("Đi sâu thêm một chút để biên độ chống đẩy đủ tốt.", pushUpCounter.getReps(), quality(frame) - 5, true);
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
            return new PoseFeedback("Giữ hông, gối và cổ chân rõ trong khung hình.", squatCounter.getReps(), quality(frame), true);
        }

        if (!Float.isNaN(hip) && hip < 55f) {
            return new PoseFeedback("Giữ ngực mở hơn, không gập người quá sâu về trước.", squatCounter.getReps(), quality(frame) - 10, true);
        }

        boolean smoothedDown = squatDownSmoother.update(knee < 95f);
        if (smoothedDown) {
            squatCounter.update(true);
            return new PoseFeedback("Độ sâu squat tốt. Đẩy gối theo hướng mũi chân.", squatCounter.getReps(), quality(frame), true);
        }

        if (knee > 160f && !smoothedDown) {
            int before = squatCounter.getReps();
            int after = squatCounter.update(false);
            if (after > before) {
                return new PoseFeedback("Hoàn thành một lần squat. Chuẩn bị lần tiếp theo.", after, quality(frame), true);
            }
            return new PoseFeedback("Hạ hông xuống chậm, giữ trọng tâm ở giữa bàn chân.", after, quality(frame), true);
        }

        return new PoseFeedback("Xuống thêm một chút để đùi gần song song với sàn.", squatCounter.getReps(), quality(frame) - 5, true);
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
            return new PoseFeedback("Giữ vai, hông và cổ chân rõ trong khung hình.", currentReps(), quality(frame), true, pausePlankHoldAndGetSeconds());
        }

        if (bodyLine < 150f) {
            return new PoseFeedback("Nâng hông và siết bụng để thân người thẳng hơn.", currentReps(), quality(frame) - 15, true, pausePlankHoldAndGetSeconds());
        }

        if (bodyLine > 175f) {
            return new PoseFeedback("Hạ hông nhẹ để thân người thẳng tự nhiên hơn.", currentReps(), quality(frame) - 5, true, pausePlankHoldAndGetSeconds());
        }

        return new PoseFeedback(
                "Form plank ổn. Giữ nhịp thở đều và siết bụng.",
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
