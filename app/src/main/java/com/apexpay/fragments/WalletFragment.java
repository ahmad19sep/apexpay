package com.apexpay.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.apexpay.R;
import com.apexpay.adapters.FrequentContactAdapter;
import com.apexpay.adapters.SubscriptionAdapter;
import com.apexpay.adapters.TransactionAdapter;
import com.apexpay.database.DatabaseHelper;
import com.apexpay.models.FrequentContact;
import com.apexpay.models.Subscription;
import com.apexpay.models.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WalletFragment extends Fragment {

    private static final String[] AVATAR_COLORS = {
        "#E53935", "#8E24AA", "#1E88E5", "#00897B", "#F4511E", "#F9A825", "#6366F1", "#00C896"
    };

    private DatabaseHelper    db;
    private SharedPreferences prefs;
    private boolean           isFrozen = false;
    private View              rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_wallet, container, false);
        db    = new DatabaseHelper(requireContext());
        prefs = requireActivity().getSharedPreferences("ApexPayPrefs",
                android.content.Context.MODE_PRIVATE);

        setupCard();
        setupQuickActions();
        setupContacts();
        setupSubscriptions();
        setupRecentTransactions();
        checkIncomingTransfers();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (rootView != null) {
            refreshBalance();
            setupRecentTransactions();
        }
    }



    private void setupCard() {
        refreshBalance();

        String holderName   = prefs.getString("holderName", "User");
        String accountNumber = prefs.getString("accountNumber", "APX-0000-0000-0000");
        String lastFour     = accountNumber.length() >= 4
                ? accountNumber.substring(accountNumber.length() - 4) : "0000";

        ((TextView) rootView.findViewById(R.id.tvCardHolder))
                .setText(holderName.toUpperCase());
        ((TextView) rootView.findViewById(R.id.tvAccountNumber))
                .setText("APX  ••••  ••••  " + lastFour);
        ((TextView) rootView.findViewById(R.id.tvCardExpiry))
                .setText("12/28");

        rootView.findViewById(R.id.btnFreeze).setOnClickListener(v -> toggleFreeze());
    }

    private void refreshBalance() {
        double balance = getWalletBalance();
        TextView tvBal = rootView.findViewById(R.id.tvWalletBalance);
        if (tvBal != null) tvBal.setText(String.format("$%,.2f", balance));
    }

    private void toggleFreeze() {
        isFrozen = !isFrozen;
        LinearLayout cardContent = rootView.findViewById(R.id.cardContent);
        Button btnFreeze         = rootView.findViewById(R.id.btnFreeze);
        View btnSend             = rootView.findViewById(R.id.btnSendAction);

        cardContent.setBackgroundResource(isFrozen
                ? R.drawable.bg_frozen_card : R.drawable.bg_virtual_card);
        ((Button) btnFreeze).setText(isFrozen ? "✓  Unfreeze Card" : "❅  Freeze Card");
        btnSend.setEnabled(!isFrozen);
        btnSend.setAlpha(isFrozen ? 0.4f : 1.0f);
    }



    private void setupQuickActions() {
        double balance      = getWalletBalance();
        String holderName   = prefs.getString("holderName", "User");
        String accountNumber = prefs.getString("accountNumber", "APX-0000-0000-0000");

        rootView.findViewById(R.id.btnSendAction).setOnClickListener(v -> {
            if (!isFrozen) {
                loadSubFragment(SendMoneyFragment.newInstance(balance, holderName));
            }
        });
        rootView.findViewById(R.id.btnReceiveAction).setOnClickListener(v ->
                loadSubFragment(ReceiveMoneyFragment.newInstance(accountNumber, holderName)));
        rootView.findViewById(R.id.btnTopUpAction).setOnClickListener(v ->
                loadSubFragment(new TopUpFragment()));
        rootView.findViewById(R.id.btnHistoryAction).setOnClickListener(v ->
                loadSubFragment(new TransactionHistoryFragment()));
    }


    private void setupContacts() {
        List<FrequentContact> contacts = db.getContacts();
        if (contacts.isEmpty()) {
            return;
        }

        FrequentContactAdapter adapter = new FrequentContactAdapter(contacts);
        adapter.setOnContactClickListener(contact -> loadSubFragment(
                SendMoneyFragment.newInstanceWithContact(
                        getWalletBalance(),
                        prefs.getString("holderName", "User"),
                        contact.name,
                        contact.accountNumber)));

        RecyclerView rv = rootView.findViewById(R.id.rvContacts);
        rv.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL, false));
        rv.setAdapter(adapter);
    }


    private void setupSubscriptions() {
        List<Subscription> subs = Arrays.asList(
                new Subscription("Netflix",         "🎬", 12.99, "May 15, 2026"),
                new Subscription("Spotify",         "🎵",  9.99, "May 18, 2026"),
                new Subscription("YouTube Premium", "▶",  13.99, "May 22, 2026"),
                new Subscription("iCloud Storage",  "☁",   2.99, "May 30, 2026")
        );
        RecyclerView rv = rootView.findViewById(R.id.rvSubscriptions);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setNestedScrollingEnabled(false);
        rv.setAdapter(new SubscriptionAdapter(subs));
    }


    private void setupRecentTransactions() {
        List<Transaction> txns = db.getRecentLedger(5);

        rootView.findViewById(R.id.tvSeeAllWallet).setOnClickListener(v ->
                loadSubFragment(new TransactionHistoryFragment()));

        RecyclerView rv = rootView.findViewById(R.id.rvWalletTransactions);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setNestedScrollingEnabled(false);
        rv.setAdapter(new TransactionAdapter(txns));
    }


    private void checkIncomingTransfers() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String myAccountNumber = prefs.getString("accountNumber", "");
        if (myAccountNumber.isEmpty() || myAccountNumber.equals("APX-0000-0000-0000")) return;

        FirebaseFirestore.getInstance()
                .collection("transfers")
                .whereEqualTo("toAccountNumber", myAccountNumber)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean received = false;
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Double amountObj = doc.getDouble("amount");
                        String fromName  = doc.getString("fromName");
                        if (amountObj == null || amountObj <= 0) continue;

                        double amount = amountObj;
                        String senderName = (fromName != null && !fromName.isEmpty())
                                ? fromName : "ApexPay User";


                        double newBal = getWalletBalance() + amount;
                        saveWalletBalance(newBal);


                        db.insertLedger("💰", "Received from " + senderName, amount, true);


                        doc.getReference().update("status", "claimed");


                        String fromAccount = doc.getString("fromAccountNumber");
                        if (fromAccount != null && !fromAccount.isEmpty()) {
                            db.upsertContact(senderName, fromAccount,
                                    AVATAR_COLORS[(int)(Math.random() * AVATAR_COLORS.length)]);
                        }

                        received = true;
                    }

                    if (received && isAdded()) {
                        refreshBalance();
                        setupRecentTransactions();
                        Toast.makeText(requireContext(),
                                "💰 You have incoming transfers!",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }


    private double getWalletBalance() {
        return Double.longBitsToDouble(
                prefs.getLong("walletBalance", Double.doubleToLongBits(0.0)));
    }

    private void saveWalletBalance(double v) {
        prefs.edit().putLong("walletBalance", Double.doubleToLongBits(v)).apply();
    }

    private void loadSubFragment(Fragment fragment) {
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
