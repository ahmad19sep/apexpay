package com.apexpay.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.apexpay.R;
import com.apexpay.models.Holding;

import java.util.ArrayList;
import java.util.List;

public class HoldingAdapter extends RecyclerView.Adapter<HoldingAdapter.VH> {

    public interface OnHoldingClickListener {
        void onHoldingClick(Holding h);
    }

    private List<Holding> items = new ArrayList<>();
    private OnHoldingClickListener listener;

    public void setItems(List<Holding> list) {
        this.items = list;
        notifyDataSetChanged();
    }

    public void setOnHoldingClickListener(OnHoldingClickListener l) { listener = l; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_holding, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Holding holding = items.get(pos);

        h.tvIcon.setText(holding.symbol.substring(0, 1));
        h.tvIcon.setBackgroundColor(iconColor(holding.assetType));

        h.tvSymbol.setText(holding.symbol);
        h.tvName.setText(holding.name);

        // Format quantity: show up to 6 decimal places for crypto, 4 for stocks
        if (holding.quantity < 1) {
            h.tvQty.setText(String.format("%.6f %s", holding.quantity, holding.symbol));
        } else {
            h.tvQty.setText(String.format("%.4f %s", holding.quantity, holding.symbol));
        }

        h.tvValue.setText(String.format("$%,.2f", holding.getTotalValue()));

        double pnl    = holding.getProfitLoss();
        double pnlPct = holding.getProfitLossPct();
        boolean gain  = pnl >= 0;
        h.tvPnl.setText(String.format("%+$,.2f (%.2f%%)", pnl, pnlPct));
        h.tvPnl.setTextColor(gain
                ? Color.parseColor("#00C896")
                : Color.parseColor("#FF5252"));

        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onHoldingClick(holding); });
    }

    @Override
    public int getItemCount() { return items.size(); }

    private int iconColor(String type) {
        switch (type) {
            case "Crypto": return Color.parseColor("#2D2F5A");
            case "Stock":  return Color.parseColor("#1A2E4A");
            default:       return Color.parseColor("#1A3030");
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvIcon, tvSymbol, tvName, tvQty, tvValue, tvPnl;
        VH(@NonNull View v) {
            super(v);
            tvIcon   = v.findViewById(R.id.tvHoldingIcon);
            tvSymbol = v.findViewById(R.id.tvHoldingSymbol);
            tvName   = v.findViewById(R.id.tvHoldingName);
            tvQty    = v.findViewById(R.id.tvHoldingQty);
            tvValue  = v.findViewById(R.id.tvHoldingValue);
            tvPnl    = v.findViewById(R.id.tvHoldingPnl);
        }
    }
}
