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
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;

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
    private SensorGraphView sensorGraph;
    private ArrayList<Float> xBuffer = new ArrayList<>();
    private ArrayList<Float> yBuffer = new ArrayList<>();
    private ArrayList<Float> zBuffer = new ArrayList<>();
    private TextView tvMotionFeature;
    private TextView tvMotionLevel;
    private EditText editThreshold1;
    private EditText editThreshold2;
    private Button btnApplyThresholds;

    private long startTime;
    private int sampleCount = 0;
    private boolean isRecording = false;
    private long lastGraphUpdate = 0;
    private static final int WINDOW_SIZE = 128;
    private double motionFeature = 0;
    private double threshold1;
    private double threshold2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        tvSensorName = findViewById(R.id.tvSensorName);
        tvElapsedTime = findViewById(R.id.tvElapsedTime);
        tvSampleCount = findViewById(R.id.tvSampleCount);
        tvSensorValues = findViewById(R.id.tvSensorValues);
        btnStopSession = findViewById(R.id.btnStopSession);
        sensorGraph = findViewById(R.id.sensorGraph);
        tvMotionFeature = findViewById(R.id.tvMotionFeature);
        tvMotionLevel = findViewById(R.id.tvMotionLevel);
        editThreshold1 = findViewById(R.id.editThreshold1);
        editThreshold2 = findViewById(R.id.editThreshold2);
        btnApplyThresholds = findViewById(R.id.btnApplyThresholds);

        sensorManager = (SensorManager)
                getSystemService(Context.SENSOR_SERVICE);

        Intent intent = getIntent();
        int sensorType = intent.getIntExtra(
                "sensorType", Sensor.TYPE_GYROSCOPE);

        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            threshold1 = 0.20;
            threshold2 = 0.80;
        } else {
            threshold1 = 0.05;
            threshold2 = 0.25;
        }

        editThreshold1.setText(String.valueOf(threshold1));
        editThreshold2.setText(String.valueOf(threshold2));

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

        btnApplyThresholds.setOnClickListener(v -> applyThresholds());

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

    private void addSample(float x, float y, float z) {
        xBuffer.add(x);
        yBuffer.add(y);
        zBuffer.add(z);

        if (xBuffer.size() > WINDOW_SIZE) {
            xBuffer.remove(0);
            yBuffer.remove(0);
            zBuffer.remove(0);
        }

        if (xBuffer.size() == WINDOW_SIZE) {
            calculateMotion();
        }
    }

    private double calculateStandardDeviation(ArrayList<Float> values) {
        double sum = 0;

        for (float value : values) {
            sum += value;
        }

        double mean = sum / values.size();

        double squaredDifferenceSum = 0;

        for (float value : values) {
            squaredDifferenceSum += Math.pow(value - mean, 2);
        }

        double variance = squaredDifferenceSum / values.size();

        return Math.sqrt(variance);
    }

    private void calculateMotion() {
        double sigmaX = calculateStandardDeviation(xBuffer);
        double sigmaY = calculateStandardDeviation(yBuffer);
        double sigmaZ = calculateStandardDeviation(zBuffer);

        motionFeature = Math.sqrt (
                sigmaX * sigmaX + sigmaY * sigmaY + sigmaZ * sigmaZ
        );

        tvMotionFeature.setText(String.format(Locale.US, "Motion Feature (M): %.4f", motionFeature));

        updateMotionLevel();
    }


    private void applyThresholds() {

        String input1 = editThreshold1.getText().toString().trim();
        String input2 = editThreshold2.getText().toString().trim();

        if (input1.isEmpty() || input2.isEmpty()) {
            Toast.makeText(this,
                    "Please enter both thresholds",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double newT1 = Double.parseDouble(input1);
            double newT2 = Double.parseDouble(input2);

            if (!Double.isFinite(newT1) ||
                    !Double.isFinite(newT2) ||
                    newT1 < 0 || newT1 >= newT2) {

                Toast.makeText(this,
                        "Thresholds must satisfy 0 <= T1 < T2",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            threshold1 = newT1;
            threshold2 = newT2;

            Toast.makeText(this,
                    "Thresholds updated",
                    Toast.LENGTH_SHORT).show();

            if (xBuffer.size() == WINDOW_SIZE) {
                updateMotionLevel();
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this,
                    "Please enter valid numbers",
                    Toast.LENGTH_SHORT).show();
        }
    }


    private void updateMotionLevel() {

        String level;

        if (motionFeature < threshold1) {
            level = "LOW";
        } else if (motionFeature < threshold2) {
            level = "MEDIUM";
        } else {
            level = "HIGH";
        }

        tvMotionLevel.setText("Motion Level: " + level);
    }

    private void startRecording() {
        if (selectedSensor == null || isRecording) {
            return;
        }

        if (!createSessionFile()) {
            finish();
            return;
        }

        xBuffer.clear();
        yBuffer.clear();
        zBuffer.clear();
        motionFeature = 0;

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

        addSample(x, y, z);

        sensorGraph.addSample(event.timestamp, x, y, z);
        long currentTime = SystemClock.elapsedRealtime();

        if (currentTime - lastGraphUpdate >= 100) {
            sensorGraph.invalidate();
            lastGraphUpdate = currentTime;
        }

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
