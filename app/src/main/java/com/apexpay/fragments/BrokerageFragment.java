package com.apexpay.fragments;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.apexpay.R;
import com.apexpay.adapters.AssetAdapter;
import com.apexpay.adapters.HoldingAdapter;
import com.apexpay.database.DatabaseHelper;
import com.apexpay.models.Asset;
import com.apexpay.models.Holding;
import com.apexpay.services.MarketDataService;

import java.util.List;

public class BrokerageFragment extends Fragment {

    private AssetAdapter   assetAdapter;
    private HoldingAdapter holdingAdapter;
    private DatabaseHelper db;
    private SharedPreferences prefs;

    private TextView  tvPortfolioValue, tvPortfolioChange, tvSimCash,
                      tvHoldingCount, tvHoldingsEmpty, tvLiveStatus;
    private ProgressBar pbLoading;

    private String activeTab = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_brokerage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db    = new DatabaseHelper(requireContext());
        prefs = requireActivity().getSharedPreferences("ApexPayPrefs",
                android.content.Context.MODE_PRIVATE);

        bindViews(view);
        setupAssetList(view);
        setupHoldingList(view);
        setupTabs(view);

        // Show static data immediately so the screen isn't blank
        refreshAll();

        // Then kick off live price fetch
        showLoading(true);
        MarketDataService.fetchLivePrices(() -> {
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (isAdded()) {
                        showLoading(false);
                        refreshAll();
                        if (tvLiveStatus != null) tvLiveStatus.setText("LIVE");
                    }
                });
            }
        });

        view.findViewById(R.id.btnViewAnalytics).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new DashboardFragment())
                        .addToBackStack(null)
                        .commit());

        getParentFragmentManager().setFragmentResultListener("trade_request",
                getViewLifecycleOwner(), (key, result) -> {
                    if (result.getBoolean("tradeCompleted", false)) refreshAll();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAll();
    }

    // ── Binding ──────────────────────────────────────────────────────────────

    private void bindViews(View v) {
        tvPortfolioValue  = v.findViewById(R.id.tvPortfolioValue);
        tvPortfolioChange = v.findViewById(R.id.tvPortfolioChange);
        tvSimCash         = v.findViewById(R.id.tvSimCash);
        tvHoldingCount    = v.findViewById(R.id.tvHoldingCount);
        tvHoldingsEmpty   = v.findViewById(R.id.tvHoldingsEmpty);
        tvLiveStatus      = v.findViewById(R.id.tvLiveStatus);
        pbLoading         = v.findViewById(R.id.pbBrokerageLoading);
    }

    private void setupAssetList(View v) {
        assetAdapter = new AssetAdapter();
        assetAdapter.setOnAssetClickListener(asset -> openAssetDetail(asset.symbol));
        RecyclerView rv = v.findViewById(R.id.rvAssets);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setNestedScrollingEnabled(false);
        rv.setAdapter(assetAdapter);
    }

    private void setupHoldingList(View v) {
        holdingAdapter = new HoldingAdapter();
        holdingAdapter.setOnHoldingClickListener(h -> openAssetDetail(h.symbol));
        RecyclerView rv = v.findViewById(R.id.rvHoldings);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setNestedScrollingEnabled(false);
        rv.setAdapter(holdingAdapter);
        v.findViewById(R.id.tvSeeAllHoldings).setOnClickListener(x -> { /* no-op */ });
    }

    private void setupTabs(View v) {
        TextView tabAll     = v.findViewById(R.id.tabAll);
        TextView tabStocks  = v.findViewById(R.id.tabStocks);
        TextView tabCrypto  = v.findViewById(R.id.tabCrypto);
        TextView tabGainers = v.findViewById(R.id.tabGainers);
        TextView tabETFs    = v.findViewById(R.id.tabETFs);

        View.OnClickListener click = tab -> {
            String id;
            if      (tab == tabAll)     id = "all";
            else if (tab == tabStocks)  id = "stocks";
            else if (tab == tabCrypto)  id = "crypto";
            else if (tab == tabGainers) id = "gainers";
            else                        id = "etf";
            activeTab = id;
            setActiveTab(id, tabAll, tabStocks, tabCrypto, tabGainers, tabETFs);
            refreshAssetList();
        };

        tabAll.setOnClickListener(click);
        tabStocks.setOnClickListener(click);
        tabCrypto.setOnClickListener(click);
        tabGainers.setOnClickListener(click);
        tabETFs.setOnClickListener(click);
    }

    // ── Loading state ─────────────────────────────────────────────────────────

    private void showLoading(boolean loading) {
        if (pbLoading != null) pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (tvLiveStatus != null) tvLiveStatus.setText(loading ? "LOADING…" : "LIVE");
    }

    // ── Refresh ──────────────────────────────────────────────────────────────

    private void refreshAll() {
        refreshAssetList();
        refreshHoldings();
        refreshHeader();
    }

    private void refreshAssetList() {
        List<Asset> assets;
        switch (activeTab) {
            case "stocks":  assets = MarketDataService.getByType(Asset.TYPE_STOCK);  break;
            case "crypto":  assets = MarketDataService.getByType(Asset.TYPE_CRYPTO); break;
            case "gainers": assets = MarketDataService.getTopGainers();              break;
            case "etf":     assets = MarketDataService.getByType(Asset.TYPE_ETF);    break;
            default:        assets = MarketDataService.getAllAssets();                break;
        }
        assetAdapter.setItems(assets);
    }

    private void refreshHoldings() {
        List<Holding> holdings = db.getAllHoldings();
        for (Holding h : holdings) {
            Asset a = MarketDataService.getAsset(h.symbol);
            h.currentPrice = (a != null && a.price > 0) ? a.price : h.avgBuyPrice;
        }
        holdingAdapter.setItems(holdings);
        boolean empty = holdings.isEmpty();
        if (tvHoldingsEmpty != null)
            tvHoldingsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void refreshHeader() {
        double simCash = getSimCash();
        if (tvSimCash != null) tvSimCash.setText(String.format("$%,.2f", simCash));

        List<Holding> holdings = db.getAllHoldings();
        double portfolioValue  = 0, portfolioCost = 0;
        for (Holding h : holdings) {
            Asset a = MarketDataService.getAsset(h.symbol);
            double cur = (a != null && a.price > 0) ? a.price : h.avgBuyPrice;
            portfolioValue += h.quantity * cur;
            portfolioCost  += h.quantity * h.avgBuyPrice;
        }

        if (tvPortfolioValue != null)
            tvPortfolioValue.setText(String.format("$%,.2f", portfolioValue));
        if (tvHoldingCount != null)
            tvHoldingCount.setText(holdings.size() + " Asset" + (holdings.size() == 1 ? "" : "s"));

        if (portfolioCost > 0 && tvPortfolioChange != null) {
            double pnl    = portfolioValue - portfolioCost;
            double pnlPct = (pnl / portfolioCost) * 100;
            tvPortfolioChange.setText(String.format("%+$,.2f  (%+.2f%%)", pnl, pnlPct));
            tvPortfolioChange.setTextColor(pnl >= 0
                    ? Color.parseColor("#00C896") : Color.parseColor("#FF5252"));
        } else if (tvPortfolioChange != null) {
            tvPortfolioChange.setText("—");
            tvPortfolioChange.setTextColor(Color.parseColor("#8892B0"));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void setActiveTab(String active, TextView... tabs) {
        String[] ids = {"all", "stocks", "crypto", "gainers", "etf"};
        for (int i = 0; i < tabs.length; i++) {
            boolean on = ids[i].equals(active);
            tabs[i].setBackgroundResource(on ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
            tabs[i].setTextColor(on ? Color.WHITE : Color.parseColor("#8892B0"));
            tabs[i].setTypeface(null, on ? android.graphics.Typeface.BOLD
                                         : android.graphics.Typeface.NORMAL);
        }
    }

    private void openAssetDetail(String symbol) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, AssetDetailFragment.newInstance(symbol))
                .addToBackStack(null)
                .commit();
    }

    private double getSimCash() {
        return Double.longBitsToDouble(
                prefs.getLong("simCash", Double.doubleToLongBits(10000.0)));
    }
}
