package com.broomhandleus.maximus.cowpitalism;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

public class InGameActivity extends AppCompatActivity {

    public static final String TAG = "InGameActivity";
    String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_game);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Cowpitalism");


        final AlertDialog nameInput = new AlertDialog.Builder(this).create();
        final EditText input = new EditText(this);
        nameInput.setTitle("Player Creation");
        nameInput.setMessage("Please Choose a Nickname");
        nameInput.setView(input);
        nameInput.setButton(AlertDialog.BUTTON_NEUTRAL, "Let's make some cows",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        name = input.getText().toString();
                        Log.d(TAG, name);
                        nameInput.hide();
                    }
                });
        nameInput.show();

        final Player player = new Player(name);

        // Game Timer
        final Chronometer gameTimer = (Chronometer) findViewById(R.id.chronometer);

        // References to all TextViews
        TextView playerName = (TextView) findViewById(R.id.titleName);
        final TextView cowCount = (TextView) findViewById(R.id.cowCount);
        final TextView milkCount = (TextView) findViewById(R.id.milkCount);
        final TextView moneyCount = (TextView) findViewById(R.id.moneyCount);
        final TextView horseCount = (TextView) findViewById(R.id.horseCount);
        final TextView barnCount = (TextView) findViewById(R.id.barnCount);
        final TextView tankerCount = (TextView) findViewById(R.id.tankerCount);
        final TextView semiCount = (TextView) findViewById(R.id.semiCount);
        final TextView hayBaleCount = (TextView) findViewById(R.id.hayBaleCount);

        //Reference to EditText
        final EditText numberInput = (EditText) findViewById(R.id.numberInput);

        // References to all Buttons/Switches
        Button cowButton = (Button) findViewById(R.id.cowButton);
        cowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int addNum;
                if (!numberInput.getText().toString().isEmpty()) {
                    addNum = Integer.parseInt(numberInput.getText().toString());
                } else {
                    addNum = 0;
                }
                player.cows = player.cows + addNum;
                cowCount.setText("Cows: " + player.cows);
                numberInput.setText("");
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);

                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });

        Button horseButton = (Button) findViewById(R.id.horseButton);
        horseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int addNum;
                if (!numberInput.getText().toString().isEmpty()) {
                    addNum = Integer.parseInt(numberInput.getText().toString());
                } else {
                    addNum = 0;
                }
                player.horses = player.horses + addNum;
                horseCount.setText("Horses: " + player.horses);
                numberInput.setText("");
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);

                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });

        Button hayBaleButton = (Button) findViewById(R.id.hayBaleButton);
        hayBaleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int addNum;
                if (!numberInput.getText().toString().isEmpty()) {
                    addNum = Integer.parseInt(numberInput.getText().toString());
                } else {
                    addNum = 0;
                }
                player.hayBales = player.hayBales + addNum;
                hayBaleCount.setText("Hay Bales: " + player.hayBales);
                numberInput.setText("");
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);

                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });

        Button gasButton = (Button) findViewById(R.id.gasButton);
        gasButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double addNum;
                if (!numberInput.getText().toString().isEmpty()) {
                    addNum = Double.parseDouble(numberInput.getText().toString());
                } else {
                    addNum = 0.0;
                }
                double moreMoney = addNum * player.milk;
                player.money += moreMoney;
                moneyCount.setText("Money: $" + player.money);
                numberInput.setText("");
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);

                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });

        Button churchButton = (Button) findViewById(R.id.churchButton);
        churchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.cows *= 2;
                cowCount.setText("Cows: " + player.cows);
            }
        });

        Button graveyardButton = (Button) findViewById(R.id.graveyardButton);
        graveyardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player.barns != 0) {
                    player.cows = Math.min(player.cows, player.barns * 20);
                } else {
                    player.cows = 0;
                }
                cowCount.setText("Cows: " + player.cows);
            }
        });

        Button tankerButton = (Button) findViewById(R.id.tankerButton);
        tankerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tankerCount.setText("Tankers: " + ++player.tankers);
            }
        });

        Button semiButton = (Button) findViewById(R.id.semiButton);
        semiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                semiCount.setText("Semis: " + ++player.semis);
            }
        });

        Button barnButton = (Button) findViewById(R.id.barnButton);
        barnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                barnCount.setText("Barns: " + ++player.barns);
            }
        });

        final Switch chickenSwitch = (Switch) findViewById(R.id.chickenSwitch);

        Button burgerButton = (Button) findViewById(R.id.burgerButton);
        burgerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!chickenSwitch.isChecked()) {
                    player.cows = Math.max(player.cows / 2, player.barns * 20);
                    cowCount.setText("Cows: " + player.cows);
                } else {
                    chickenSwitch.toggle();
                }
            }
        });

        Button waterTowerButton = (Button) findViewById(R.id.waterTowerButton);
        waterTowerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.cows = player.cows + (10 * player.horses);
                player.horses = 0;
                cowCount.setText("Cows: " + player.cows);
                horseCount.setText("Horses: 0");
            }
        });

        playerName.setText(player.name);
        gameTimer.start();
    }

    // Private Class for keeping track of player data
    private class Player {

        public String name;
        public int cows;
        public int milk;
        public int horses;
        public double money;
        public int hayBales;
        public int semis;
        public int tankers;
        public int barns;
        private int kitties;

        public Player(String newName) {
            name = newName;
            cows = 0;
            milk = 0;
            horses = 0;
            money = 0;
            hayBales = 0;
            semis = 0;
            tankers = 0;
            barns = 0;

            // everyone should have a kittie... or 5
            kitties = (int) (5 * Math.random());
        }
    }
}
