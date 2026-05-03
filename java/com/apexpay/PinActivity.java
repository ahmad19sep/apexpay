package com.apexpay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PinActivity extends AppCompatActivity {

    public static final String MODE_SETUP = "SETUP";
    public static final String MODE_VERIFY = "VERIFY";

    private static final int PIN_LENGTH = 4;

    private TextView tvTitle, tvSubtitle, tvUsePassword;
    private View[] dots;

    private String currentPin = "";
    private String firstPin = "";
    private String mode;
    private boolean awaitingConfirm = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        mode = getIntent().getStringExtra("mode");
        if (mode == null) {
            mode = MODE_VERIFY;
        }

        tvTitle = findViewById(R.id.tvPinTitle);
        tvSubtitle = findViewById(R.id.tvPinSubtitle);
        tvUsePassword = findViewById(R.id.tvUsePassword);

        dots = new View[]{
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3),
            findViewById(R.id.dot4)
        };

        updateTitleAndSubtitle();

        if (MODE_SETUP.equals(mode)) {
            tvUsePassword.setVisibility(View.GONE);
        } else {
            tvUsePassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(PinActivity.this, LoginActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                }
            });
        }

        int[] digitIds = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3,
            R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7,
            R.id.btn8, R.id.btn9
        };
        String[] digits = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        for (int i = 0; i < digitIds.length; i++) {
            final String d = digits[i];
            findViewById(digitIds[i]).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onDigit(d);
                }
            });
        }

        findViewById(R.id.btnDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDelete();
            }
        });
    }

    private void onDigit(String digit) {
        if (currentPin.length() >= PIN_LENGTH) {
            return;
        }
        currentPin += digit;
        updateDots();
        if (currentPin.length() == PIN_LENGTH) {
            processPin();
        }
    }

    private void onDelete() {
        if (currentPin.isEmpty()) {
            return;
        }
        currentPin = currentPin.substring(0, currentPin.length() - 1);
        updateDots();
    }

    private void updateDots() {
        for (int i = 0; i < PIN_LENGTH; i++) {
            if (i < currentPin.length()) {
                dots[i].setBackgroundResource(R.drawable.dot_filled);
            } else {
                dots[i].setBackgroundResource(R.drawable.dot_empty);
            }
        }
    }

    private void processPin() {
        if (MODE_SETUP.equals(mode)) {
            if (!awaitingConfirm) {
                firstPin = currentPin;
                awaitingConfirm = true;
                currentPin = "";
                updateDots();
                tvTitle.setText(getString(R.string.pin_title_confirm));
                tvSubtitle.setText(getString(R.string.pin_subtitle_confirm));
                tvSubtitle.setTextColor(getColor(R.color.text_secondary));
            } else {
                if (currentPin.equals(firstPin)) {
                    savePin(currentPin);
                    goToMain();
                } else {
                    awaitingConfirm = false;
                    firstPin = "";
                    shakeAndReset(getString(R.string.pin_error_no_match));
                    tvTitle.setText(getString(R.string.pin_title_create));
                }
            }
        } else {
            String saved = getSharedPreferences("ApexPayPrefs", MODE_PRIVATE)
                    .getString("userPin", "");
            if (currentPin.equals(saved)) {
                goToMain();
            } else {
                shakeAndReset(getString(R.string.pin_error_incorrect));
            }
        }
    }

    private void savePin(String pin) {
        getSharedPreferences("ApexPayPrefs", MODE_PRIVATE)
                .edit().putString("userPin", pin).apply();
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    private void shakeAndReset(String errorMsg) {
        tvSubtitle.setText(errorMsg);
        tvSubtitle.setTextColor(getColor(R.color.error));
        vibrate();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                currentPin = "";
                updateDots();
                tvSubtitle.setTextColor(getColor(R.color.text_secondary));
                updateTitleAndSubtitle();
            }
        }, 700);
    }

    private void updateTitleAndSubtitle() {
        if (MODE_SETUP.equals(mode)) {
            tvTitle.setText(getString(R.string.pin_title_create));
            tvSubtitle.setText(getString(R.string.pin_subtitle_create));
        } else {
            tvTitle.setText(getString(R.string.pin_title_enter));
            tvSubtitle.setText(getString(R.string.pin_subtitle_enter));
        }
    }

    @SuppressWarnings("deprecation")
    private void vibrate() {
        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v == null || !v.hasVibrator()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(200);
        }
    }
}
