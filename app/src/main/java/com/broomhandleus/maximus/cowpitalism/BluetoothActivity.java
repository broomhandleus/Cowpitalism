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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class BluetoothActivity extends AppCompatActivity {

    Button hostGameButton;
    Button joinGameButton;

    public static final String NAME = "Cowpitalism";
    public static final String TAG = "BluetoothActivity";
    public static final UUID MY_UUID = UUID.fromString("c12380c7-0d88-4250-83d1-fc835d3833d9");
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 23;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice maxsPhone;
    TextView messageBox;

    private boolean discoverable;
    private int discoveryCounter;
    private int serverCheckCounter;
    private List<BluetoothDevice> discoveredDevices;
    private List<BluetoothDevice> confirmedPeers;
    private List<BluetoothDevice> playerDevices;

    ArrayList<BluetoothDevice> potentialHosts;
    CustomArrayAdapter hostsAdapter;
    private BluetoothServerSocket serverSocket;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        hostGameButton = (Button) findViewById(R.id.hostGameButton);
        joinGameButton = (Button) findViewById(R.id.joinGameButton);


        discoveredDevices = new ArrayList<>();
        confirmedPeers = new ArrayList<>();
        playerDevices = new ArrayList<>();

        hostGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AcceptThread acceptThread = new AcceptThread();
                acceptThread.start();
            }
        });

        joinGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
                        BluetoothDevice gameHost = potentialHosts.get(position);
                        Log.d(TAG, "Game host will be: " + gameHost);
                        dialog.dismiss();

                        ConnectThread connectThread = new ConnectThread(gameHost);
                        connectThread.start();
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
                // Begin searching for nearby devices
                boolean success = mBluetoothAdapter.startDiscovery();
                if (success)
                    Log.d(TAG, "Started DISCOVERING!!!");
                else
                    Log.e(TAG, "Failed to start Discovering!!!");

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
                Log.d(TAG, "PERMISSION GRANTED BY USER!!!");
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
                } else {
                    discoverable = false;
                    Log.d(TAG, "We are no longer discoverable!");
                }
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
     * AcceptThread is run on the game host.
     * It will receive all incoming messages from other devices
     * at first they will be messages to join the game,
     * then gameplay messages.
     */
    private class AcceptThread extends Thread {

        public AcceptThread() {
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
        }

        public void run() {

            Log.d(TAG, "Now discoverable for the next 45 seconds!");
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 45);
            startActivity(discoverableIntent);

            BluetoothSocket socket = null;
            while (true) {
                try {
                    Log.d(TAG, "Now accepting join requests in the background!");
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted
                    try {
                        InputStream rawInputStream = socket.getInputStream();
                        ObjectInputStream messageInputStream = new ObjectInputStream(rawInputStream);
                        Log.d(TAG, "Attempting to read message!");
                        BluetoothMessage joinMessage = (BluetoothMessage) messageInputStream.readObject();
                        Log.d(TAG, "Message READ!!!");

                        // If we are in discovery/join mode before the game, we only accept join messages
                        if (discoverable) {
                            if (joinMessage.type == BluetoothMessage.Type.JOIN_REQUEST
                                    && joinMessage.value == BluetoothMessage.JOIN_REQUEST_VALUE) {
                                BluetoothDevice device = socket.getRemoteDevice();
                                Log.d(TAG, "Adding Device: " + device.getName() + " to the game!");
                                playerDevices.add(device);
                                socket.close();
                                socket = null;
                            } else {
                                Log.e(TAG, "NOT a join message!");
                            }
                        } else {
                            // if we are in gameplay mode
                            if (joinMessage.type == BluetoothMessage.Type.JOIN_REQUEST) {
                                BluetoothDevice device = socket.getRemoteDevice();
                                Log.d(TAG, "Denying device trying to join in middle of game: " + device.getName() +"!");
                                socket.close();
                                socket = null;
                            } else {
                                Log.d(TAG, "Some other kind of message has arrived!");
                            }
                        }


                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    /**
     * ConnectThread allows a device to connect to the specified game host in the background
     * and request to join the game.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice hostDevice) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = hostDevice;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = hostDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // Send the Game host a message requesting to join.
            try {
                // Open up a channel to the game host
                OutputStream rawOutputStream = mmSocket.getOutputStream();
                ObjectOutputStream messageOutputStream = new ObjectOutputStream(rawOutputStream);

                // Send a message asking to join the game
                BluetoothMessage joinMessage = new BluetoothMessage(BluetoothMessage.Type.JOIN_REQUEST, BluetoothMessage.JOIN_REQUEST_VALUE, "");
                messageOutputStream.writeObject(joinMessage);

                // Close connection with the host
                mmSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
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
            JOIN_REQUEST,
            PING_CLIENT
        }

        public Type type;
        public int value;
        public String body;

        public BluetoothMessage(Type type, int value, String body) {
            this.type = type;
            this.value = value;
            this.body = body;
        }
    }
    public class CustomArrayAdapter extends ArrayAdapter<BluetoothDevice> {
        public CustomArrayAdapter(Context context, ArrayList<BluetoothDevice> devices) {
            super(context, 0, devices);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            BluetoothDevice device = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
            if (device.getName() == null) {
                textView.setText(device.getAddress());
            } else {
                textView.setText(device.getName() + ": " + device.getAddress());
            }
            return convertView;
        }
    }
}
