package com.broomhandleus.maximus.cowpitalism;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.TextView;

import org.w3c.dom.Text;

public class InGameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_game);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Cowpitalism");

        final Player player = new Player("Beta_Tester");

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
                double addNum;
                if (!numberInput.getText().toString().isEmpty()) {
                    addNum = Double.parseDouble(numberInput.getText().toString());
                } else {
                    addNum = 0.0;
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
                double addNum;
                if (!numberInput.getText().toString().isEmpty()) {
                    addNum = Double.parseDouble(numberInput.getText().toString());
                } else {
                    addNum = 0.0;
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
                double addNum;
                if (!numberInput.getText().toString().isEmpty()) {
                    addNum = Double.parseDouble(numberInput.getText().toString());
                } else {
                    addNum = 0.0;
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

        playerName.setText(player.name);
        gameTimer.start();
    }

    // Private Class for keeping track of player data
    private class Player {

        public String name;
        public double cows;
        public double milk;
        public double horses;
        public double money;
        public double hayBales;
        public double semis;
        public double tankers;
        public double barns;
        public boolean chicken;

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
            chicken = false;
        }
    }
}
