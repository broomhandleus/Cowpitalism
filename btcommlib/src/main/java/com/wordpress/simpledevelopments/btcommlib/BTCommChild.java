package com.wordpress.simpledevelopments.btcommlib;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by connor on 10/8/17.
 */

public class BTCommChild implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "BTCommChild";

    public static final int MAX_DEVICES = 7;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 23;

    // Bluetooth-Related
    private BluetoothAdapter mBluetoothAdapter;
    private List<BluetoothDevice> potentialHosts;
    private BluetoothDevice hostDevice;
    private AcceptThread playerAcceptThread;
    private CustomArrayAdapter hostsAdapter;
    private ExecutorService executor;

    private AppCompatActivity contextActivity;
    private UUID[] uuidList;
    private String serviceName;
    private final Map<String, Callback> messageActions;

    Object waitObject;
    Callback obtainParentCallback;


    public BTCommChild(AppCompatActivity contextActivity, String serviceName, UUID[] uuidList) {
        // General Bluetooth accessibility
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth non-existent!");
        }

        this.contextActivity = contextActivity;
        this.serviceName = serviceName;
        this.uuidList = new UUID[MAX_DEVICES];
        if (uuidList.length != MAX_DEVICES) {
            Log.e(TAG, "Could not instantiate BTCommParent! uuidList must contain 7 entries!");
            System.exit(-1);
        }
        for (int i = 0; i < MAX_DEVICES; i++) {
            this.uuidList[i] = uuidList[i];
        }
        messageActions = new HashMap<>();

        executor = Executors.newSingleThreadExecutor();

        // Joining the game
        hostDevice = null;
        // Create ArrayList to contain the devices found (potential game hosts)
        potentialHosts = new ArrayList<>();
        hostsAdapter = new CustomArrayAdapter(contextActivity, potentialHosts);

        waitObject = new Object();
    }

    public void enableBluetooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            BluetoothActivationFragment dialogFragment = new BluetoothActivationFragment();
            dialogFragment.show(contextActivity.getSupportFragmentManager(),"bluetoothActivateFragment");
        }
    }

    private boolean checkBTEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    public void obtainParent(Callback callback) {
        if (!checkBTEnabled()) {
            Log.e(TAG, "Bluetooth not enabled!");
            return;
        }

        obtainParentCallback = callback;

        // Create Dialog that displays the constantly-updating list of devices found
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Select Game Host");
        ListView listView = new ListView(contextActivity);
        listView.setAdapter(hostsAdapter);
        builder.setView(listView);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                contextActivity.finish();
            }
        });
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

                BluetoothMessage joinMessage = new BluetoothMessage(BluetoothMessage.Type.INTERNAL_USE,"JOIN_REQUEST", BluetoothMessage.JOIN_REQUEST_CONTENT);
                SendMessageRunnable sendMessageRunnable = new SendMessageRunnable(hostDevice, uuidList[0], joinMessage);
                executor.submit(sendMessageRunnable);

                obtainParentCallback.action(0,"");
            }
        });

        // Make it so that we are notified when a new nearby BluetoothDevice is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        contextActivity.registerReceiver(discoverDevicesReceiver, filter);

        // Request permission to search for nearby devices
        Log.d(TAG, "Attempting to discover nearby devices....");
        ActivityCompat.requestPermissions(contextActivity,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        // Begin searching for nearby
        boolean success = mBluetoothAdapter.startDiscovery();
        if (success)
            Log.d(TAG, "Started DISCOVERING!!!");
        else
            Log.e(TAG, "Failed to start Discovering!!!");
    }


    /**
     * Sending a BluetoothMessage to the host
     * @param type - user-defined type of the message
     * @param content - content/body of the message
     */
    public void sendToHost(String type, String content) {
        BluetoothMessage actionMessage = new BluetoothMessage(BluetoothMessage.Type.CLIENT_USE,type, content);
        SendMessageRunnable sendMessageRunnable = new SendMessageRunnable(hostDevice, uuidList[0], actionMessage);
        executor.submit(sendMessageRunnable);
    }

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
                contextActivity.unregisterReceiver(discoverDevicesReceiver);
            } else {
                Log.e(TAG, "Unknown type of action received!");
            }
        }
    };

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
                serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(serviceName, uuidList[playerIdx]);
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
                    // If this communication is not from the host, then ignore
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

                    // A connection was accepted
                    Log.v(TAG, "Correctly Accepted Connection on " + uuidList[playerIdx]);
                    ReceiveMessageRunnable receiveMessageRunnable = new ReceiveMessageRunnable(playerIdx, socket);
                    executor.submit(receiveMessageRunnable);
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
                BluetoothMessage ackMessage = new BluetoothMessage(BluetoothMessage.Type.INTERNAL_USE,"ACK",inMessage.id.toString());
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
                if (inMessage.type == BluetoothMessage.Type.INTERNAL_USE) {
                    if (inMessage.contentType.equals("JOIN_RESPONSE")) {
                        Log.d(TAG, "I have been accepted to join game!. I am player: " + inMessage.content);
                        // Cancel playerAcceptThread on the default channel (0) and start on correct one
                        playerAcceptThread.cancel();
                        playerAcceptThread = new AcceptThread(Integer.parseInt(inMessage.content));
                        Log.v(TAG, "Starting REAL playerAcceptThread: " + playerAcceptThread);
                        playerAcceptThread.start();
                    }
                } else if (inMessage.type == BluetoothMessage.Type.CLIENT_USE) {
                    messageActions.get(inMessage.contentType).action(playerIdx,inMessage.content);
                }

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
     * Adds the provided message actions to this parent.
     * @param messageActions
     */
    public void addMessageActions(Map<String, Callback> messageActions) {
        this.messageActions.putAll(messageActions);
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
                if (potentialAck.type == BluetoothMessage.Type.INTERNAL_USE
                        && potentialAck.contentType.equals("ACK")
                        && potentialAck.content.equals(message.id.toString())) {
                    Log.v(TAG, "Received CORRECT ACK");
                    messageOutputStream.close();
                    messageInputStream.close();
                    socket.close();
                } else {
                    Log.e(TAG, "Incorrect ACK!!!");
                    Log.e(TAG, "Seeking: ACK-" + message.id + ", Found: " + potentialAck.content);
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
