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
import com.apexpay.database.DatabaseHelper;
import com.apexpay.models.Transaction;

import java.util.ArrayList;
import java.util.List;

public class TransactionHistoryFragment extends Fragment {

    private DatabaseHelper      db;
    private List<Transaction>   allTransactions;
    private int                 selectedFilter = 0; // 0=All, 1=Sent, 2=Received, 3=Deposits
    private View                rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_transaction_history, container, false);
        db = new DatabaseHelper(requireContext());

        rootView.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        loadFromDatabase();
        setupFilterChips();
        setupRecyclerView();

        return rootView;
    }

    private void loadFromDatabase() {
        allTransactions = db.getAllLedger();
    }

    private void setupFilterChips() {
        int[] chipIds = {R.id.chipAll, R.id.chipSent, R.id.chipReceived, R.id.chipDeposits};
        for (int i = 0; i < chipIds.length; i++) {
            final int filter = i;
            TextView chip = rootView.findViewById(chipIds[i]);
            chip.setOnClickListener(v -> {
                selectedFilter = filter;
                updateChipStyles(chipIds, filter);
                applyFilter();
            });
        }
        updateChipStyles(chipIds, 0);
    }

    private void updateChipStyles(int[] chipIds, int selected) {
        for (int i = 0; i < chipIds.length; i++) {
            TextView chip = rootView.findViewById(chipIds[i]);
            if (i == selected) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected);
                chip.setTextColor(Color.WHITE);
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected);
                chip.setTextColor(Color.parseColor("#8892B0"));
            }
        }
    }

    private void setupRecyclerView() {
        RecyclerView rv = rootView.findViewById(R.id.rvAllTransactions);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new TransactionAdapter(getFiltered()));
    }

    private void applyFilter() {
        RecyclerView rv = rootView.findViewById(R.id.rvAllTransactions);
        rv.setAdapter(new TransactionAdapter(getFiltered()));
    }

    private List<Transaction> getFiltered() {
        List<Transaction> result = new ArrayList<>();
        for (Transaction t : allTransactions) {
            switch (selectedFilter) {
                case 0: result.add(t); break;
                case 1: if (!t.isCredit) result.add(t); break;
                case 2: if (t.isCredit && !isDeposit(t.title)) result.add(t); break;
                case 3: if (t.isCredit && isDeposit(t.title)) result.add(t); break;
            }
        }
        return result;
    }

    private boolean isDeposit(String title) {
        String lower = title.toLowerCase();
        return lower.contains("top-up") || lower.contains("topup")
                || lower.contains("deposit") || lower.contains("bank");
    }
}
