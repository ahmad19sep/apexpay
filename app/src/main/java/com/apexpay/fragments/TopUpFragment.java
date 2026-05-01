package com.apexpay.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.apexpay.R;

public class TopUpFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_top_up, container, false);

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        EditText etCustomAmount = view.findViewById(R.id.etCustomAmount);

        // Preset amount chips
        int[] chipIds = {R.id.chipAmt50, R.id.chipAmt100, R.id.chipAmt200,
                         R.id.chipAmt500, R.id.chipAmt1000};
        String[] amounts = {"50", "100", "200", "500", "1000"};

        for (int i = 0; i < chipIds.length; i++) {
            String val = amounts[i];
            view.findViewById(chipIds[i]).setOnClickListener(v -> {
                etCustomAmount.setText(val);
                etCustomAmount.clearFocus();
            });
        }

        // Top Up button
        view.findViewById(R.id.btnTopUp).setOnClickListener(v -> {
            String amountStr = etCustomAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                etCustomAmount.setError("Enter an amount");
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                etCustomAmount.setError("Invalid amount");
                return;
            }

            if (amount <= 0) {
                etCustomAmount.setError("Amount must be greater than 0");
                return;
            }

            // Find selected source
            String source = "HBL Bank ••3421";
            RadioButton rb1 = view.findViewById(R.id.rbBank1);
            RadioButton rb2 = view.findViewById(R.id.rbBank2);
            if (rb2.isChecked()) source = "Meezan Bank ••7890";

            confirmTopUp(amount, source);
        });

        return view;
    }

    private void confirmTopUp(double amount, String source) {
        String message = String.format("Add $%.2f from\n%s to your Liquid Cash?", amount, source);
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Top-Up")
                .setMessage(message)
                .setPositiveButton("Top Up", (d, w) -> {
                    Toast.makeText(requireContext(),
                            String.format("✓ $%.2f added to your wallet!", amount),
                            Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}