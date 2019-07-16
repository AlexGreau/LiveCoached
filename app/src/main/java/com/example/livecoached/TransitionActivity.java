package com.example.livecoached;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;

import com.example.livecoached.Service.ClientTask;
import com.example.livecoached.Service.Decoder;

public class TransitionActivity extends WearableActivity implements Decoder {

    private TextView textView;
    private int state;
    private String text, expectedResponse;
    private int delay;
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
                nextActivity = StartingActivity.class;
                expectedResponse = "Reset";
                initClient();
                return;

            case 1:
                // waiting for server response before launching mainActivity
                text = "You finished the experiment, congrats !!";
                nextActivity = MainActivity.class;
                expectedResponse = "Continue";
                initClient();
                return;

            case 2:

                return;

            default:
                text = "unexpected state";
                expectedResponse = "";
                delay = 10000;
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

    protected void timeBomb(int miliDelay) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // action to do after delay
                launchNextActivity();
                finish();
            }
        }, miliDelay);
    }

    protected void initClient() {
        client = new ClientTask(null, this);
        client.execute();
    }

    protected void launchNextActivity() {
        Intent intent = new Intent(TransitionActivity.this, nextActivity);
        startActivity(intent);
    }

    @Override
    public void decodeResponse(String rep) {
        if (rep.equals(expectedResponse)) {
            launchNextActivity();
            finish();
        }
    }
}
