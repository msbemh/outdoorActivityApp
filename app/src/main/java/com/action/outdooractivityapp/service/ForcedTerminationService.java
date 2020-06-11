package com.action.outdooractivityapp.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.action.outdooractivityapp.util.Util;

//앱 강제 종료시켰을때 Destroy 동작 시키기 위해서 추가
public class ForcedTerminationService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Util.toastText(this,"강제종료");
        stopSelf();
    }
}
