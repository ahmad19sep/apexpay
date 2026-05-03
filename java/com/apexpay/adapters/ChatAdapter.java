package com.apexpay.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.apexpay.R;
import com.apexpay.models.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 0;
    private static final int TYPE_AI   = 1;

    private final List<ChatMessage> items = new ArrayList<>();

    public void setMessages(List<ChatMessage> messages) {
        items.clear();
        items.addAll(messages);
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessage message) {
        items.add(message);
        notifyItemInserted(items.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).role.equals(ChatMessage.ROLE_USER) ? TYPE_USER : TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            View v = inf.inflate(R.layout.item_chat_user, parent, false);
            return new UserVH(v);
        } else {
            View v = inf.inflate(R.layout.item_chat_ai, parent, false);
            return new AiVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = items.get(position);
        if (holder instanceof UserVH) ((UserVH) holder).tvMsg.setText(msg.content);
        else                          ((AiVH)   holder).tvMsg.setText(msg.content);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class UserVH extends RecyclerView.ViewHolder {
        TextView tvMsg;
        UserVH(@NonNull View v) {
            super(v);
            tvMsg = v.findViewById(R.id.tvUserMsg);
        }
    }

    static class AiVH extends RecyclerView.ViewHolder {
        TextView tvMsg;
        AiVH(@NonNull View v) {
            super(v);
            tvMsg = v.findViewById(R.id.tvAiMsg);
        }
    }
}
