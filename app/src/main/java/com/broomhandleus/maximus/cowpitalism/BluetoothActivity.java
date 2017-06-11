package com.broomhandleus.maximus.cowpitalism;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
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


public class BluetoothActivity extends AppCompatActivity {

    // View Elements
    private Button hostGameButton;
    private Button joinGameButton;
    private Button approvePlayersButton;
    private Button pingAllPlayersButton;

    // Final Stuff
    public static final String SERVICE_NAME = "Cowpitalism";
    public static final String TAG = "Cowpitalism";
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

    // Bluetooth Stuff
    private BluetoothAdapter mBluetoothAdapter;
    private volatile boolean discoverable;

    // Device-Related Lists
    private List<BluetoothDevice> potentialPlayers;
    private BluetoothDevice[] playersList;
    private ExecutorService[] executorsList;
    private List<BluetoothDevice> potentialHosts;
    private AcceptThread[] hostAcceptThreads;

    // Variables for the Players Only
    private BluetoothDevice hostDevice;
    private AcceptThread playerAcceptThread;
    private CustomArrayAdapter hostsAdapter;

    private boolean isHost;

    private Handler handler;

    // TODO: Maybe add an infinite progressbar spinner that says "Waiting for host approval once"
    // TODO:    upon sending a join message.

    // TODO: Approval of potentialPlayers into playersList and sending them each a joinResponse msg.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        // Initialize view elements
        hostGameButton = (Button) findViewById(R.id.hostGameButton);
        joinGameButton = (Button) findViewById(R.id.joinGameButton);
        approvePlayersButton = (Button) findViewById(R.id.approvePlayersButton);
        pingAllPlayersButton = (Button) findViewById(R.id.pingPlayersButton);
        handler = new Handler();

        // Initialize lists
        // TODO: Maybe change later to initialize these upon starting a game
        playersList = new BluetoothDevice[MAX_DEVICES];
        executorsList = new ExecutorService[MAX_DEVICES];
        for (int i = 0; i < MAX_DEVICES; i++) {
            executorsList[i] = Executors.newSingleThreadExecutor();
        }
        hostAcceptThreads = new AcceptThread[MAX_DEVICES];
        potentialPlayers = new ArrayList<>();

        hostGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isHost = true;
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

        joinGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isHost = false;
                hostDevice = null;
                // Create ArrayList to contain the devices found (potential game hosts)
                potentialHosts = new ArrayList<>();
                hostsAdapter = new CustomArrayAdapter(getApplicationContext(), potentialHosts);

                // Create Dialog that displays the constantly-updating list of devices found
                AlertDialog.Builder builder = new AlertDialog.Builder(BluetoothActivity.this);
                builder.setTitle("Select Game Host");
                ListView listView = new ListView(BluetoothActivity.this);
                listView.setAdapter(hostsAdapter);
                builder.setView(listView);
                final Dialog dialog = builder.create();
                dialog.show();

                // Setup what happens when we select our game host from the list
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        hostDevice = potentialHosts.get(position);
                        Log.d(TAG, "Game host will be: " + hostDevice);
                        dialog.dismiss();

                        /**
                         * Start temporary Background Thread that receives the first incoming message
                         * from the host. It will contain the channelUUID. At that point,
                         * this AcceptThread will cancel itself, and start a new permanent one with
                         * the correct channelUUID rather than the default one(0).
                         */
                        playerAcceptThread = new AcceptThread(0);
                        Log.v(TAG, "Starting default playerAcceptThread: " + playerAcceptThread);
                        playerAcceptThread.start();

                        BluetoothMessage joinMessage = new BluetoothMessage(BluetoothMessage.Type.JOIN_REQUEST, BluetoothMessage.JOIN_REQUEST_VALUE, "");
                        SendMessageRunnable sendMessageRunnable = new SendMessageRunnable(hostDevice, MY_UUIDS[0], joinMessage);
                        executorsList[0].submit(sendMessageRunnable);
                    }
                });

                // Make it so that we are notified when a new nearby BluetoothDevice is discovered
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                registerReceiver(discoverDevicesReceiver, filter);

                // Request permission to search for nearby devices
                Log.d(TAG, "Attempting to discover nearby devices....");
                ActivityCompat.requestPermissions(BluetoothActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
                // Begin searching for nearby
                boolean success = mBluetoothAdapter.startDiscovery();
                if (success)
                    Log.d(TAG, "Started DISCOVERING!!!");
                else
                    Log.e(TAG, "Failed to start Discovering!!!");

            }
        });

        approvePlayersButton.setOnClickListener(new View.OnClickListener() {
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


        pingAllPlayersButton.setOnClickListener(new View.OnClickListener() {
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

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(discoverableReceiver, filter);


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth non-existent!");
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    /**
     * Checks to see whether we got permission from the user to use location services.
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "PERMISSION GRANTED BY USER!!!");
            } else {
                Log.e(TAG, "PERMISSIONS DENIED BY USER!!!");
                System.exit(-1);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(discoverableReceiver);
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
//                else {
//                    discoverable = false;
//                }
            }
        }
    };

    /**
     * BroadcastReceiver that handles searching for nearby game hosts and adds them to the list.
     */
    private BroadcastReceiver discoverDevicesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "Device Found: " + device.getName() + ": " + device.getAddress());
                if (!potentialHosts.contains(device)) {
                    potentialHosts.add(device);
                    hostsAdapter.notifyDataSetChanged();
                }
            } else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                Log.d(TAG, "Finished Discovering");
                if (potentialHosts.size() != 0) {
                    Log.d(TAG, "Potential Hosts:");
                    Log.d(TAG, "--------------");
                    for (int i = 0; i < potentialHosts.size(); i++) {
                        Log.d(TAG, "Device-" + (i + 1) + ": " + potentialHosts.get(i).getName() + " -> " + potentialHosts.get(i).getAddress());
                    }
                    Log.d(TAG, "--------------");
                } else {
                    Log.e(TAG, "No potential hosts found!!!");
                }
                unregisterReceiver(discoverDevicesReceiver);
            } else {
                Log.e(TAG, "Unknown type of action received!");
            }
        }
    };

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
                    // If I am a player, and this communication is not from the host, then ignore
                    if (!isHost) {
                        if (hostDevice == null) {
                            /**
                             * If I am a player than hasn't selected a host yet, I shouldn't
                             * receive anything
                             */
                            return;
                        } else if (!hostDevice.equals(socket.getRemoteDevice())) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return;
                        }
                    } else {
                        /**
                         * TODO: Check if the device corresponds to the player that should be
                         * TODO:    communicating with me at this UUID.
                         * TODO:    e.g. socket.getRemoteDevice().equals(playerList[playerIdx])
                         */
                    }

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

                } else if (inMessage.type == BluetoothMessage.Type.JOIN_RESPONSE) {
                    Log.d(TAG, "I have been accepted to join game!. I am player: " + inMessage.value);
                    // Cancel playerAcceptThread on the default channel (0) and start on correct one
                    playerAcceptThread.cancel();
                    playerAcceptThread = new AcceptThread(inMessage.value);
                    Log.v(TAG, "Starting REAL playerAcceptThread: " + playerAcceptThread);
                    playerAcceptThread.start();
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
     * Helps us to display BluetoothDevices in a ListView of some kind.
     */
    public class CustomArrayAdapter extends ArrayAdapter<BluetoothDevice> {
        public CustomArrayAdapter(Context context, List<BluetoothDevice> devices) {
            super(context, 0, devices);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            BluetoothDevice device = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
            textView.setTextColor(Color.BLACK);
            if (device.getName() == null) {
                textView.setText(device.getAddress());
            } else {
                textView.setText(device.getName() + ": " + device.getAddress());
            }
            return convertView;
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
