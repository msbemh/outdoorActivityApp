package com.action.outdooractivityapp;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.action.outdooractivityapp.activity.MainActivity;
import com.action.outdooractivityapp.service.ForcedTerminationService;

public class AdminApplication extends Application {

    private final String TAG = "AdminApplication";
    public static boolean isNotification = false;
    public final static String AUDIO_COMMUNICATION_CHANGED = "AUDIO_COMMUNICATION_CHANGED";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"관리 어플리케이션 onCreate()");
    }
}
