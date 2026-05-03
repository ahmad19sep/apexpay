package com.apexpay.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.apexpay.R;
import com.apexpay.database.DatabaseHelper;
import com.apexpay.models.Asset;
import com.apexpay.models.Holding;
import com.apexpay.services.MarketDataService;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private DatabaseHelper db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new DatabaseHelper(requireContext());

        view.findViewById(R.id.btnDashBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        buildSummaryCards(view);
        buildDonutChart(view);
        buildCashFlowChart(view);
    }

    private void buildSummaryCards(View v) {
        List<Holding> holdings = db.getAllHoldings();
        double totalValue = 0;
        double totalCost = 0;

        for (Holding h : holdings) {
            Asset a = MarketDataService.getAsset(h.symbol);
            double price;
            if (a != null && a.price > 0) {
                price = a.price;
            } else {
                price = h.avgBuyPrice;
            }
            totalValue += h.quantity * price;
            totalCost += h.quantity * h.avgBuyPrice;
        }

        double pnl = totalValue - totalCost;
        double pnlPct;
        if (totalCost > 0) {
            pnlPct = (pnl / totalCost) * 100;
        } else {
            pnlPct = 0;
        }

        ((TextView) v.findViewById(R.id.tvDashPortfolioValue))
                .setText(String.format("$%,.2f", totalValue));
        ((TextView) v.findViewById(R.id.tvDashInvested))
                .setText(String.format("$%,.2f", totalCost));

        TextView tvPnl = v.findViewById(R.id.tvDashPnl);
        String pnlSign;
        if (pnl >= 0) {
            pnlSign = "+";
        } else {
            pnlSign = "-";
        }
        String pnlStr = pnlSign + String.format("$%,.2f", Math.abs(pnl));
        tvPnl.setText(String.format("%s  (%+.1f%%)", pnlStr, pnlPct));
        if (pnl >= 0) {
            tvPnl.setTextColor(ContextCompat.getColor(requireContext(), R.color.gain_green));
        } else {
            tvPnl.setTextColor(ContextCompat.getColor(requireContext(), R.color.loss_red));
        }

        ((TextView) v.findViewById(R.id.tvDashAssetCount))
                .setText(holdings.size() + " positions");
    }

    private void buildDonutChart(View v) {
        PieChart chart = v.findViewById(R.id.pieChart);
        List<Holding> holdings = db.getAllHoldings();

        if (holdings.isEmpty()) {
            chart.setNoDataText(getString(R.string.no_data_chart));
            chart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            chart.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        int[] palette = {
            0xFF6366F1, 0xFF00C896, 0xFFFF5252, 0xFFFFB347,
            0xFF48CAE4, 0xFFE040FB, 0xFF69F0AE, 0xFFFF6E40,
            0xFFB2EBF2, 0xFFCCFF90
        };

        double total = 0;
        for (Holding h : holdings) {
            Asset a = MarketDataService.getAsset(h.symbol);
            double price;
            if (a != null && a.price > 0) {
                price = a.price;
            } else {
                price = h.avgBuyPrice;
            }
            total += h.quantity * price;
        }

        List<Integer> colors = new ArrayList<>();
        int ci = 0;
        for (Holding h : holdings) {
            Asset a = MarketDataService.getAsset(h.symbol);
            double price;
            if (a != null && a.price > 0) {
                price = a.price;
            } else {
                price = h.avgBuyPrice;
            }
            double val = h.quantity * price;
            if (val > 0) {
                entries.add(new PieEntry((float) val, h.symbol));
                colors.add(palette[ci % palette.length]);
                ci++;
            }
        }

        PieDataSet ds = new PieDataSet(entries, "");
        ds.setColors(colors);
        ds.setSliceSpace(2f);
        ds.setSelectionShift(5f);
        ds.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        ds.setValueTextSize(11f);

        PieData pd = new PieData(ds);

        chart.setData(pd);
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(55f);
        chart.setTransparentCircleRadius(60f);
        chart.setHoleColor(ContextCompat.getColor(requireContext(), R.color.background));
        chart.setTransparentCircleColor(ContextCompat.getColor(requireContext(), R.color.background));
        chart.setCenterText(getString(R.string.portfolio_center_text));
        chart.setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        chart.setCenterTextSize(14f);
        chart.setDrawEntryLabels(true);
        chart.setEntryLabelColor(ContextCompat.getColor(requireContext(), R.color.white));
        chart.setEntryLabelTextSize(10f);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        chart.setRotationEnabled(false);
        chart.animateY(600);
        chart.invalidate();
    }

    private void buildCashFlowChart(View v) {
        BarChart chart = v.findViewById(R.id.barChartCashFlow);
        int months = 6;
        float[][] data = db.getMonthlyCashFlow(months);

        List<BarEntry> inEntries = new ArrayList<>();
        List<BarEntry> outEntries = new ArrayList<>();
        String[] labels = new String[months];

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -(months - 1));
        SimpleDateFormat sdf = new SimpleDateFormat("MMM", Locale.getDefault());

        for (int i = 0; i < months; i++) {
            inEntries.add(new BarEntry(i * 2f, data[i][0]));
            outEntries.add(new BarEntry(i * 2f + 0.8f, data[i][1]));
            labels[i] = sdf.format(cal.getTime());
            cal.add(Calendar.MONTH, 1);
        }

        BarDataSet dsIn = new BarDataSet(inEntries, "Money In");
        BarDataSet dsOut = new BarDataSet(outEntries, "Money Out");
        dsIn.setColor(ContextCompat.getColor(requireContext(), R.color.gain_green));
        dsOut.setColor(ContextCompat.getColor(requireContext(), R.color.loss_red));
        dsIn.setDrawValues(false);
        dsOut.setDrawValues(false);

        BarData bd = new BarData(dsIn, dsOut);
        bd.setBarWidth(0.7f);

        chart.setData(bd);
        chart.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.getLegend().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        chart.getLegend().setTextSize(11f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        xAxis.setLabelCount(months);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        chart.getAxisLeft().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        chart.getAxisLeft().setGridColor(ContextCompat.getColor(requireContext(), R.color.divider));
        chart.getAxisRight().setEnabled(false);
        chart.animateY(500);
        chart.invalidate();
    }
}
