package com.action.outdooractivityapp.service;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.action.outdooractivityapp.socket.SocketClient;

public class SocketService extends Service {

    //client와 serivce가 통신하기 위한 Binder
    private IBinder binder = new MyBinder();

    private static String TAG = "SocketService";
    private int roomNo = -1;
    SocketClient socketClient;

    //client에게 service객체를 주어 서로 통신하기위함.
    public class MyBinder extends Binder {
        public SocketService getService() { // 서비스 객체를 리턴
            return SocketService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG,"소켓서비스 onBind()");
        roomNo = intent.getIntExtra("roomNo",-1);
        //소켓 연결
        socketClient = new SocketClient(roomNo);
        //소켓 실행
        socketClient.execute();
        return binder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG,"소켓서비스 onCreate()");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"소켓서비스 onDestroy()");
        //소켓종료
        socketClient.stopClient();

        //AsyncTask종료
        if(socketClient.getStatus() == AsyncTask.Status.RUNNING){
            Log.d(TAG,"AsyncTask강제종료");
            socketClient.cancel(true);
        }

        super.onDestroy();
    }

    //메시지 보내기
    public void send(String message){
        socketClient.send(message);
    }
}
