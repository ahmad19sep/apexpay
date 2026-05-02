package com.apexpay;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText     etEmail, etPassword;
    private Button       btnLogin, btnFaceLogin;
    private TextView     tvGoToRegister;
    private ProgressBar  progressBar;
    private PreviewView  previewView;
    private FirebaseAuth mAuth;
    private ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        cameraExecutor = Executors.newSingleThreadExecutor();

        etEmail        = findViewById(R.id.etEmail);
        etPassword     = findViewById(R.id.etPassword);
        btnLogin       = findViewById(R.id.btnLogin);
        btnFaceLogin   = findViewById(R.id.btnFaceLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        progressBar    = findViewById(R.id.progressBar);
        previewView    = findViewById(R.id.previewView);

        String registeredEmail = getIntent().getStringExtra("registeredEmail");
        if (registeredEmail != null) {
            etEmail.setText(registeredEmail);
        }

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnFaceLogin.setOnClickListener(v -> startFaceDetection());

        tvGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void startFaceDetection() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
            return;
        }

        previewView.setVisibility(View.VISIBLE);
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCamera(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Camera setup failed", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCamera(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build();
        FaceDetector detector = FaceDetection.getClient(options);

        imageAnalysis.setAnalyzer(cameraExecutor, new FaceAnalyzer(detector));

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private class FaceAnalyzer implements ImageAnalysis.Analyzer {
        private final FaceDetector detector;

        public FaceAnalyzer(FaceDetector detector) {
            this.detector = detector;
        }

        @Override
        @OptIn(markerClass = ExperimentalGetImage.class)
        public void analyze(@NonNull ImageProxy imageProxy) {
            Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                detector.process(image)
                        .addOnSuccessListener(faces -> {
                            if (!faces.isEmpty()) {
                                runOnUiThread(() -> {
                                    Toast.makeText(LoginActivity.this, "Face Detected! Logging in...", Toast.LENGTH_SHORT).show();
                                    previewView.setVisibility(View.GONE);
                                    // In a real app, you would match the face here. 
                                    // For this module, we trigger the standard login for the user.
                                    attemptLogin(); 
                                });
                                detector.close();
                            }
                        })
                        .addOnCompleteListener(task -> imageProxy.close());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private void attemptLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Minimum 6 characters");
            etPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);

                    if (task.isSuccessful()) {
                        SharedPreferences prefs = getSharedPreferences("ApexPayPrefs", MODE_PRIVATE);
                        prefs.edit()
                                .putBoolean("isLoggedIn", true)
                                .putString("userEmail", email)
                                .apply();

                        Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("userEmail", email);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);

                    } else {
                        showErrorDialog(task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed. Please try again.");
                    }
                });
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Login Failed")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
