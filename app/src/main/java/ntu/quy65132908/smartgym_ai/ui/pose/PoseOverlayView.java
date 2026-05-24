package ntu.quy65132908.smartgym_ai.ui.pose;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class PoseOverlayView extends View {
    private static final float MIN_VISIBILITY = 0.55f;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint weakPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private PoseFrame poseFrame;

    public PoseOverlayView(Context context) {
        super(context);
        init();
    }

    public PoseOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PoseOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint.setColor(Color.rgb(195, 244, 0));
        linePaint.setStrokeWidth(6f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        pointPaint.setColor(Color.WHITE);
        pointPaint.setStyle(Paint.Style.FILL);

        weakPointPaint.setColor(Color.argb(130, 255, 255, 255));
        weakPointPaint.setStyle(Paint.Style.FILL);
    }

    public void setPoseFrame(@Nullable PoseFrame poseFrame) {
        this.poseFrame = poseFrame;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (poseFrame == null || poseFrame.getImageWidth() <= 0 || poseFrame.getImageHeight() <= 0) {
            return;
        }

        drawSegment(canvas, BodyLandmark.LEFT_SHOULDER, BodyLandmark.RIGHT_SHOULDER);
        drawSegment(canvas, BodyLandmark.LEFT_HIP, BodyLandmark.RIGHT_HIP);
        drawSegment(canvas, BodyLandmark.LEFT_SHOULDER, BodyLandmark.LEFT_ELBOW);
        drawSegment(canvas, BodyLandmark.LEFT_ELBOW, BodyLandmark.LEFT_WRIST);
        drawSegment(canvas, BodyLandmark.RIGHT_SHOULDER, BodyLandmark.RIGHT_ELBOW);
        drawSegment(canvas, BodyLandmark.RIGHT_ELBOW, BodyLandmark.RIGHT_WRIST);
        drawSegment(canvas, BodyLandmark.LEFT_SHOULDER, BodyLandmark.LEFT_HIP);
        drawSegment(canvas, BodyLandmark.RIGHT_SHOULDER, BodyLandmark.RIGHT_HIP);
        drawSegment(canvas, BodyLandmark.LEFT_HIP, BodyLandmark.LEFT_KNEE);
        drawSegment(canvas, BodyLandmark.LEFT_KNEE, BodyLandmark.LEFT_ANKLE);
        drawSegment(canvas, BodyLandmark.RIGHT_HIP, BodyLandmark.RIGHT_KNEE);
        drawSegment(canvas, BodyLandmark.RIGHT_KNEE, BodyLandmark.RIGHT_ANKLE);

        for (PosePoint point : poseFrame.getPoints().values()) {
            float x = transformX(point.getX());
            float y = transformY(point.getY());
            canvas.drawCircle(x, y, point.isVisible(MIN_VISIBILITY) ? 8f : 5f,
                    point.isVisible(MIN_VISIBILITY) ? pointPaint : weakPointPaint);
        }
    }

    private void drawSegment(Canvas canvas, BodyLandmark start, BodyLandmark end) {
        PosePoint a = poseFrame.get(start);
        PosePoint b = poseFrame.get(end);
        if (a == null || b == null || !a.isVisible(MIN_VISIBILITY) || !b.isVisible(MIN_VISIBILITY)) {
            return;
        }
        canvas.drawLine(transformX(a.getX()), transformY(a.getY()), transformX(b.getX()), transformY(b.getY()), linePaint);
    }

    private float transformX(float imageX) {
        float scale = previewScale();
        float scaledWidth = poseFrame.getImageWidth() * scale;
        float offsetX = (getWidth() - scaledWidth) / 2f;
        if (poseFrame.isFrontCamera()) {
            imageX = poseFrame.getImageWidth() - imageX;
        }
        return imageX * scale + offsetX;
    }

    private float transformY(float imageY) {
        float scale = previewScale();
        float scaledHeight = poseFrame.getImageHeight() * scale;
        float offsetY = (getHeight() - scaledHeight) / 2f;
        return imageY * scale + offsetY;
    }

    private float previewScale() {
        if (poseFrame == null || poseFrame.getImageWidth() <= 0 || poseFrame.getImageHeight() <= 0) {
            return 1f;
        }
        float scaleX = getWidth() / (float) poseFrame.getImageWidth();
        float scaleY = getHeight() / (float) poseFrame.getImageHeight();
        return Math.max(scaleX, scaleY);
    }
}
