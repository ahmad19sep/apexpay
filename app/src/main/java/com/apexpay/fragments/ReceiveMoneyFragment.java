package com.apexpay.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.apexpay.R;

public class
ReceiveMoneyFragment extends Fragment {

    private static final String ARG_ACCOUNT = "account";
    private static final String ARG_NAME    = "name";

    public static ReceiveMoneyFragment newInstance(String accountNumber, String holderName) {
        ReceiveMoneyFragment f = new ReceiveMoneyFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ACCOUNT, accountNumber);
        args.putString(ARG_NAME, holderName);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_receive_money, container, false);

        Bundle args      = getArguments();
        String account   = args != null ? args.getString(ARG_ACCOUNT, "APX-0000-0000-0000") : "APX-0000-0000-0000";
        String name      = args != null ? args.getString(ARG_NAME, "User") : "User";

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        TextView tvHolderName = view.findViewById(R.id.tvReceiveHolderName);
        tvHolderName.setText(name);

        TextView tvAccountDisplay = view.findViewById(R.id.tvAccountDisplay);
        tvAccountDisplay.setText(account);

        // Copy to clipboard
        view.findViewById(R.id.btnCopy).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager)
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Account Number", account);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), "Account number copied!", Toast.LENGTH_SHORT).show();
        });

        // Share
        view.findViewById(R.id.btnShare).setOnClickListener(v -> {
            android.content.Intent shareIntent = new android.content.Intent(
                    android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                    "Send money to " + name + " on ApexPay\nAccount: " + account);
            startActivity(android.content.Intent.createChooser(shareIntent, "Share via"));
        });

        return view;
    }
}