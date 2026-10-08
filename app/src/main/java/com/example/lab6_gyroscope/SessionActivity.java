package com.example.lab6_gyroscope;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class SessionActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor selectedSensor;

    private TextView tvSensorName;
    private TextView tvElapsedTime;
    private TextView tvSampleCount;
    private TextView tvSensorValues;
    private Button btnStopSession;

    private long startTime;
    private int sampleCount = 0;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        tvSensorName = findViewById(R.id.tvSensorName);
        tvElapsedTime = findViewById(R.id.tvElapsedTime);
        tvSampleCount = findViewById(R.id.tvSampleCount);
        tvSensorValues = findViewById(R.id.tvSensorValues);
        btnStopSession = findViewById(R.id.btnStopSession);

        sensorManager = (SensorManager)
                getSystemService(Context.SENSOR_SERVICE);

        Intent intent = getIntent();
        int sensorType = intent.getIntExtra(
                "sensorType", Sensor.TYPE_GYROSCOPE);

        selectedSensor = sensorManager.getDefaultSensor(sensorType);

        if (selectedSensor == null) {
            finish();
            return;
        }

        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            tvSensorName.setText("Accelerometer");
        } else {
            tvSensorName.setText("Gyroscope");
        }

        btnStopSession.setOnClickListener(view -> {
            stopRecording();
            finish();
        });

        startRecording();
    }

    private void startRecording() {
        sampleCount = 0;
        startTime = SystemClock.elapsedRealtime();
        isRecording = true;

        sensorManager.registerListener(
                this,
                selectedSensor,
                SensorManager.SENSOR_DELAY_GAME);
    }

    private void stopRecording() {
        isRecording = false;
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (!isRecording) {
            return;
        }

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        sampleCount++;

        long elapsedTime =
                SystemClock.elapsedRealtime() - startTime;

        tvElapsedTime.setText(String.format(
                Locale.US, "Time: %.1f s",
                elapsedTime / 1000.0));

        tvSampleCount.setText(
                "Samples: " + sampleCount);

        tvSensorValues.setText(String.format(
                Locale.US,
                "X: %.3f\nY: %.3f\nZ: %.3f",
                x, y, z));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No action needed for this project.
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRecording();
    }
}
