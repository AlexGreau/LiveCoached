package com.example.livecoached;

import android.Manifest;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.example.livecoached.Service.ClientTask;
import com.example.livecoached.Service.Decoder;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class MainActivity extends WearableActivity implements SensorEventListener, Decoder {

    private final String TAG = MainActivity.class.getSimpleName();
    private final static int REQUEST_CHECK_SETTINGS = 6;

    // communication
    private ClientTask myClientTask;

    // Fused Location
    private FusedLocationProviderClient fusedLocationProviderClient;
    private int locationRequestCode = 1000;
    private double wayLatitude = 0.0, wayLongitude = 0.0;

    // Location updates request
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private boolean locationUpdateRequested;

    private TextView mTextView;
    private SensorManager sensorManager;

    private Button startButton;
    private Button stopButton;

    private Sensor orientationSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkHardware();
        init();
        setAmbientEnabled();
    }

    // ~~~~~~~~~~~~~~~~~~~~~~ init functions ~~~~~~~~~~~~~~~~~~~~~~
    public void init() {
        setContentView(R.layout.activity_main);
        initSensors();
        initTestText();
        initButtons();
        initLocation();
    }

    private void initButtons() {
        initStartButton();
        initStopButton();
    }

    private void initStartButton() {
        startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startExp();
            }
        });
    }

    private void initStopButton() {
        stopButton = findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopExp();
            }
        });
    }

    private void initTestText() {
        mTextView = (TextView) findViewById(R.id.text);
        //  Sensor name="Cywee Magnetic field Sensor", vendor="CyWee Group Ltd.", version=2, type=2, maxRange=200.0, resolution=0.01, power=5.0, minDelay=10000}
        //  Sensor name="akm09911 Magnetometer", vendor="AKM", version=1, type=2, maxRange=4900.0, resolution=0.6, power=0.23, minDelay=10000}
    }

    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void initLocation() {
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
       // checkLocationPermission();
        initLocationSettings();
        initLocationRequest();
        initLocationCallback();
        retrieveLastLocation();
        locationUpdateRequested = false;
    }

    public void initLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(2 * 1000); // millis
    }

    public void initLocationSettings() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    public void initLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        actualizeLocationVariables(location);
                        sendActualPosition("Running");
                    }
                }
            }
        };
    }

    public void retrieveLastLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("Permission not granted");
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            actualizeLocationVariables(location);
                        }
                    }
                });
        if (fusedLocationProviderClient != null) {
            // removing location continuous updates else will get multiple locations updates
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~ checking functions ~~~~~~~~~~~~~~~~~~~~~~
    public void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    locationRequestCode);
        } else {
            // permission granted
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    System.out.println("Permission granted !");
                    if (location != null) {
                        actualizeLocationVariables(location);
                    } else {
                        System.out.println("last location is null");
                    }
                }
            });
        }
    }

    public void checkHardware() {
        boolean hasGPS = getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        if (!hasGPS) {
            System.out.println("This hardware does not have GPS");
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~ other functions ~~~~~~~~~~~~~~~~~~~~~~
    public void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        long[] vibrationPattern = {0, 500, 50, 300};
        //-1 - don't repeat
        final int indexInPatternToRepeat = -1;
        vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
        System.out.println("vibrating");
    }

    public void actualizeLocationVariables(Location loc) {
        wayLatitude = loc.getLatitude();
        wayLongitude = loc.getLongitude();
        mTextView.setText(String.format("%s -- %s", wayLatitude, wayLongitude));
        System.out.println("new location values : " + wayLatitude + ", " + wayLongitude);
    }

    public void startLocationUpdates() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("Permission not granted");
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    public void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void startExp() {
        if (!locationUpdateRequested) {
            System.out.println("Starting the experiment");
            startLocationUpdates();
            locationUpdateRequested = true;
            vibrate();
        } else {
            System.out.println("Already locationUpdateRequested");
        }
    }

    private void stopExp() {
        if (locationUpdateRequested) {
            System.out.println("stopping the experiment");
            stopLocationUpdates();
            locationUpdateRequested = false;
            sendActualPosition("Stop");
            // TODO : transit to end screen, then to starting activity
            vibrate();
        } else {
            System.out.println("No locationUpdateRequested already");
        }
    }

    private void sendActualPosition(String state) {
        String msg = state + ":" + wayLatitude + "-" + wayLongitude;
        myClientTask = new ClientTask(msg,this);
        myClientTask.execute();
    }

    @Override
    public void  decodeResponse(String rep) {
        System.out.println("Main Activity Decoder " + rep);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        mTextView.setText("location changed : " + wayLatitude + ", " + wayLongitude);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        System.out.println("Sensors' accuracy changed");
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        stopLocationUpdates();
    }
}
