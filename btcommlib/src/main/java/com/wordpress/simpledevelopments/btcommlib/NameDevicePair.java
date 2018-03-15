package com.wordpress.simpledevelopments.btcommlib;

import android.bluetooth.BluetoothDevice;

/**
 * Created by connor on 3/14/18.
 */

public class NameDevicePair {
    public String name;
    public BluetoothDevice device;
    public NameDevicePair(String name, BluetoothDevice device) {
        this.name = name;
        this.device = device;
    }
}
