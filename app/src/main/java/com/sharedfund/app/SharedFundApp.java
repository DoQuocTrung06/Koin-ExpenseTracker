package com.sharedfund.app;

import android.app.Application;
import com.sharedfund.app.data.UserSessionManager;

public class SharedFundApp extends Application {

    private static SharedFundApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        UserSessionManager.getInstance(this);
    }

    public static SharedFundApp getInstance() {
        return instance;
    }
}