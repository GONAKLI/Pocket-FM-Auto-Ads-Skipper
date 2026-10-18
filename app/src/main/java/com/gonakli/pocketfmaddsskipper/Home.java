package com.gonakli.pocketfmaddsskipper;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Home extends AppCompatActivity {
    AppCompatButton openAccessibility;
    TextView serviceStatusTV;
    private final int notificationReqCode = 501;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        find_all_id();
        show_status();
        getNotificationPermissions();

        openAccessibility.setOnClickListener(v -> {
            Intent iSetting = new Intent();
            iSetting.setAction(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(iSetting);
        });

    }

    private void getNotificationPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(Home.this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(Home.this, new String[]{
                        Manifest.permission.POST_NOTIFICATIONS
                }, notificationReqCode);
            }
        }

    }

    private void show_status() {
        boolean isActive = check_service_status();
        Log.d("homeCheck", "isActive: " + isActive);

        if (isActive) {
            serviceStatusTV.setText(R.string.serviceStatusActive);
            serviceStatusTV.setBackgroundColor(Color.GREEN);

        } else {
            serviceStatusTV.setText(R.string.serviceStatusInactive);
            serviceStatusTV.setBackgroundColor(Color.RED);
        }

    }


    private boolean check_service_status() {
        // to check whether service is on or off, so that we can display about it on the ui
        String expected = getPackageName() + "/" + AdsSkipper.class.getName();
        Log.d("homeCheck", "expected: " + expected);
        String enabled = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        Log.d("homeCheck", "enabled: " + enabled);

        if (enabled == null) return false;
        for (String s : enabled.split(":")) {
            if (s.equalsIgnoreCase(expected)) return true;
        }
        return false;
    }

    private void find_all_id() {
        openAccessibility = findViewById(R.id.openAccessibility);
        serviceStatusTV = findViewById(R.id.serviceStatusTV);
    }

    @Override
    protected void onResume() {
        super.onResume();
        show_status();
    }
}
