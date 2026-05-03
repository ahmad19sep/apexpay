package com.apexpay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View imgLogo = findViewById(R.id.imgLogo);
        TextView tvAppName = findViewById(R.id.tvAppName);
        TextView tvTagline = findViewById(R.id.tvTagline);

        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        imgLogo.startAnimation(fadeIn);
        tvAppName.startAnimation(slideUp);
        tvTagline.startAnimation(slideUp);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                navigateNext();
            }
        }, SPLASH_DELAY_MS);
    }

    private void navigateNext() {
        SharedPreferences prefs = getSharedPreferences("ApexPayPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        boolean hasPin = !prefs.getString("userPin", "").isEmpty();

        Intent intent;
        if (isLoggedIn && hasPin) {
            intent = new Intent(this, PinActivity.class);
            intent.putExtra("mode", PinActivity.MODE_VERIFY);
        } else if (isLoggedIn) {
            intent = new Intent(this, MainActivity.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }

        startActivity(intent);
        finish();
    }
}
