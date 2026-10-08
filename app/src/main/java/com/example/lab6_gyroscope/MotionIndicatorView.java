package com.example.lab6_gyroscope;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

public class MotionIndicatorView extends View {

    private Paint paint = new Paint();

    private float circleX = -1;
    private float circleY = -1;

    private float directionX = 1;
    private float directionY = 1;

    private float speed = 0;
    private float radius;

    private long lastUpdateTime = 0;

    public MotionIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint.setAntiAlias(true);

        radius = 20 * getResources()
                .getDisplayMetrics().density;
    }

    public void setMotionLevel(String level) {

        if (level.equals("LOW")) {
            speed = 0;
        } else if (level.equals("MEDIUM")) {
            speed = 100;
        } else {
            speed = 300;
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        if (width <= 2 * radius ||
                height <= 2 * radius) {
            return;
        }

        // Center the circle
        if (circleX < 0 || circleY < 0) {
            circleX = width / 2;
            circleY = height / 2;
        }

        long currentTime = SystemClock.elapsedRealtime();

        if (lastUpdateTime == 0) {
            lastUpdateTime = currentTime;
        }

        float elapsedSeconds =
                (currentTime - lastUpdateTime) / 1000f;

        lastUpdateTime = currentTime;

        // dp/s -> pixels/s
        float density = getResources()
                .getDisplayMetrics().density;

        float distance = speed * density * elapsedSeconds;

        circleX += directionX * distance;
        circleY += directionY * distance;

        // Bounce off left and right
        if (circleX + radius >= width) {
            circleX = width - radius;
            directionX = -1;
        } else if (circleX - radius <= 0) {
            circleX = radius;
            directionX = 1;
        }

        // Bounce off top and bottom
        if (circleY + radius >= height) {
            circleY = height - radius;
            directionY = -1;
        } else if (circleY - radius <= 0) {
            circleY = radius;
            directionY = 1;
        }

        paint.setColor(Color.BLUE);
        canvas.drawCircle(circleX, circleY, radius, paint);

        if (speed > 0) {
            postInvalidateDelayed(16);
        }
    }
}

