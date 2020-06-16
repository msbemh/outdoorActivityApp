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

    //Manifest.xml 파일에서 stopWithTask속성을 false로 주어 Task가 날라가도 service의 프로세스는 살 수 있게 함.
    //그리고 아래 onTaskRemoved()로 감지하여 필요한 기능을 수행후 stopself()로 멈추게함.
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Util.toastText(this,"강제종료");
        stopSelf();
    }
}
