package com.example.livecoached;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.example.livecoached.Service.ClientTask;
import com.example.livecoached.Service.Decoder;

public class TransitionActivity extends WearableActivity implements Decoder {

    private TextView textView;
    private int state;
    private String text, expectedResponse;
    private Class nextActivity;
    private ClientTask client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transition);
        init();
    }

    protected void init() {
        getState();
        setupVariables();
        initText();
    }

    protected void setupVariables() {
        // depending on the state, varies the delay, text and the next activity to launch
        switch (state) {
            case 0:
                // end of exp, wait for server orders to launch startingActivity
                text = "Currenty waiting for server's response";
                nextActivity = MainActivity.class;
                expectedResponse = "Continue";
                return;

            case 1:
                // waiting for server response before launching mainActivity
                text = "You finished the experiment, congrats !!";
                nextActivity = StartingActivity.class;
                expectedResponse = "Reset";
                sendToServer();
                return;

            case 2:

                return;

            default:
                text = "unexpected state";
                expectedResponse = "";
                nextActivity = MainActivity.class;
        }
    }

    protected void initText() {
        textView = findViewById(R.id.explanationText);
        textView.setText(text);
    }

    protected void getState() {
        state = getIntent().getIntExtra("state", 0);
    }

    protected void sendToServer() {
        client = new ClientTask("End", this);
        client.execute();
    }

    protected void launchNextActivity() {
        Intent intent = new Intent(TransitionActivity.this, nextActivity);
        startActivity(intent);
    }

    @Override
    public void decodeResponse(String rep) {
        System.out.println("Transition Decoder : " + rep);
        if (rep.equals(expectedResponse)) {
            launchNextActivity();
            finish();
        }
    }

    @Override
    public void errorMessage(String err) {
        Toast.makeText(getApplicationContext(), err, Toast.LENGTH_LONG).show();
    }

}
