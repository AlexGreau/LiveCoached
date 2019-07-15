package com.example.livecoached;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.livecoached.Service.ClientTask;
import com.example.livecoached.Service.Decoder;

public class StartingActivity extends WearableActivity implements Decoder {

    // UI components
    private TextView text;
    private Button firstOption;
    private Button secondOption;

    // Client
    private ClientTask client;

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
                sendToServer();
            }
        });
    }

    private void sendToServer() {
        System.out.println("sending Ready to server");
        client = new ClientTask("Ready", this);
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
}
