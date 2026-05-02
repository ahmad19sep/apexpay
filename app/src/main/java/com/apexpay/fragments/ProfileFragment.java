package com.apexpay.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.apexpay.LoginActivity;
import com.apexpay.PinActivity;
import com.apexpay.R;
import com.apexpay.database.DatabaseHelper;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
        DatabaseHelper db = new DatabaseHelper(requireContext());

        String name          = prefs.getString("holderName",    "");
        String email         = prefs.getString("userEmail",     "");
        String accountNumber = prefs.getString("accountNumber", "APX-0000-0000-0000");

        ((TextView) view.findViewById(R.id.tvAvatar)).setText(getInitials(name));
        ((TextView) view.findViewById(R.id.tvName)).setText(name.isEmpty() ? "—" : name);
        ((TextView) view.findViewById(R.id.tvEmailDisplay)).setText(email);
        ((TextView) view.findViewById(R.id.tvAccountNumber)).setText(accountNumber);

        // Copy account number
        view.findViewById(R.id.btnCopy).setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager)
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("account_number", accountNumber));
            Toast.makeText(requireContext(), "Account number copied", Toast.LENGTH_SHORT).show();
        });

        // Stats
        double walletBal  = Double.longBitsToDouble(prefs.getLong("walletBalance", Double.doubleToLongBits(0.0)));
        double simCash    = Double.longBitsToDouble(prefs.getLong("simCash", Double.doubleToLongBits(10000.0)));
        double netWorth   = walletBal + simCash;
        ((TextView) view.findViewById(R.id.tvStatBalance)).setText(String.format("$%,.0f", netWorth));

        int txnCount = db.getRecentLedger(Integer.MAX_VALUE).size();
        ((TextView) view.findViewById(R.id.tvStatTxns)).setText(String.valueOf(txnCount));

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getMetadata() != null) {
            long creationMs = user.getMetadata().getCreationTimestamp();
            String date = new SimpleDateFormat("MMM. yyyy", Locale.getDefault()).format(new Date(creationMs));
            ((TextView) view.findViewById(R.id.tvMemberSince)).setText(date);
        } else {
            ((TextView) view.findViewById(R.id.tvMemberSince)).setText("—");
        }

        // Biometric status from prefs
        boolean bioEnabled = prefs.getBoolean("biometricEnabled", true);
        ((TextView) view.findViewById(R.id.tvBiometricStatus))
                .setText(bioEnabled ? "Face ID / Fingerprint enabled" : "Biometric disabled");

        // ── Row click handlers ─────────────────────────────────────────────

        view.findViewById(R.id.btnProfileBack).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        view.findViewById(R.id.btnEditProfile).setOnClickListener(v ->
                showEditProfileDialog(prefs, (TextView) view.findViewById(R.id.tvName)));

        view.findViewById(R.id.rowEditProfile).setOnClickListener(v ->
                showEditProfileDialog(prefs, (TextView) view.findViewById(R.id.tvName)));

        view.findViewById(R.id.rowChangePassword).setOnClickListener(v ->
                showChangePasswordDialog());

        view.findViewById(R.id.rowLinkedBanks).setOnClickListener(v ->
                showLinkedBanksDialog());

        view.findViewById(R.id.rowNotifications).setOnClickListener(v ->
                showNotificationsDialog(prefs, (TextView) view.findViewById(R.id.tvNotifStatus)));

        view.findViewById(R.id.rowBiometric).setOnClickListener(v ->
                showBiometricDialog(prefs, (TextView) view.findViewById(R.id.tvBiometricStatus)));

        view.findViewById(R.id.rowPrivacy).setOnClickListener(v ->
                showPrivacyDialog());

        view.findViewById(R.id.btnSignOut).setOnClickListener(v -> confirmLogout());

        return view;
    }

    // ── Edit Profile ──────────────────────────────────────────────────────────

    private void showEditProfileDialog(SharedPreferences prefs, TextView tvName) {
        String currentName = prefs.getString("holderName", "");
        EditText etName = new EditText(requireContext());
        etName.setText(currentName);
        etName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Profile Name")
                .setMessage("Enter your display name:")
                .setView(etName)
                .setPositiveButton("Save", (d, w) -> {
                    String newName = etName.getText().toString().trim();
                    if (newName.isEmpty()) { Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show(); return; }
                    prefs.edit().putString("holderName", newName).apply();
                    tvName.setText(newName);
                    ((TextView) requireView().findViewById(R.id.tvAvatar)).setText(getInitials(newName));
                    // Update Firestore
                    FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                    if (u != null) {
                        Map<String, Object> upd = new HashMap<>();
                        upd.put("name", newName);
                        FirebaseFirestore.getInstance().collection("users").document(u.getUid()).update(upd);
                    }
                    Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Change Password ────────────────────────────────────────────────────────

    private void showChangePasswordDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 0);

        EditText etCurrent = new EditText(requireContext());
        etCurrent.setHint("Current password");
        etCurrent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        EditText etNew = new EditText(requireContext());
        etNew.setHint("New password (min 6 chars)");
        etNew.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        EditText etConfirm = new EditText(requireContext());
        etConfirm.setHint("Confirm new password");
        etConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        layout.addView(etCurrent);
        layout.addView(etNew);
        layout.addView(etConfirm);

        new AlertDialog.Builder(requireContext())
                .setTitle("Change Password")
                .setView(layout)
                .setPositiveButton("Update", (d, w) -> {
                    String current = etCurrent.getText().toString().trim();
                    String newPw   = etNew.getText().toString().trim();
                    String confirm = etConfirm.getText().toString().trim();

                    if (current.isEmpty() || newPw.isEmpty()) {
                        Toast.makeText(requireContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPw.length() < 6) {
                        Toast.makeText(requireContext(), "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newPw.equals(confirm)) {
                        Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String email = user.getEmail() != null ? user.getEmail() : "";
                    AuthCredential credential = EmailAuthProvider.getCredential(email, current);
                    user.reauthenticate(credential).addOnSuccessListener(a ->
                            user.updatePassword(newPw).addOnSuccessListener(b ->
                                    Toast.makeText(requireContext(), "Password updated successfully", Toast.LENGTH_SHORT).show()
                            ).addOnFailureListener(e ->
                                    Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            )
                    ).addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Current password incorrect", Toast.LENGTH_SHORT).show()
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Linked Banks ───────────────────────────────────────────────────────────

    private void showLinkedBanksDialog() {
        String[] banks = {"HBL Bank  ••3421  (Primary)", "Meezan Bank  ••7890"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Linked Bank Accounts")
                .setItems(banks, (d, which) ->
                        Toast.makeText(requireContext(), "Bank account management coming soon", Toast.LENGTH_SHORT).show())
                .setPositiveButton("Add Bank", (d, w) ->
                        Toast.makeText(requireContext(), "Bank linking coming soon", Toast.LENGTH_SHORT).show())
                .setNegativeButton("Close", null)
                .show();
    }

    // ── Notifications ──────────────────────────────────────────────────────────

    private void showNotificationsDialog(SharedPreferences prefs, TextView tvStatus) {
        boolean[] checked = {
                prefs.getBoolean("notif_transfers", true),
                prefs.getBoolean("notif_trades",    true),
                prefs.getBoolean("notif_security",  true)
        };
        String[] items = {"Transfer alerts", "Trade confirmations", "Security alerts"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Notifications")
                .setMultiChoiceItems(items, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("Save", (d, w) -> {
                    prefs.edit()
                            .putBoolean("notif_transfers", checked[0])
                            .putBoolean("notif_trades",    checked[1])
                            .putBoolean("notif_security",  checked[2])
                            .apply();
                    int count = (checked[0] ? 1 : 0) + (checked[1] ? 1 : 0) + (checked[2] ? 1 : 0);
                    tvStatus.setText(count > 0 ? "Enabled — " + count + " alert types" : "All notifications disabled");
                    Toast.makeText(requireContext(), "Notification preferences saved", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Biometric & PIN ────────────────────────────────────────────────────────

    private void showBiometricDialog(SharedPreferences prefs, TextView tvStatus) {
        boolean bioEnabled = prefs.getBoolean("biometricEnabled", true);
        String[] options = {"Change PIN", bioEnabled ? "Disable Biometric / Face ID" : "Enable Biometric / Face ID"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Biometric & PIN")
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(requireContext(), PinActivity.class);
                        intent.putExtra("mode", PinActivity.MODE_SETUP);
                        startActivity(intent);
                    } else {
                        boolean newState = !bioEnabled;
                        prefs.edit().putBoolean("biometricEnabled", newState).apply();
                        tvStatus.setText(newState ? "Face ID / Fingerprint enabled" : "Biometric disabled");
                        Toast.makeText(requireContext(),
                                newState ? "Biometric authentication enabled" : "Biometric authentication disabled",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Privacy & Data ─────────────────────────────────────────────────────────

    private void showPrivacyDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Privacy & Data")
                .setMessage("Apex Pay stores your financial data securely using:\n\n" +
                        "• SQLite for local transaction history and holdings\n" +
                        "• Firebase Authentication for secure login\n" +
                        "• Firebase Firestore for P2P transfer routing\n\n" +
                        "Your data is never sold or shared with third parties.")
                .setPositiveButton("OK", null)
                .setNeutralButton("Clear Local Data", (d, w) ->
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Clear Local Data")
                                .setMessage("This will clear your local transaction history and chat messages. Wallet balance and holdings are preserved.")
                                .setPositiveButton("Clear", (d2, w2) ->
                                        Toast.makeText(requireContext(), "Local data cleared", Toast.LENGTH_SHORT).show())
                                .setNegativeButton("Cancel", null)
                                .show())
                .show();
    }

    // ── Logout ─────────────────────────────────────────────────────────────────

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out of Apex Pay?")
                .setPositiveButton("Sign Out", (dialog, which) -> performLogout())
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

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return String.valueOf(parts[0].charAt(0)).toUpperCase();
        return (String.valueOf(parts[0].charAt(0)) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
