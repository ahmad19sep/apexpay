package com.apexpay.fragments;

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

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String ARG_EMAIL = "email";

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
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        String email = getArguments() != null ? getArguments().getString(ARG_EMAIL, "") : "";
        String name = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;

        TextView tvGreeting = view.findViewById(R.id.tvGreeting);
        TextView tvUserName = view.findViewById(R.id.tvUserName);
        tvGreeting.setText(getGreeting());
        tvUserName.setText(capitalize(name));

        setupTransactions(view);
        return view;
    }

    private void setupTransactions(View view) {
        List<Transaction> transactions = Arrays.asList(
                new Transaction("💼", "Salary Received",   "Apr 28, 2026", "+$3,500.00", true),
                new Transaction("🛒", "Grocery Store",     "Apr 27, 2026",   "-$85.00", false),
                new Transaction("🎬", "Netflix",           "Apr 26, 2026",   "-$12.99", false),
                new Transaction("↗️", "Transfer to Ali",   "Apr 25, 2026",  "-$200.00", false),
                new Transaction("💻", "Freelance Project", "Apr 24, 2026",  "+$450.00", true)
        );

        RecyclerView rv = view.findViewById(R.id.rvTransactions);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new TransactionAdapter(transactions));
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
