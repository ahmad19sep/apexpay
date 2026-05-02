package com.apexpay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText     etName, etEmail, etPassword, etConfirmPassword;
    private Button       btnRegister;
    private TextView     tvGoToLogin;
    private ProgressBar  progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        etName            = findViewById(R.id.etName);
        etEmail           = findViewById(R.id.etEmail);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister       = findViewById(R.id.btnRegister);
        tvGoToLogin       = findViewById(R.id.tvGoToLogin);
        progressBar       = findViewById(R.id.progressBar);

        btnRegister.setOnClickListener(v -> attemptRegister());
        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void attemptRegister() {
        String name            = etName.getText().toString().trim();
        String email           = etEmail.getText().toString().trim();
        String password        = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Full name is required"); etName.requestFocus(); return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required"); etEmail.requestFocus(); return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required"); etPassword.requestFocus(); return;
        }
        if (password.length() < 6) {
            etPassword.setError("Minimum 6 characters"); etPassword.requestFocus(); return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match"); etConfirmPassword.requestFocus(); return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserProfile(user.getUid(), name, email);
                        }
                        Toast.makeText(this, "Account created! Please login.", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.putExtra("registeredEmail", email);
                        startActivity(intent);
                        finish();
                    } else {
                        showErrorDialog(task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed. Please try again.");
                    }
                });
    }

    private void saveUserProfile(String uid, String name, String email) {
        String accountNumber = generateAccountNumber(uid);

        // Save to SharedPreferences so wallet works immediately after first login
        SharedPreferences prefs = getSharedPreferences("ApexPayPrefs", MODE_PRIVATE);
        prefs.edit()
                .putString("holderName",    name)
                .putString("accountNumber", accountNumber)
                .putString("userEmail",     email)
                .putLong("walletBalance",   Double.doubleToLongBits(0.0))
                .apply();

        // Save to Firestore so other users can send money to this account
        Map<String, Object> profile = new HashMap<>();
        profile.put("name",          name);
        profile.put("email",         email);
        profile.put("accountNumber", accountNumber);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(profile);
    }

    /** Generates a deterministic 12-digit account number from the Firebase UID. */
    private String generateAccountNumber(String uid) {
        long hash = Math.abs(uid.hashCode());
        return String.format("APX-%04d-%04d-%04d",
                (hash / 100000000L) % 10000,
                (hash / 10000L) % 10000,
                hash % 10000);
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Registration Failed")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
