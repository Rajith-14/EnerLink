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

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Message> messageList;
    private Context context;

    // View types
    private static final int TYPE_TEXT_OUTGOING = 0;
    private static final int TYPE_TEXT_INCOMING = 1;
    private static final int TYPE_IMAGE_OUTGOING = 2;
    private static final int TYPE_IMAGE_INCOMING = 3;

    public MessageAdapter(List<Message> messageList, Context context) {
        this.messageList = messageList;
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getType() == Message.TYPE_TEXT) {
            return message.isReceived() ? TYPE_TEXT_INCOMING : TYPE_TEXT_OUTGOING;
        } else {
            return message.isReceived() ? TYPE_IMAGE_INCOMING : TYPE_IMAGE_OUTGOING;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TYPE_TEXT_OUTGOING) {
            View view = inflater.inflate(R.layout.item_message_text_outgoing, parent, false);
            return new TextViewHolder(view);
        } else if (viewType == TYPE_TEXT_INCOMING) {
            View view = inflater.inflate(R.layout.item_message_text_incoming, parent, false);
            return new TextViewHolder(view);
        } else if (viewType == TYPE_IMAGE_OUTGOING) {
            View view = inflater.inflate(R.layout.item_message_image_outgoing, parent, false);
            return new ImageViewHolder(view);
        } else { // TYPE_IMAGE_INCOMING
            View view = inflater.inflate(R.layout.item_message_image_incoming, parent, false);
            return new ImageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        String time = message.getTime();

        if (holder instanceof TextViewHolder) {
            TextViewHolder textHolder = (TextViewHolder) holder;
            textHolder.tvMessage.setText(message.getContent());
            textHolder.tvTime.setText(time);
        } else if (holder instanceof ImageViewHolder) {
            ImageViewHolder imageHolder = (ImageViewHolder) holder;
            imageHolder.ivMessage.setImageURI(Uri.parse(message.getContent()));
            imageHolder.tvTime.setText(time);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class TextViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        public TextViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.textMessage);
            tvTime = itemView.findViewById(R.id.textTime);
        }
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMessage;
        TextView tvTime;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMessage = itemView.findViewById(R.id.imageMessage);
            tvTime = itemView.findViewById(R.id.textTime);
        }
    }
}
