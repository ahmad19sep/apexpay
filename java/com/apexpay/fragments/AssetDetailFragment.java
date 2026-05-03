package com.apexpay.fragments;

import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.apexpay.R;
import com.apexpay.database.DatabaseHelper;
import com.apexpay.models.Asset;
import com.apexpay.models.CandlePoint;
import com.apexpay.services.MarketDataService;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.CandleStickChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AssetDetailFragment extends Fragment {

    private static final String ARG_SYMBOL = "symbol";

    private Asset asset;
    private int   currentDays = 1;

    private CandleStickChart candleChart;
    private BarChart          barChart;
    private TextView          tvDetailPrice, tvDetailChange,
                              tvDetailSymbol, tvDetailName, tvDetailType,
                              tvMarketCap, tvVolume24h, tvHigh52w, tvLow52w,
                              tvPeRatio, tvYourHolding;
    private LinearLayout      rowPeRatio;
    private DatabaseHelper    db;

    private TextView btn1D, btn1W, btn1M, btn3M, btn1Y;

    public static AssetDetailFragment newInstance(String symbol) {
        AssetDetailFragment f = new AssetDetailFragment();
        Bundle b = new Bundle();
        b.putString(ARG_SYMBOL, symbol);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_asset_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String symbol = getArguments() != null ? getArguments().getString(ARG_SYMBOL, "BTC") : "BTC";
        asset = MarketDataService.getAsset(symbol);
        db    = new DatabaseHelper(requireContext());

        if (asset == null) { requireActivity().getSupportFragmentManager().popBackStack(); return; }

        bindViews(view);
        populateHeader();
        populateStats();
        setupTimeButtons();
        setupCharts(1);
        setupActionButtons(view);

        // Listen for trade completions to refresh holding count
        getParentFragmentManager().setFragmentResultListener("trade_request",
                getViewLifecycleOwner(), (key, result) -> {
            if (result.getBoolean("tradeCompleted", false)) refreshHolding();
        });
    }

    private void bindViews(View v) {
        tvDetailSymbol  = v.findViewById(R.id.tvDetailSymbol);
        tvDetailName    = v.findViewById(R.id.tvDetailName);
        tvDetailType    = v.findViewById(R.id.tvDetailType);
        tvDetailPrice   = v.findViewById(R.id.tvDetailPrice);
        tvDetailChange  = v.findViewById(R.id.tvDetailChange);
        tvMarketCap     = v.findViewById(R.id.tvMarketCap);
        tvVolume24h     = v.findViewById(R.id.tvVolume24h);
        tvHigh52w       = v.findViewById(R.id.tvHigh52w);
        tvLow52w        = v.findViewById(R.id.tvLow52w);
        tvPeRatio       = v.findViewById(R.id.tvPeRatio);
        tvYourHolding   = v.findViewById(R.id.tvYourHolding);
        rowPeRatio      = v.findViewById(R.id.rowPeRatio);
        candleChart     = v.findViewById(R.id.candlestickChart);
        barChart        = v.findViewById(R.id.volumeBarChart);
        btn1D = v.findViewById(R.id.btn1D);
        btn1W = v.findViewById(R.id.btn1W);
        btn1M = v.findViewById(R.id.btn1M);
        btn3M = v.findViewById(R.id.btn3M);
        btn1Y = v.findViewById(R.id.btn1Y);
    }

    private void populateHeader() {
        tvDetailSymbol.setText(asset.symbol);
        tvDetailName.setText(asset.name);
        tvDetailType.setText(asset.type);
        tvDetailPrice.setText(String.format("$%,.2f", asset.price));

        boolean gain = asset.changePercent >= 0;
        tvDetailChange.setText(String.format("%+.2f%% today", asset.changePercent));
        tvDetailChange.setTextColor(gain ? Color.parseColor("#00C896") : Color.parseColor("#FF5252"));
    }

    private void populateStats() {
        tvMarketCap.setText(formatLarge(asset.marketCap));
        tvVolume24h.setText(formatLarge(asset.volume24h));
        tvHigh52w.setText(String.format("$%,.2f", asset.get52wHigh()));
        tvLow52w.setText(String.format("$%,.2f", asset.get52wLow()));

        if (asset.peRatio > 0) {
            tvPeRatio.setText(String.format("%.1f×", asset.peRatio));
            rowPeRatio.setVisibility(View.VISIBLE);
        } else {
            rowPeRatio.setVisibility(View.GONE);
        }

        refreshHolding();
    }

    private void refreshHolding() {
        double qty = db.getQuantity(asset.symbol);
        if (qty > 0) {
            String display = qty < 1
                    ? String.format("%.6f %s", qty, asset.symbol)
                    : String.format("%.4f %s", qty, asset.symbol);
            tvYourHolding.setText(display);
        } else {
            tvYourHolding.setText("None");
        }
    }

    private void setupTimeButtons() {
        View.OnClickListener rangeClick = v -> {
            int days;
            if      (v == btn1D) days = 1;
            else if (v == btn1W) days = 7;
            else if (v == btn1M) days = 30;
            else if (v == btn3M) days = 90;
            else                 days = 365;

            currentDays = days;
            setActiveTimeBtn(v);
            setupCharts(days);
        };

        btn1D.setOnClickListener(rangeClick);
        btn1W.setOnClickListener(rangeClick);
        btn1M.setOnClickListener(rangeClick);
        btn3M.setOnClickListener(rangeClick);
        btn1Y.setOnClickListener(rangeClick);
    }

    private void setActiveTimeBtn(View active) {
        TextView[] all = {btn1D, btn1W, btn1M, btn3M, btn1Y};
        for (TextView t : all) {
            t.setBackgroundResource(R.drawable.bg_time_btn_inactive);
            t.setTextColor(Color.parseColor("#8892B0"));
        }
        ((TextView) active).setBackgroundResource(R.drawable.bg_time_btn_active);
        ((TextView) active).setTextColor(Color.WHITE);
    }

    private void setupCharts(int days) {
        List<CandlePoint> candles = MarketDataService.getCandleData(asset.symbol, days);
        setupCandleChart(candles);
        setupVolumeChart(candles);
    }

    private void setupCandleChart(List<CandlePoint> candles) {
        candleChart.setBackgroundColor(Color.TRANSPARENT);
        candleChart.getDescription().setEnabled(false);
        candleChart.setDrawGridBackground(false);
        candleChart.getLegend().setEnabled(false);
        candleChart.setNoDataTextColor(Color.parseColor("#8892B0"));

        XAxis xAxis = candleChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.parseColor("#8892B0"));
        xAxis.setTextSize(9f);
        xAxis.setDrawLabels(false);

        YAxis left = candleChart.getAxisLeft();
        left.setTextColor(Color.parseColor("#8892B0"));
        left.setTextSize(9f);
        left.setGridColor(Color.parseColor("#22253A"));
        left.setGridLineWidth(0.5f);

        candleChart.getAxisRight().setEnabled(false);

        List<CandleEntry> entries = new ArrayList<>();
        for (int i = 0; i < candles.size(); i++) {
            CandlePoint cp = candles.get(i);
            entries.add(new CandleEntry(i, cp.high, cp.low, cp.open, cp.close));
        }

        CandleDataSet dataset = new CandleDataSet(entries, "Price");
        dataset.setShadowColor(Color.parseColor("#666688"));
        dataset.setShadowWidth(1f);
        dataset.setDecreasingColor(Color.parseColor("#FF5252"));
        dataset.setDecreasingPaintStyle(Paint.Style.FILL);
        dataset.setIncreasingColor(Color.parseColor("#00C896"));
        dataset.setIncreasingPaintStyle(Paint.Style.FILL);
        dataset.setNeutralColor(Color.GRAY);
        dataset.setDrawValues(false);

        candleChart.setData(new CandleData(dataset));
        candleChart.animateX(400);
        candleChart.invalidate();
    }

    private void setupVolumeChart(List<CandlePoint> candles) {
        barChart.setBackgroundColor(Color.TRANSPARENT);
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.getLegend().setEnabled(false);

        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setTextColor(Color.parseColor("#8892B0"));
        barChart.getXAxis().setDrawLabels(false);
        barChart.getAxisLeft().setTextColor(Color.parseColor("#8892B0"));
        barChart.getAxisLeft().setGridColor(Color.parseColor("#22253A"));
        barChart.getAxisLeft().setTextSize(9f);
        barChart.getAxisRight().setEnabled(false);

        Random r = new Random(asset.symbol.hashCode());
        List<BarEntry> barEntries = new ArrayList<>();
        for (int i = 0; i < candles.size(); i++) {
            float vol = (float) (asset.volume24h / candles.size() * (0.6 + r.nextFloat() * 0.8));
            barEntries.add(new BarEntry(i, vol));
        }

        BarDataSet ds = new BarDataSet(barEntries, "Volume");
        ds.setColor(Color.parseColor("#4D4F8A"));
        ds.setDrawValues(false);

        BarData bd = new BarData(ds);
        bd.setBarWidth(0.8f);
        barChart.setData(bd);
        barChart.animateY(400);
        barChart.invalidate();
    }

    private void setupActionButtons(View v) {
        v.findViewById(R.id.btnDetailBack).setOnClickListener(x ->
                requireActivity().getSupportFragmentManager().popBackStack());

        v.findViewById(R.id.btnMockBuy).setOnClickListener(x ->
                openTrade(true));

        v.findViewById(R.id.btnMockSell).setOnClickListener(x ->
                openTrade(false));
    }

    private void openTrade(boolean buy) {
        MockTradeFragment sheet = MockTradeFragment.newInstance(asset.symbol, buy);
        sheet.show(getParentFragmentManager(), "trade");
    }

    /** Format large numbers: B = Billion, M = Million */
    private String formatLarge(double value) {
        if (value >= 1e12) return String.format("$%.2fT", value / 1e12);
        if (value >= 1e9)  return String.format("$%.2fB", value / 1e9);
        if (value >= 1e6)  return String.format("$%.2fM", value / 1e6);
        return String.format("$%,.0f", value);
    }
}
