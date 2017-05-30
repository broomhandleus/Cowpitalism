package com.broomhandleus.maximus.cowpitalism;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

public class InGameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_game);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Cowpitalism");

        Player player = new Player("Beta_Tester");

        TextView playerName = (TextView) findViewById(R.id.titleName);
        playerName.setText(player.name);
    }

    private class Player {

        public String name;
        public int cows;
        public int milk;
        public int horses;
        public int money;
        public int hayBales;
        public int semis;
        public int tankers;
        public int barns;
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
