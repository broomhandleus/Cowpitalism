package com.broomhandleus.maximus.cowpitalism.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.broomhandleus.maximus.cowpitalism.R;

public class MainActivity extends AppCompatActivity {

    private Button startButton;
    private Button rulesButton;
    private Button joinButton;
    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent screenSwitch = new Intent(getApplication(), HostInGameActivity.class);
                startActivity(screenSwitch);
            }
        });

        joinButton = (Button) findViewById(R.id.joinButton);
        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent screenSwitch = new Intent(getApplication(), PlayerInGameActivity.class);
                startActivity(screenSwitch);
            }
        });

        rulesButton = (Button) findViewById(R.id.rulesButton);
        rulesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent screenSwitch = new Intent(getApplication(), RulesActivity.class);
                startActivity(screenSwitch);
            }
        });
    }


}
