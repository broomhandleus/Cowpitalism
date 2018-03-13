package com.broomhandleus.maximus.cowpitalism.activities;

import android.app.ActionBar;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.broomhandleus.maximus.cowpitalism.R;
import com.broomhandleus.maximus.cowpitalism.types.Player;

public class MainActivity extends AppCompatActivity {

    private String playerName;
    private Intent screenSwitch;
    private Button startButton;
    private Button rulesButton;
    private Button joinButton;
    public static final String TAG = "MainActivity";
    RelativeLayout.LayoutParams layoutparams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog nameInput = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogCustom).create();
                final EditText input = new EditText(MainActivity.this);
                input.setTextColor(Color.parseColor("#FF000000"));
                nameInput.setTitle("Player Creation");
                nameInput.setMessage("Please Choose a Nickname:");
                nameInput.setView(input);
                nameInput.setButton(AlertDialog.BUTTON_NEUTRAL, "Let's make some cows",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                playerName = input.getText().toString();
                                nameInput.dismiss();
                                Intent screenSwitch = new Intent(getApplication(), HostInGameActivity.class);
                                screenSwitch.putExtra("PLAYER_NAME", playerName);
                                startActivity(screenSwitch);
                            }
                        });
                nameInput.show();
                Button neutral = nameInput.getButton(AlertDialog.BUTTON_NEUTRAL);
                neutral.setTextColor(Color.parseColor("#FFA28532"));
            }
        });

        joinButton = (Button) findViewById(R.id.joinButton);
        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog nameInput = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogCustom).create();
                final EditText input = new EditText(MainActivity.this);
                input.setTextColor(Color.parseColor("#FF000000"));
                nameInput.setTitle("Player Creation");
                nameInput.setMessage("Please Choose a Nickname:");
                nameInput.setView(input);
                nameInput.setButton(AlertDialog.BUTTON_NEUTRAL, "Let's make some cows",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                playerName = input.getText().toString();
                                nameInput.dismiss();
                                Intent screenSwitch = new Intent(getApplication(), PlayerInGameActivity.class);
                                screenSwitch.putExtra("PLAYER_NAME", playerName);
                                startActivity(screenSwitch);
                            }
                        });
                nameInput.show();
                Button neutral = nameInput.getButton(AlertDialog.BUTTON_NEUTRAL);
                neutral.setTextColor(Color.parseColor("#FFA28532"));
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
