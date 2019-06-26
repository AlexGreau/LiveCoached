package com.example.livecoached;

import android.content.Context;
import android.hardware.Sensor;
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
        initConnection();
    }

    private void initButtons() {
        initVibrateButton();
        initSendingButton();
    }

    private void initTestText() {
        mTextView = (TextView) findViewById(R.id.text);
    }

    private void initSensors() {
        System.out.println("printing sensors");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        System.out.println(sensors);
    }

    public void initSendingButton(){
        ImageButton sendButton = (ImageButton) findViewById(R.id.button_send);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendSensors();
            }
        });
    }

    public void initVibrateButton(){
        Button vibrateButton = (Button) findViewById(R.id.button_vibrate);
        vibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrate();
            }
        });
    }

    public void initConnection(){
        System.out.println("initiating connection");
    }

    public void sendSensors(){
        System.out.println("sending sensors");
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
