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

import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String ARG_EMAIL = "email";

    private View              rootView;
    private DatabaseHelper    db;
    private SharedPreferences prefs;

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
        db       = new DatabaseHelper(requireContext());
        prefs    = requireActivity().getSharedPreferences("ApexPayPrefs", android.content.Context.MODE_PRIVATE);

        String email = getArguments() != null ? getArguments().getString(ARG_EMAIL, "") : "";
        String name  = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;

        rootView.<TextView>findViewById(R.id.tvGreeting).setText(getGreeting());
        rootView.<TextView>findViewById(R.id.tvUserName).setText(capitalize(name));

        setupQuickActions();
        setupNavigation();
        refreshData();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (rootView != null) refreshData();
    }


    private void refreshData() {
        double walletCash     = getPrefsDouble("walletBalance", 0.0);
        double simCash        = getPrefsDouble("simCash",       10000.0);
        double portfolioValue = computePortfolioValue();
        double totalNetWorth  = walletCash + simCash + portfolioValue;

        rootView.<TextView>findViewById(R.id.tvBalance)
                .setText(String.format("$%,.2f", totalNetWorth));
        rootView.<TextView>findViewById(R.id.tvIncome)
                .setText(String.format("$%,.2f", walletCash + simCash));
        rootView.<TextView>findViewById(R.id.tvExpenses)
                .setText(String.format("$%,.2f", portfolioValue));

        refreshTransactions();
    }

    private double computePortfolioValue() {
        double total = 0;
        for (Holding h : db.getAllHoldings()) {
            Asset a = MarketDataService.getAsset(h.symbol);
            if (a != null) total += h.quantity * a.price;
        }
        return total;
    }

    private void refreshTransactions() {
        List<Transaction> items = db.getRecentLedger(5);

        RecyclerView rv = rootView.findViewById(R.id.rvTransactions);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new TransactionAdapter(items));
    }



    private void setupQuickActions() {
        rootView.findViewById(R.id.btnSend).setOnClickListener(v ->
                navigateTo(new SendMoneyFragment()));
        rootView.findViewById(R.id.btnReceive).setOnClickListener(v ->
                navigateTo(new ReceiveMoneyFragment()));
        rootView.findViewById(R.id.btnTopUp).setOnClickListener(v ->
                navigateTo(new TopUpFragment()));
        rootView.findViewById(R.id.btnScan).setOnClickListener(v ->
                navigateTo(new TransactionHistoryFragment()));
    }

    private void setupNavigation() {
        rootView.findViewById(R.id.tvSeeAll).setOnClickListener(v ->
                navigateTo(new TransactionHistoryFragment()));
        rootView.findViewById(R.id.ivNotifications).setOnClickListener(v ->
                Toast.makeText(getContext(), "No new notifications", Toast.LENGTH_SHORT).show());
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
        if (hour < 12) return "Good Morning,";
        if (hour < 17) return "Good Afternoon,";
        return "Good Evening,";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
