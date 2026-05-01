package com.apexpay.fragments;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.apexpay.R;
import com.apexpay.adapters.FrequentContactAdapter;
import com.apexpay.adapters.SubscriptionAdapter;
import com.apexpay.adapters.TransactionAdapter;
import com.apexpay.models.FrequentContact;
import com.apexpay.models.Subscription;
import com.apexpay.models.Transaction;
import com.apexpay.models.Wallet;

import java.util.ArrayList;

public class WalletFragment extends Fragment {

    private static final String ARG_EMAIL = "email";

    private Wallet wallet;
    private boolean isFrozen = false;
    private View rootView;

    public static WalletFragment newInstance(String email) {
        WalletFragment fragment = new WalletFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_wallet, container, false);

        String email = getArguments() != null ? getArguments().getString(ARG_EMAIL, "") : "";
        String name  = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
        String displayName = capitalize(name);

        wallet = new Wallet(displayName, "APX-7823-4521-9901", 12450.00, "12/28");

        setupCard();
        setupQuickActions();
        setupFrequentContacts();
        setupSubscriptions();
        setupRecentTransactions();

        return rootView;
    }

    private void setupCard() {
        TextView tvBalance = rootView.findViewById(R.id.tvWalletBalance);
        tvBalance.setText(String.format("$%,.2f", wallet.balance));

        TextView tvHolder = rootView.findViewById(R.id.tvCardHolder);
        tvHolder.setText(wallet.holderName.toUpperCase());

        TextView tvAccount = rootView.findViewById(R.id.tvAccountNumber);
        tvAccount.setText("APX  ••••  ••••  9901");

        TextView tvExpiry = rootView.findViewById(R.id.tvCardExpiry);
        tvExpiry.setText(wallet.cardExpiry);

        Button btnFreeze = rootView.findViewById(R.id.btnFreeze);
        btnFreeze.setOnClickListener(v -> toggleFreeze());
    }

    private void toggleFreeze() {
        isFrozen = !isFrozen;
        LinearLayout cardContent = rootView.findViewById(R.id.cardContent);
        Button btnFreeze         = rootView.findViewById(R.id.btnFreeze);
        View btnSend             = rootView.findViewById(R.id.btnSendAction);

        cardContent.setBackgroundResource(isFrozen
                ? R.drawable.bg_frozen_card
                : R.drawable.bg_virtual_card);
        btnFreeze.setText(isFrozen ? "✓  Unfreeze Card" : "❅  Freeze Card");
        btnSend.setEnabled(!isFrozen);
        btnSend.setAlpha(isFrozen ? 0.4f : 1.0f);
    }

    private void setupQuickActions() {
        rootView.findViewById(R.id.btnSendAction).setOnClickListener(v -> {
            if (!isFrozen) {
                loadSubFragment(SendMoneyFragment.newInstance(wallet.balance, wallet.holderName));
            }
        });
        rootView.findViewById(R.id.btnReceiveAction).setOnClickListener(v ->
                loadSubFragment(ReceiveMoneyFragment.newInstance(wallet.accountNumber, wallet.holderName)));
        rootView.findViewById(R.id.btnTopUpAction).setOnClickListener(v ->
                loadSubFragment(new TopUpFragment()));
        rootView.findViewById(R.id.btnHistoryAction).setOnClickListener(v ->
                loadSubFragment(new TransactionHistoryFragment()));
    }

    private void loadSubFragment(Fragment fragment) {
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void setupFrequentContacts() {
        ArrayList<FrequentContact> contacts = new ArrayList<>();
        contacts.add(new FrequentContact("Ali",   "AL", "APX-1234-5678-0001", "#E53935"));
        contacts.add(new FrequentContact("Sara",  "SA", "APX-2345-6789-0002", "#8E24AA"));
        contacts.add(new FrequentContact("Omar",  "OM", "APX-3456-7890-0003", "#1E88E5"));
        contacts.add(new FrequentContact("Hana",  "HA", "APX-4567-8901-0004", "#00897B"));
        contacts.add(new FrequentContact("Zaid",  "ZA", "APX-5678-9012-0005", "#F4511E"));
        contacts.add(new FrequentContact("Noor",  "NO", "APX-6789-0123-0006", "#F9A825"));

        FrequentContactAdapter adapter = new FrequentContactAdapter(contacts);
        adapter.setOnContactClickListener(contact -> {
            loadSubFragment(SendMoneyFragment.newInstanceWithContact(
                    wallet.balance, wallet.holderName,
                    contact.name, contact.accountNumber));
        });

        RecyclerView rv = rootView.findViewById(R.id.rvContacts);
        rv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rv.setAdapter(adapter);
    }

    private void setupSubscriptions() {
        ArrayList<Subscription> subscriptions = new ArrayList<>();
        subscriptions.add(new Subscription("Netflix",          "🎬", 12.99, "May 15, 2026"));
        subscriptions.add(new Subscription("Spotify",          "🎵",  9.99, "May 18, 2026"));
        subscriptions.add(new Subscription("YouTube Premium",  "▶",       13.99, "May 22, 2026"));
        subscriptions.add(new Subscription("iCloud Storage",   "☁",        2.99, "May 30, 2026"));

        RecyclerView rv = rootView.findViewById(R.id.rvSubscriptions);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setNestedScrollingEnabled(false);
        rv.setAdapter(new SubscriptionAdapter(subscriptions));
    }

    private void setupRecentTransactions() {
        ArrayList<Transaction> transactions = new ArrayList<>();
        transactions.add(new Transaction("💸", "Sent to Ali",          "Apr 30, 2026", "-$200.00", false));
        transactions.add(new Transaction("💰", "Received from Sara",   "Apr 29, 2026", "+$150.00", true));
        transactions.add(new Transaction("🏦", "Bank Top-Up",          "Apr 28, 2026", "+$500.00", true));
        transactions.add(new Transaction("🛒", "Market Pay",           "Apr 27, 2026",  "-$45.50", false));
        transactions.add(new Transaction("💸", "Sent to Hana",         "Apr 26, 2026", "-$300.00", false));

        TextView tvSeeAll = rootView.findViewById(R.id.tvSeeAllWallet);
        tvSeeAll.setOnClickListener(v -> loadSubFragment(new TransactionHistoryFragment()));

        RecyclerView rv = rootView.findViewById(R.id.rvWalletTransactions);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setNestedScrollingEnabled(false);
        rv.setAdapter(new TransactionAdapter(transactions));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}