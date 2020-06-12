package com.action.outdooractivityapp.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.action.outdooractivityapp.socket.SocketUDPClient;


public class RadioCommunicationService extends Service {

    private IBinder binder = new RadioCommunicationService.MyBinder();
    private static String TAG = "RadioCommunicationService";
    private int roomNo = -1;
    SocketUDPClient socketUDPClient = null;

    //client에게 service객체를 주어 서로 통신하기위함.
    public class MyBinder extends Binder {
        public RadioCommunicationService getService() { // 서비스 객체를 리턴
            return RadioCommunicationService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG,"무전기 서비스 onBind()");
        roomNo = intent.getIntExtra("roomNo",-1);

        //싱글톤으로 동작하기 위해서
        Log.d(TAG,"무전기 서비스 socketUDPClient생성!!");
        //무전기 통신하기
        socketUDPClient = new SocketUDPClient(this, roomNo);
        socketUDPClient.execute();
        return binder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG,"무전기 서비스 onCreate()");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"무전기 서비스 onDestroy()");
        super.onDestroy();
        //스피커와 마이크 끄기
        Log.d(TAG,"무전기 서비스 스피커와 마이크꺼짐(소켓포함, 서버에도 알림)");
        socketUDPClient.sendCommunicationEnd();
    }

    public void startMic(){
        socketUDPClient.startMic();
    }

    public void muteMic(){
        socketUDPClient.muteMic();
    }
}
