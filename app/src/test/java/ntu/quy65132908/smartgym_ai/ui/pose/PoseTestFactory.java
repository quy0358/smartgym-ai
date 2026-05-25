package ntu.quy65132908.smartgym_ai.ui.pose;

import java.util.EnumMap;
import java.util.Map;

final class PoseTestFactory {
    private PoseTestFactory() {
    }

    static PoseFrame frame(Object... values) {
        Map<BodyLandmark, PosePoint> points = new EnumMap<>(BodyLandmark.class);
        for (int i = 0; i < values.length; i += 3) {
            BodyLandmark landmark = (BodyLandmark) values[i];
            float x = ((Number) values[i + 1]).floatValue();
            float y = ((Number) values[i + 2]).floatValue();
            points.put(landmark, new PosePoint(x, y, 0.95f));
        }
        return new PoseFrame(points, 1000, 1000, true);
    }

    static PoseFrame pushUpTop() {
        return frame(
                BodyLandmark.LEFT_SHOULDER, 200, 500,
                BodyLandmark.LEFT_ELBOW, 300, 500,
                BodyLandmark.LEFT_WRIST, 420, 500,
                BodyLandmark.RIGHT_SHOULDER, 200, 620,
                BodyLandmark.RIGHT_ELBOW, 300, 620,
                BodyLandmark.RIGHT_WRIST, 420, 620,
                BodyLandmark.LEFT_HIP, 450, 500,
                BodyLandmark.LEFT_ANKLE, 700, 500,
                BodyLandmark.RIGHT_HIP, 450, 620,
                BodyLandmark.RIGHT_ANKLE, 700, 620);
    }

    static PoseFrame pushUpDown() {
        return frame(
                BodyLandmark.LEFT_SHOULDER, 200, 500,
                BodyLandmark.LEFT_ELBOW, 300, 500,
                BodyLandmark.LEFT_WRIST, 300, 600,
                BodyLandmark.RIGHT_SHOULDER, 200, 620,
                BodyLandmark.RIGHT_ELBOW, 300, 620,
                BodyLandmark.RIGHT_WRIST, 300, 720,
                BodyLandmark.LEFT_HIP, 450, 500,
                BodyLandmark.LEFT_ANKLE, 700, 500,
                BodyLandmark.RIGHT_HIP, 450, 620,
                BodyLandmark.RIGHT_ANKLE, 700, 620);
    }

    static PoseFrame plankAligned() {
        return frame(
                BodyLandmark.LEFT_SHOULDER, 200, 500,
                BodyLandmark.LEFT_HIP, 430, 500,
                BodyLandmark.LEFT_ANKLE, 700, 600,
                BodyLandmark.RIGHT_SHOULDER, 200, 620,
                BodyLandmark.RIGHT_HIP, 430, 620,
                BodyLandmark.RIGHT_ANKLE, 700, 720);
    }

    static PoseFrame squatTop() {
        return frame(
                BodyLandmark.LEFT_SHOULDER, 470, 280,
                BodyLandmark.LEFT_HIP, 470, 430,
                BodyLandmark.LEFT_KNEE, 470, 650,
                BodyLandmark.LEFT_ANKLE, 470, 860,
                BodyLandmark.RIGHT_SHOULDER, 540, 280,
                BodyLandmark.RIGHT_HIP, 540, 430,
                BodyLandmark.RIGHT_KNEE, 540, 650,
                BodyLandmark.RIGHT_ANKLE, 540, 860);
    }

    static PoseFrame squatDown() {
        return frame(
                BodyLandmark.LEFT_SHOULDER, 420, 300,
                BodyLandmark.LEFT_HIP, 470, 520,
                BodyLandmark.LEFT_KNEE, 610, 640,
                BodyLandmark.LEFT_ANKLE, 470, 780,
                BodyLandmark.RIGHT_SHOULDER, 500, 300,
                BodyLandmark.RIGHT_HIP, 550, 520,
                BodyLandmark.RIGHT_KNEE, 690, 640,
                BodyLandmark.RIGHT_ANKLE, 550, 780);
    }
}
