package com.wordpress.simpledevelopments.btcommlib;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

/**
 * Created by connor on 10/10/17.
 */

public class BluetoothActivationFragment extends DialogFragment {
    private static final String TAG = "BluetoothFragment";
    private static BroadcastReceiver broadcastReceiver;
    EnablingDialogFragment enablingDialogFragment;

    public BluetoothActivationFragment() {
        super();
        setCancelable(false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(TAG, "onCreateDialog");

        AlertDialog.Builder parentBuilder = new AlertDialog.Builder(getContext());
        AlertDialog dialog = parentBuilder.setMessage("Bluetooth must be enabled to continue. Would you like to enable it?")
                .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("MainActivity", "Allowed!!!");
                        enablingDialogFragment = new EnablingDialogFragment();
                        enablingDialogFragment.show(getFragmentManager(), "bluetoothActivateFragment");

                        // General Bluetooth accessibility
                        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        if (bluetoothAdapter == null) {
                            Log.e(TAG, "Bluetooth non-existent!");
                        }
                        if (!bluetoothAdapter.isEnabled()) {
                            bluetoothAdapter.enable();
                        }
                        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                        getContext().registerReceiver(broadcastReceiver,filter);

                    }
                })
                .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("MainActivity", "Denied!!!");
                    }
                })
                .create();
        return dialog;
    }

    public static class EnablingDialogFragment extends DialogFragment {

        public EnablingDialogFragment() {
            super();
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                        if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0) == BluetoothAdapter.STATE_ON) {
                            Log.d(TAG, "Attempting to unregister: " + getContext());
                            getContext().unregisterReceiver(broadcastReceiver);
                            dismiss();
                        }
                    }
                }
            };
        }
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage("Enabling Bluetooth...");
            return builder.create();
        }
    }
}