package com.action.outdooractivityapp.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.service.RadioCommunicationService;
import com.action.outdooractivityapp.util.Util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class RoomMicrophoneActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "RoomMicrophoneActivity";
    private Intent intent;
    private Bundle extras;
    private int roomNo = -1;
    private Button button_push_to_talk;
    private Button button_push_to_talk_pause;

    RadioCommunicationService radioCommunicationService; // 무전기 서비스 객체

    //무전기 서비스와 연결되는 부분
    ServiceConnection radioServiceConnection = new ServiceConnection() {
        // 서비스와 연결되었을 때 호출되는 메서드
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            radioCommunicationService = ((RadioCommunicationService.MyBinder) service).getService();
        }

        // 서비스와 연결이 끊겼을 때 호출되는 메서드
        @Override
        public void onServiceDisconnected(ComponentName name) {
            radioServiceConnection = null;
            radioCommunicationService = null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_microphone);

        initializeView();

        registerListener();

        //방번호 받기
        /*data 받아오기*/
        extras = getIntent().getExtras();
        roomNo = extras.getInt("room_no",-1);
        Log.d(TAG, "roomNo:"+roomNo);

        //무전기 바인드 서비스 시작
        intent = new Intent(this, RadioCommunicationService.class);
        Log.d(TAG,"[무전기 서비스 실행전(무전하는곳!)]roomNo:"+roomNo);
        intent.putExtra("roomNo", roomNo);
        bindService(intent, radioServiceConnection, Context.BIND_AUTO_CREATE);


    }

    void initializeView(){
        button_push_to_talk = findViewById(R.id.button_push_to_talk);
        button_push_to_talk_pause = findViewById(R.id.button_push_to_talk_pause);
    }

    void registerListener(){
        button_push_to_talk.setOnClickListener(this);
        button_push_to_talk_pause.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        //통신 클릭
        if(v.getId() == R.id.button_push_to_talk) {
            Log.d(TAG, "통신 클릭");
            //마이크 녹음 시작
            radioCommunicationService.startMic();
            //통신 중단 클릭
        }else if(v.getId() == R.id.button_push_to_talk_pause){
            radioCommunicationService.muteMic();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //무전기 서비스 unbind시키기
        unbindService(radioServiceConnection);
    }


}
