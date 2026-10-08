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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SessionActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor selectedSensor;

    private TextView tvSensorName;
    private TextView tvElapsedTime;
    private TextView tvSampleCount;
    private TextView tvSensorValues;
    private Button btnStopSession;
    private BufferedWriter writer;
    private File sessionFile;

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

    private boolean createSessionFile() {
        String timestamp = new SimpleDateFormat(
                "yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String prefix;
        if (selectedSensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            prefix = "ACCEL_";
        } else {
            prefix = "GYRO_";
        }
        File directory = getExternalFilesDir("sessions");

        if (directory == null) {
            Toast.makeText(this, "Storage unavailable", Toast.LENGTH_LONG).show();
            return false;
        }

        try {
            sessionFile = new File(directory, prefix + timestamp + ".csv");

            int secondsToAdd = 1;

            while (sessionFile.exists()) {
                String nextTimestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date(System.currentTimeMillis() + secondsToAdd * 1000L));
                sessionFile = new File(directory, prefix + nextTimestamp + ".csv");
                secondsToAdd++;
            }

            writer = new BufferedWriter(
                    new FileWriter(sessionFile)
            );
            writer.write("timestamp_ns,x,y,z");
            writer.newLine();

            return true;
        } catch (IOException e) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {

                }
                writer = null;
            }

            Toast.makeText(this, "Error creating session file", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private boolean writeSensorData(SensorEvent event) {
        if (writer == null) {
            return false;
        }

        try {
            String line = String.format(Locale.US, "%d,%s,%s,%s", event.timestamp, Float.toString(event.values[0]), Float.toString(event.values[1]), Float.toString(event.values[2]));
            writer.write(line);
            writer.newLine();
            return true;
        } catch (IOException e) {
            Toast.makeText(this, "Error writing sensor data", Toast.LENGTH_SHORT).show();
            stopRecording();
            finish();
            return false;
        }
    }

    private void startRecording() {
        if (selectedSensor == null || isRecording) {
            return;
        }

        if (!createSessionFile()) {
            finish();
            return;
        }

        sampleCount = 0;
        startTime = SystemClock.elapsedRealtime();
        isRecording = true;

        boolean registered = sensorManager.registerListener(this, selectedSensor, SensorManager.SENSOR_DELAY_GAME);

        if (!registered) {
            Toast.makeText(this, "Unable to start sensor", Toast.LENGTH_LONG).show();
            stopRecording();
            finish();
        }
    }

    private void stopRecording() {
        isRecording = false;
        sensorManager.unregisterListener(this);

        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                Toast.makeText(this, "Error closing session file", Toast.LENGTH_SHORT).show();
            } finally {
                writer = null;
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (!isRecording) {
            return;
        }

        if (!writeSensorData(event)) {
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
    }

    @Override
    protected void onPause() {
        stopRecording();
        super.onPause();

        if (!isFinishing()) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        stopRecording();
        super.onDestroy();
    }
}
