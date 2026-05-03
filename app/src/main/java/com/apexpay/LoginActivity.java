package com.apexpay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.Executor;

import javax.crypto.Cipher;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnFaceLogin;
    private LinearLayout llBiometricSection;
    private TextView tvGoToRegister;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnFaceLogin = findViewById(R.id.btnFaceLogin);
        llBiometricSection = findViewById(R.id.llBiometricSection);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        progressBar = findViewById(R.id.progressBar);

        String registeredEmail = getIntent().getStringExtra("registeredEmail");
        if (registeredEmail != null) {
            etEmail.setText(registeredEmail);
        }

        SharedPreferences prefs = getSharedPreferences("ApexPayPrefs", MODE_PRIVATE);
        boolean biometricEnabled = prefs.getBoolean("biometricEnabled", false);
        if (biometricEnabled) {
            llBiometricSection.setVisibility(View.VISIBLE);
        }

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });

        btnFaceLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBiometricAuth();
            }
        });

        if (biometricEnabled && mAuth.getCurrentUser() != null) {
            btnFaceLogin.post(new Runnable() {
                @Override
                public void run() {
                    startBiometricAuth();
                }
            });
        }

        tvGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        TextView btnTogglePw = findViewById(R.id.btnTogglePassword);
        final boolean[] showPw = {false};
        btnTogglePw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPw[0] = !showPw[0];
                int inputType;
                if (showPw[0]) {
                    inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
                } else {
                    inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
                }
                etPassword.setInputType(inputType);
                etPassword.setSelection(etPassword.getText().length());
                float alpha;
                if (showPw[0]) {
                    alpha = 1f;
                } else {
                    alpha = 0.5f;
                }
                btnTogglePw.setAlpha(alpha);
            }
        });
    }

    private void startBiometricAuth() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.toast_session_expired),
                    Toast.LENGTH_LONG).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("ApexPayPrefs", MODE_PRIVATE);
        String ivStr = prefs.getString("biometricIV", null);
        String storedEmail = prefs.getString("biometricEmail", null);

        if (storedEmail == null) {
            prefs.edit().putBoolean("biometricEnabled", false).apply();
            llBiometricSection.setVisibility(View.GONE);
            Toast.makeText(this, "Please re-enable biometric login in your profile.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        BiometricPrompt.AuthenticationCallback cb = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                String emailToVerify;
                try {
                    if (result.getCryptoObject() != null) {
                        emailToVerify = BiometricHelper.decryptFromBase64(
                                result.getCryptoObject().getCipher(), storedEmail);
                    } else {
                        emailToVerify = storedEmail;
                    }
                } catch (Exception e) {
                    Toast.makeText(LoginActivity.this,
                            "Biometric verification failed.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String currentEmail = currentUser.getEmail();
                if (currentEmail != null && !currentEmail.equals(emailToVerify)) {
                    Toast.makeText(LoginActivity.this,
                            "Account mismatch. Please log in with your password.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                SharedPreferences p = getSharedPreferences("ApexPayPrefs", MODE_PRIVATE);
                p.edit().putBoolean("isLoggedIn", true).apply();
                Toast.makeText(LoginActivity.this,
                        getString(R.string.toast_welcome_back), Toast.LENGTH_SHORT).show();
                navigateAfterLogin(p);
            }
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                        && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    Toast.makeText(LoginActivity.this,
                            getString(R.string.toast_auth_error_prefix) + errString,
                            Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onAuthenticationFailed() {
                Toast.makeText(LoginActivity.this,
                        getString(R.string.toast_biometric_not_recognised), Toast.LENGTH_SHORT).show();
            }
        };

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt = new BiometricPrompt(this, executor, cb);
        BiometricPrompt.PromptInfo.Builder infoBuilder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_prompt_title))
                .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                .setNegativeButtonText(getString(R.string.biometric_prompt_negative));

        if (ivStr != null) {
            Cipher cipher;
            try {
                cipher = BiometricHelper.getCipherForDecrypt(BiometricHelper.ivFromBase64(ivStr));
            } catch (Exception e) {
                // Key invalidated (new biometric enrolled on device)
                prefs.edit()
                        .putBoolean("biometricEnabled", false)
                        .remove("biometricEmail")
                        .remove("biometricIV")
                        .apply();
                BiometricHelper.deleteKey();
                llBiometricSection.setVisibility(View.GONE);
                Toast.makeText(this,
                        "Biometric changed on device. Please re-enable in your profile.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            prompt.authenticate(
                    infoBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG).build(),
                    new BiometricPrompt.CryptoObject(cipher));
        } else {
            BiometricManager bm = BiometricManager.from(this);
            if (bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    != BiometricManager.BIOMETRIC_SUCCESS) {
                Toast.makeText(this, getString(R.string.toast_no_biometric_enrolled),
                        Toast.LENGTH_LONG).show();
                return;
            }
            prompt.authenticate(
                    infoBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK).build());
        }
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError(getString(R.string.error_email_required));
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError(getString(R.string.error_password_required));
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError(getString(R.string.error_min_6_chars));
            etPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new com.google.android.gms.tasks.OnCompleteListener<com.google.firebase.auth.AuthResult>() {
                    @Override
                    public void onComplete(@NonNull com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);

                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, getString(R.string.toast_welcome_back), Toast.LENGTH_SHORT).show();
                            loadProfileThenNavigate(email);
                        } else {
                            String message;
                            if (task.getException() != null) {
                                message = task.getException().getMessage();
                            } else {
                                message = "Login failed. Please try again.";
                            }
                            showErrorDialog(message);
                        }
                    }
                });
    }

    private void loadProfileThenNavigate(String email) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        SharedPreferences prefs = getSharedPreferences("ApexPayPrefs", MODE_PRIVATE);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.firestore.DocumentSnapshot>() {
                    @Override
                    public void onSuccess(com.google.firebase.firestore.DocumentSnapshot doc) {
                        SharedPreferences.Editor editor = prefs.edit()
                                .putBoolean("isLoggedIn", true)
                                .putString("userEmail", email);

                        if (doc.exists()) {
                            String name = doc.getString("name");
                            String accountNumber = doc.getString("accountNumber");
                            Double cloudBalance = doc.getDouble("walletBalance");

                            if (name != null) {
                                editor.putString("holderName", name);
                            }
                            if (accountNumber != null) {
                                editor.putString("accountNumber", accountNumber);
                            }
                            if (cloudBalance != null && cloudBalance >= 0) {
                                editor.putLong("walletBalance", Double.doubleToLongBits(cloudBalance));
                            }
                        }
                        editor.apply();
                        navigateAfterLogin(prefs);
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        prefs.edit()
                                .putBoolean("isLoggedIn", true)
                                .putString("userEmail", email)
                                .apply();
                        navigateAfterLogin(prefs);
                    }
                });
    }

    private void navigateAfterLogin(SharedPreferences prefs) {
        boolean hasPin = !prefs.getString("userPin", "").isEmpty();
        Intent intent;
        if (hasPin) {
            intent = new Intent(LoginActivity.this, PinActivity.class);
            intent.putExtra("mode", PinActivity.MODE_VERIFY);
        } else {
            intent = new Intent(LoginActivity.this, PinActivity.class);
            intent.putExtra("mode", PinActivity.MODE_SETUP);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_title_login_failed))
                .setMessage(message)
                .setPositiveButton(getString(R.string.dialog_btn_ok), null)
                .show();
    }
}
