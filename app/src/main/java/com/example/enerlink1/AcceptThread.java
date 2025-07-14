package com.example.enerlink1;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.UUID;

public class AcceptThread extends Thread {
    private final BluetoothServerSocket serverSocket;
    private final Handler handler;
    private final Context context;
    private final ConnectedCallback callback;

    private static final String APP_NAME = "EnerLinkChat";
    private static final UUID APP_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    public interface ConnectedCallback {
        void onSocketConnected(BluetoothSocket socket);
    }

    public AcceptThread(BluetoothAdapter adapter, Context context, ConnectedCallback callback) {
        this.context = context;
        this.callback = callback;
        this.handler = new Handler();

        BluetoothServerSocket tmp = null;
        try {
            tmp = adapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID);
        } catch (IOException e) {
            e.printStackTrace();
        }
        serverSocket = tmp;
    }

    public void run() {
        BluetoothSocket socket;

        while (true) {
            try {
                socket = serverSocket.accept();
                if (socket != null) {
                    callback.onSocketConnected(socket);
                    break;
                }
            } catch (IOException e) {
                Log.e("AcceptThread", "Accept failed", e);
                break;
            }
        }
    }

    public void cancel() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.e("AcceptThread", "Close failed", e);
        }
    }
}
