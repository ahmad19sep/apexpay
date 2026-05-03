package com.apexpay.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.apexpay.MainActivity;
import com.apexpay.R;
import com.apexpay.adapters.TransactionAdapter;
import com.apexpay.database.DatabaseHelper;
import com.apexpay.models.Asset;
import com.apexpay.models.Holding;
import com.apexpay.models.Transaction;
import com.apexpay.services.MarketDataService;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String ARG_EMAIL = "email";

    private View rootView;
    private DatabaseHelper db;
    private SharedPreferences prefs;
    private PieChart pieChart;
    private BarChart barChart;

    public static HomeFragment newInstance(String email) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home, container, false);
        db = new DatabaseHelper(requireContext());
        prefs = requireActivity().getSharedPreferences("ApexPayPrefs", android.content.Context.MODE_PRIVATE);

        String email;
        if (getArguments() != null) {
            email = getArguments().getString(ARG_EMAIL, "");
        } else {
            email = "";
        }
        String name;
        if (email.contains("@")) {
            name = email.substring(0, email.indexOf("@"));
        } else {
            name = email;
        }
        String saved = prefs.getString("holderName", name);
        String displayName;
        if (saved != null && !saved.isEmpty()) {
            displayName = saved;
        } else {
            displayName = capitalize(name);
        }

        rootView.<TextView>findViewById(R.id.tvGreeting).setText(getGreeting());
        rootView.<TextView>findViewById(R.id.tvUserName).setText(displayName);

        pieChart = rootView.findViewById(R.id.pieChartAllocation);
        barChart = rootView.findViewById(R.id.barChartFlow);

        setupQuickActions();
        setupNavigation();
        refreshData();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (rootView != null) {
            refreshData();
        }
    }

    private void refreshData() {
        double walletCash = getPrefsDouble("walletBalance", 0.0);
        double simCash = getPrefsDouble("simCash", 10000.0);
        double portfolioValue = computePortfolioValue();
        double totalNetWorth = walletCash + simCash + portfolioValue;

        rootView.<TextView>findViewById(R.id.tvBalance)
                .setText(String.format("$%,.2f", totalNetWorth));
        rootView.<TextView>findViewById(R.id.tvIncome)
                .setText(String.format("$%,.2f", walletCash));
        rootView.<TextView>findViewById(R.id.tvExpenses)
                .setText(String.format("$%,.2f", portfolioValue));

        refreshTransactions();
        renderAllocationChart(walletCash, portfolioValue);
        renderCashFlowChart();
    }

    private double computePortfolioValue() {
        double total = 0;
        for (Holding h : db.getAllHoldings()) {
            Asset a = MarketDataService.getAsset(h.symbol);
            if (a != null) {
                double price;
                if (a.price > 0) {
                    price = a.price;
                } else {
                    price = h.avgBuyPrice;
                }
                total += h.quantity * price;
            }
        }
        return total;
    }

    private void refreshTransactions() {
        List<Transaction> items = db.getRecentLedger(5);
        RecyclerView rv = rootView.findViewById(R.id.rvTransactions);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new TransactionAdapter(items));
    }

    private void renderAllocationChart(double cashBalance, double portfolioValue) {
        if (pieChart == null) {
            return;
        }

        double stocks = 0;
        double crypto = 0;
        for (Holding h : db.getAllHoldings()) {
            Asset a = MarketDataService.getAsset(h.symbol);
            double val;
            if (a != null && a.price > 0) {
                val = h.quantity * a.price;
            } else {
                val = h.quantity * h.avgBuyPrice;
            }
            if (Asset.TYPE_CRYPTO.equals(h.assetType)) {
                crypto += val;
            } else {
                stocks += val;
            }
        }

        double total = cashBalance + portfolioValue;
        if (total <= 0) {
            total = 1;
        }

        List<PieEntry> entries = new ArrayList<>();
        if (stocks > 0) {
            entries.add(new PieEntry((float) (stocks / total * 100), "Stocks"));
        }
        if (crypto > 0) {
            entries.add(new PieEntry((float) (crypto / total * 100), "Crypto"));
        }
        if (cashBalance > 0) {
            entries.add(new PieEntry((float) (cashBalance / total * 100), "Cash"));
        }
        if (entries.isEmpty()) {
            entries.add(new PieEntry(100f, "No data"));
        }

        PieDataSet dataset = new PieDataSet(entries, "");
        dataset.setColors(0x886366F1, 0x8800C896, 0x888892B0, 0xAAFF5252);
        dataset.setDrawValues(false);
        dataset.setSliceSpace(2f);

        PieData data = new PieData(dataset);

        pieChart.setData(data);
        pieChart.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(55f);
        pieChart.setTransparentCircleRadius(60f);
        pieChart.setHoleColor(ContextCompat.getColor(requireContext(), R.color.surface));
        pieChart.setTransparentCircleColor(ContextCompat.getColor(requireContext(), R.color.surface));
        pieChart.setCenterText(String.format("$%,.0f", total));
        pieChart.setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        pieChart.setCenterTextSize(12f);
        pieChart.setRotationEnabled(false);
        pieChart.setTouchEnabled(false);
        pieChart.animateY(600);
        pieChart.invalidate();
    }

    private void renderCashFlowChart() {
        if (barChart == null) {
            return;
        }

        float[][] flow = db.getMonthlyCashFlow(6);

        List<BarEntry> inEntries = new ArrayList<>();
        List<BarEntry> outEntries = new ArrayList<>();

        for (int i = 0; i < flow.length; i++) {
            inEntries.add(new BarEntry(i, flow[i][0]));
            outEntries.add(new BarEntry(i, flow[i][1]));
        }

        BarDataSet inSet = new BarDataSet(inEntries, "In");
        BarDataSet outSet = new BarDataSet(outEntries, "Out");

        inSet.setColor(ContextCompat.getColor(requireContext(), R.color.primary));
        inSet.setDrawValues(false);
        outSet.setColor(ContextCompat.getColor(requireContext(), R.color.loss_red));
        outSet.setDrawValues(false);

        BarData barData = new BarData(inSet, outSet);
        float groupSpace = 0.2f;
        float barSpace = 0.02f;
        float barWidth = 0.38f;
        barData.setBarWidth(barWidth);

        barChart.setData(barData);
        barChart.groupBars(0f, groupSpace, barSpace);
        barChart.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawGridBackground(false);

        XAxis x = barChart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        x.setTextSize(9f);
        x.setDrawLabels(false);
        x.setAxisMinimum(0f);
        x.setAxisMaximum(6f);

        YAxis left = barChart.getAxisLeft();
        left.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        left.setGridColor(ContextCompat.getColor(requireContext(), R.color.divider));
        left.setTextSize(9f);
        left.setAxisMinimum(0f);
        barChart.getAxisRight().setEnabled(false);

        barChart.setTouchEnabled(false);
        barChart.animateY(500);
        barChart.invalidate();
    }

    private void setupQuickActions() {
        rootView.findViewById(R.id.btnSend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateTo(new SendMoneyFragment());
            }
        });

        rootView.findViewById(R.id.btnReceive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateTo(new ReceiveMoneyFragment());
            }
        });

        rootView.findViewById(R.id.btnTopUp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateTo(new TopUpFragment());
            }
        });

        rootView.findViewById(R.id.btnScan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateTo(new TransactionHistoryFragment());
            }
        });
    }

    private void setupNavigation() {
        rootView.findViewById(R.id.tvSeeAll).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateTo(new TransactionHistoryFragment());
            }
        });

        rootView.findViewById(R.id.ivNotifications).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), getString(R.string.toast_no_notifications), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateTo(Fragment fragment) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadFragment(fragment);
        }
    }

    private double getPrefsDouble(String key, double defaultValue) {
        return Double.longBitsToDouble(
                prefs.getLong(key, Double.doubleToLongBits(defaultValue)));
    }

    private String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            return getString(R.string.greeting_morning);
        }
        if (hour < 17) {
            return getString(R.string.greeting_afternoon);
        }
        return getString(R.string.greeting_evening);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
