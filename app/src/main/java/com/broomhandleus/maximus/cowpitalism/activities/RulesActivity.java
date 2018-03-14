package com.broomhandleus.maximus.cowpitalism.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.broomhandleus.maximus.cowpitalism.R;

public class RulesActivity extends AppCompatActivity {

    public static final String TAG = "RulesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rules);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("The Rules of Play");
    }

}
