package com.apexpay.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.apexpay.R;
import com.apexpay.models.Asset;

import java.util.ArrayList;
import java.util.List;

public class AssetAdapter extends RecyclerView.Adapter<AssetAdapter.VH> {

    public interface OnAssetClickListener {
        void onAssetClick(Asset asset);
    }

    private List<Asset> items = new ArrayList<>();
    private OnAssetClickListener listener;

    public void setItems(List<Asset> assets) {
        this.items = assets;
        notifyDataSetChanged();
    }

    public void setOnAssetClickListener(OnAssetClickListener l) { listener = l; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_asset, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Asset a = items.get(pos);

        h.tvIcon.setText(iconChar(a));
        h.tvIcon.setBackgroundColor(iconColor(a));

        h.tvSymbol.setText(a.symbol);
        h.tvName.setText(a.name);
        h.tvPrice.setText(String.format("$%,.2f", a.price));

        boolean gain = a.changePercent >= 0;
        h.tvChange.setText(String.format("%+.2f%%", a.changePercent));
        h.tvChange.setTextColor(gain
                ? Color.parseColor("#00C896")
                : Color.parseColor("#FF5252"));

        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onAssetClick(a); });
    }

    @Override
    public int getItemCount() { return items.size(); }

    private String iconChar(Asset a) {
        if (!a.iconEmoji.isEmpty()) return a.iconEmoji;
        return a.symbol.substring(0, 1);
    }

    private int iconColor(Asset a) {
        switch (a.type) {
            case Asset.TYPE_CRYPTO: return Color.parseColor("#2D2F5A");
            case Asset.TYPE_STOCK:  return Color.parseColor("#1A2E4A");
            default:                return Color.parseColor("#1A3030");
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvIcon, tvSymbol, tvName, tvPrice, tvChange;
        VH(@NonNull View v) {
            super(v);
            tvIcon   = v.findViewById(R.id.tvAssetIcon);
            tvSymbol = v.findViewById(R.id.tvAssetSymbol);
            tvName   = v.findViewById(R.id.tvAssetName);
            tvPrice  = v.findViewById(R.id.tvAssetPrice);
            tvChange = v.findViewById(R.id.tvAssetChange);
        }
    }
}
