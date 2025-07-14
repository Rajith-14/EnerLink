package com.example.enerlink1;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatActivity extends AppCompatActivity {

    private ImageButton btnSend, btnAttachment, btnPdf;
    private EditText etMessage;
    private RecyclerView rvMessages;
    private MessageAdapter messageAdapter;
    private List<Message> messageList = new ArrayList<>();
    private ConnectedThread connectedThread;
    private AcceptThread acceptThread;

    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1002;
    private static final int REQUEST_BLUETOOTH_CONNECT = 1003;
    private static final String CHANNEL_ID = "chat_channel";

    private final Handler messageHandler = new Handler(msg -> {
        onMessageReceived((String) msg.obj);
        return true;
    });

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) sendImageMessage(imageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        btnSend = findViewById(R.id.btnSend);
        btnAttachment = findViewById(R.id.btnAttachment);
        btnPdf = findViewById(R.id.btnPdf);
        etMessage = findViewById(R.id.etMessage);
        rvMessages = findViewById(R.id.rvMessages);

        messageAdapter = new MessageAdapter(messageList, this);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(messageAdapter);

        btnSend.setOnClickListener(v -> sendTextMessage());
        btnAttachment.setOnClickListener(v -> pickImage());
        btnPdf.setOnClickListener(v -> exportMessagesToPdf());

        createNotificationChannel();
        setupBluetoothConnection();
    }

    private void setupBluetoothConnection() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        // Start AcceptThread (server mode)
        acceptThread = new AcceptThread(adapter, this, new AcceptThread.ConnectedCallback() {
            @Override
            public void onSocketConnected(BluetoothSocket socket) {
                runOnUiThread(() ->
                        Toast.makeText(ChatActivity.this, "Connected as Server", Toast.LENGTH_SHORT).show()
                );
                startConnectedThread(socket);
            }
        });
        acceptThread.start();

        // Start ConnectThread (client mode)
        String name = getIntent().getStringExtra("device_name");
        String address = getIntent().getStringExtra("device_address");

        if (name != null && address != null) {
            BluetoothDevice device = adapter.getRemoteDevice(address);
            ConnectThread connectThread = new ConnectThread(device, adapter, this, new ConnectThread.ConnectionListener() {
                @Override
                public void onSocketConnected(BluetoothSocket socket) {
                    runOnUiThread(() ->
                            Toast.makeText(ChatActivity.this, "Connected as Client", Toast.LENGTH_SHORT).show()
                    );
                    startConnectedThread(socket);
                }

                @Override
                public void onConnectionFailed(IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(ChatActivity.this, "Client connection failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            });
            connectThread.start();
        }
    }

    private void startConnectedThread(BluetoothSocket socket) {
        runOnUiThread(() -> {
            try {
                if (connectedThread == null) {
                    connectedThread = new ConnectedThread(socket, messageHandler, this);
                    connectedThread.start();

                    ((TextView) findViewById(R.id.tvConnection)).setText("Connected");
                    ((TextView) findViewById(R.id.tvConnectionTime)).setText("Connected at " + getCurrentTime());

                    if (acceptThread != null) {
                        acceptThread.cancel();
                        acceptThread = null;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void sendTextMessage() {
        String text = etMessage.getText().toString().trim();
        if (!TextUtils.isEmpty(text)) {
            if (connectedThread == null) {
                Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                String encrypted = AESUtil.encrypt("TXT:" + text);
                connectedThread.write(encrypted + "\n");

                messageList.add(new Message(text, Message.TYPE_TEXT, false, getCurrentTime()));
                messageAdapter.notifyItemInserted(messageList.size() - 1);
                rvMessages.scrollToPosition(messageList.size() - 1);
                etMessage.setText("");
            } catch (Exception e) {
                Toast.makeText(this, "Encryption failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void sendImageMessage(Uri uri) {
        if (connectedThread == null) {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            InputStream stream = getContentResolver().openInputStream(uri);
            byte[] bytes = new byte[stream.available()];
            stream.read(bytes);
            stream.close();

            String base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
            String encrypted = AESUtil.encrypt("IMG:" + base64);
            connectedThread.write(encrypted + "\n");  // newline is used as message delimiter

            // Add to local UI
            messageList.add(new Message(uri.toString(), Message.TYPE_IMAGE, false, getCurrentTime()));
            messageAdapter.notifyItemInserted(messageList.size() - 1);
            rvMessages.scrollToPosition(messageList.size() - 1);
        } catch (Exception e) {
            Toast.makeText(this, "Image send failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }


    private void onMessageReceived(String encryptedMsg) {
        runOnUiThread(() -> {
            try {
                String decrypted = AESUtil.decrypt(encryptedMsg);
                String messageText = "";

                if (decrypted.startsWith("IMG:")) {
                    String base64 = decrypted.substring(4);
                    byte[] imageBytes = android.util.Base64.decode(base64, android.util.Base64.NO_WRAP);

                    File imgFile = new File(getCacheDir(), "img_" + System.currentTimeMillis() + ".jpg");
                    FileOutputStream fos = new FileOutputStream(imgFile);
                    fos.write(imageBytes);
                    fos.close();

                    Uri uri = Uri.fromFile(imgFile);
                    messageText = "Image received";
                    messageList.add(new Message(uri.toString(), Message.TYPE_IMAGE, true, getCurrentTime()));
                } else if (decrypted.startsWith("TXT:")) {
                    messageText = decrypted.substring(4);
                    messageList.add(new Message(messageText, Message.TYPE_TEXT, true, getCurrentTime()));
                }

                messageAdapter.notifyItemInserted(messageList.size() - 1);
                rvMessages.scrollToPosition(messageList.size() - 1);

                if (!isAppInForeground()) showNotification("New Message", messageText);
            } catch (Exception e) {
                e.printStackTrace(); // Add this
                Toast.makeText(this, "Decryption error", Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void showNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.outline_chat_24)
                .setContentTitle(title)
                .setContentText(message.length() > 40 ? message.substring(0, 40) + "..." : message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Chat Messages", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Incoming chat messages");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private boolean isAppInForeground() {
        return getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED);
    }

    private void exportMessagesToPdf() {
        PdfDocument doc = new PdfDocument();
        int pageNum = 1, y = 50;
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(595, 842, pageNum).create();
        PdfDocument.Page page = doc.startPage(info);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        int x = 40, lineHeight = 40;

        for (Message msg : messageList) {
            if (msg.getType() == Message.TYPE_TEXT) {
                String prefix = msg.isReceived() ? "Friend" : "You";
                String line = prefix + ": " + msg.getContent() + " (" + msg.getTime() + ")";
                canvas.drawText(line, x, y, paint);
                y += lineHeight;
                if (y > 800) {
                    doc.finishPage(page);
                    info = new PdfDocument.PageInfo.Builder(595, 842, ++pageNum).create();
                    page = doc.startPage(info);
                    canvas = page.getCanvas();
                    y = 50;
                }
            }
        }

        doc.finishPage(page);
        try {
            File pdf = new File(getExternalFilesDir(null), "chat.pdf");
            FileOutputStream fos = new FileOutputStream(pdf);
            doc.writeTo(fos);
            doc.close();
            fos.close();
            Toast.makeText(this, "PDF saved: " + pdf.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "PDF Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
    }

    @Override
    protected void onDestroy() {
        if (connectedThread != null) connectedThread.cancel();
        if (acceptThread != null) acceptThread.cancel();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            exportMessagesToPdf();
        } else if (requestCode == REQUEST_BLUETOOTH_CONNECT && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            recreate();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
