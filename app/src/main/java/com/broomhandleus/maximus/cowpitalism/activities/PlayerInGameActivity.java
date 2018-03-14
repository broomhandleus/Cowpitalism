package com.broomhandleus.maximus.cowpitalism.activities;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.broomhandleus.maximus.cowpitalism.R;
import com.broomhandleus.maximus.cowpitalism.types.Player;
import com.wordpress.simpledevelopments.btcommlib.BTCommChild;
import com.wordpress.simpledevelopments.btcommlib.Callback;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;



public class PlayerInGameActivity extends AppCompatActivity {

    // Static Final Values
    public static final String TAG = "PlayerInGameActivity";
    public static final String SERVICE_NAME = "Cowpitalism";
    public static final UUID[] MY_UUIDS = {
            UUID.fromString("c12380c7-0d88-4250-83d1-fc835d3833d9"),
            UUID.fromString("cb8cd1c1-fc37-4395-838f-728d818b2485"),
            UUID.fromString("ebb1690b-5b07-450f-b915-4d41698b199d"),
            UUID.fromString("dc1d38dd-222d-4c9c-aa0c-a9f0cd1dfcb6"),
            UUID.fromString("8c864b60-b369-44fa-85ff-86111fd4ff33"),
            UUID.fromString("f7b45c10-7602-487c-9bba-5a2be3ddfff4"),
            UUID.fromString("e89f9548-492b-4bcd-824d-cc80d204f47b")
    };


    // View Elements
    private TextView playerName;
    private TextView cowCount;
    private TextView milkCount;
    private TextView moneyCount;
    private TextView horseCount;
    private TextView barnCount;
    private TextView tankerCount;
    private TextView semiCount;
    private TextView hayBaleCount;
    private Switch chickenSwitch;
    private EditText numberInput;

    // Local Variables
    private Player player;
    private int inputVar;
    private double gasPrice;
    private double moreMoney;
    private Chronometer gameTimer;

    // Navigation Drawer
    private String[] drawerOptions;
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;

    private BTCommChild btCommChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_in_game);
        getSupportActionBar().setTitle("Cowpitalism");

        drawerOptions = new String[1];
        drawerOptions[0] = "Leaderboard";

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        // Navigation drawer icon
        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                R.string.drawer_open,
                R.string.drawer_close) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                supportInvalidateOptionsMenu();
                getSupportActionBar().setTitle("Cowpitalism");
            }

            public void onDrawerOpened(View view) {
                super.onDrawerOpened(view);
                supportInvalidateOptionsMenu();
                getSupportActionBar().setTitle("Cowpitalism");
            }
        };

        drawerLayout.addDrawerListener(drawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Set adapter for the ListView
        drawerList.setAdapter(new ArrayAdapter<String>(PlayerInGameActivity.this, android.R.layout.simple_list_item_activated_1, drawerOptions));
        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    Log.d(TAG, "Leaderboard Button is clicked");
                    final AlertDialog leaderboard = new AlertDialog.Builder(PlayerInGameActivity.this, R.style.AlertDialogCustom).create();
                    leaderboard.setTitle("Leaderboard");
                    leaderboard.setButton(AlertDialog.BUTTON_NEUTRAL, "Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Nothing to be done
                            Log.d(TAG, "Closed leaderboard");
                        }
                    });
                    leaderboard.show();
                    Button neutral = leaderboard.getButton(AlertDialog.BUTTON_NEUTRAL);
                    neutral.setTextColor(Color.parseColor("#FFA28532"));
                }
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Log.d(TAG, "Yo, something went wrong with getting the player name.");
        } else {
            player = new Player(extras.getString("PLAYER_NAME"));
            Log.d(TAG, "Player name " + player.name + ", " + extras.getString("PLAYER_NAME"));
        }

        btCommChild = new BTCommChild(this, SERVICE_NAME, MY_UUIDS);

        Map<String, Callback> messageActions = new HashMap<>();
        messageActions.put("PING_CLIENT", new Callback() {
            @Override
            public void action(int childIndex, String argument) {
                Log.d(TAG,"I HAVE BEEN PINGED!!!");
//                AlertDialog.Builder builder = new AlertDialog.Builder(PlayerInGameActivity.this);
//                builder.setMessage("I have been pinged!");
//                builder.create().show();
                DisplayFragment displayFragment = new DisplayFragment();
                displayFragment.show(getSupportFragmentManager(),"displayFragment","I have been pinged!");
                Log.d(TAG, "Displaying ping window");
            }
        });

        messageActions.put("GRAVEYARD", new Callback() {
            @Override
            public void action(int childIndex, String argument) {
                Log.d(TAG, "Graveyard message received");
                player.cows = 0;
                cowCount.setText("Cows: 0");
            }
        });

        messageActions.put("BURGER_JOINT", new Callback() {
            @Override
            public void action(int childIndex, String argument) {
                Log.d(TAG, "Burger Joint message received");
                if (player.chickenShield == true) {
                    player.chickenShield = false;
                    chickenSwitch.toggle();
                } else {
                    player.cows = player.cows / 2;
                    cowCount.setText("Cows: " + player.cows);
                }
            }
        });

        btCommChild.addMessageActions(messageActions);
        btCommChild.enableBluetooth();

        // Game Timer
        gameTimer = (Chronometer) findViewById(R.id.chronometer);
        btCommChild.obtainParent(new Callback() {
            @Override
            public void action(int childIndex, String argument) {
                Log.d(TAG, "Timer start!");
                gameTimer.start();
                gameTimer.setBase(SystemClock.elapsedRealtime());
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
            }
        });



        // TextView Instantiations
        playerName = (TextView) findViewById(R.id.titleName);
        playerName.setText(player.name.toUpperCase());
        playerName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog nameInput = new AlertDialog.Builder(PlayerInGameActivity.this, R.style.AlertDialogCustom).create();
                final EditText input = new EditText(PlayerInGameActivity.this);
                input.setTextColor(Color.parseColor("#FF000000"));
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
                Button negative = nameInput.getButton(AlertDialog.BUTTON_NEGATIVE);
                negative.setTextColor(Color.parseColor("#FFA28532"));
                Button positive = nameInput.getButton(AlertDialog.BUTTON_POSITIVE);
                positive.setTextColor(Color.parseColor("#FFA28532"));
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

        // Number Input
        numberInput = (EditText) findViewById(R.id.numberInput);

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
                    final AlertDialog hayBaleInput = new AlertDialog.Builder(PlayerInGameActivity.this, R.style.AlertDialogCustom).create();
                    final EditText input = new EditText(PlayerInGameActivity.this);
                    input.setTextColor(Color.parseColor("#FF000000"));
                    hayBaleInput.setTitle("User input required");
                    hayBaleInput.setMessage("How many hay bales do you want to use?");
                    hayBaleInput.setView(input);
                    hayBaleInput.setButton(AlertDialog.BUTTON_NEUTRAL, "Done",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    if (input.getText().toString().equals("")) {
                                        inputVar = 0;
                                    } else {
                                        inputVar = Integer.parseInt(input.getText().toString());
                                        if (!(inputVar >= 0 && inputVar <= player.hayBales)) {
                                            inputVar = 0;
                                        }
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
                    Button neutral = hayBaleInput.getButton(AlertDialog.BUTTON_NEUTRAL);
                    neutral.setTextColor(Color.parseColor("#FFA28532"));

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
                // Send GRAVEYARD message to the host
                btCommChild.sendToHost("GRAVEYARD","");
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

        Switch chickenSwitch = (Switch) findViewById(R.id.chickenSwitch);
        chickenSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                player.chickenShield = !player.chickenShield;
            }
        });

        Button burgerButton = (Button) findViewById(R.id.burgerButton);
        burgerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Send BURGER_JOINT message to the host
                btCommChild.sendToHost("BURGER_JOINT", "");
            }
        });

        Button waterTowerButton = (Button) findViewById(R.id.waterTowerButton);
        waterTowerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog hayBaleInput = new AlertDialog.Builder(PlayerInGameActivity.this, R.style.AlertDialogCustom).create();
                final EditText input = new EditText(PlayerInGameActivity.this);
                input.setTextColor(Color.parseColor("#FF000000"));
                hayBaleInput.setTitle("User input required");
                hayBaleInput.setMessage("How many horses do you want to convert?");
                hayBaleInput.setView(input);
                hayBaleInput.setButton(DialogInterface.BUTTON_NEUTRAL, "Trade 'em!",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (input.getText().toString().equals("")) {
                                    inputVar = 0;
                                } else {
                                    inputVar = Integer.parseInt(input.getText().toString());
                                    if (!(inputVar >= 0 && inputVar <= player.horses)) {
                                        inputVar = 0;
                                    }
                                }
                                player.cows = player.cows + (10 * inputVar);
                                player.horses -= inputVar;
                                cowCount.setText("Cows: " + player.cows);
                                horseCount.setText("Horses: " + player.horses);
                            }
                        });
                hayBaleInput.show();
                Button neutral = hayBaleInput.getButton(AlertDialog.BUTTON_NEUTRAL);
                neutral.setTextColor(Color.parseColor("#FFA28532"));
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

    /**
     * Processes for post creation in the app
     * @param savedInstanceState
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    /**
     * reconfiguring when state changes
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * makes the hamburger icon open the drawer layout
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        btCommChild.onRequestPermissionsResult(requestCode,permissions, grantResults);
    }

    public static class DisplayFragment extends DialogFragment {
        String text = "";
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage(text);
            return builder.create();
        }

        public void show(FragmentManager fm, String tag, String text) {
            this.text = text;
            show(fm,tag);
        }
    }
}
