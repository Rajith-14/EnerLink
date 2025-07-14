package com.example.enerlink1;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

public class BluetoothGroupManager {

    public interface GroupMessageListener {
        void onMessageReceived(String message, BluetoothSocket sender);
    }

    private Set<ConnectedThread> connectedThreads = new HashSet<>();
    private GroupMessageListener listener;
    private Handler handler = new Handler();

    public BluetoothGroupManager(GroupMessageListener listener) {
        this.listener = listener;
    }

    public void addDevice(BluetoothSocket socket) {
        ConnectedThread thread = new ConnectedThread(socket);
        connectedThreads.add(thread);
        thread.start();
    }

    public void broadcastMessage(String message) {
        for (ConnectedThread thread : connectedThreads) {
            thread.write(message);
        }
    }

    public void stopAll() {
        for (ConnectedThread thread : connectedThreads) {
            thread.cancel();
        }
        connectedThreads.clear();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inStream;
        private final OutputStream outStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inStream = tmpIn;
            outStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inStream.read(buffer);
                    String message = new String(buffer, 0, bytes);
                    if (listener != null) {
                        handler.post(() -> listener.onMessageReceived(message, socket));
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(String message) {
            try {
                outStream.write(message.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

