package com.lam.lam_project.activities;

import androidx.annotation.NonNull;

import android.content.pm.PackageManager;
import android.os.Bundle;

import com.lam.lam_project.databinding.ActivitySettingsBinding;

public class SettingsActivity extends MenuActivity {
    ActivitySettingsBinding activitySettingsBinding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activitySettingsBinding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(activitySettingsBinding.getRoot());

        setActivityTitle("Settings");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 5) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                System.out.println("Notification permission given");
            }
        }
    }
}