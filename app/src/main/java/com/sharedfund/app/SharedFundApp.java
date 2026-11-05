package com.sharedfund.app;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.sharedfund.app.data.UserSessionManager;

public class SharedFundApp extends Application {

    private static SharedFundApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        SharedPreferences prefs = getSharedPreferences("koin_ui_settings", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_theme", false);
        AppCompatDelegate.setDefaultNightMode(isDark
                ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        UserSessionManager.getInstance(this);
    }

    public static SharedFundApp getInstance() {
        return instance;
    }
}
