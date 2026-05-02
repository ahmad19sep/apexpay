package com.apexpay.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.apexpay.LoginActivity;
import com.apexpay.PinActivity;
import com.apexpay.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

public class ProfileFragment extends Fragment {

    private static final String ARG_EMAIL = "email";

    public static ProfileFragment newInstance(String email) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        SharedPreferences prefs = requireContext().getSharedPreferences("ApexPayPrefs", MODE_PRIVATE);
        String name          = prefs.getString("holderName",    "");
        String email         = prefs.getString("userEmail",     "");
        String accountNumber = prefs.getString("accountNumber", "—");

        ((TextView) view.findViewById(R.id.tvAvatar)).setText(getInitials(name));
        ((TextView) view.findViewById(R.id.tvName)).setText(name.isEmpty() ? "—" : name);
        ((TextView) view.findViewById(R.id.tvEmailDisplay)).setText(email);
        ((TextView) view.findViewById(R.id.tvAccountNumber)).setText(accountNumber);

        view.findViewById(R.id.btnCopy).setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager)
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("account_number", accountNumber));
            Toast.makeText(requireContext(), "Account number copied", Toast.LENGTH_SHORT).show();
        });

        TextView tvMemberSince = view.findViewById(R.id.tvMemberSince);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getMetadata() != null) {
            long creationMs = user.getMetadata().getCreationTimestamp();
            String date = new SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    .format(new Date(creationMs));
            tvMemberSince.setText(date);
        } else {
            tvMemberSince.setText("—");
        }

        view.findViewById(R.id.btnChangePin).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PinActivity.class);
            intent.putExtra("mode", PinActivity.MODE_SETUP);
            startActivity(intent);
        });

        view.findViewById(R.id.btnLogout).setOnClickListener(v -> confirmLogout());

        return view;
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return String.valueOf(parts[0].charAt(0)).toUpperCase();
        return (String.valueOf(parts[0].charAt(0)) + parts[parts.length - 1].charAt(0))
                .toUpperCase();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        requireContext().getSharedPreferences("ApexPayPrefs", MODE_PRIVATE)
                .edit().clear().apply();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
