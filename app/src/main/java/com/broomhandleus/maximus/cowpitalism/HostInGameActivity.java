package com.broomhandleus.maximus.cowpitalism;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
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

public class HostInGameActivity extends AppCompatActivity {

    public static final String TAG = "HostInGameActivity";
    public Player player;
    private int inputVar;
    private double gasPrice;
    private double moreMoney;

    // Declarations
    private TextView playerName;
    private TextView cowCount;
    private TextView milkCount;
    private TextView moneyCount;
    private TextView horseCount;
    private TextView barnCount;
    private TextView tankerCount;
    private TextView semiCount;
    private TextView hayBaleCount;

    private Chronometer gameTimer;

    private EditText numberInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_in_game);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Cowpitalism");

        // TextView Instantiations
        playerName = (TextView) findViewById(R.id.titleName);
        playerName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog nameInput = new AlertDialog.Builder(HostInGameActivity.this).create();
                final EditText input = new EditText(HostInGameActivity.this);
                nameInput.setTitle("Change Player Name");
                nameInput.setMessage("Please Choose a Nickname:");
                nameInput.setView(input);
                nameInput.setButton(DialogInterface.BUTTON_POSITIVE, "Change",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                player.name = input.getText().toString();
                                playerName.setText(player.name.toUpperCase());
                                nameInput.dismiss();
                            }
                        });
                nameInput.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                nameInput.dismiss();
                            }
                        });
                nameInput.show();
            }
        });
        cowCount = (TextView) findViewById(R.id.cowCount);
        milkCount = (TextView) findViewById(R.id.milkCount);
        moneyCount = (TextView) findViewById(R.id.moneyCount);
        horseCount = (TextView) findViewById(R.id.horseCount);
        barnCount = (TextView) findViewById(R.id.barnCount);
        tankerCount = (TextView) findViewById(R.id.tankerCount);
        semiCount = (TextView) findViewById(R.id.semiCount);
        hayBaleCount = (TextView) findViewById(R.id.hayBaleCount);

        // Game Timer
        gameTimer = (Chronometer) findViewById(R.id.chronometer);

        // Number Input
        numberInput = (EditText) findViewById(R.id.numberInput);

        final AlertDialog nameInput = new AlertDialog.Builder(this).create();
        final EditText input = new EditText(this);
        nameInput.setTitle("Player Creation");
        nameInput.setMessage("Please Choose a Nickname:");
        nameInput.setView(input);
        nameInput.setButton(AlertDialog.BUTTON_NEUTRAL, "Let's make some cows",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        player = new Player(input.getText().toString());
                        playerName.setText(player.name.toUpperCase());
                        Log.d(TAG, input.getText().toString());
                        gameTimer.start();
                        gameTimer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
                            @Override
                            public void onChronometerTick(Chronometer chronometer) {
                                if (gameTimer.getText().toString().substring(gameTimer.length() - 2).equals("00")) {
                                    if ((player.milk + player.cows) < ((player.cows * 25) + (player.tankers * 1000))) {
                                        player.milk += player.cows;
                                    } else {
                                        player.milk = (player.cows * 25) + (player.tankers * 1000);
                                    }
                                    milkCount.setText("Milk: " + player.milk + " gallons");
                                }
                            }
                        });
                        nameInput.dismiss();
                    }
                });
        nameInput.show();


        // References to all Buttons/Switches
        Button cowButton = (Button) findViewById(R.id.cowButton);
        cowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int addNum;
                if (!numberInput.getText().toString().isEmpty()) {
                    addNum = Integer.parseInt(numberInput.getText().toString());
                } else {
                    addNum = 1;
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
                    addNum = 1;
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
                    addNum = 1;
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
                if (!numberInput.getText().toString().isEmpty()) {
                    gasPrice = Double.parseDouble(numberInput.getText().toString());
                    final AlertDialog hayBaleInput = new AlertDialog.Builder(HostInGameActivity.this).create();
                    final EditText input = new EditText(HostInGameActivity.this);
                    hayBaleInput.setTitle("User input required");
                    hayBaleInput.setMessage("How many hay bales do you want to use?");
                    hayBaleInput.setView(input);
                    hayBaleInput.setButton(AlertDialog.BUTTON_NEUTRAL, "Done",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    inputVar = Integer.parseInt(input.getText().toString());
                                    if (!(inputVar >= 0 && inputVar <= player.hayBales)) {
                                        inputVar = 0;
                                    }
                                    moreMoney = gasPrice * player.milk * (1 + (0.01 * inputVar) + (0.03 * player.semis));
                                    player.hayBales -= inputVar;
                                    player.milk = 0;
                                    hayBaleCount.setText("Hay Bales: " + player.hayBales);
                                    player.money += moreMoney;
                                    moneyCount.setText("Money: $" + player.money);
                                    milkCount.setText("Milk: 0 gallons");
                                    hayBaleInput.dismiss();
                                }
                            });
                    hayBaleInput.show();
                } else {
                    gasPrice = 0.0;
                    moreMoney = gasPrice;
                }
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
                final AlertDialog hayBaleInput = new AlertDialog.Builder(HostInGameActivity.this).create();
                final EditText input = new EditText(HostInGameActivity.this);
                hayBaleInput.setTitle("User input required");
                hayBaleInput.setMessage("How many horses do you want to convert?");
                hayBaleInput.setView(input);
                hayBaleInput.setButton(DialogInterface.BUTTON_NEUTRAL, "Trade 'em!",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                inputVar = Integer.parseInt(input.getText().toString());
                                if (!(inputVar >= 0 && inputVar <= player.horses)) {
                                    inputVar = 0;
                                }
                                player.cows = player.cows + (10 * inputVar);
                                player.horses -= inputVar;
                                cowCount.setText("Cows: " + player.cows);
                                horseCount.setText("Horses: " + player.horses);
                            }
                        });
                hayBaleInput.show();
            }
        });

        Button purchaseCowButton = (Button) findViewById(R.id.purchaseCowButton);
        purchaseCowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player.money >= 500.0) {
                    player.cows++;
                    player.money -= 500.0;
                    cowCount.setText("Cows: " + player.cows);
                    moneyCount.setText("Money: $" + player.money);
                }
            }
        });

        Button purchaseHorseButton = (Button) findViewById(R.id.purchaseHorseButton);
        purchaseHorseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player.money >= 4500.0) {
                    player.horses++;
                    player.money -= 4500.0;
                    horseCount.setText("Horses: " + player.horses);
                    moneyCount.setText("Money: $" + player.money);
                }
            }
        });
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

            // everyone should have a kitty... or 5
            kitties = (int) (5 * Math.random());
        }
    }
}
