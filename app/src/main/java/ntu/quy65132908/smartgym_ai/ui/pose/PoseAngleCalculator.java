package ntu.quy65132908.smartgym_ai.ui.pose;

import androidx.annotation.Nullable;

public final class PoseAngleCalculator {
    private static final float MIN_VISIBILITY = 0.55f;

    private PoseAngleCalculator() {
    }

    @Nullable
    public static Float angle(PoseFrame frame,
                              BodyLandmark first,
                              BodyLandmark middle,
                              BodyLandmark last) {
        if (frame == null || !frame.has(first) || !frame.has(middle) || !frame.has(last)) {
            return null;
        }

        PosePoint a = frame.get(first);
        PosePoint b = frame.get(middle);
        PosePoint c = frame.get(last);
        if (a == null || b == null || c == null
                || !a.isVisible(MIN_VISIBILITY)
                || !b.isVisible(MIN_VISIBILITY)
                || !c.isVisible(MIN_VISIBILITY)) {
            return null;
        }

        double radians = Math.atan2(c.getY() - b.getY(), c.getX() - b.getX())
                - Math.atan2(a.getY() - b.getY(), a.getX() - b.getX());
        double angle = Math.abs(Math.toDegrees(radians));
        if (angle > 180.0) {
            angle = 360.0 - angle;
        }
        return (float) angle;
    }

    public static float averageAvailable(@Nullable Float first, @Nullable Float second) {
        if (first == null && second == null) return Float.NaN;
        if (first == null) return second;
        if (second == null) return first;
        return (first + second) / 2f;
    }

    public static float verticalDistance(PoseFrame frame, BodyLandmark top, BodyLandmark bottom) {
        PosePoint a = frame != null ? frame.get(top) : null;
        PosePoint b = frame != null ? frame.get(bottom) : null;
        if (a == null || b == null || !a.isVisible(MIN_VISIBILITY) || !b.isVisible(MIN_VISIBILITY)) {
            return Float.NaN;
        }
        return b.getY() - a.getY();
    }
}
