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
import java.util.HashMap;
import java.util.List;

public class WalletFragment extends Fragment {

    private DatabaseHelper db;
    private SharedPreferences prefs;
    private boolean isFrozen = false;
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_wallet, container, false);
        db = new DatabaseHelper(requireContext());
        prefs = requireActivity().getSharedPreferences("ApexPayPrefs",
                android.content.Context.MODE_PRIVATE);

        setupCard();
        setupQuickActions();
        setupContacts();
        setupSubscriptions();
        setupRecentTransactions();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (rootView != null) {
            refreshBalance();
            setupRecentTransactions();
            checkIncomingTransfers();
        }
    }

    private void setupCard() {
        refreshBalance();

        String holderName = prefs.getString("holderName", "User");
        String accountNumber = prefs.getString("accountNumber", "APX-0000-0000-0000");
        String lastFour;
        if (accountNumber.length() >= 4) {
            lastFour = accountNumber.substring(accountNumber.length() - 4);
        } else {
            lastFour = "0000";
        }

        ((TextView) rootView.findViewById(R.id.tvCardHolder))
                .setText(holderName.toUpperCase());
        ((TextView) rootView.findViewById(R.id.tvAccountNumber))
                .setText(getString(R.string.card_account_mask_prefix) + lastFour);
        ((TextView) rootView.findViewById(R.id.tvCardExpiry))
                .setText(getString(R.string.card_expiry));

        rootView.findViewById(R.id.btnFreeze).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFreeze();
            }
        });
    }

    private void refreshBalance() {
        double balance = getWalletBalance();
        TextView tvBal = rootView.findViewById(R.id.tvWalletBalance);
        if (tvBal != null) {
            tvBal.setText(String.format("$%,.2f", balance));
        }
    }

    private void toggleFreeze() {
        isFrozen = !isFrozen;
        LinearLayout cardContent = rootView.findViewById(R.id.cardContent);
        Button btnFreeze = rootView.findViewById(R.id.btnFreeze);
        View btnSend = rootView.findViewById(R.id.btnSendAction);

        if (isFrozen) {
            cardContent.setBackgroundResource(R.drawable.bg_frozen_card);
        } else {
            cardContent.setBackgroundResource(R.drawable.bg_virtual_card);
        }

        if (isFrozen) {
            btnFreeze.setText(getString(R.string.unfreeze_card_label));
        } else {
            btnFreeze.setText(getString(R.string.freeze_card_label));
        }

        btnSend.setEnabled(!isFrozen);
        if (isFrozen) {
            btnSend.setAlpha(0.4f);
        } else {
            btnSend.setAlpha(1.0f);
        }
    }

    private void setupQuickActions() {
        double balance = getWalletBalance();
        String holderName = prefs.getString("holderName", "User");
        String accountNumber = prefs.getString("accountNumber", "APX-0000-0000-0000");

        final double finalBalance = balance;
        final String finalHolderName = holderName;
        final String finalAccountNumber = accountNumber;

        rootView.findViewById(R.id.btnSendAction).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isFrozen) {
                    loadSubFragment(SendMoneyFragment.newInstance(finalBalance, finalHolderName));
                }
            }
        });

        rootView.findViewById(R.id.btnReceiveAction).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadSubFragment(ReceiveMoneyFragment.newInstance(finalAccountNumber, finalHolderName));
            }
        });

        rootView.findViewById(R.id.btnTopUpAction).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadSubFragment(new TopUpFragment());
            }
        });

        rootView.findViewById(R.id.btnHistoryAction).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadSubFragment(new TransactionHistoryFragment());
            }
        });
    }

    private void setupContacts() {
        List<FrequentContact> contacts = db.getContacts();
        if (contacts.isEmpty()) {
            return;
        }

        FrequentContactAdapter adapter = new FrequentContactAdapter(contacts);
        adapter.setOnContactClickListener(new FrequentContactAdapter.OnContactClickListener() {
            @Override
            public void onContactClicked(FrequentContact contact) {
                loadSubFragment(
                        SendMoneyFragment.newInstanceWithContact(
                                getWalletBalance(),
                                prefs.getString("holderName", "User"),
                                contact.name,
                                contact.accountNumber));
            }
        });

        RecyclerView rv = rootView.findViewById(R.id.rvContacts);
        rv.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL, false));
        rv.setAdapter(adapter);
    }

    private void setupSubscriptions() {
        List<Subscription> subs = Arrays.asList(
                new Subscription("Netflix", "🎬", 12.99, "May 15, 2026"),
                new Subscription("Spotify", "🎵", 9.99, "May 18, 2026"),
                new Subscription("YouTube Premium", "▶", 13.99, "May 22, 2026"),
                new Subscription("iCloud Storage", "☁", 2.99, "May 30, 2026")
        );
        RecyclerView rv = rootView.findViewById(R.id.rvSubscriptions);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setNestedScrollingEnabled(false);
        rv.setAdapter(new SubscriptionAdapter(subs));
    }

    private void setupRecentTransactions() {
        List<Transaction> txns = db.getRecentLedger(5);

        rootView.findViewById(R.id.tvSeeAllWallet).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadSubFragment(new TransactionHistoryFragment());
            }
        });

        RecyclerView rv = rootView.findViewById(R.id.rvWalletTransactions);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setNestedScrollingEnabled(false);
        rv.setAdapter(new TransactionAdapter(txns));
    }

    private void checkIncomingTransfers() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }
        String myAccountNumber = prefs.getString("accountNumber", "");
        if (myAccountNumber.isEmpty() || myAccountNumber.equals("APX-0000-0000-0000")) {
            return;
        }

        int[] avatarColors = requireContext().getResources().getIntArray(R.array.avatarColors);

        FirebaseFirestore.getInstance()
                .collection("transfers")
                .whereEqualTo("toAccountNumber", myAccountNumber)
                .get()
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.firestore.QuerySnapshot>() {
                    @Override
                    public void onSuccess(com.google.firebase.firestore.QuerySnapshot querySnapshot) {
                        boolean received = false;
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String status = doc.getString("status");
                            if (!"pending".equals(status)) {
                                continue;
                            }

                            Double amountObj = doc.getDouble("amount");
                            String fromName = doc.getString("fromName");
                            if (amountObj == null || amountObj <= 0) {
                                continue;
                            }

                            double amount = amountObj;
                            String senderName;
                            if (fromName != null && !fromName.isEmpty()) {
                                senderName = fromName;
                            } else {
                                senderName = "ApexPay User";
                            }

                            double newBal = getWalletBalance() + amount;
                            saveWalletBalance(newBal);

                            db.insertLedger("💰", "Received from " + senderName, amount, true);

                            doc.getReference().update("status", "claimed");

                            String fromAccount = doc.getString("fromAccountNumber");
                            if (fromAccount != null && !fromAccount.isEmpty()) {
                                int colorInt = avatarColors[(int) (Math.random() * avatarColors.length)];
                                db.upsertContact(senderName, fromAccount,
                                        String.format("#%06X", (0xFFFFFF & colorInt)));
                            }

                            received = true;
                        }

                        if (received && isAdded()) {
                            refreshBalance();
                            setupRecentTransactions();
                            Toast.makeText(requireContext(),
                                    getString(R.string.toast_incoming_transfers),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(),
                                    getString(R.string.toast_transfer_sync_error_prefix) + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private double getWalletBalance() {
        return Double.longBitsToDouble(
                prefs.getLong("walletBalance", Double.doubleToLongBits(0.0)));
    }

    private void saveWalletBalance(double v) {
        prefs.edit().putLong("walletBalance", Double.doubleToLongBits(v)).apply();
        syncBalanceToFirestore(v);
    }

    private void syncBalanceToFirestore(double balance) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            return;
        }
        HashMap<String, Object> update = new HashMap<>();
        update.put("walletBalance", balance);
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(auth.getCurrentUser().getUid())
                .update(update);
    }

    private void loadSubFragment(Fragment fragment) {
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
