package com.example.enerlink1;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class GroupChatActivity extends AppCompatActivity {

    private ImageButton btnSend, btnAttachment, btnPdf;
    private EditText etMessage;
    private RecyclerView rvMessages;
    private MessageAdapter messageAdapter;
    private final List<Message> messageList = new ArrayList<>();
    private final Map<String, ConnectedThread> connectedThreads = new HashMap<>();

    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 2001;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        sendImageMessage(imageUri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

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

        ((TextView) findViewById(R.id.tvConnection)).setText("Connected to Group");
        ((TextView) findViewById(R.id.tvConnectionTime)).setText("Connection established at " + getCurrentTime());

        BluetoothGroupManager groupManager = new BluetoothGroupManager((message, socket) -> {
            String address = socket.getRemoteDevice().getAddress();
            if (!connectedThreads.containsKey(address)) {
                try {
                    ConnectedThread thread = new ConnectedThread(socket);
                    connectedThreads.put(address, thread);
                    thread.start();
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(this, "Failed to start connection thread", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void sendTextMessage() {
        String text = etMessage.getText().toString().trim();
        if (!TextUtils.isEmpty(text)) {
            try {
                String encrypted = AESUtil.encrypt("TXT:" + text);
                for (ConnectedThread thread : connectedThreads.values()) {
                    thread.write(encrypted + "\n");
                }
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
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            byte[] imageBytes = new byte[inputStream.available()];
            inputStream.read(imageBytes);
            inputStream.close();

            String base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP);
            String encrypted = AESUtil.encrypt("IMG:" + base64Image);

            for (ConnectedThread thread : connectedThreads.values()) {
                thread.write(encrypted + "\n");
            }

            messageList.add(new Message(uri.toString(), Message.TYPE_IMAGE, false, getCurrentTime()));
            messageAdapter.notifyItemInserted(messageList.size() - 1);
            rvMessages.scrollToPosition(messageList.size() - 1);
        } catch (Exception e) {
            Toast.makeText(this, "Image send failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void onMessageReceived(String encryptedMsg) {
        runOnUiThread(() -> {
            try {
                String decrypted = AESUtil.decrypt(encryptedMsg);
                if (decrypted.startsWith("IMG:")) {
                    String base64 = decrypted.substring(4);
                    byte[] imageBytes = android.util.Base64.decode(base64, android.util.Base64.NO_WRAP);

                    File imgFile = new File(getCacheDir(), "img_" + System.currentTimeMillis() + ".jpg");
                    FileOutputStream fos = new FileOutputStream(imgFile);
                    fos.write(imageBytes);
                    fos.close();

                    Uri uri = Uri.fromFile(imgFile);
                    messageList.add(new Message(uri.toString(), Message.TYPE_IMAGE, true, getCurrentTime()));
                } else if (decrypted.startsWith("TXT:")) {
                    String text = decrypted.substring(4);
                    messageList.add(new Message(text, Message.TYPE_TEXT, true, getCurrentTime()));
                }

                messageAdapter.notifyItemInserted(messageList.size() - 1);
                rvMessages.scrollToPosition(messageList.size() - 1);
            } catch (Exception e) {
                Toast.makeText(this, "Decryption error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void exportMessagesToPdf() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
            return;
        }

        PdfDocument doc = new PdfDocument();
        int pageNum = 1, y = 50;
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(595, 842, pageNum).create();
        PdfDocument.Page page = doc.startPage(info);
        Canvas canvas = page.getCanvas();
        int x = 40, lineHeight = 40;

        for (Message msg : messageList) {
            if (msg.getType() == Message.TYPE_TEXT) {
                String text = (msg.isReceived() ? "Friend: " : "You: ") + msg.getContent() + " (" + msg.getTime() + ")";
                canvas.drawText(text, x, y, new android.graphics.Paint());
                y += lineHeight;
                if (y > 800) {
                    doc.finishPage(page);
                    pageNum++;
                    info = new PdfDocument.PageInfo.Builder(595, 842, pageNum).create();
                    page = doc.startPage(info);
                    canvas = page.getCanvas();
                    y = 50;
                }
            }
        }
        doc.finishPage(page);

        try {
            File pdf = new File(getExternalFilesDir(null), "group_chat.pdf");
            FileOutputStream fos = new FileOutputStream(pdf);
            doc.writeTo(fos);
            doc.close();
            fos.close();
            Toast.makeText(this, "PDF saved: " + pdf.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "PDF Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
    }

    public class ConnectedThread extends Thread {
        private final InputStream in;
        private final OutputStream out;

        public ConnectedThread(BluetoothSocket socket) throws IOException {
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
        }

        public void run() {
            byte[] buffer = new byte[8192];
            int bytes;
            StringBuilder sb = new StringBuilder();

            while (true) {
                try {
                    bytes = in.read(buffer);
                    if (bytes > 0) {
                        String part = new String(buffer, 0, bytes);
                        sb.append(part);

                        int index;
                        while ((index = sb.indexOf("\n")) != -1) {
                            String completeMessage = sb.substring(0, index).trim();
                            sb.delete(0, index + 1);
                            onMessageReceived(completeMessage);
                        }
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(String message) {
            try {
                out.write(message.getBytes());
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
