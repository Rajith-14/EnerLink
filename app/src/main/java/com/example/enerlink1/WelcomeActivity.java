package com.example.enerlink1;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class WelcomeActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 1001;

    private Button btnGrantPermissions, btnExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        btnGrantPermissions = findViewById(R.id.btnGrantPermissions);
        btnExit = findViewById(R.id.btnExit);

        btnGrantPermissions.setOnClickListener(v -> {
            if (hasPermissions()) {
                goToMainApp(); // or ScanActivity, or ChatActivity
            } else {
                requestAllPermissions();
            }
        });

        btnExit.setOnClickListener(v -> finish());
    }

    private boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.POST_NOTIFICATIONS
                    }, REQUEST_PERMISSIONS);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    }, REQUEST_PERMISSIONS);
        }
    }

    private void goToMainApp() {
        // Save flag so splash knows welcome screen was already shown
        SharedPreferences.Editor editor = getSharedPreferences("enerlink_prefs", MODE_PRIVATE).edit();
        editor.putBoolean("isFirstRun", false);
        editor.apply();

        startActivity(new Intent(this, ScanActivity.class));
        finish();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (hasPermissions()) {
                goToMainApp();
            } else {
                Toast.makeText(this, "Please grant all permissions to proceed.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
