package ntu.quy65132908.smartgym_ai.ui.pose;

import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.EnumMap;
import java.util.Map;

public final class MlKitPoseMapper {
    private MlKitPoseMapper() {
    }

    public static PoseFrame map(Pose pose, int imageWidth, int imageHeight, boolean frontCamera) {
        Map<BodyLandmark, PosePoint> points = new EnumMap<>(BodyLandmark.class);
        put(points, BodyLandmark.LEFT_SHOULDER, pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER));
        put(points, BodyLandmark.RIGHT_SHOULDER, pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER));
        put(points, BodyLandmark.LEFT_ELBOW, pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW));
        put(points, BodyLandmark.RIGHT_ELBOW, pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW));
        put(points, BodyLandmark.LEFT_WRIST, pose.getPoseLandmark(PoseLandmark.LEFT_WRIST));
        put(points, BodyLandmark.RIGHT_WRIST, pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST));
        put(points, BodyLandmark.LEFT_HIP, pose.getPoseLandmark(PoseLandmark.LEFT_HIP));
        put(points, BodyLandmark.RIGHT_HIP, pose.getPoseLandmark(PoseLandmark.RIGHT_HIP));
        put(points, BodyLandmark.LEFT_KNEE, pose.getPoseLandmark(PoseLandmark.LEFT_KNEE));
        put(points, BodyLandmark.RIGHT_KNEE, pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE));
        put(points, BodyLandmark.LEFT_ANKLE, pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE));
        put(points, BodyLandmark.RIGHT_ANKLE, pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE));
        return new PoseFrame(points, imageWidth, imageHeight, frontCamera);
    }

    private static void put(Map<BodyLandmark, PosePoint> points,
                            BodyLandmark type,
                            PoseLandmark landmark) {
        if (landmark == null) {
            return;
        }
        points.put(type, new PosePoint(
                landmark.getPosition().x,
                landmark.getPosition().y,
                landmark.getInFrameLikelihood()));
    }
}
