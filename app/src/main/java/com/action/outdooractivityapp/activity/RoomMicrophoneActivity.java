package com.action.outdooractivityapp.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.action.outdooractivityapp.AdminApplication;
import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.service.RadioCommunicationService;
import com.action.outdooractivityapp.util.Util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class RoomMicrophoneActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener {

    private static final String TAG = "RoomMicrophoneActivity";
    private Intent intent;
    private Bundle extras;
    private int roomNo = -1;
    private ImageView image_push_to_talk;
    private final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1;
//    private Button button_push_to_talk_pause;

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

    //서비스의 브로드캐스트 수신받는곳
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        Log.d(TAG,"무전기 브로드캐스트 리시버 동작");
        String result = intent.getStringExtra("result");
        Log.d(TAG,"result:"+result);
            //마이크 동작 가능
            if("connectSuccess".equals(result)){
                image_push_to_talk.setImageResource(R.drawable.radio_communication_send_image);
                AdminApplication.isAvailableMicrophone = true;
            //마이크 동작 불가처리
            }else if("connectFail".equals(result)){
                image_push_to_talk.setImageResource(R.drawable.radio_communication_receive_image);
                AdminApplication.isAvailableMicrophone = false;
            //해제완료(마이크 동작 가능)
            } else if("endSuccess".equals(result)){
                image_push_to_talk.setImageResource(R.drawable.radio_communication_image);
                radioCommunicationService.muteMic();
                AdminApplication.isAvailableMicrophone = true;
            //해제실패(마이크 동작 불가처리)
            } else if("endFail".equals(result)){
                image_push_to_talk.setImageResource(R.drawable.radio_communication_waiting_image);
                radioCommunicationService.muteMic();
                AdminApplication.isAvailableMicrophone = false;
            }

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_microphone);

        initializeView();

        registerListener();

        registerBroadcast();

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

        //오디오 권한 요구하기
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        }
    }

    //권한 설정 결과
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                //마이크 사용 허가했을 때
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                //마이크 사용 허가 안했을 때
                } else {
                    Log.d("TAG", "permission denied by user");
                    Util.toastText(this,"마이크 권한을 허용해야합니다.");
                    finish();
                }
                return;
            }
        }
    }

    void initializeView(){
        image_push_to_talk = findViewById(R.id.image_push_to_talk);
//        button_push_to_talk_pause = findViewById(R.id.button_push_to_talk_pause);
    }

    void registerListener(){
        image_push_to_talk.setOnClickListener(this);
//        button_push_to_talk_pause.setOnClickListener(this);
        image_push_to_talk.setOnTouchListener(this);
    }

    public void registerBroadcast(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(AdminApplication.AUDIO_COMMUNICATION_CHANGED);
        registerReceiver(broadcastReceiver, filter);
    }

    public void unregisterBroadcast(){
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onClick(View v) {
//        //통신 클릭
//        if(v.getId() == R.id.button_push_to_talk) {
//            Log.d(TAG, "통신 클릭");
//            //마이크 녹음 시작
//            radioCommunicationService.startMic();
//        //통신 중단 클릭
//        }else if(v.getId() == R.id.button_push_to_talk_pause){
//            radioCommunicationService.muteMic();
//        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(v.getId() == R.id.image_push_to_talk){
            if(MotionEvent.ACTION_DOWN == event.getAction()) {
                Log.d(TAG, "녹음중...");
                //마이크 녹음 시작
                if(AdminApplication.isAvailableMicrophone){
                    image_push_to_talk.setImageResource(R.drawable.radio_communication_waiting_image);
                    radioCommunicationService.micCheckAndGo();
                }

//                radioCommunicationService.startMic();
            }else if(MotionEvent.ACTION_UP == event.getAction()) {
                Log.d(TAG, "녹음 종료");
                //마이크 녹음 끝내기
//                radioCommunicationService.muteMic();
                if(AdminApplication.isAvailableMicrophone) {
                    radioCommunicationService.micEndCheck();
                }
            }
            return true;
        }
        return false;
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
        //리시버 해제
        unregisterBroadcast();
    }



}
