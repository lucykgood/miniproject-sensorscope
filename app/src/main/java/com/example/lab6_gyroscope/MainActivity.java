package com.example.lab6_gyroscope;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private TextView tvGx;
    private TextView tvGy;
    private TextView tvGz;
    private TextView tvMagnitude;
    private TextView tvStatus;
    private TextView tvFilename;

    private Button btnStart;
    private Button btnStop;

    private SensorManager sensorManager;
    private Sensor gyroscope;
    private BufferedWriter writer;
    private boolean recording = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        tvGx = findViewById(R.id.tvGx);
        tvGy = findViewById(R.id.tvGy);
        tvGz = findViewById(R.id.tvGz);
        tvMagnitude = findViewById(R.id.tvMagnitude);

        tvStatus = findViewById(R.id.tvStatus);
        tvFilename = findViewById(R.id.tvFileName);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        if (gyroscope == null) {
            tvStatus.setText("Status: Gyroscope not available");
            tvStatus.setTextColor(Color.RED);
            Toast.makeText(this, "Gyroscope not available", Toast.LENGTH_LONG).show();

        }

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecording();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecording();
            }
        });
    }

    private void startRecording() {
        if (gyroscope == null || recording) {
            return;
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "session_" + timestamp + ".csv";

        writer = createWriter(fileName);

        writeLine("timestamp,gx,gy,gz");
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);

        recording = true;

        tvStatus.setText("Status: Recording");
        tvStatus.setTextColor(Color.rgb(0, 128, 0));
        tvFilename.setText(fileName);

        Toast.makeText(this, "Recording started", Toast.LENGTH_LONG).show();
    }

    private void stopRecording() {
        if (!recording) {
            return;
        }

        sensorManager.unregisterListener(this);
        recording = false;

        closeWriter();

        tvGx.setText("-- rad/s");
        tvGy.setText("-- rad/s");
        tvGz.setText("-- rad/s");
        tvMagnitude.setText("-- rad/s");

        tvStatus.setText("Status: Stopped");
        tvStatus.setTextColor(Color.DKGRAY);

        Toast.makeText(this, "Recording stopped", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float gx = event.values[0];
            float gy = event.values[1];
            float gz = event.values[2];

            double magnitude = Math.sqrt(gx * gx + gy * gy + gz * gz);

            tvGx.setText(String.format(Locale.US, "%.2f rad/s", gx));
            tvGy.setText(String.format(Locale.US, "%.2f rad/s", gy));
            tvGz.setText(String.format(Locale.US, "%.2f rad/s", gz));
            tvMagnitude.setText(String.format(Locale.US, "%.2f rad/s", magnitude));

            if (recording) {
                long timestamp = System.currentTimeMillis();
                writeLine(timestamp + "," + gx + "," + gy + "," + gz);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private BufferedWriter createWriter(String filename) {
        try {
            FileOutputStream fileOutputStream = openFileOutput(filename, MODE_PRIVATE);
            return new BufferedWriter(new OutputStreamWriter(fileOutputStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeLine(String Line) {
        try {
            writer.write(Line);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void closeWriter() {
        try {
            if (writer != null) {
                writer.close();
                writer = null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}