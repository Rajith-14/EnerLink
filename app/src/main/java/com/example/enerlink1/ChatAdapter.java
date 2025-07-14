package com.example.enerlink1;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private List<Message> messageList;
    private Context context;

    public ChatAdapter(Context context, List<Message> messages) {
        this.context = context;
        this.messageList = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).isReceived() ? TYPE_RECEIVED : TYPE_SENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_SENT) {
            view = LayoutInflater.from(context).inflate(R.layout.item_chat_sent, parent, false);
            return new ChatViewHolder(view);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_chat_received, parent, false);
            return new ChatViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatViewHolder chatHolder = (ChatViewHolder) holder;
        Message message = messageList.get(position);

        chatHolder.textTime.setText(message.getTime());

        if (message.isImage()) {
            chatHolder.imageMessage.setVisibility(View.VISIBLE);
            chatHolder.textMessage.setVisibility(View.GONE);
            chatHolder.imageMessage.setImageURI(Uri.parse(message.getContent())); // Optional: use Glide/Picasso
        } else {
            chatHolder.textMessage.setVisibility(View.VISIBLE);
            chatHolder.imageMessage.setVisibility(View.GONE);
            chatHolder.textMessage.setText(message.getContent());
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, textTime;
        ImageView imageMessage;

        ChatViewHolder(View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
            textTime = itemView.findViewById(R.id.textTime);
            imageMessage = itemView.findViewById(R.id.imageMessage);
        }
    }
}
