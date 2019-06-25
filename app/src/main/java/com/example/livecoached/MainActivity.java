package com.example.livecoached;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends WearableActivity {

    private TextView mTextView;
    private SensorManager sensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        setAmbientEnabled();
    }

    public void init(){
        setContentView(R.layout.activity_main);
        initSensors();
        initTestText();
        initButtons();
    }

    private void initButtons() {

    }

    private void initTestText() {
        mTextView = (TextView) findViewById(R.id.text);

        // Enables Always-on
    }

    private void initSensors() {
        System.out.println("printing sensors");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        System.out.println(sensors);
    }
}
