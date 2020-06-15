package com.action.outdooractivityapp;

import android.app.Application;
import android.graphics.Bitmap;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class AdminApplication extends Application {

    private final String TAG = "AdminApplication";
    /* 로그인된 user정보 저장 */
    public static Map userMap = new HashMap();
    public static Bitmap profileImage;

    public static boolean isNotification = false;
    public static boolean isAvailableMicrophone = true;
    public final static String AUDIO_COMMUNICATION_CHANGED = "AUDIO_COMMUNICATION_CHANGED";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"관리 어플리케이션 onCreate()");
    }

}
