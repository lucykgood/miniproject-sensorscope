package com.example.lab6_gyroscope;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RadioGroup radioGroupSensors;
    private TextView tvSensorInfo;
    private Button btnStartSession;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor selectedSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        radioGroupSensors = findViewById(R.id.radioGroupSensors);
        tvSensorInfo = findViewById(R.id.tvSensorInfo);
        btnStartSession = findViewById(R.id.btnStartSession);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        radioGroupSensors.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioAccelerometer) {
                selectedSensor = accelerometer;
            } else if (checkedId == R.id.radioGyroscope) {
                selectedSensor = gyroscope;
            } else {
                selectedSensor = null;
            }

            updateSensorInfo();
        });

        btnStartSession.setOnClickListener(view -> {
            if (selectedSensor != null) {
                Intent intent = new Intent(
                        MainActivity.this,
                        SessionActivity.class);
                intent.putExtra(
                        "sensorType",
                        selectedSensor.getType());
                startActivity(intent);
            }
        });
    }

    private void updateSensorInfo() {
        if (selectedSensor == null) {
            tvSensorInfo.setText("Status: Not Available");
            btnStartSession.setEnabled(false);
            return;
        }

        String info = String.format(Locale.US, "Status: Available\n" + "Name: %s\n" + "Maximum Range: %.2f\n" + "Resolution: %.4f\n" + "Minimum Delay: %d microseconds", selectedSensor.getName(), selectedSensor.getMaximumRange(), selectedSensor.getResolution(), selectedSensor.getMinDelay());

        tvSensorInfo.setText(info);
        btnStartSession.setEnabled(true);
    }
}