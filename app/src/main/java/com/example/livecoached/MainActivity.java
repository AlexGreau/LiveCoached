package com.example.livecoached;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
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
        Button vibrateButton = (Button) findViewById(R.id.button_vibrate);
        vibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrate();
            }
        });
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

    public void vibrate(){
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        long[] vibrationPattern = {0, 500, 50, 300};
        //-1 - don't repeat
        final int indexInPatternToRepeat = -1;
        vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
        System.out.println("vibrating");
    }
}
