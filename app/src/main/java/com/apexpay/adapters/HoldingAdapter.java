package com.apexpay.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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

    public void setOnHoldingClickListener(OnHoldingClickListener l) {
        listener = l;
    }

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
        h.tvIcon.setBackgroundColor(iconColor(h.itemView, holding.assetType));

        h.tvSymbol.setText(holding.symbol);
        h.tvName.setText(holding.name);

        if (holding.quantity < 1) {
            h.tvQty.setText(String.format("%.6f %s", holding.quantity, holding.symbol));
        } else {
            h.tvQty.setText(String.format("%.4f %s", holding.quantity, holding.symbol));
        }

        h.tvValue.setText(String.format("$%,.2f", holding.getTotalValue()));

        double pnl = holding.getProfitLoss();
        double pnlPct = holding.getProfitLossPct();
        boolean gain = pnl >= 0;
        String pnlSign;
        if (gain) {
            pnlSign = "+";
        } else {
            pnlSign = "-";
        }
        h.tvPnl.setText(String.format("%s$%,.2f (%.2f%%)", pnlSign, Math.abs(pnl), Math.abs(pnlPct)));
        if (gain) {
            h.tvPnl.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.gain_green));
        } else {
            h.tvPnl.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.loss_red));
        }

        h.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onHoldingClick(holding);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private int iconColor(View itemView, String type) {
        if ("Crypto".equals(type)) {
            return ContextCompat.getColor(itemView.getContext(), R.color.holding_crypto_bg);
        } else if ("Stock".equals(type)) {
            return ContextCompat.getColor(itemView.getContext(), R.color.holding_stock_bg);
        } else {
            return ContextCompat.getColor(itemView.getContext(), R.color.holding_etf_bg);
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvIcon, tvSymbol, tvName, tvQty, tvValue, tvPnl;

        VH(@NonNull View v) {
            super(v);
            tvIcon = v.findViewById(R.id.tvHoldingIcon);
            tvSymbol = v.findViewById(R.id.tvHoldingSymbol);
            tvName = v.findViewById(R.id.tvHoldingName);
            tvQty = v.findViewById(R.id.tvHoldingQty);
            tvValue = v.findViewById(R.id.tvHoldingValue);
            tvPnl = v.findViewById(R.id.tvHoldingPnl);
        }
    }
}
