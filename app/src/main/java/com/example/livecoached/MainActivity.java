package com.example.livecoached;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Image;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends WearableActivity implements SensorEventListener {

    private TextView mTextView;
    private SensorManager sensorManager;
    private Button vibrateButton;
    private ImageButton sendButton;
    private Button orientationButton;

    private Sensor geomagenticSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        setAmbientEnabled();
    }

    public void init() {
        setContentView(R.layout.activity_main);
        initSensors();
        initTestText();
        initButtons();
        initConnection();
    }

    private void initButtons() {
        initVibrateButton();
        initSendingButton();
        initOrientationButton();
    }

    private void initTestText() {
        mTextView = (TextView) findViewById(R.id.text);
    }

    private void initSensors() {
        System.out.println("printing sensors");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        geomagenticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(this, geomagenticSensor, SensorManager.SENSOR_DELAY_FASTEST);
        System.out.println(geomagenticSensor);
    }

    public void initSendingButton() {
        sendButton = (ImageButton) findViewById(R.id.button_send);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendSensors();
            }
        });
    }

    public void initVibrateButton() {
        vibrateButton = (Button) findViewById(R.id.button_vibrate);
        vibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrate();
            }
        });
    }

    public void initOrientationButton() {
        orientationButton = (Button) findViewById(R.id.button_magnetic);
        orientationButton.setText("Give Orientation");
        orientationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = getMagneticInfos();
                mTextView.setText(text);
            }
        });
    }

    public void initConnection() {
        System.out.println("initiating connection");
    }

    public void sendSensors() {
        System.out.println("sending sensors");
    }

    public void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        long[] vibrationPattern = {0, 500, 50, 300};
        //-1 - don't repeat
        final int indexInPatternToRepeat = -1;
        vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
        System.out.println("vibrating");
    }

    public String getMagneticInfos() {
        String s = "heya";
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (geomagenticSensor != null) {
            // Success! There's a magnetometer.
            s = geomagenticSensor.getName();
        } else {
            // Failure! No magnetometer.
            s = "Sorry but there is no magnetometer on this device";
        }
        return s;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, geomagenticSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }


}
