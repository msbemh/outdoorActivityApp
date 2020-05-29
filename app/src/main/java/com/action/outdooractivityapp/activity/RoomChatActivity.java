package com.action.outdooractivityapp.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.socket.SocketClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class RoomChatActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "RoomChatActivity";
    private Intent intent;
    private Bundle extras;

    private Button button_send;
    private SocketClient socketClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_chat);

        initializeView();

        registerListener();

        //방번호 받기
        /*data 받아오기*/
        extras = getIntent().getExtras();
        int room_no = Integer.parseInt(extras.getString("room_no"));
        Log.d(TAG, "room_no:"+room_no);

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"채팅방 onResume()");

        //소켓 연결
        socketClient = new SocketClient();
        socketClient.startClient();
    }

    void initializeView(){
        button_send = findViewById(R.id.button_send);
    }

    void registerListener(){
        button_send.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.button_send){
            Log.d(TAG,"버튼클릭");
            String message = "안녕하세요";
            socketClient.send(message);
        }
    }
}
