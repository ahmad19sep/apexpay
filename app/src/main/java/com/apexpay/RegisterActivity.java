package com.apexpay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvGoToLogin;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
        progressBar = findViewById(R.id.progressBar);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRegister();
            }
        });

        tvGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });

        setupPasswordToggles();
    }

    private void setupPasswordToggles() {
        TextView btnTogglePw = findViewById(R.id.btnTogglePassword);
        TextView btnToggleCnf = findViewById(R.id.btnToggleConfirmPassword);

        final boolean[] show = {false, false};

        btnTogglePw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                show[0] = !show[0];
                int inputType;
                if (show[0]) {
                    inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
                } else {
                    inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
                }
                etPassword.setInputType(inputType);
                etPassword.setSelection(etPassword.getText().length());
                float alpha;
                if (show[0]) {
                    alpha = 1f;
                } else {
                    alpha = 0.5f;
                }
                btnTogglePw.setAlpha(alpha);
            }
        });

        btnToggleCnf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                show[1] = !show[1];
                int inputType;
                if (show[1]) {
                    inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
                } else {
                    inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
                }
                etConfirmPassword.setInputType(inputType);
                etConfirmPassword.setSelection(etConfirmPassword.getText().length());
                float alpha;
                if (show[1]) {
                    alpha = 1f;
                } else {
                    alpha = 0.5f;
                }
                btnToggleCnf.setAlpha(alpha);
            }
        });
    }

    private void attemptRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError(getString(R.string.error_full_name_required));
            etName.requestFocus();
            return;
        }
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
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError(getString(R.string.error_passwords_no_match));
            etConfirmPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new com.google.android.gms.tasks.OnCompleteListener<com.google.firebase.auth.AuthResult>() {
                    @Override
                    public void onComplete(@NonNull com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);

                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                saveUserProfile(user.getUid(), name, email);
                            }
                            Toast.makeText(RegisterActivity.this, getString(R.string.toast_account_created), Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            intent.putExtra("registeredEmail", email);
                            startActivity(intent);
                            finish();
                        } else {
                            String message;
                            if (task.getException() != null) {
                                message = task.getException().getMessage();
                            } else {
                                message = "Registration failed. Please try again.";
                            }
                            showErrorDialog(message);
                        }
                    }
                });
    }

    private void saveUserProfile(String uid, String name, String email) {
        String accountNumber = generateAccountNumber(uid);
        double startingBalance = 5000.0;

        SharedPreferences prefs = getSharedPreferences("ApexPayPrefs", MODE_PRIVATE);
        prefs.edit()
                .putString("holderName", name)
                .putString("accountNumber", accountNumber)
                .putString("userEmail", email)
                .putLong("walletBalance", Double.doubleToLongBits(startingBalance))
                .putBoolean("biometricEnabled", false)
                .apply();

        Map<String, Object> profile = new HashMap<>();
        profile.put("name", name);
        profile.put("email", email);
        profile.put("accountNumber", accountNumber);
        profile.put("walletBalance", startingBalance);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(profile);
    }

    private String generateAccountNumber(String uid) {
        long hash = Math.abs(uid.hashCode());
        return String.format("APX-%04d-%04d-%04d",
                (hash / 100000000L) % 10000,
                (hash / 10000L) % 10000,
                hash % 10000);
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_title_registration_failed))
                .setMessage(message)
                .setPositiveButton(getString(R.string.dialog_btn_ok), null)
                .show();
    }
}
