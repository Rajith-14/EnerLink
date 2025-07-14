package com.example.enerlink1;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.*;

public class ConnectedThread extends Thread {
    private final BluetoothSocket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final Handler handler;
    private final Context context;

    public ConnectedThread(BluetoothSocket socket, Handler handler, Context context) throws IOException {
        this.socket = socket;
        this.handler = handler;
        this.context = context;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;
        StringBuilder sb = new StringBuilder();

        while (true) {
            try {
                bytes = inputStream.read(buffer);
                String readMessage = new String(buffer, 0, bytes);
                sb.append(readMessage);

                int index;
                while ((index = sb.indexOf("\n")) != -1) {
                    String completeMessage = sb.substring(0, index).trim();
                    sb.delete(0, index + 1);

                    android.os.Message msg = handler.obtainMessage(0, completeMessage);

                    msg.sendToTarget();
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }



    public void write(String msg) {
        try {
            outputStream.write(msg.getBytes());
        } catch (IOException e) {
            Log.e("ConnectedThread", "Error writing", e);
        }
    }

    public void cancel() {
        try {
            socket.close();
        } catch (IOException e) {
            Log.e("ConnectedThread", "Socket close failed", e);
        }
    }
}
