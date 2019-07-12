package com.example.livecoached;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.livecoached.Service.ClientTask;

public class StartingActivity extends WearableActivity {

    // UI components
    private TextView text;
    private Button firstOption;
    private Button secondOption;

    // Client
    private ClientTask clientTask;

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
                startMainActivity();
            }
        });
    }

    private void initSecondOptionButton(){
        secondOption = findViewById(R.id.secondOptionButton);
        secondOption.setText(R.string.second_option_text);
        secondOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendToServer();
                startMainActivity();
            }
        });
    }

    private void sendToServer(){
        System.out.println("sending to server");
        clientTask = new ClientTask("Ready");
    }

    private void startMainActivity(){
        Intent intent = new Intent(StartingActivity.this, MainActivity.class);
        startActivity(intent);
    }

}
