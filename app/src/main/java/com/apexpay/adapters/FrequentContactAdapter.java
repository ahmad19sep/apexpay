package com.apexpay.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.apexpay.R;
import com.apexpay.models.FrequentContact;

import java.util.ArrayList;

public class FrequentContactAdapter extends RecyclerView.Adapter<FrequentContactAdapter.ViewHolder> {

    public interface OnContactClickListener {
        void onContactClicked(FrequentContact contact);
    }

    private final ArrayList<FrequentContact> contacts;
    private OnContactClickListener listener;

    public FrequentContactAdapter(ArrayList<FrequentContact> list) {
        contacts = list;
    }

    public void setOnContactClickListener(OnContactClickListener l) {
        listener = l;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_frequent_contact, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FrequentContact contact = contacts.get(position);
        holder.tvInitials.setText(contact.initials);
        holder.tvName.setText(contact.name);
        try {
            holder.tvInitials.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor(contact.avatarColor)));
        } catch (Exception ignored) {}

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onContactClicked(contact);
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitials, tvName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitials = itemView.findViewById(R.id.tvInitials);
            tvName     = itemView.findViewById(R.id.tvContactName);
        }
    }
}