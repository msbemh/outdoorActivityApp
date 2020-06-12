package com.action.outdooractivityapp.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class RadioCommunicationService extends Service {

    private static String TAG = "RadioCommunicationService";
    private int roomNo = -1;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG,"무전기 서비스 onBind()");
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG,"무전기 서비스 onCreate()");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"무전기 서비스 onDestroy()");
        super.onDestroy();
    }
}
