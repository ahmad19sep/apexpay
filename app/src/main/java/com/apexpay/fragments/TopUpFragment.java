package com.apexpay.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.apexpay.R;
import com.apexpay.database.DatabaseHelper;

public class TopUpFragment extends Fragment {

    private DatabaseHelper    db;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_top_up, container, false);

        db    = new DatabaseHelper(requireContext());
        prefs = requireActivity().getSharedPreferences("ApexPayPrefs",
                android.content.Context.MODE_PRIVATE);

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        EditText etCustomAmount = view.findViewById(R.id.etCustomAmount);

        int[] chipIds    = {R.id.chipAmt50, R.id.chipAmt100, R.id.chipAmt200,
                            R.id.chipAmt500, R.id.chipAmt1000};
        String[] amounts = {"50", "100", "200", "500", "1000"};
        for (int i = 0; i < chipIds.length; i++) {
            String val = amounts[i];
            view.findViewById(chipIds[i]).setOnClickListener(v -> {
                etCustomAmount.setText(val);
                etCustomAmount.clearFocus();
            });
        }

        view.findViewById(R.id.btnTopUp).setOnClickListener(v -> {
            String amtStr = etCustomAmount.getText().toString().trim();
            if (amtStr.isEmpty()) { etCustomAmount.setError("Enter an amount"); return; }

            double amount;
            try { amount = Double.parseDouble(amtStr); }
            catch (NumberFormatException e) { etCustomAmount.setError("Invalid amount"); return; }

            if (amount <= 0) { etCustomAmount.setError("Must be > 0"); return; }

            String source = "HBL Bank ••3421";
            if (((RadioButton) view.findViewById(R.id.rbBank2)).isChecked())
                source = "Meezan Bank ••7890";

            confirmTopUp(amount, source);
        });

        return view;
    }

    private void confirmTopUp(double amount, String source) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Top-Up")
                .setMessage(String.format("Add $%.2f from\n%s to your Liquid Cash?", amount, source))
                .setPositiveButton("Top Up", (d, w) -> {
                    // Add to balance
                    double newBalance = getWalletBalance() + amount;
                    saveWalletBalance(newBalance);

                    // Record in ledger
                    db.insertLedger("🏦", "Bank Top-Up via " + source, amount, true);

                    Toast.makeText(requireContext(),
                            String.format("✓ $%.2f added to your wallet!", amount),
                            Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private double getWalletBalance() {
        return Double.longBitsToDouble(
                prefs.getLong("walletBalance", Double.doubleToLongBits(0.0)));
    }

    private void saveWalletBalance(double v) {
        prefs.edit().putLong("walletBalance", Double.doubleToLongBits(v)).apply();
    }
}
