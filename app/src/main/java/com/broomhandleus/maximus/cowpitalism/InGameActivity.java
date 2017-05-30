package com.broomhandleus.maximus.cowpitalism;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
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
                double addNum = Double.parseDouble(numberInput.getText().toString());
                player.cows = player.cows + addNum;
                cowCount.setText("Cows: " + player.cows);
                numberInput.setText("");
            }
        });

        Button horseButton = (Button) findViewById(R.id.horseButton);
        horseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double addNum = Double.parseDouble(numberInput.getText().toString());
                player.horses = player.horses + addNum;
                cowCount.setText("Horses: " + player.cows);
                numberInput.setText("");
            }
        });

        Button hayBaleButton = (Button) findViewById(R.id.hayBaleButton);
        hayBaleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double addNum = Double.parseDouble(numberInput.getText().toString());
                player.hayBales = player.hayBales + addNum;
                numberInput.setText("");
            }
        });

        playerName.setText(player.name);
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
