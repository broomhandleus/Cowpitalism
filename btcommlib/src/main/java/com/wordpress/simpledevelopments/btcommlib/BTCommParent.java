package com.wordpress.simpledevelopments.btcommlib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
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

public class BTCommParent {
    private static final String TAG = "BTCommLib";
    private AcceptThread[] hostAcceptThreads;
    public static final int MAX_DEVICES = 7;

    // Bluetooth-Related
    private BluetoothAdapter mBluetoothAdapter;
    private volatile boolean discoverable;
    private List<BluetoothDevice> potentialPlayers;
    private BluetoothDevice[] childList;
    private ExecutorService[] executorsList;


    private AppCompatActivity contextActivity;
    private UUID[] uuidList;
    private String serviceName;
    private final Map<String, Callback> messageActions;


    public BTCommParent(AppCompatActivity contextActivity, String serviceName, UUID[] uuidList) {
        hostAcceptThreads = new AcceptThread[MAX_DEVICES];

        // General Bluetooth accessibility
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth non-existent!");
        }

//        if (!mBluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            context.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//            context.start
//        }

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
        // Do a complete (but shallow) copy of the map of MessageActions
        this.messageActions = new HashMap<>();

        // Making Discoverable
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        contextActivity.registerReceiver(discoverableReceiver, filter);

        // Initialize lists
        childList = new BluetoothDevice[MAX_DEVICES];
        executorsList = new ExecutorService[MAX_DEVICES];
        for (int i = 0; i < MAX_DEVICES; i++) {
            executorsList[i] = Executors.newSingleThreadExecutor();
        }

        potentialPlayers = new ArrayList<>();
    }

    /**
     * Adds the provided message actions to this parent.
     * @param messageActions
     */
    public void addMessageActions(Map<String, Callback> messageActions) {
        this.messageActions.putAll(messageActions);
    }

    /**
     * Sends a bluetooth message to ALL children
     * @param type - The user-defined type of the message
     * @param content - The content/body of the message
     */
    public void sendToAll(String type, String content) {
        sendToAll(type, content,-1);
    }

    /**
     * Sends a bluetooth message to all children except the one specified
     * @param type - The user-defined type of the message
     * @param content - The content/body of the message
     * @param childIndex - index of the child to skip
     */
    public void sendToAll(String type, String content, int childIndex) {
        // Iterate through all peer devices and fire off a thread for each one which sends it the message
        for (int i = 0; i < MAX_DEVICES; i++) {
            // .....because we don't associate ourselves with those null BluetoothDevice's.
            if (childList[i] == null || i == childIndex) {
                continue;
            }

            if (childList[i].getName() != null) {
                Log.d(TAG, "Sending message to: " + childList[i].getName());
            } else {
                Log.d(TAG, "Sending message to: " + childList[i].getAddress());
            }
            BluetoothMessage message = new BluetoothMessage(BluetoothMessage.Type.CLIENT_USE, type, content);
            SendMessageRunnable sendMessageRunnable = new SendMessageRunnable(childList[i],uuidList[i], message);
            executorsList[i].submit(sendMessageRunnable);
        }
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
                            contextActivity.unregisterReceiver(discoverableReceiver);
                            Log.d(TAG, "We are no longer discoverable!");
                        }
                    };
                    discoverableThread.start();
                }
            }
        }
    };

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

                Log.v(TAG, "Pulled in message");
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
                    if (inMessage.contentType.equals("JOIN_REQUEST")
                            && inMessage.content.equals(BluetoothMessage.JOIN_REQUEST_CONTENT)) {
                        if (discoverable) {
                            Log.v(TAG, "Recv'd Join request from: " + deviceName(socket.getRemoteDevice()));
                            if (!potentialPlayers.contains(socket.getRemoteDevice())) {
                                potentialPlayers.add(socket.getRemoteDevice());
                            }
                            Log.d(TAG, deviceName(socket.getRemoteDevice()) + " trying to join!");
                        } else {
                            Log.d(TAG, deviceName(socket.getRemoteDevice()) + " tried to join too late!");

                        }
                    }
                } else if (inMessage.type == BluetoothMessage.Type.CLIENT_USE) {
                    // Execute the users action for the specific type of content
                    // Pass the BluetoothMessage content as argument
                    messageActions.get(inMessage.contentType).action(playerIdx,inMessage.content);
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
                if (potentialAck.type == BluetoothMessage.Type.INTERNAL_USE
                        && potentialAck.contentType.equals("ACK" + message.id)
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

    public void enableBluetooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            BluetoothActivationFragment dialogFragment = new BluetoothActivationFragment();
            dialogFragment.show(contextActivity.getSupportFragmentManager(),"bluetoothActivateFragment");
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

                    /**
                     * TODO: Check if the device corresponds to the player that should be
                     * TODO:    communicating with me at this UUID.
                     * TODO:    e.g. socket.getRemoteDevice().equals(playerList[playerIdx])
                     */

                    // A connection was accepted
                    Log.v(TAG, "Correctly Accepted Connection on " + uuidList[playerIdx]);
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
}






