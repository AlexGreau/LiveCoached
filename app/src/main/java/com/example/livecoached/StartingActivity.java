package com.example.livecoached;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.example.livecoached.Service.ClientTask;
import com.example.livecoached.Service.Decoder;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class StartingActivity extends WearableActivity implements Decoder {

    // UI components
    private TextView text;
    private Button firstOption;
    private ImageButton secondOption;

    // Client
    private ClientTask client;

    // Location
    private FusedLocationProviderClient fusedLocationProviderClient;
    private int locationRequestCode = 1000;
    private double wayLatitude = 0.0, wayLongitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starting);
        init();
    }

    private void init() {
        initText();
        initFirstOptionButton();
        initLocation();
    }

    private void initText() {
        text = findViewById(R.id.welcomeText);
        text.setText(R.string.welcome_text);
    }

    private void initFirstOptionButton() {
        firstOption = findViewById(R.id.firstOptionButton);
        firstOption.setText("Yes !");
        firstOption.setTextColor(Color.WHITE);
        firstOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                proceed();
            }
        });
    }

    private void proceed() {
        retrieveLastLocation();
        sendToServer();
    }

    private void initLocation() {
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        checkLocationPermission();
    }

    private void sendToServer() {
        client = new ClientTask("Ready:" + wayLatitude + "-" + wayLongitude, this);
        client.execute();
    }

    private void startTransitionActivity() {
        Intent intent = new Intent(StartingActivity.this, TransitionActivity.class);
        intent.putExtra("state", 1);
        startActivity(intent);
    }

    private void startMainActivity() {
        Intent intent = new Intent(StartingActivity.this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void decodeResponse(String rep) {
        if (rep.equals("Continue")) {
            startMainActivity();
            finish();
        } else {
            System.out.println("invalid response : " + rep);
        }
    }

    @Override
    public void errorMessage(String err) {
        Toast.makeText(getApplicationContext(), err, Toast.LENGTH_LONG).show();
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
    }

    private void actualizeLocationVariables(Location loc) {
        wayLatitude = loc.getLatitude();
        wayLongitude = loc.getLongitude();
    }

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
}
