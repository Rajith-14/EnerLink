package com.example.enerlink1;

public class Message {
    public static final int TYPE_TEXT = 0;
    public static final int TYPE_IMAGE = 1;

    private String content;     // Text or Image URI (as string)
    private int type;           // TYPE_TEXT or TYPE_IMAGE
    private boolean isReceived; // true = received, false = sent
    private String time;        // Timestamp (e.g., "10:30 AM")

    public Message(String content, int type, boolean isReceived, String time) {
        this.content = content;
        this.type = type;
        this.isReceived = isReceived;
        this.time = time;
    }

    public String getContent() { return content; }
    public int getType() { return type; }
    public boolean isReceived() { return isReceived; }
    public String getTime() { return time; }

    public void setContent(String content) { this.content = content; }
    public void setType(int type) { this.type = type; }
    public void setReceived(boolean received) { isReceived = received; }
    public void setTime(String time) { this.time = time; }

    // Optional helper
    public boolean isImage() {
        return type == TYPE_IMAGE;
    }

    public boolean isSentByMe() {
        return !isReceived;
    }
}
