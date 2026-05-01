package com.apexpay.fragments;

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
import com.apexpay.models.FrequentContact;

import java.util.ArrayList;

public class SendMoneyFragment extends Fragment {

    private static final String ARG_BALANCE   = "balance";
    private static final String ARG_NAME      = "name";
    private static final String ARG_RECIPIENT = "recipient";
    private static final String ARG_ACCT      = "acct";

    private double walletBalance;
    private String holderName;

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

        Bundle args = getArguments();
        walletBalance = args != null ? args.getDouble(ARG_BALANCE, 0) : 0;
        holderName    = args != null ? args.getString(ARG_NAME, "")  : "";

        // Back navigation
        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Balance display
        TextView tvAvailable = view.findViewById(R.id.tvAvailableBalance);
        tvAvailable.setText(String.format("Available: $%,.2f", walletBalance));

        // Pre-fill recipient if coming from contact tap
        EditText etRecipient = view.findViewById(R.id.etRecipient);
        if (args != null && args.getString(ARG_RECIPIENT) != null) {
            etRecipient.setText(args.getString(ARG_RECIPIENT));
        }

        // Preset amount chips
        EditText etAmount = view.findViewById(R.id.etAmount);
        int[] presetIds = {R.id.chip50, R.id.chip100, R.id.chip200, R.id.chip500};
        String[] presets = {"50", "100", "200", "500"};
        for (int i = 0; i < presetIds.length; i++) {
            String val = presets[i];
            view.findViewById(presetIds[i]).setOnClickListener(v -> etAmount.setText(val));
        }

        // Frequent contacts quick-select
        setupContacts(view, etRecipient);

        // Send button
        EditText etNote = view.findViewById(R.id.etNote);
        Button btnSend  = view.findViewById(R.id.btnSendMoney);
        btnSend.setOnClickListener(v -> {
            String recipient = etRecipient.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String note      = etNote.getText().toString().trim();

            if (recipient.isEmpty()) {
                etRecipient.setError("Enter recipient name or account");
                return;
            }
            if (amountStr.isEmpty()) {
                etAmount.setError("Enter amount");
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                etAmount.setError("Invalid amount");
                return;
            }

            if (amount <= 0) {
                etAmount.setError("Amount must be greater than 0");
                return;
            }
            if (amount > walletBalance) {
                etAmount.setError("Insufficient balance");
                return;
            }

            confirmSend(recipient, amount, note.isEmpty() ? "No note" : note);
        });

        return view;
    }

    private void confirmSend(String recipient, double amount, String note) {
        String message = String.format(
                "Send $%.2f to %s?\n\nNote: %s", amount, recipient, note);
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Transfer")
                .setMessage(message)
                .setPositiveButton("Send", (d, w) -> {
                    walletBalance -= amount;
                    Toast.makeText(requireContext(),
                            String.format("✓ $%.2f sent to %s", amount, recipient),
                            Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupContacts(View view, EditText etRecipient) {
        ArrayList<FrequentContact> contacts = new ArrayList<>();
        contacts.add(new FrequentContact("Ali",  "AL", "APX-1234-5678-0001", "#E53935"));
        contacts.add(new FrequentContact("Sara", "SA", "APX-2345-6789-0002", "#8E24AA"));
        contacts.add(new FrequentContact("Omar", "OM", "APX-3456-7890-0003", "#1E88E5"));
        contacts.add(new FrequentContact("Hana", "HA", "APX-4567-8901-0004", "#00897B"));
        contacts.add(new FrequentContact("Zaid", "ZA", "APX-5678-9012-0005", "#F4511E"));
        contacts.add(new FrequentContact("Noor", "NO", "APX-6789-0123-0006", "#F9A825"));

        FrequentContactAdapter adapter = new FrequentContactAdapter(contacts);
        adapter.setOnContactClickListener(c -> etRecipient.setText(c.name));

        RecyclerView rv = view.findViewById(R.id.rvSendContacts);
        rv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rv.setAdapter(adapter);
    }
}
