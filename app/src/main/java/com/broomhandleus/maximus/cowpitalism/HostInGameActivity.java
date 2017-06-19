package com.broomhandleus.maximus.cowpitalism;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HostInGameActivity extends AppCompatActivity {

    public static final String TAG = "HostInGameActivity";
    public Player player;
    private int inputVar;
    private double gasPrice;
    private double moreMoney;

    // Bluetooth declarations
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
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 23;
    public static final int MAX_DEVICES = 7;

    private BluetoothAdapter mBluetoothAdapter;
    private volatile boolean discoverable;

    // Device-Related Lists
    private List<BluetoothDevice> potentialPlayers;
    private BluetoothDevice[] playersList;
    private ExecutorService[] executorsList;
    private AcceptThread[] hostAcceptThreads;

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
    private Button startButton;

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

        // Initialize lists
        playersList = new BluetoothDevice[MAX_DEVICES];
        executorsList = new ExecutorService[MAX_DEVICES];
        for (int i = 0; i < MAX_DEVICES; i++) {
            executorsList[i] = Executors.newSingleThreadExecutor();
        }
        hostAcceptThreads = new AcceptThread[MAX_DEVICES];
        potentialPlayers = new ArrayList<>();


        // General Bluetooth accessibility
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth non-existent!");
        } else {
            Log.d(TAG, "We have an adapter!");
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }




        startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Making the host device discoverable
                Log.d(TAG, "Now discoverable for the next 30 seconds!");
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30);
                startActivity(discoverableIntent);

                /**
                 * Since this device is now going to be the host, we will start accepting incoming
                 * connections from potential players, we will start a background thread listening
                 * for those incoming connections on the first UUID's channel.
                 *
                 * Once the host is done letting people try to join, this AcceptThread will be
                 * canceled/closed somehow. Then each approved player, will receive a message
                 * containing their channelUUID (one of the seven available).
                 *
                 * e.g. MY_UUIDS[0] will no longer be receiving join requests from people,
                 * but rather it will be assigned to communicate with one specific player.
                 * TODO: Make sure the individual AcceptThreads for each device checks the mac
                 * TODO: address of incoming messages to make sure their from the only perm. device.
                 */
                hostAcceptThreads[0] = new AcceptThread(0);
                hostAcceptThreads[0].start();
            }
        });




        Button approveButton = (Button) findViewById(R.id.approveButton);
        approveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Approving players...." + potentialPlayers.size());
                Log.d(TAG, "-----------------------");
                if (potentialPlayers.size() > MAX_DEVICES) {
                    Log.e(TAG, "Too many people joined game!");
                    return;
                }

                // Stop initial accept thread
                hostAcceptThreads[0].cancel();
                hostAcceptThreads[0] = null;


                // Ensure we clear out the playersList from a previous game (maybe not necessary)
                for (int i = 0; i < MAX_DEVICES; i++) {
                    playersList[i] = null;
                }

                /**
                 * Put each player into the list, start a thread to accept incoming connections
                 * from them on their given channelUUID, then send them a message containing that channelUUID
                 * with which they will communicate with this host for the remainder of the game.
                 * Upon recv'ing this message, each player will stop listening on the default
                 * channel (0) and will only listen on this given channelUUID.
                 */
                for (int i = 0 ; i < potentialPlayers.size(); i++) {
                    playersList[i] = potentialPlayers.get(i);
                    Log.d(TAG, "Adding " + deviceName(playersList[i]) + " to position " + i);

                    hostAcceptThreads[i] = new AcceptThread(i);
                    hostAcceptThreads[i].start();

                    // Message contains "i", which is the playerIdx which will yield the UUID
                    BluetoothMessage joinResponseMessage = new BluetoothMessage(BluetoothMessage.Type.JOIN_RESPONSE,i,"");
                    SendMessageRunnable sendMessageRunnable = new SendMessageRunnable(playersList[i],MY_UUIDS[0],joinResponseMessage);
                    executorsList[i].submit(sendMessageRunnable);
                }
                Log.d(TAG, "-----------------------");
            }
        });

        Button pingButton = (Button) findViewById(R.id.pingButton);
        pingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Pinging the clients!!!");
                Log.d(TAG, "----------------------");
                // Iterate through all peer devices and fire off a thread for each one which sends it a ping message
                for (int i = 0; i < MAX_DEVICES; i++) {
                    // .....because we don't associate ourselves with those null BluetoothDevice's.
                    if (playersList[i] == null) {
                        continue;
                    }

                    if (playersList[i].getName() != null) {
                        Log.d(TAG, "Pinging " + playersList[i].getName());
                    } else {
                        Log.d(TAG, "Pinging " + playersList[i].getAddress());
                    }
                    BluetoothMessage pingMessage = new BluetoothMessage(BluetoothMessage.Type.PING_CLIENT, 0, "");
                    SendMessageRunnable sendMessageRunnable = new SendMessageRunnable(playersList[i],MY_UUIDS[i],pingMessage);
                    executorsList[i].submit(sendMessageRunnable);
                }
            }
        });

        // Making Discoverable
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(discoverableReceiver, filter);







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

    /**
     * BroadcastReceiver that sets our "discoverable" variable correctly every time the phone's
     * discoverable state changes. This will be used by the game host.
     */
    private BroadcastReceiver discoverableReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.SCAN_MODE_NONE)
                        == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    discoverable = true;
                    Log.d(TAG, "We are now discoverable!");
                    Thread discoverableThread = new Thread() {
                        public void run() {
                            try {
                                Thread.sleep(30000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            discoverable = false;
                            Log.d(TAG, "We are no longer discoverable!");
                        }
                    };
                    discoverableThread.start();
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(discoverableReceiver);
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

    /**
     * AcceptThread can is used in BOTH the Host and the players.
     * It will receive all incoming messages from the device
     * corresponding to the given playerIdx.
     */
    private class AcceptThread extends Thread {
        private int playerIdx;
        private BluetoothServerSocket serverSocket;
        private volatile boolean canceled;

        public AcceptThread(int playerIdx) {
            this.playerIdx = playerIdx;
            this.canceled = false;
            try {
                // Open an RFCOMM channel with the channelUUID corresponding to the player.
                Log.d(TAG, "adapter: " + mBluetoothAdapter);
                serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, MY_UUIDS[playerIdx]);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
        }
        public void run() {

            BluetoothSocket socket = null;
            while (true) {
                try {
                    Log.v(TAG, "Now waiting to receive a message!");
                    socket = serverSocket.accept();
                    Log.v(TAG, "Exited accept!");
                } catch (IOException e) {
                    if (!canceled) {
                        Log.e(TAG, "Socket's accept() method failed: " + this);
                        e.printStackTrace();
                    }
                    break;
                }

                if (socket != null) {

                    /**
                     * TODO: Check if the device corresponds to the player that should be
                     * TODO:    communicating with me at this UUID.
                     * TODO:    e.g. socket.getRemoteDevice().equals(playerList[playerIdx])
                     */

                    // A connection was accepted
                    Log.v(TAG, "Correctly Accepted Connection on " + MY_UUIDS[playerIdx]);
                    ReceiveMessageRunnable receiveMessageRunnable = new ReceiveMessageRunnable(playerIdx, socket);
                    executorsList[playerIdx].submit(receiveMessageRunnable);
                    // TODO: Is this next line REALLY necessary? Remove and test without
                    // TODO:    only once everything is in decent working order.
                    socket = null;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                Log.v(TAG, "Canceled accept thread on idx: " + playerIdx + "!");
                canceled = true;
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    /**
     *
     * A runnable that handles that receives a single msg, ACKs it, then acts upon the msg.
     *
     */
    private class ReceiveMessageRunnable implements Runnable {
        private int playerIdx;
        private BluetoothSocket socket;
        public ReceiveMessageRunnable(int playerIdx, BluetoothSocket socket) {
            this.playerIdx = playerIdx;
            this.socket = socket;
        }

        public void run() {
            try {
                Log.v(TAG, "connected: " + socket.isConnected());
                // Open communication streams to receive message and sent ACK
                ObjectInputStream messageInputStream = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream messageOutputStream = new ObjectOutputStream(socket.getOutputStream());
                BluetoothMessage inMessage = (BluetoothMessage) messageInputStream.readObject();

                // Sent ACK
                BluetoothMessage ackMessage = new BluetoothMessage(BluetoothMessage.Type.ACK,0,"ACK-" + inMessage.id);
                Log.v(TAG, "Sending ACK!!");
                messageOutputStream.writeObject(ackMessage);

                try {
                    messageInputStream.read();
                } catch (Exception ex) {
                    Log.v(TAG, "ACK should've been recv'd....remote seems closed...closing");
                }

                socket.close();

                //
                /**
                 * Act accordingly depending on the type of message just received.
                 * All the logic for different kinds of messages goes here.
                 */
                if (inMessage.type == BluetoothMessage.Type.JOIN_REQUEST
                        && inMessage.value == BluetoothMessage.JOIN_REQUEST_VALUE) {
                    if (discoverable) {
                        Log.v(TAG, "Recv'd Join request from: " + deviceName(socket.getRemoteDevice()));
                        if (!potentialPlayers.contains(socket.getRemoteDevice())) {
                            potentialPlayers.add(socket.getRemoteDevice());
                        }
                        Log.d(TAG, deviceName(socket.getRemoteDevice()) + " trying to join!");
                    } else {
                        Log.d(TAG, deviceName(socket.getRemoteDevice()) + " tried to join too late!");

                    }
                } else if (inMessage.type == BluetoothMessage.Type.PING_CLIENT){
                    Log.d(TAG,"I HAVE BEEN PINGED!!!");
                } else {
                    Log.e(TAG, "Some other kind of message has arrived!");
                }

            } catch (StreamCorruptedException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * SendMessageRunnable sends, a message, and waits for an ACK.
     */
    private class SendMessageRunnable implements Runnable {
        private BluetoothDevice device;
        private UUID channelUUID;
        private BluetoothMessage message;
        // Not set until run()
        private BluetoothSocket socket;

        public SendMessageRunnable(BluetoothDevice device, UUID channelUUID, BluetoothMessage message) {
            this.device = device;
            this.channelUUID = channelUUID;
            this.message = message;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();


            try {
                // Open up a RFCOMM channel using the provided UUID
                socket = device.createRfcommSocketToServiceRecord(channelUUID);
                socket.connect();
                Log.v(TAG, "Correctly Connected on " + channelUUID);

                // Open bi-directional communication streams on the channel
                ObjectOutputStream messageOutputStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream messageInputStream = new ObjectInputStream(socket.getInputStream());

                // Actually send the message
                messageOutputStream.writeObject(message);
                messageOutputStream.flush();
                Log.v(TAG, "Correctly Sent a message");

                // Attempt to receive ACK message
                // TODO: Has the potential to block forever if ack never arrives
                BluetoothMessage potentialAck = (BluetoothMessage) messageInputStream.readObject();
                if (potentialAck.type == BluetoothMessage.Type.ACK
                        && potentialAck.body.equals("ACK-" + message.id)) {
                    Log.v(TAG, "Received CORRECT ACK");
                    messageOutputStream.close();
                    messageInputStream.close();
                    socket.close();
                } else {
                    Log.e(TAG, "Incorrect ACK!!!");
                    Log.e(TAG, "Seeking: ACK-" + message.id + ", Found: " + potentialAck.body);
                    messageOutputStream.close();
                    messageInputStream.close();
                    socket.close();
                }
            } catch (IOException e) {
                try {
                    if (socket != null) {
                        socket.close();
                    }

                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                e.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    /**
     * Class defining a single message sent over a bluetooth connection.
     *
     * type - The type of the message.
     * value - A number being sent in the message.
     * body - A string beng sent in the message.
     *
     * Upon connecting to the game-hosting device, the first thing a player
     *  should do is send a message with type=Type.JOIN_REQUEST and value=JOIN_REQUEST_VALUE.
     *  This will allow the game-host to verify that the player is in fact running Cowpitalism
     *
     */
    private static class BluetoothMessage implements Serializable {
        public static final int JOIN_REQUEST_VALUE = 12345;
        private enum Type {
            ACK,
            PING_CLIENT,
            JOIN_REQUEST,
            JOIN_RESPONSE
        }

        public Type type;
        public int value;
        public String body;
        public UUID id;

        public BluetoothMessage(Type type, int value, String body) {
            this.type = type;
            this.value = value;
            this.body = body;
            this.id = UUID.randomUUID();
        }
    }

    /**
     * A helper method that helps us print out the best name for a device.
     * @param device the BluetoothDevice that is being referenced
     * @return the device's Name if it has one, otherwise its mac address.
     */
    private static String deviceName(BluetoothDevice device) {
        if (device.getName() == null) {
            return device.getAddress();
        } else {
            return device.getName();
        }
    }
}
