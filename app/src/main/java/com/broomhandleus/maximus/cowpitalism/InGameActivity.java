package com.broomhandleus.maximus.cowpitalism;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class InGameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_game);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private class Player {

        private int cows;
        private int horses;
        private int money;
        private int hayBales;
        private int semis;
        private int tankers;
        private int barns;
        private boolean chicken;

        public Player() {
            cows = 0;
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
