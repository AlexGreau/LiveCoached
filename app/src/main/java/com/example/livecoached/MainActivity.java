package com.example.livecoached;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends WearableActivity implements SensorEventListener, Decoder {

    private final String TAG = MainActivity.class.getSimpleName();
    private final static int REQUEST_CHECK_SETTINGS = 6;

    // communication
    private ClientTask myClientTask;

    // Fused Location
    private FusedLocationProviderClient fusedLocationProviderClient;
    private int locationRequestCode = 1000;

    // Location updates request
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private boolean locationUpdateRequested;

    private TextView orientationText;
    private TextView distanceText;
    private SensorManager sensorManager;

    private Sensor orientationSensor;
    private double azimuth;

    private ArrayList<Location> pathToFollow;
    private int indexNextCP = 0;
    private Location actualLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkHardware();
        init();
        setAmbientEnabled();
        System.out.println(TAG);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~ init functions ~~~~~~~~~~~~~~~~~~~~~~
    public void init() {
        setContentView(R.layout.activity_main);
        initSensors();
        initText();
        initLocation();
        initPath();
    }

    private void initText() {
        orientationText = findViewById(R.id.angle);
        distanceText = findViewById(R.id.distance);
    }

    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_FASTEST);


        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void initLocation() {
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        initLocationSettings();
        initLocationRequest();
        initLocationCallback();
        retrieveLastLocation();
        locationUpdateRequested = false;
    }

    public void initLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000); // millis
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
                        checkDistance();
                    } else {
                        System.out.println("location in callback is null");
                    }
                }
            }
        };
    }

    public void initPath() {
        pathToFollow = new ArrayList<>();
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

    public void checkAngle() {
        double idealAngle = actualLocation.bearingTo(pathToFollow.get(pathToFollow.size() - 1));
        if (idealAngle <= 0) {
            idealAngle += 360;
        }
        // compare ideal angle to actual angle
        double diffAngles = idealAngle - azimuth;
        Log.d(TAG,"angle ideal : " + idealAngle + "; azimuth :"  + azimuth + "; difference : " + diffAngles);
        double tolerance = 10; // with x degrees error allowed
        String message;

        if (diffAngles < -180){

        }
        if (diffAngles - tolerance <= 0 && diffAngles + tolerance >= 0) {
            // on the good angle
            message = "go straight";
        } else if (diffAngles < 0) {
            // left
            message = "go to the left";
            if (diffAngles < -180){
                message = "go to the right";
            }
        } else if (diffAngles > 0 ) {
            // right
            message = "go to the right";
            if (diffAngles < -180){
                message = "go to the left";
            }
        } else {
            message = " U-Turn !!";
        }
        // image
        ImageView arrow = findViewById(R.id.arrow);
        arrow.setVisibility(View.VISIBLE);
        Float angle = (float) diffAngles;
        arrow.setRotation(angle);

        // text
        if (!orientationText.getText().equals(message)) {
            Log.d(TAG, message);
            orientationText.setText(message);
            vibrate();
        }
    }

    public void checkDistance() {
        double tolerance = 2; // error margin allowed
        if (indexNextCP == 0) {
            System.out.println("index CP = 0");
        } else {
            double mesuredDistance = actualLocation.distanceTo(pathToFollow.get(indexNextCP));
            DecimalFormat df = new DecimalFormat("#.#");
            String msg = df.format(mesuredDistance) + " m";
            distanceText.setText(msg);
            if (mesuredDistance <= tolerance) {
                // target reached
                if (indexNextCP == pathToFollow.size() - 1) {
                    // last critical point
                    // notify arrival
                    // let user decide to end exp
                } else {
                    // checkpoint passed, onto the next
                    indexNextCP++;
                }
            }
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~ other functions ~~~~~~~~~~~~~~~~~~~~~~
    public void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        long[] vibrationPattern = {0, 500, 50, 300};
        //-1 - don't repeat
        final int indexInPatternToRepeat = -1;
        vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
    }

    public void actualizeLocationVariables(Location loc) {
        this.actualLocation = loc;
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
            sendActualPosition("Asking");
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
            indexNextCP = 0;
            vibrate();
            sendActualPosition("Stop");
            startStartingActivity();
        } else {
            System.out.println("No locationUpdateRequested already");
        }
    }

    private void sendActualPosition(String state) {
        String msg = state + ":" + actualLocation.getLatitude() + "-" + actualLocation.getLongitude();
        myClientTask = new ClientTask(msg, this);
        myClientTask.execute();
    }

    private void startStartingActivity() {
        Intent intent = new Intent(MainActivity.this, StartingActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void decodeResponse(String rep) {
        // System.out.println("Main Activity Decoder " + rep);
        Pattern p = Pattern.compile("route:[[0-9]+\\.[0-9]+\\-[0-9]+\\.[0-9]+;]+");
        Matcher m = p.matcher(rep);
        // if orders received from server act accordingly
        if (rep.equals("reset")) {
            startStartingActivity();
        } else if (rep.equals("stop")) {
            startStartingActivity();
        } else if (m.matches()) {
            // change layout
            extractRoute(rep);
            // start the feedback
        } else {
            // System.out.println("unexpected reply : " + rep);
        }
    }

    private void extractRoute(String s) {
        String[] mainParts = s.split(":");
        String[] infoParts = mainParts[1].split(";");
        pathToFollow.clear();
        indexNextCP++;
        for (String info : infoParts) {
            Location loc = new Location(LocationManager.GPS_PROVIDER);
            String latitude = info.split("-")[0];
            String longitude = info.split("-")[1];
            loc.setLatitude(Double.parseDouble(latitude));
            loc.setLongitude(Double.parseDouble(longitude));
            pathToFollow.add(loc);
        }

        if (actualLocation != null) {
            // Log.d(TAG, "bearing to arrival place : " + actualLocation.bearingTo(pathToFollow.get(pathToFollow.size() - 1)) + ", Azimuth : " + azimuth);
            checkAngle();
        }
    }

    public boolean handleWristGestureIN() {
        stopExp();
        return true;
    }

    public boolean handleWristGestureOUT() {
        startExp();
        return true;
    }

    @Override
    public void errorMessage(String err) {
        System.err.println(err);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            azimuth = event.values[0];
            if (pathToFollow.size() > 1) {
                checkAngle();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_NAVIGATE_NEXT:
                // flick wrist out
                return handleWristGestureOUT();
            case KeyEvent.KEYCODE_NAVIGATE_PREVIOUS:
                //flick wrist in
                return handleWristGestureIN();
        }
        // If you did not handle it, let it be handled by the next possible element as deemed by the Activity.
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // System.out.println("Sensors' accuracy changed");
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        stopLocationUpdates();
    }
}
