package com.apexpay.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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

    private AssetAdapter assetAdapter;
    private HoldingAdapter holdingAdapter;
    private DatabaseHelper db;
    private SharedPreferences prefs;

    private TextView tvPortfolioValue, tvPortfolioChange, tvSimCash,
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

        db = new DatabaseHelper(requireContext());
        prefs = requireActivity().getSharedPreferences("ApexPayPrefs",
                android.content.Context.MODE_PRIVATE);

        bindViews(view);
        setupAssetList(view);
        setupHoldingList(view);
        setupTabs(view);

        refreshAll();

        showLoading(true);
        MarketDataService.fetchLivePrices(new Runnable() {
            @Override
            public void run() {
                android.app.Activity act = getActivity();
                if (isAdded() && act != null) {
                    act.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isAdded()) {
                                showLoading(false);
                                refreshAll();
                                if (tvLiveStatus != null) {
                                    tvLiveStatus.setText(getString(R.string.live_label));
                                }
                            }
                        }
                    });
                }
            }
        });

        view.findViewById(R.id.btnViewAnalytics).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new DashboardFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        getParentFragmentManager().setFragmentResultListener("trade_request",
                getViewLifecycleOwner(), new androidx.fragment.app.FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String key, @NonNull Bundle result) {
                        if (result.getBoolean("tradeCompleted", false)) {
                            refreshAll();
                        }
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAll();
    }

    private void bindViews(View v) {
        tvPortfolioValue = v.findViewById(R.id.tvPortfolioValue);
        tvPortfolioChange = v.findViewById(R.id.tvPortfolioChange);
        tvSimCash = v.findViewById(R.id.tvSimCash);
        tvHoldingCount = v.findViewById(R.id.tvHoldingCount);
        tvHoldingsEmpty = v.findViewById(R.id.tvHoldingsEmpty);
        tvLiveStatus = v.findViewById(R.id.tvLiveStatus);
        pbLoading = v.findViewById(R.id.pbBrokerageLoading);
    }

    private void setupAssetList(View v) {
        assetAdapter = new AssetAdapter();
        assetAdapter.setOnAssetClickListener(new AssetAdapter.OnAssetClickListener() {
            @Override
            public void onAssetClick(Asset asset) {
                openAssetDetail(asset.symbol);
            }
        });
        RecyclerView rv = v.findViewById(R.id.rvAssets);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setNestedScrollingEnabled(false);
        rv.setAdapter(assetAdapter);
    }

    private void setupHoldingList(View v) {
        holdingAdapter = new HoldingAdapter();
        holdingAdapter.setOnHoldingClickListener(new HoldingAdapter.OnHoldingClickListener() {
            @Override
            public void onHoldingClick(Holding h) {
                openAssetDetail(h.symbol);
            }
        });
        RecyclerView rv = v.findViewById(R.id.rvHoldings);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setNestedScrollingEnabled(false);
        rv.setAdapter(holdingAdapter);
        v.findViewById(R.id.tvSeeAllHoldings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View x) {
            }
        });
    }

    private void setupTabs(View v) {
        TextView tabAll = v.findViewById(R.id.tabAll);
        TextView tabStocks = v.findViewById(R.id.tabStocks);
        TextView tabCrypto = v.findViewById(R.id.tabCrypto);
        TextView tabGainers = v.findViewById(R.id.tabGainers);
        TextView tabETFs = v.findViewById(R.id.tabETFs);

        View.OnClickListener click = new View.OnClickListener() {
            @Override
            public void onClick(View tab) {
                String id;
                if (tab == tabAll) {
                    id = "all";
                } else if (tab == tabStocks) {
                    id = "stocks";
                } else if (tab == tabCrypto) {
                    id = "crypto";
                } else if (tab == tabGainers) {
                    id = "gainers";
                } else {
                    id = "etf";
                }
                activeTab = id;
                setActiveTab(id, tabAll, tabStocks, tabCrypto, tabGainers, tabETFs);
                refreshAssetList();
            }
        };

        tabAll.setOnClickListener(click);
        tabStocks.setOnClickListener(click);
        tabCrypto.setOnClickListener(click);
        tabGainers.setOnClickListener(click);
        tabETFs.setOnClickListener(click);
    }

    private void showLoading(boolean loading) {
        if (pbLoading != null) {
            if (loading) {
                pbLoading.setVisibility(View.VISIBLE);
            } else {
                pbLoading.setVisibility(View.GONE);
            }
        }
        if (tvLiveStatus != null) {
            if (loading) {
                tvLiveStatus.setText(getString(R.string.loading_label));
            } else {
                tvLiveStatus.setText(getString(R.string.live_label));
            }
        }
    }

    private void refreshAll() {
        refreshAssetList();
        refreshHoldings();
        refreshHeader();
    }

    private void refreshAssetList() {
        List<Asset> assets;
        switch (activeTab) {
            case "stocks":
                assets = MarketDataService.getByType(Asset.TYPE_STOCK);
                break;
            case "crypto":
                assets = MarketDataService.getByType(Asset.TYPE_CRYPTO);
                break;
            case "gainers":
                assets = MarketDataService.getTopGainers();
                break;
            case "etf":
                assets = MarketDataService.getByType(Asset.TYPE_ETF);
                break;
            default:
                assets = MarketDataService.getAllAssets();
                break;
        }
        assetAdapter.setItems(assets);
    }

    private void refreshHoldings() {
        List<Holding> holdings = db.getAllHoldings();
        for (Holding h : holdings) {
            Asset a = MarketDataService.getAsset(h.symbol);
            if (a != null && a.price > 0) {
                h.currentPrice = a.price;
            } else {
                h.currentPrice = h.avgBuyPrice;
            }
        }
        holdingAdapter.setItems(holdings);
        boolean empty = holdings.isEmpty();
        if (tvHoldingsEmpty != null) {
            if (empty) {
                tvHoldingsEmpty.setVisibility(View.VISIBLE);
            } else {
                tvHoldingsEmpty.setVisibility(View.GONE);
            }
        }
    }

    private void refreshHeader() {
        double simCash = getSimCash();
        if (tvSimCash != null) {
            tvSimCash.setText(String.format("$%,.2f", simCash));
        }

        List<Holding> holdings = db.getAllHoldings();
        double portfolioValue = 0;
        double portfolioCost = 0;
        for (Holding h : holdings) {
            Asset a = MarketDataService.getAsset(h.symbol);
            double cur;
            if (a != null && a.price > 0) {
                cur = a.price;
            } else {
                cur = h.avgBuyPrice;
            }
            portfolioValue += h.quantity * cur;
            portfolioCost += h.quantity * h.avgBuyPrice;
        }

        if (tvPortfolioValue != null) {
            tvPortfolioValue.setText(String.format("$%,.2f", portfolioValue));
        }

        String holdingCountText;
        if (holdings.size() == 1) {
            holdingCountText = holdings.size() + " Asset";
        } else {
            holdingCountText = holdings.size() + " Assets";
        }
        if (tvHoldingCount != null) {
            tvHoldingCount.setText(holdingCountText);
        }

        if (portfolioCost > 0 && tvPortfolioChange != null) {
            double pnl = portfolioValue - portfolioCost;
            double pnlPct = (pnl / portfolioCost) * 100;
            String pnlStr;
            if (pnl >= 0) {
                pnlStr = "+" + String.format("$%,.2f", Math.abs(pnl));
            } else {
                pnlStr = "-" + String.format("$%,.2f", Math.abs(pnl));
            }
            tvPortfolioChange.setText(String.format("%s  (%+.2f%%)", pnlStr, pnlPct));
            if (pnl >= 0) {
                tvPortfolioChange.setTextColor(ContextCompat.getColor(requireContext(), R.color.gain_green));
            } else {
                tvPortfolioChange.setTextColor(ContextCompat.getColor(requireContext(), R.color.loss_red));
            }
        } else if (tvPortfolioChange != null) {
            tvPortfolioChange.setText("—");
            tvPortfolioChange.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    private void setActiveTab(String active, TextView... tabs) {
        String[] ids = {"all", "stocks", "crypto", "gainers", "etf"};
        for (int i = 0; i < tabs.length; i++) {
            boolean on = ids[i].equals(active);
            if (on) {
                tabs[i].setBackgroundResource(R.drawable.bg_tab_active);
                tabs[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                tabs[i].setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                tabs[i].setBackgroundResource(R.drawable.bg_tab_inactive);
                tabs[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                tabs[i].setTypeface(null, android.graphics.Typeface.NORMAL);
            }
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
