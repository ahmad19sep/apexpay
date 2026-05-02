package com.apexpay.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.apexpay.R;
import com.apexpay.adapters.FrequentContactAdapter;
import com.apexpay.database.DatabaseHelper;
import com.apexpay.models.FrequentContact;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SendMoneyFragment extends Fragment {

    private static final String ARG_BALANCE   = "balance";
    private static final String ARG_NAME      = "name";
    private static final String ARG_RECIPIENT = "recipient";
    private static final String ARG_ACCT      = "acct";

    private static final String[] AVATAR_COLORS = {
        "#E53935", "#8E24AA", "#1E88E5", "#00897B", "#F4511E", "#F9A825", "#6366F1"
    };

    private DatabaseHelper    db;
    private SharedPreferences prefs;

    public static SendMoneyFragment newInstance(double balance, String name) {
        SendMoneyFragment f = new SendMoneyFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_BALANCE, balance);
        args.putString(ARG_NAME, name);
        f.setArguments(args);
        return f;
    }

    public static SendMoneyFragment newInstanceWithContact(
            double balance, String name, String recipient, String acct) {
        SendMoneyFragment f = new SendMoneyFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_BALANCE, balance);
        args.putString(ARG_NAME, name);
        args.putString(ARG_RECIPIENT, recipient);
        args.putString(ARG_ACCT, acct);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_send_money, container, false);

        db    = new DatabaseHelper(requireContext());
        prefs = requireActivity().getSharedPreferences("ApexPayPrefs",
                android.content.Context.MODE_PRIVATE);

        Bundle args = getArguments();
        String holderName = args != null ? args.getString(ARG_NAME, "") : "";

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Always read live balance from SharedPreferences
        double currentBalance = getWalletBalance();
        ((TextView) view.findViewById(R.id.tvAvailableBalance))
                .setText(String.format(Locale.getDefault(), "Available: $%,.2f", currentBalance));

        EditText etRecipient = view.findViewById(R.id.etRecipient);
        EditText etAmount    = view.findViewById(R.id.etAmount);
        EditText etNote      = view.findViewById(R.id.etNote);

        // Pre-fill recipient if coming from contact tap
        if (args != null && args.getString(ARG_RECIPIENT) != null)
            etRecipient.setText(args.getString(ARG_RECIPIENT));

        // Quick preset chips
        int[] chipIds    = {R.id.chip50, R.id.chip100, R.id.chip200, R.id.chip500};
        String[] presets = {"50", "100", "200", "500"};
        for (int i = 0; i < chipIds.length; i++) {
            String val = presets[i];
            view.findViewById(chipIds[i]).setOnClickListener(v -> etAmount.setText(val));
        }

        // Contacts from SQLite (people you've previously sent to)
        setupContacts(view, etRecipient);

        // Send button
        view.findViewById(R.id.btnSendMoney).setOnClickListener(v -> {
            String recipient = etRecipient.getText().toString().trim();
            String amtStr    = etAmount.getText().toString().trim();
            String note      = etNote.getText().toString().trim();

            if (recipient.isEmpty()) {
                etRecipient.setError("Enter recipient name or account");
                return;
            }
            if (amtStr.isEmpty()) { etAmount.setError("Enter amount"); return; }

            double amount;
            try { amount = Double.parseDouble(amtStr); }
            catch (NumberFormatException e) { etAmount.setError("Invalid amount"); return; }

            if (amount <= 0)               { etAmount.setError("Must be > 0"); return; }
            if (amount > getWalletBalance()) { etAmount.setError("Insufficient balance"); return; }

            confirmSend(holderName, recipient, amount,
                    note.isEmpty() ? "No note" : note,
                    args != null ? args.getString(ARG_ACCT, "") : "");
        });

        return view;
    }

    private void confirmSend(String senderName, String recipientLabel,
                             double amount, String note, String recipientAccountHint) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Transfer")
                .setMessage(String.format(Locale.getDefault(), "Send $%.2f to %s?\n\nNote: %s",
                        amount, recipientLabel, note))
                .setPositiveButton("Send", (d, w) ->
                        executeSend(senderName, recipientLabel, recipientAccountHint, amount, note))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeSend(String senderName, String recipientLabel,
                             String recipientAccountHint, double amount, String note) {

        double newBalance = getWalletBalance() - amount;
        saveWalletBalance(newBalance);


        db.insertLedger("💸", "Sent to " + recipientLabel, amount, false);


        String recipientAccount = "";
        if (recipientAccountHint != null && recipientAccountHint.startsWith("APX-")) {
            recipientAccount = recipientAccountHint;
        } else if (recipientLabel.startsWith("APX-")) {
            recipientAccount = recipientLabel;
        }

        if (!recipientAccount.isEmpty()) {
            String color = AVATAR_COLORS[(int)(Math.random() * AVATAR_COLORS.length)];
            db.upsertContact(recipientLabel, recipientAccount, color);
            pushFirestoreTransfer(senderName, recipientAccount, amount, note);
        } else {
            lookupByName(senderName, recipientLabel, amount, note);
        }

        Toast.makeText(requireContext(),
                String.format(Locale.getDefault(), "✓ $%.2f sent to %s", amount, recipientLabel),
                Toast.LENGTH_SHORT).show();
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void pushFirestoreTransfer(String senderName, String toAccountNumber,
                                       double amount, String note) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String myAccountNumber = prefs.getString("accountNumber", "");

        Map<String, Object> transfer = new HashMap<>();
        transfer.put("fromUid",          FirebaseAuth.getInstance().getCurrentUser().getUid());
        transfer.put("fromName",         senderName);
        transfer.put("fromAccountNumber", myAccountNumber);
        transfer.put("toAccountNumber",  toAccountNumber);
        transfer.put("amount",           amount);
        transfer.put("note",             note);
        transfer.put("status",           "pending");
        transfer.put("timestamp",        System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("transfers")
                .add(transfer);
    }

    /** Look up a user by display name in Firestore — best-effort for demo. */
    private void lookupByName(String senderName, String recipientName,
                              double amount, String note) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("name", recipientName)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        String acct = doc.getString("accountNumber");
                        if (acct != null) {
                            db.upsertContact(recipientName, acct,
                                    AVATAR_COLORS[(int)(Math.random() * AVATAR_COLORS.length)]);
                            pushFirestoreTransfer(senderName, acct, amount, note);
                        }
                    }
                });
    }

    private void setupContacts(View view, EditText etRecipient) {
        List<FrequentContact> contacts = db.getContacts();
        if (contacts.isEmpty()) 
            return;

        FrequentContactAdapter adapter = new FrequentContactAdapter(contacts);
        adapter.setOnContactClickListener(c -> etRecipient.setText(c.accountNumber));

        RecyclerView rv = view.findViewById(R.id.rvSendContacts);
        rv.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL, false));
        rv.setAdapter(adapter);
    }

    private double getWalletBalance() {
        return Double.longBitsToDouble(
                prefs.getLong("walletBalance", Double.doubleToLongBits(0.0)));
    }

    private void saveWalletBalance(double v) {
        prefs.edit().putLong("walletBalance", Double.doubleToLongBits(v)).apply();
    }
}
