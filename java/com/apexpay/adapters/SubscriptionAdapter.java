package com.apexpay.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.apexpay.R;
import com.apexpay.models.Subscription;

import java.util.List;

public class SubscriptionAdapter extends RecyclerView.Adapter<SubscriptionAdapter.ViewHolder> {

    private final List<Subscription> subscriptions;

    public SubscriptionAdapter(List<Subscription> list) {
        subscriptions = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subscription, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Subscription sub = subscriptions.get(position);
        holder.tvIcon.setText(sub.icon);
        holder.tvName.setText(sub.name);
        holder.tvNextBilling.setText("Next: " + sub.nextBillingDate);
        holder.tvAmount.setText(String.format("-$%.2f/mo", sub.monthlyAmount));
    }

    @Override
    public int getItemCount()
    {
        return subscriptions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvName, tvNextBilling, tvAmount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon       = itemView.findViewById(R.id.tvSubIcon);
            tvName       = itemView.findViewById(R.id.tvSubName);
            tvNextBilling = itemView.findViewById(R.id.tvSubNextBilling);
            tvAmount     = itemView.findViewById(R.id.tvSubAmount);
        }
    }
}