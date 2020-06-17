package com.action.outdooractivityapp.service;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.action.outdooractivityapp.socket.SocketLocationSharingClient;

public class LocationSharingService extends Service {

    //client와 serivce가 통신하기 위한 Binder
    private IBinder binder = new LocationSharingService.MyBinder();
    private int roomNo = -1;
    private static String TAG = "LocationSharingService";
    SocketLocationSharingClient socketLocationSharingClient = null;

    //client에게 service객체를 주어 서로 통신하기위함.
    public class MyBinder extends Binder {
        public LocationSharingService getService() { // 서비스 객체를 리턴
            return LocationSharingService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG,"위치공유 서비스 onBind()");
        roomNo = intent.getIntExtra("roomNo",-1);
        //위치공유 연결
        socketLocationSharingClient = new SocketLocationSharingClient(this, roomNo);
        //위치공유 실행
        socketLocationSharingClient.execute();

        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"위치공유 서비스 onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"위치공유 서비스 onDestroy()");
        //소켓종료
        socketLocationSharingClient.stopClient();
        //AsyncTask종료
        if(socketLocationSharingClient.getStatus() == AsyncTask.Status.RUNNING){
            Log.d(TAG,"AsyncTask강제종료");
            socketLocationSharingClient.cancel(true);
        }
    }
}
