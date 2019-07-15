package com.example.livecoached;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.livecoached.Service.ClientTask;
import com.example.livecoached.Service.Decoder;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class StartingActivity extends WearableActivity implements Decoder {

    // UI components
    private TextView text;
    private Button firstOption;
    private Button secondOption;

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
        initSecondOptionButton();
        initLocation();
    }

    private void initText() {
        text = findViewById(R.id.welcomeText);
        text.setText(R.string.welcome_text);
    }

    private void initFirstOptionButton() {
        firstOption = findViewById(R.id.firstOptionButton);
        firstOption.setText(R.string.first_option_text);
        firstOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retrieveLastLocation();
                sendToServer();
            }
        });
    }

    private void initSecondOptionButton() {
        secondOption = findViewById(R.id.secondOptionButton);
        secondOption.setText(R.string.second_option_text);
        secondOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retrieveLastLocation();
                sendToServer();
            }
        });
    }

    private void initLocation(){
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void sendToServer() {
        System.out.println("sending Ready to server");
        client = new ClientTask("Ready:" + wayLatitude + "-" + wayLongitude, this);
        client.execute();
    }

    private void startMainActivity() {
        Intent intent = new Intent(StartingActivity.this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void decodeResponse(String rep) {
        System.out.println("Starting Activity Decoder : " + rep);
        if (rep.equals("Continue")) {
            startMainActivity();
            finish();
        } else {
            System.out.println("invalid response : " + rep);
        }
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
                        System.out.println("got last location");
                        if (location != null) {
                            actualizeLocationVariables(location);
                        }
                    }
                });
    }

    private void actualizeLocationVariables( Location loc){
        wayLatitude = loc.getLatitude();
        wayLongitude = loc.getLongitude();
    }
}
