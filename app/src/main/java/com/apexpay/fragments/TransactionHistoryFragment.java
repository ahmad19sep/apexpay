package com.apexpay.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.apexpay.R;
import com.apexpay.adapters.TransactionAdapter;
import com.apexpay.models.Transaction;

import java.util.ArrayList;

public class TransactionHistoryFragment extends Fragment {

    private ArrayList<Transaction> allTransactions;
    private TransactionAdapter adapter;
    private int selectedFilter = 0; // 0=All, 1=Sent, 2=Received, 3=Deposits
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_transaction_history, container, false);

        rootView.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        buildTransactionList();
        setupFilterChips();
        setupRecyclerView();

        return rootView;
    }

    private void buildTransactionList() {
        allTransactions = new ArrayList<>();
        allTransactions.add(new Transaction("💸", "Sent to Ali",           "Apr 30, 2026", "-$200.00", false));
        allTransactions.add(new Transaction("💰", "Received from Sara",    "Apr 29, 2026", "+$150.00", true));
        allTransactions.add(new Transaction("🏦", "Bank Top-Up",           "Apr 28, 2026", "+$500.00", true));
        allTransactions.add(new Transaction("🛒", "Market Pay",            "Apr 27, 2026",  "-$45.50", false));
        allTransactions.add(new Transaction("💸", "Sent to Hana",          "Apr 26, 2026", "-$300.00", false));
        allTransactions.add(new Transaction("💰", "Received from Omar",    "Apr 25, 2026", "+$250.00", true));
        allTransactions.add(new Transaction("🏦", "Bank Top-Up",           "Apr 24, 2026", "+$200.00", true));
        allTransactions.add(new Transaction("💸", "Sent to Zaid",          "Apr 23, 2026",  "-$75.00", false));
        allTransactions.add(new Transaction("💰", "Received from Noor",    "Apr 22, 2026", "+$100.00", true));
        allTransactions.add(new Transaction("🛒", "Online Purchase",       "Apr 21, 2026",  "-$29.99", false));
        allTransactions.add(new Transaction("💸", "Sent to Ali",           "Apr 20, 2026", "-$150.00", false));
        allTransactions.add(new Transaction("🏦", "Salary Deposit",        "Apr 15, 2026", "+$3,500.00", true));
    }

    private void setupFilterChips() {
        int[] chipIds = {R.id.chipAll, R.id.chipSent, R.id.chipReceived, R.id.chipDeposits};

        for (int i = 0; i < chipIds.length; i++) {
            final int filter = i;
            TextView chip = rootView.findViewById(chipIds[i]);
            chip.setOnClickListener(v -> {
                selectedFilter = filter;
                updateChipStyles(chipIds, filter, rootView);
                applyFilter();
            });
        }
        updateChipStyles(chipIds, 0, rootView);
    }

    private void updateChipStyles(int[] chipIds, int selected, View root) {
        for (int i = 0; i < chipIds.length; i++) {
            TextView chip = root.findViewById(chipIds[i]);
            if (i == selected) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected);
                chip.setTextColor(Color.WHITE);
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected);
                chip.setTextColor(Color.parseColor("#757575"));
            }
        }
    }

    private void setupRecyclerView() {
        ArrayList<Transaction> filtered = getFiltered();
        adapter = new TransactionAdapter(filtered);
        RecyclerView rv = rootView.findViewById(R.id.rvAllTransactions);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);
    }

    private void applyFilter() {
        ArrayList<Transaction> filtered = getFiltered();
        RecyclerView rv = rootView.findViewById(R.id.rvAllTransactions);
        rv.setAdapter(new TransactionAdapter(filtered));
    }

    private ArrayList<Transaction> getFiltered() {
        ArrayList<Transaction> result = new ArrayList<>();
        for (Transaction t : allTransactions) {
            if (selectedFilter == 0) {
                result.add(t);
            } else if (selectedFilter == 1 && !t.isCredit) {
                result.add(t);
            } else if (selectedFilter == 2 && t.isCredit && !t.title.contains("Deposit") && !t.title.contains("Top-Up")) {
                result.add(t);
            } else if (selectedFilter == 3 && t.isCredit && (t.title.contains("Deposit") || t.title.contains("Top-Up"))) {
                result.add(t);
            }
        }
        return result;
    }
}