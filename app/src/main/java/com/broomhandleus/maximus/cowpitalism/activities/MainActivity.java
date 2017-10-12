package com.broomhandleus.maximus.cowpitalism.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.TextView;

import com.broomhandleus.maximus.cowpitalism.R;
import com.broomhandleus.maximus.cowpitalism.types.Player;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {

    private String playerName = "Player 1";
    private Intent screenSwitch;
    private Button startButton;
    private Button rulesButton;
    private Button joinButton;
    private TextView nameTextView;
    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nameTextView = (TextView) findViewById(R.id.titleName);
        nameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog nameInput = new AlertDialog.Builder(MainActivity.this).create();
                final EditText input = new EditText(MainActivity.this);
                nameInput.setTitle("Player Creation");
                nameInput.setMessage("Please Choose a Nickname:");
                nameInput.setView(input);
                nameInput.setButton(AlertDialog.BUTTON_NEUTRAL, "Let's make some cows",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                playerName = input.getText().toString();
                                nameTextView.setText(playerName);
                                nameInput.dismiss();
                            }
                        });
                nameInput.show();
            }
        });

        startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent screenSwitch = new Intent(getApplication(), HostInGameActivity.class);
                screenSwitch.putExtra("PLAYER_NAME", playerName);
                startActivity(screenSwitch);
            }
        });

        joinButton = (Button) findViewById(R.id.joinButton);
        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent screenSwitch = new Intent(getApplication(), PlayerInGameActivity.class);
                screenSwitch.putExtra("PLAYER_NAME", playerName);
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
