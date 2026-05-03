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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TopUpFragment extends Fragment {

    private DatabaseHelper db;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_top_up, container, false);

        db = new DatabaseHelper(requireContext());
        prefs = requireActivity().getSharedPreferences("ApexPayPrefs",
                android.content.Context.MODE_PRIVATE);

        view.findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        EditText etCustomAmount = view.findViewById(R.id.etCustomAmount);

        int[] chipIds = {R.id.chipAmt50, R.id.chipAmt100, R.id.chipAmt200,
                         R.id.chipAmt500, R.id.chipAmt1000, R.id.chipAmt5000};
        String[] amounts = {"50", "100", "200", "500", "1000", "5000"};
        for (int i = 0; i < chipIds.length; i++) {
            final String val = amounts[i];
            view.findViewById(chipIds[i]).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    etCustomAmount.setText(val);
                    etCustomAmount.clearFocus();
                }
            });
        }

        view.findViewById(R.id.btnTopUp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amtStr = etCustomAmount.getText().toString().trim();
                if (amtStr.isEmpty()) {
                    etCustomAmount.setError(getString(R.string.error_enter_an_amount));
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(amtStr);
                } catch (NumberFormatException e) {
                    etCustomAmount.setError(getString(R.string.error_invalid_amount));
                    return;
                }

                if (amount <= 0) {
                    etCustomAmount.setError(getString(R.string.error_must_be_positive));
                    return;
                }

                String source;
                if (((RadioButton) view.findViewById(R.id.rbBank2)).isChecked()) {
                    source = getString(R.string.bank_meezan);
                } else {
                    source = getString(R.string.bank_hbl);
                }

                confirmTopUp(amount, source);
            }
        });

        return view;
    }

    private void confirmTopUp(double amount, String source) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_title_confirm_topup))
                .setMessage(String.format("Add $%.2f from\n%s to your Liquid Cash?", amount, source))
                .setPositiveButton(getString(R.string.dialog_btn_topup), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        double newBalance = getWalletBalance() + amount;
                        saveWalletBalance(newBalance);

                        db.insertLedger("🏦", "Bank Top-Up via " + source, amount, true);

                        Toast.makeText(requireContext(),
                                String.format("✓ $%.2f added to your wallet!", amount),
                                Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                })
                .setNegativeButton(getString(R.string.dialog_btn_cancel), null)
                .show();
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
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }
        Map<String, Object> update = new HashMap<>();
        update.put("walletBalance", balance);
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .update(update);
    }
}
