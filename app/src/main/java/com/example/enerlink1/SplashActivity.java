package com.example.enerlink1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("enerlink_prefs", MODE_PRIVATE);
            boolean isFirstRun = prefs.getBoolean("isFirstRun", true);

            if (isFirstRun) {
                startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, ScanActivity.class));
            }
            finish();
        }, SPLASH_DELAY);
    }
}
