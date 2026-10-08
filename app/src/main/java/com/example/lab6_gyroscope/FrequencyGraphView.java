package com.example.lab6_gyroscope;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

public class FrequencyGraphView extends View {
    private Paint paint = new Paint();
    private double[] xMagnitudes;
    private double[] yMagnitudes;
    private double[] zMagnitudes;

    private double sampleRate = 0;

    public FrequencyGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setAntiAlias(true);
    }

    public void setSpectrum(double[] x, double[] y, double[] z, double fs) {
        xMagnitudes = x;
        yMagnitudes = y;
        zMagnitudes = z;
        sampleRate = fs;

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        float left = 95;
        float right = width - 25;
        float top = 45;
        float bottom = height - 80;

        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2);
        paint.setTextSize(25);

        // Axes
        canvas.drawLine(left, top, left, bottom, paint);
        canvas.drawLine(left, bottom, right, bottom, paint);

        // Labels
        canvas.drawText("Frequency (Hz)", width / 2 - 80, height - 15, paint);

        canvas.save();
        canvas.rotate(-90);
        canvas.drawText("Magnitude", -height / 2 - 55, 30, paint);
        canvas.restore();

        // Legend
        paint.setColor(Color.RED);
        canvas.drawText("X", left + 10, 28, paint);

        paint.setColor(Color.GREEN);
        canvas.drawText("Y", left + 70, 28, paint);

        paint.setColor(Color.BLUE);
        canvas.drawText("Z", left + 130, 28, paint);

        if (xMagnitudes == null || yMagnitudes == null || zMagnitudes == null || sampleRate <= 0) {
            return;
        }

        double maxMagnitude = 0.01;

        for (int i = 1; i < 64; i++) {
            maxMagnitude = Math.max(maxMagnitude, xMagnitudes[i]);
            maxMagnitude = Math.max(maxMagnitude, yMagnitudes[i]);
            maxMagnitude = Math.max(maxMagnitude, zMagnitudes[i]);
        }

        paint.setColor(Color.BLACK);
        paint.setTextSize(20);

        canvas.drawText("0", left - 5, bottom + 25, paint);
        canvas.drawText(String.format(Locale.US, "%.1f", sampleRate / 4), (left + right) / 2 - 20, bottom + 25, paint);
        canvas.drawText(String.format(Locale.US, "%.1f", sampleRate / 2), right - 40, bottom + 25, paint);
        canvas.drawText(String.format(Locale.US, "%.2f", maxMagnitude), left + 5, top + 20, paint);

        // X, Y, and Z spectra.
        drawSpectrum(canvas, xMagnitudes, Color.RED, maxMagnitude, left, right, top, bottom);
        drawSpectrum(canvas, yMagnitudes, Color.GREEN, maxMagnitude, left, right, top, bottom);
        drawSpectrum(canvas, zMagnitudes, Color.BLUE, maxMagnitude, left, right, top, bottom);
    }

    private void drawSpectrum(Canvas canvas, double[] magnitudes, int color, double maxMagnitude, float left, float right, float top, float bottom) {
        paint.setColor(color);
        paint.setStrokeWidth(3);

        for (int i = 2; i < 64; i++) {
            float x1 = left + (i - 1) / 64f * (right - left);
            float x2 = left + i / 64f * (right - left);
            float y1 = bottom - (float) (magnitudes[i - 1] / maxMagnitude) * (bottom - top);
            float y2 = bottom - (float) (magnitudes[i] / maxMagnitude) * (bottom - top);

            canvas.drawLine(x1, y1, x2, y2, paint);
        }
    }
}

