package com.github.welcomeworld.debugMessageView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    List<String> messages = new ArrayList<>();
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MessageHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.debug_message_view_item_message,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageHolder messageHolder = (MessageHolder) holder;
        messageHolder.messageView.setText(messages.get(position));
    }

    private static class MessageHolder extends RecyclerView.ViewHolder{
        TextView messageView;

        public MessageHolder(@NonNull View itemView) {
            super(itemView);
            messageView = itemView.findViewById(R.id.debug_message_item);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }
}
