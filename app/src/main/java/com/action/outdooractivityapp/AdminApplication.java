package com.action.outdooractivityapp;

import android.app.Application;
import android.util.Log;

public class AdminApplication extends Application {

    private final String TAG = "AdminApplication";
    public static boolean isNotification = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"관리 어플리케이션 onCreate()");
    }
}
