package com.broomhandleus.maximus.cowpitalism;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
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
                AlertDialog.Builder builder = new AlertDialog.Builder(getApplication());
                builder.setView(R.).setTitle(R.string.dialog_title);
                AlertDialog dialog = builder.Create ();
            }
        });

//        serverButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                AcceptThread acceptThread = new AcceptThread();
//                acceptThread.start();
//            }
//        });
//
//        clientButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                ConnectThread connectThread = new ConnectThread(maxsPhone);
//                connectThread.start();
//            }
//        });

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(discoverableReceiver, filter);
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(deviceFoundReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(finishedDiscoveringReceiver, filter);

//        discoverableButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.d(TAG, "Attempting to make discoverable for ten seconds....");
//                AcceptThread acceptThread = new AcceptThread();
//                acceptThread.start();
//                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 10);
//                startActivity(discoverableIntent);
//            }
//        });

//        discoveryButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.d(TAG, "Attempting to discover nearby devices....");
//                discoveryCounter = 0;
//                ActivityCompat.requestPermissions(BluetoothActivity.this,
//                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
//                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
//                boolean success = mBluetoothAdapter.startDiscovery();
//                if (success)
//                    Log.d(TAG, "Started DISCOVERING!!!");
//                else
//                    Log.e(TAG, "Failed to start Discovering!!!");
//            }
//        });

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth non-existent!");
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

//        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

//        Log.d(TAG, "Looking at devices");
//        if (pairedDevices.size() > 0) {
//            // There are paired devices. Get the name and address of each paired device.
//            for (BluetoothDevice device : pairedDevices) {
//                String deviceName = device.getName();
//                String deviceHardwareAddress = device.getAddress(); // MAC address
//                if (device.getAddress().equals("F8:CF:C5:DE:1B:72")) {
//                    Log.d(TAG, "GOT IT!!!");
//                    maxsPhone = device;
//                }
//
//                Log.d(TAG, deviceName + ": " + deviceHardwareAddress);
//            }
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    Log.d(TAG, "PERMISSION GRANTED BY USER!!!");
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.e(TAG, "PERMISSIONS DENIED BY USER!!!");
                    System.exit(-1);
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister the ACTION_FOUND receiver
        unregisterReceiver(discoverableReceiver);
        unregisterReceiver(deviceFoundReceiver);
        unregisterReceiver(finishedDiscoveringReceiver);
    }


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

    private BroadcastReceiver deviceFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getUuids() != null) {
                    Log.d(TAG, "Device Found: " + device.getName() + ": " + device.getAddress() + ", with " + device.getUuids()[0]);
                } else {
                    Log.d(TAG, "Device Found: " + device.getName() + ": " + device.getAddress() + ", with no UUIDS!!!");
                }
                if (!discoveredDevices.contains(device)) {
                    discoveredDevices.add(device);
                }
            }
        }
    };
    private BroadcastReceiver finishedDiscoveringReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
//                Log.d(TAG, "Finished Discovering: " + discoveryCounter);
                if (discoveryCounter++ < 0) {
                    mBluetoothAdapter.startDiscovery();
                } else {
                    if (discoveredDevices.size() != 0) {
                        Log.d(TAG, "Found devices:");
                        Log.d(TAG, "--------------");
                        serverCheckCounter = 0;
                        for (int i = 0; i < discoveredDevices.size(); i++) {
                            Log.d(TAG, "Device-" + (i + 1) + ": " + discoveredDevices.get(i).getName() + " -> " + discoveredDevices.get(i).getAddress());
                        }
                        Log.d(TAG, "--------------");


                        Log.d(TAG, "Spot0: " + serverCheckCounter + " " + discoveredDevices.get(serverCheckCounter).getName() + ", " + discoveredDevices.get(serverCheckCounter));
                        BluetoothDevice device = discoveredDevices.get(serverCheckCounter++);
                        if (!device.fetchUuidsWithSdp()) {
                            Log.e(TAG, "Failed to fetch uuids for: " + device.getName() + ": " + device.getAddress());
                        }
                    } else {
                        Log.e(TAG, "No devices found!!!");
                    }

                }
            }
        }
    };

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    Log.d(TAG, "Now discoverable for the next 60 seconds!");
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 10);
                    startActivity(discoverableIntent);
                    Log.d(TAG, "Now accepting join requests in the background!");
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
//                    manageMyConnectedSocket(socket);
                    try {
                        InputStream rawInputStream = socket.getInputStream();
                        ObjectInputStream messageInputStream = new ObjectInputStream(rawInputStream);
                        BluetoothMessage joinMessage = (BluetoothMessage) messageInputStream.readObject();
                        if (joinMessage.type == BluetoothMessage.Type.JOIN_REQUEST
                                && joinMessage.value == BluetoothMessage.JOIN_REQUEST_VALUE
                                && discoverable) {
                            BluetoothDevice device = socket.getRemoteDevice();
                            Log.d(TAG, "Adding Device: " + device.getName() + "to the game!");
                            playerDevices.add(device);
                            socket.close();
                            socket = null;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    byte[] toByteArray(int value) {
        return  ByteBuffer.allocate(4).putInt(value).array();
    }

    int fromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }


    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
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
            // the connection in a separate thread.
//            manageMyConnectedSocket(mmSocket);
            byte[] bytes = new byte[4];
            try {
                mmSocket.getInputStream().read(bytes);
                int num = fromByteArray(bytes);
                Log.d(TAG, "num: " + num);
            } catch (IOException e) {
                e.printStackTrace();
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
            JOIN_REQUEST
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
}
