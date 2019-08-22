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
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
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

import static android.util.TypedValue.COMPLEX_UNIT_SP;

public class MainActivity extends WearableActivity implements SensorEventListener, Decoder {

    private final String TAG = MainActivity.class.getSimpleName();
    private final static int REQUEST_CHECK_SETTINGS = 6;

    // ui components
    private TextView orientationText;
    private TextView distanceText;
    private TextView hapticExplanation;
    private ImageView arrow;

    // sensors
    private SensorManager sensorManager;
    private Sensor orientationSensor;
    private double azimuth;

    // communication
    private ClientTask myClientTask;

    // Location updates
    private FusedLocationProviderClient fusedLocationProviderClient;
    private int locationRequestCode = 1000;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private boolean locationUpdateRequested;

    // path
    private ArrayList<Location> pathToFollow;
    private int indexNextCP = 0;
    private Location actualLocation;

    // feedback
    private Vibrator vibrator;
    private int interactionType;
    private boolean vibroIsForbidden;
    private int patternIndex = 100; // 3: strait, -1: left, 1: right

    private long[] pattern;
    private int[] amplitudes;
    private int indexInPatternToRepeat = 0;

    // flags
    private boolean flickHandled = false;
    private boolean isRunning = false;
    private boolean hasSucceeded = false;


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
        setupConstraints();
        initSensors();
        initUI();
        initLocation();
        initPath();
    }

    public void setupConstraints() {
        interactionType = getIntent().getIntExtra("interactionType", 0);
        vibroIsForbidden = false;
        if (interactionType == 1){
            vibroIsForbidden = true;
        }
    }

    private void initUI() {
        orientationText = findViewById(R.id.angle);
        distanceText = findViewById(R.id.distance);
        hapticExplanation = findViewById(R.id.hapticExplanation);
        arrow = findViewById(R.id.arrow);
    }

    private void initHapticExplanationText() {
        hapticExplanation.setVisibility(View.VISIBLE);
        String explanationText = " - : Straight \n - . . : Left \n . . - : Right \n ... : Checkpoint \n - - - : Finish line";
        hapticExplanation.setText(explanationText);
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

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
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
        double tolerance = 30; // with x degrees error allowed
        String message;
        patternIndex = 100; // triggers default case

        if (diffAngles - tolerance <= 0 && diffAngles + tolerance >= 0) {
            // on the good angle
            message = "go straight";
            patternIndex = 3;
        } else if (diffAngles < 0) {
            // left
            message = "go to the left";
            patternIndex = -1;
            if (diffAngles < -180) {
                message = "go to the right";
                patternIndex = 1;
            }
        } else if (diffAngles > 0) {
            // right
            message = "go to the right";
            patternIndex = 1;
            if (diffAngles < -180) {
                patternIndex = -1;
                message = "go to the left";
            }
        } else {
            message = " U-Turn !!";
        }
        // image
        if (interactionType != 0) {
            arrow.setVisibility(View.VISIBLE);
            Float angle = (float) diffAngles;
            arrow.setRotation(angle);
        }

        // text
        if (!orientationText.getText().equals(message)) {
            orientationText.setText(message);
            vibrate(patternIndex);
        }

        return;
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
                vibrate(0);
                orientationText.setText("reached a CP");
                if (indexNextCP == pathToFollow.size() - 1) {
                    // end exp
                    hapticExplanation.setVisibility(View.GONE);
                    distanceText.setVisibility(View.GONE);
                    findViewById(R.id.arrow).setVisibility(View.GONE);
                    orientationText.setVisibility(View.VISIBLE);
                    String message = "Congrats on reaching the end !";
                    hasSucceeded = true;
                    orientationText.setText(message);
                    stopExp();
                    flickHandled = true;
                } else {
                    // checkpoint passed, onto the next
                    indexNextCP++;
                }
            }
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~ other functions ~~~~~~~~~~~~~~~~~~~~~~
    public void vibrate(int pat) {
        if (!vibroIsForbidden) {
            setVibroValues(pat);
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, indexInPatternToRepeat));
        }
    }

    private void setVibroValues(int style) {
        long shortSig = 200;
        long longSig = 450;
        long delay = 300;
        long pause = 2000;

        int weakAmpli = 70;
        int midAmpli = 150;
        int highAmpli = 250;

        switch (style) {
            case 0:
                // CP
                pattern = new long[]{shortSig, 2 * delay / 3, shortSig, 2 * delay / 3, shortSig, pause};
                amplitudes = new int[]{weakAmpli, 0, midAmpli, 0, highAmpli, 0};
                indexInPatternToRepeat = -1;
                return;
            case -1:
                // left
                pattern = new long[]{longSig, delay, shortSig, delay, shortSig, pause};
                amplitudes = new int[]{midAmpli, 0, highAmpli, 0, highAmpli, 0};
                indexInPatternToRepeat = 0;
                return;
            case 1:
                // right
                pattern = new long[]{shortSig, delay, shortSig, delay, longSig, pause};
                amplitudes = new int[]{midAmpli, 0, highAmpli, 0, highAmpli, 0};
                indexInPatternToRepeat = 0;
                return;
            case 2:
                // end
                pattern = new long[]{longSig, delay, longSig, delay, longSig, pause};
                amplitudes = new int[]{midAmpli, 0, midAmpli, 0, midAmpli, 0};
                indexInPatternToRepeat = -1;
                return;
            case 3:
                // straight
                pattern = new long[]{longSig, pause};
                amplitudes = new int[]{midAmpli, 0};
                indexInPatternToRepeat = 0;
                return;
            default:
                //standard
                pattern = new long[]{shortSig, pause};
                amplitudes = new int[]{midAmpli, 0};
                indexInPatternToRepeat = -1;
                return;
        }
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
            orientationText.setVisibility(View.GONE);
            System.out.println("Starting the experiment");
            startLocationUpdates();
            sendActualPosition("Asking");
            locationUpdateRequested = true;
            vibrate(0);
            loadCorrectUI();
            hasSucceeded = false;
        } else {
            System.out.println("Already locationUpdateRequested");
        }
    }

    private void stopExp() {
        isRunning = false;
        if (locationUpdateRequested) {
            System.out.println("stopping the experiment");
            stopLocationUpdates();
            locationUpdateRequested = false;
            indexNextCP = 0;
            vibrate(2);
            vibrator.cancel();
            sendActualPosition("Stop");
            startStartingActivity();
        } else {
            System.out.println("No locationUpdateRequested already");
        }
    }

    private void sendActualPosition(String state) {
        String msg = state + ":" + actualLocation.getLatitude() + "-" + actualLocation.getLongitude();
        if (state.equals("Running")) {
            msg = msg + ":" + indexNextCP + ":" + patternIndex;
        } else if  (state.equals("Stop")){
            msg = msg + ":" + hasSucceeded;
        }
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
        Pattern patternRoute = Pattern.compile("route:[[0-9]+\\.[0-9]+\\-[0-9]+\\.[0-9]+;]+");
        Matcher matcherRoute = patternRoute.matcher(rep);
        if (rep.equals("reset")) {
            stopExp();
        } else if (rep.equals("stop")) {
            stopExp();
        } else if (!isRunning && matcherRoute.matches()) {
            isRunning = true;
            extractRoute(rep);
        } else {
            // System.out.println("unexpected reply : " + rep);
        }
    }

    private void loadCorrectUI() {
        if (interactionType == 0) {
            switchToHapticUI();
        } else {
            hapticExplanation.setVisibility(View.GONE);
        }
    }

    private void switchToHapticUI() {
        orientationText.setVisibility(View.GONE);
        distanceText.setVisibility(View.GONE);
        arrow.setVisibility(View.GONE);
        initHapticExplanationText();
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
    }

    public void handleWristGestureIN() {
        if (flickHandled) {
            hapticExplanation.setVisibility(View.GONE);
            distanceText.setVisibility(View.GONE);
            findViewById(R.id.arrow).setVisibility(View.GONE);
            orientationText.setVisibility(View.VISIBLE);
            String message = "Gesture recognized, please wait";
            orientationText.setText(message);
            stopExp();
            flickHandled = true;
        }
        return;
    }

    public void handleWristGestureOUT() {
        if (!flickHandled) {
            orientationText.setVisibility(View.VISIBLE);
            String message = "Gesture recognized, please wait";
            orientationText.setText(message);
            startExp();
            flickHandled = true;
        }
        return;
    }

    @Override
    public void errorMessage(String err) {
        System.err.println(err);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            azimuth = event.values[0];
            if (pathToFollow.size() > 1 && locationUpdateRequested) {
                checkAngle();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_NAVIGATE_NEXT:
                // flick wrist out
                handleWristGestureOUT();
                return true;
            case KeyEvent.KEYCODE_NAVIGATE_PREVIOUS:
                //flick wrist in
                handleWristGestureIN();
                return true;
        }
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
