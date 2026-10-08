package com.example.lab6_gyroscope;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.util.AttributeSet;

import java.util.ArrayList;

public class SensorGraphView extends View {
    private ArrayList<Float> xValues = new ArrayList<>();
    private ArrayList<Float> yValues = new ArrayList<>();
    private ArrayList<Float> zValues = new ArrayList<>();
    private ArrayList<Long> timestamps = new ArrayList<>();
    private Paint paint = new Paint();
    private static final long WINDOW_NS = 5000000000L;

    public SensorGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setAntiAlias(true);
    }

    public void addSample(long timestamp, float x, float y, float z) {
        timestamps.add(timestamp);
        xValues.add(x);
        yValues.add(y);
        zValues.add(z);

        while (!timestamps.isEmpty() && timestamp - timestamps.get(0) > WINDOW_NS) {
            timestamps.remove(0);
            xValues.remove(0);
            yValues.remove(0);
            zValues.remove(0);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        float left = 90;
        float right = width - 20;
        float top = 40;
        float bottom = height - 75;

        // Axes
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2);

        canvas.drawLine(left, top, left, bottom, paint);
        canvas.drawLine(left, bottom, right, bottom, paint);

        paint.setTextSize(28);
        canvas.drawText("Time (s)", width / 2 - 45,height - 15, paint);

        canvas.save();
        canvas.rotate(-90);
        canvas.drawText("Sensor Value", -height / 2 - 65, 30, paint);
        canvas.restore();

        // Key
        paint.setTextSize(25);

        paint.setColor(Color.RED);
        canvas.drawText("X", left + 10, 25, paint);

        paint.setColor(Color.GREEN);
        canvas.drawText("Y", left + 70, 25, paint);

        paint.setColor(Color.BLUE);
        canvas.drawText("Z", left + 130, 25, paint);

        if (timestamps.size() < 2) {
            return;
        }

        // Find largest sensor value
        float maxValue = 1;

        for (int i = 0; i < timestamps.size(); i++) {
            maxValue = Math.max(maxValue,
                    Math.abs(xValues.get(i)));
            maxValue = Math.max(maxValue,
                    Math.abs(yValues.get(i)));
            maxValue = Math.max(maxValue,
                    Math.abs(zValues.get(i)));
        }

        // Sensor signals
        drawSignal(canvas, xValues, timestamps,
                Color.RED, maxValue,
                left, right, top, bottom);

        drawSignal(canvas, yValues, timestamps,
                Color.GREEN, maxValue,
                left, right, top, bottom);

        drawSignal(canvas, zValues, timestamps,
                Color.BLUE, maxValue,
                left, right, top, bottom);
    }

    private void drawSignal(Canvas canvas, ArrayList<Float> values, ArrayList<Long> times, int color, float maxValue, float left, float right, float top, float bottom) {
        paint.setColor(color);
        paint.setStrokeWidth(3);

        long latestTime = times.get(times.size() - 1);
        long startTime = latestTime - WINDOW_NS;

        float centerY = (top + bottom) / 2;
        float scaleY = (bottom - top) / (2 * maxValue);

        for (int i = 1; i < values.size(); i++) {

            float x1 = left + (times.get(i - 1) - startTime)
                    / (float) WINDOW_NS * (right - left);

            float x2 = left + (times.get(i) - startTime)
                    / (float) WINDOW_NS * (right - left);

            float y1 = centerY - values.get(i - 1) * scaleY;
            float y2 = centerY - values.get(i) * scaleY;

            canvas.drawLine(x1, y1, x2, y2, paint);
        }
    }
}
