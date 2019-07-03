package com.example.livecoached;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends WearableActivity implements SensorEventListener {

    private TextView mTextView;
    private SensorManager sensorManager;
    private Button vibrateButton;
    private ImageButton sendButton;
    private Button magneticButton;

    private Sensor geomagenticSensor;
    private boolean print;
    float geoX,geoY,geoZ;

    private final int PORT = 8080;
    private final String SERVER_IP = "192.168.43.239";


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
        print = true;
    }

    private void initButtons() {
        initVibrateButton();
        initSendingButton();
        initMagneticButton();
    }

    private void initTestText() {
        mTextView = (TextView) findViewById(R.id.text);
        //  Sensor name="Cywee Magnetic field Sensor", vendor="CyWee Group Ltd.", version=2, type=2, maxRange=200.0, resolution=0.01, power=5.0, minDelay=10000}
        //  Sensor name="akm09911 Magnetometer", vendor="AKM", version=1, type=2, maxRange=4900.0, resolution=0.6, power=0.23, minDelay=10000}
    }

    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        geomagenticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(this, geomagenticSensor, SensorManager.SENSOR_DELAY_FASTEST);
       System.out.println(geomagenticSensor.getResolution());
    }

    public void initSendingButton() {
        sendButton = (ImageButton) findViewById(R.id.button_send);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCommTest();
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

    public void initMagneticButton() {
        magneticButton = (Button) findViewById(R.id.button_magnetic);
        magneticButton.setText("Give Orientation");
        magneticButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                print = true;
                /*
                String text = getMagneticInfos();
                mTextView.setText(text);
                */
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
            print = true;
        } else {
            // Failure! No magnetometer.
            s = "Sorry but there is no magnetometer on this device";
        }
        return s;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        geoX = event.values[0];
        geoY = event.values[1];
        geoZ = event.values[2];
        if (print) {
            mTextView.setText("X : " + geoX +", Y : " + geoY + ", Z : " + geoZ);
            print = false;
        }
    }

    public void startCommTest(){
        MyClientTask myClientTask = new MyClientTask(SERVER_IP,
                PORT,"yoyoyo");
        myClientTask.execute();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        System.out.println("Geomagnetic sensors accuracy changed");
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


    public class MyClientTask extends AsyncTask<Void, Void, Void> {

        String dstAddress;
        int dstPort;
        String response = "";
        String msgToServer;

        MyClientTask(String addr, int port, String msgTo) {
            dstAddress = addr;
            dstPort = port;
            msgToServer = msgTo;
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            Socket socket = null;
            DataOutputStream dataOutputStream = null;
            DataInputStream dataInputStream = null;

            try {
                socket = new Socket(dstAddress, dstPort);
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());

                if (msgToServer != null) {
                    dataOutputStream.writeUTF(msgToServer);
                }

                response = dataInputStream.readUTF();

            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                response = "UnknownHostException: " + e.toString();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                response = "IOException: " + e.toString();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            System.out.println(response);
            super.onPostExecute(result);
        }
    }
}
