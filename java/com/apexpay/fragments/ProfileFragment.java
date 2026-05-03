package com.apexpay.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.apexpay.BiometricHelper;

import java.util.concurrent.Executor;

import javax.crypto.Cipher;

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
    private ActivityResultLauncher<String> pickImageLauncher;

    public static ProfileFragment newInstance(String email) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) saveProfilePhoto(uri);
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        SharedPreferences prefs = requireContext().getSharedPreferences("ApexPayPrefs", MODE_PRIVATE);
        DatabaseHelper db = new DatabaseHelper(requireContext());

        String name = prefs.getString("holderName", "");
        String email = prefs.getString("userEmail", "");
        String accountNumber = prefs.getString("accountNumber", "APX-0000-0000-0000");

        TextView tvAvatar = view.findViewById(R.id.tvAvatar);
        ImageView ivProfilePhoto = view.findViewById(R.id.ivProfilePhoto);
        tvAvatar.setText(getInitials(name));

        String photoPath = prefs.getString("profilePhotoPath", null);
        if (photoPath != null) {
            Bitmap bmp = BitmapFactory.decodeFile(photoPath);
            if (bmp != null) {
                ivProfilePhoto.setImageBitmap(bmp);
                ivProfilePhoto.setVisibility(View.VISIBLE);
                tvAvatar.setVisibility(View.GONE);
            }
        }

        view.findViewById(R.id.flAvatar).setOnClickListener(v ->
                pickImageLauncher.launch("image/*"));

        String displayName;
        if (name.isEmpty()) {
            displayName = "—";
        } else {
            displayName = name;
        }
        ((TextView) view.findViewById(R.id.tvName)).setText(displayName);
        ((TextView) view.findViewById(R.id.tvEmailDisplay)).setText(email);
        ((TextView) view.findViewById(R.id.tvAccountNumber)).setText(accountNumber);

        view.findViewById(R.id.btnCopy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager)
                        requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("account_number", accountNumber));
                Toast.makeText(requireContext(), getString(R.string.toast_account_number_copied), Toast.LENGTH_SHORT).show();
            }
        });

        double walletBal = Double.longBitsToDouble(prefs.getLong("walletBalance", Double.doubleToLongBits(0.0)));
        double simCash = Double.longBitsToDouble(prefs.getLong("simCash", Double.doubleToLongBits(10000.0)));
        double netWorth = walletBal + simCash;
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

        boolean bioEnabled = prefs.getBoolean("biometricEnabled", false);
        String biometricStatus;
        if (bioEnabled) {
            biometricStatus = getString(R.string.biometric_status_enabled);
        } else {
            biometricStatus = getString(R.string.biometric_status_disabled);
        }
        ((TextView) view.findViewById(R.id.tvBiometricStatus)).setText(biometricStatus);

        view.findViewById(R.id.btnProfileBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        view.findViewById(R.id.btnEditProfile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditProfileDialog(prefs, (TextView) view.findViewById(R.id.tvName));
            }
        });

        view.findViewById(R.id.rowEditProfile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditProfileDialog(prefs, (TextView) view.findViewById(R.id.tvName));
            }
        });

        view.findViewById(R.id.rowChangePassword).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChangePasswordDialog();
            }
        });

        view.findViewById(R.id.rowLinkedBanks).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLinkedBanksDialog();
            }
        });

        view.findViewById(R.id.rowNotifications).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNotificationsDialog(prefs, (TextView) view.findViewById(R.id.tvNotifStatus));
            }
        });

        view.findViewById(R.id.rowBiometric).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBiometricDialog(prefs, (TextView) view.findViewById(R.id.tvBiometricStatus));
            }
        });

        view.findViewById(R.id.rowPrivacy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPrivacyDialog();
            }
        });

        view.findViewById(R.id.btnSignOut).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmLogout();
            }
        });

        return view;
    }

    private void showEditProfileDialog(SharedPreferences prefs, TextView tvName) {
        String currentName = prefs.getString("holderName", "");
        EditText etName = new EditText(requireContext());
        etName.setText(currentName);
        etName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_title_edit_profile))
                .setMessage(getString(R.string.dialog_msg_edit_profile))
                .setView(etName)
                .setPositiveButton(getString(R.string.dialog_btn_save), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        String newName = etName.getText().toString().trim();
                        if (newName.isEmpty()) {
                            Toast.makeText(requireContext(), getString(R.string.error_name_empty), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        prefs.edit().putString("holderName", newName).apply();
                        tvName.setText(newName);
                        ((TextView) requireView().findViewById(R.id.tvAvatar)).setText(getInitials(newName));
                        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                        if (u != null) {
                            Map<String, Object> upd = new HashMap<>();
                            upd.put("name", newName);
                            FirebaseFirestore.getInstance().collection("users").document(u.getUid()).update(upd);
                        }
                        Toast.makeText(requireContext(), getString(R.string.toast_profile_updated), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.dialog_btn_cancel), null)
                .show();
    }

    private void showChangePasswordDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 0);

        EditText etCurrent = new EditText(requireContext());
        etCurrent.setHint(getString(R.string.hint_current_password));
        etCurrent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        EditText etNew = new EditText(requireContext());
        etNew.setHint(getString(R.string.hint_new_password));
        etNew.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        EditText etConfirm = new EditText(requireContext());
        etConfirm.setHint(getString(R.string.hint_confirm_new_password));
        etConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        layout.addView(etCurrent);
        layout.addView(etNew);
        layout.addView(etConfirm);

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_title_change_password))
                .setView(layout)
                .setPositiveButton(getString(R.string.dialog_btn_update), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        String current = etCurrent.getText().toString().trim();
                        String newPw = etNew.getText().toString().trim();
                        String confirm = etConfirm.getText().toString().trim();

                        if (current.isEmpty() || newPw.isEmpty()) {
                            Toast.makeText(requireContext(), getString(R.string.error_fill_all_fields), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (newPw.length() < 6) {
                            Toast.makeText(requireContext(), getString(R.string.error_min_password_6), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (!newPw.equals(confirm)) {
                            Toast.makeText(requireContext(), getString(R.string.error_passwords_no_match), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String emailAddr;
                        if (user.getEmail() != null) {
                            emailAddr = user.getEmail();
                        } else {
                            emailAddr = "";
                        }
                        AuthCredential credential = EmailAuthProvider.getCredential(emailAddr, current);
                        user.reauthenticate(credential).addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void a) {
                                user.updatePassword(newPw).addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void b) {
                                        Toast.makeText(requireContext(), getString(R.string.toast_password_updated), Toast.LENGTH_SHORT).show();
                                    }
                                }).addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }).addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(requireContext(), getString(R.string.toast_current_password_wrong), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton(getString(R.string.dialog_btn_cancel), null)
                .show();
    }

    private void showLinkedBanksDialog() {
        String[] banks = {getString(R.string.bank_hbl_primary), getString(R.string.bank_meezan_full)};
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_title_linked_banks))
                .setItems(banks, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        Toast.makeText(requireContext(), getString(R.string.toast_bank_management_soon), Toast.LENGTH_SHORT).show();
                    }
                })
                .setPositiveButton(getString(R.string.dialog_btn_add_bank), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        Toast.makeText(requireContext(), getString(R.string.toast_bank_linking_soon), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.dialog_btn_close), null)
                .show();
    }

    private void showNotificationsDialog(SharedPreferences prefs, TextView tvStatus) {
        boolean[] checked = {
                prefs.getBoolean("notif_transfers", true),
                prefs.getBoolean("notif_trades", true),
                prefs.getBoolean("notif_security", true)
        };
        String[] items = {
                getString(R.string.notif_transfer_alerts),
                getString(R.string.notif_trade_confirmations),
                getString(R.string.notif_security_alerts)
        };

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_title_notifications))
                .setMultiChoiceItems(items, checked, new android.content.DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which, boolean isChecked) {
                        checked[which] = isChecked;
                    }
                })
                .setPositiveButton(getString(R.string.dialog_btn_save), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        prefs.edit()
                                .putBoolean("notif_transfers", checked[0])
                                .putBoolean("notif_trades", checked[1])
                                .putBoolean("notif_security", checked[2])
                                .apply();
                        int count = 0;
                        if (checked[0]) {
                            count++;
                        }
                        if (checked[1]) {
                            count++;
                        }
                        if (checked[2]) {
                            count++;
                        }
                        String statusText;
                        if (count > 0) {
                            statusText = "Enabled — " + count + " alert types";
                        } else {
                            statusText = "All notifications disabled";
                        }
                        tvStatus.setText(statusText);
                        Toast.makeText(requireContext(), getString(R.string.toast_notif_saved), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.dialog_btn_cancel), null)
                .show();
    }

    private void showBiometricDialog(SharedPreferences prefs, TextView tvStatus) {
        boolean bioEnabled = prefs.getBoolean("biometricEnabled", false);

        BiometricManager bm = BiometricManager.from(requireContext());
        if (bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(requireContext(),
                    getString(R.string.toast_no_biometric_enrolled), Toast.LENGTH_LONG).show();
            return;
        }

        String toggleLabel = bioEnabled ? "Disable Biometric / Face ID" : "Enable Biometric / Face ID";
        String[] options = {getString(R.string.biometric_option_change_pin), toggleLabel};

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_title_biometric_pin))
                .setItems(options, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        if (which == 0) {
                            Intent intent = new Intent(requireContext(), PinActivity.class);
                            intent.putExtra("mode", PinActivity.MODE_SETUP);
                            startActivity(intent);
                        } else if (bioEnabled) {
                            triggerBiometricDisable(prefs, tvStatus);
                        } else {
                            triggerBiometricEnable(prefs, tvStatus);
                        }
                    }
                })
                .setNegativeButton(getString(R.string.dialog_btn_cancel), null)
                .show();
    }

    private void triggerBiometricEnable(SharedPreferences prefs, TextView tvStatus) {
        BiometricManager bm = BiometricManager.from(requireContext());
        boolean strongAvailable = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS;

        if (strongAvailable) {
            // Fingerprint / Class-3 face: use Keystore key locked to biometric
            Cipher cipher;
            try {
                cipher = BiometricHelper.getCipherForEncrypt();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Biometric setup failed: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                return;
            }
            BiometricPrompt.AuthenticationCallback cb = new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(
                        @NonNull BiometricPrompt.AuthenticationResult result) {
                    try {
                        Cipher c = result.getCryptoObject().getCipher();
                        String email = prefs.getString("userEmail", "");
                        prefs.edit()
                                .putBoolean("biometricEnabled", true)
                                .putString("biometricEmail", BiometricHelper.encryptToBase64(c, email))
                                .putString("biometricIV", BiometricHelper.ivToBase64(c.getIV()))
                                .apply();
                        tvStatus.setText(getString(R.string.biometric_status_enabled));
                        Toast.makeText(requireContext(),
                                getString(R.string.toast_biometric_enabled), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(requireContext(),
                                "Failed to save biometric: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
                @Override
                public void onAuthenticationError(int code, @NonNull CharSequence msg) {
                    if (code != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                            && code != BiometricPrompt.ERROR_USER_CANCELED)
                        Toast.makeText(requireContext(),
                                getString(R.string.toast_auth_error_prefix) + msg,
                                Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onAuthenticationFailed() {
                    Toast.makeText(requireContext(),
                            getString(R.string.toast_biometric_not_recognised), Toast.LENGTH_SHORT).show();
                }
            };
            BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Enable Biometric Login")
                    .setSubtitle("Scan your biometric to activate quick login")
                    .setNegativeButtonText(getString(R.string.dialog_btn_cancel))
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build();
            new BiometricPrompt(requireActivity(), ContextCompat.getMainExecutor(requireContext()), cb)
                    .authenticate(info, new BiometricPrompt.CryptoObject(cipher));
        } else {
            // Face unlock (Class-2 / software): no Keystore, email stored for account verification
            BiometricPrompt.AuthenticationCallback cb = new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(
                        @NonNull BiometricPrompt.AuthenticationResult result) {
                    prefs.edit()
                            .putBoolean("biometricEnabled", true)
                            .putString("biometricEmail", prefs.getString("userEmail", ""))
                            .remove("biometricIV")
                            .apply();
                    tvStatus.setText(getString(R.string.biometric_status_enabled));
                    Toast.makeText(requireContext(),
                            getString(R.string.toast_biometric_enabled), Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onAuthenticationError(int code, @NonNull CharSequence msg) {
                    if (code != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                            && code != BiometricPrompt.ERROR_USER_CANCELED)
                        Toast.makeText(requireContext(),
                                getString(R.string.toast_auth_error_prefix) + msg,
                                Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onAuthenticationFailed() {
                    Toast.makeText(requireContext(),
                            getString(R.string.toast_biometric_not_recognised), Toast.LENGTH_SHORT).show();
                }
            };
            BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Enable Biometric Login")
                    .setSubtitle("Scan your face to activate quick login")
                    .setNegativeButtonText(getString(R.string.dialog_btn_cancel))
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .build();
            new BiometricPrompt(requireActivity(), ContextCompat.getMainExecutor(requireContext()), cb)
                    .authenticate(info);
        }
    }

    private void triggerBiometricDisable(SharedPreferences prefs, TextView tvStatus) {
        BiometricPrompt.AuthenticationCallback cb = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                clearBiometricData(prefs, tvStatus);
            }
            @Override
            public void onAuthenticationError(int code, @NonNull CharSequence msg) {
                if (code != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                        && code != BiometricPrompt.ERROR_USER_CANCELED)
                    Toast.makeText(requireContext(),
                            getString(R.string.toast_auth_error_prefix) + msg,
                            Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onAuthenticationFailed() {
                Toast.makeText(requireContext(),
                        getString(R.string.toast_biometric_not_recognised), Toast.LENGTH_SHORT).show();
            }
        };

        String ivStr = prefs.getString("biometricIV", null);
        if (ivStr != null) {
            // STRONG mode: use CryptoObject so the Keystore key is exercised
            Cipher cipher;
            try {
                cipher = BiometricHelper.getCipherForDecrypt(BiometricHelper.ivFromBase64(ivStr));
            } catch (Exception e) {
                clearBiometricData(prefs, tvStatus);
                return;
            }
            BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Disable Biometric Login")
                    .setSubtitle("Verify your identity to disable")
                    .setNegativeButtonText(getString(R.string.dialog_btn_cancel))
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build();
            new BiometricPrompt(requireActivity(), ContextCompat.getMainExecutor(requireContext()), cb)
                    .authenticate(info, new BiometricPrompt.CryptoObject(cipher));
        } else {
            // WEAK mode (face unlock): simple prompt, no CryptoObject
            BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Disable Biometric Login")
                    .setSubtitle("Verify your identity to disable")
                    .setNegativeButtonText(getString(R.string.dialog_btn_cancel))
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .build();
            new BiometricPrompt(requireActivity(), ContextCompat.getMainExecutor(requireContext()), cb)
                    .authenticate(info);
        }
    }

    private void clearBiometricData(SharedPreferences prefs, TextView tvStatus) {
        prefs.edit()
                .putBoolean("biometricEnabled", false)
                .remove("biometricEmail")
                .remove("biometricIV")
                .apply();
        BiometricHelper.deleteKey();
        tvStatus.setText(getString(R.string.biometric_status_disabled));
        Toast.makeText(requireContext(), getString(R.string.toast_biometric_disabled),
                Toast.LENGTH_SHORT).show();
    }

    private void showPrivacyDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_title_privacy))
                .setMessage(getString(R.string.dialog_msg_privacy))
                .setPositiveButton(getString(R.string.dialog_btn_ok), null)
                .setNeutralButton(getString(R.string.dialog_btn_clear_local_data), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        new AlertDialog.Builder(requireContext())
                                .setTitle(getString(R.string.dialog_title_clear_local_data))
                                .setMessage(getString(R.string.dialog_msg_clear_local_data))
                                .setPositiveButton(getString(R.string.dialog_btn_clear), new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(android.content.DialogInterface d2, int w2) {
                                        Toast.makeText(requireContext(), getString(R.string.toast_local_data_cleared), Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton(getString(R.string.dialog_btn_cancel), null)
                                .show();
                    }
                })
                .show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_title_sign_out))
                .setMessage(getString(R.string.dialog_msg_sign_out))
                .setPositiveButton(getString(R.string.dialog_btn_sign_out), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        performLogout();
                    }
                })
                .setNegativeButton(getString(R.string.dialog_btn_cancel), null)
                .show();
    }

    private void performLogout() {
        requireContext().getSharedPreferences("ApexPayPrefs", MODE_PRIVATE)
                .edit().putBoolean("isLoggedIn", false).apply();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void saveProfilePhoto(Uri uri) {
        try {
            // First pass: read dimensions only
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            InputStream probe = requireContext().getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(probe, null, opts);
            probe.close();

            // Downsample to fit within 512x512 before decoding into memory
            opts.inSampleSize = computeSampleSize(opts.outWidth, opts.outHeight, 512);
            opts.inJustDecodeBounds = false;
            InputStream in = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bmp = BitmapFactory.decodeStream(in, null, opts);
            in.close();

            if (bmp == null) {
                Toast.makeText(requireContext(), "Could not read image", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save as compressed JPEG to internal storage
            File dest = new File(requireContext().getFilesDir(), "profile_photo.jpg");
            FileOutputStream out = new FileOutputStream(dest);
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.close();

            requireContext().getSharedPreferences("ApexPayPrefs", MODE_PRIVATE)
                    .edit().putString("profilePhotoPath", dest.getAbsolutePath()).apply();

            ImageView iv = requireView().findViewById(R.id.ivProfilePhoto);
            TextView tv = requireView().findViewById(R.id.tvAvatar);
            iv.setImageBitmap(bmp);
            iv.setVisibility(View.VISIBLE);
            tv.setVisibility(View.GONE);

            Toast.makeText(requireContext(), "Profile photo updated", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Failed to save photo", Toast.LENGTH_SHORT).show();
        }
    }

    private static int computeSampleSize(int width, int height, int maxDim) {
        int sampleSize = 1;
        while (width / (sampleSize * 2) >= maxDim && height / (sampleSize * 2) >= maxDim) {
            sampleSize *= 2;
        }
        return sampleSize;
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return String.valueOf(parts[0].charAt(0)).toUpperCase();
        }
        return (String.valueOf(parts[0].charAt(0)) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
