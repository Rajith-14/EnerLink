package com.example.enerlink1;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.UUID;

public class ConnectThread extends Thread {
    private final BluetoothDevice device;
    private final BluetoothAdapter adapter;
    private final Context context;
    private final ConnectionListener listener;

    private static final UUID APP_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    public interface ConnectionListener {
        void onSocketConnected(BluetoothSocket socket);
        void onConnectionFailed(IOException e);
    }

    public ConnectThread(BluetoothDevice device, BluetoothAdapter adapter, Context context, ConnectionListener listener) {
        this.device = device;
        this.adapter = adapter;
        this.context = context;
        this.listener = listener;
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    public void run() {
        BluetoothSocket socket = null;

        try {
            socket = device.createRfcommSocketToServiceRecord(APP_UUID);
        } catch (IOException e) {
            listener.onConnectionFailed(e);
            return;
        }

        adapter.cancelDiscovery();

        try {
            socket.connect();
            listener.onSocketConnected(socket);
        } catch (IOException connectException) {
            listener.onConnectionFailed(connectException);
            try {
                socket.close();
            } catch (IOException closeException) {
                Log.e("ConnectThread", "Could not close socket", closeException);
            }
        }
    }

    public void cancel() {
        try {
            // Socket closure handled by upper layers (ChatActivity or ConnectedThread)
        } catch (Exception ignored) {}
    }
}
