package com.action.outdooractivityapp.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.action.outdooractivityapp.AdminApplication;
import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.adapter.RVChatMessageAdapter;
import com.action.outdooractivityapp.service.SocketService;
import com.action.outdooractivityapp.socket.SocketClient;
import com.action.outdooractivityapp.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RoomChatActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "RoomChatActivity";
    private Intent intent;
    private Bundle extras;

    private Button button_send;
    private ImageView image_back;
    private EditText editText_message;


    public static List<Map> messageList = new ArrayList<Map>();

    public static RecyclerView recyclerView_chat_message;
    public static RVChatMessageAdapter rvChatMessageAdapter;
    private int roomNo = -1;

    SocketService socketService; // 서비스 객체

    //서비스와 연결되는 부분
    ServiceConnection serviceConnection = new ServiceConnection() {
        // 서비스와 연결되었을 때 호출되는 메서드
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            socketService = ((SocketService.MyBinder) service).getService();
        }

        // 서비스와 연결이 끊겼을 때 호출되는 메서드
        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceConnection = null;
            socketService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_chat);

        initializeView();

        registerListener();

        //방번호 받기
        /*data 받아오기*/
        extras = getIntent().getExtras();
        roomNo = Integer.parseInt(extras.getString("room_no"));
        Log.d(TAG, "roomNo:"+roomNo);

        //--------메시지 리스트 받아오기---------
        String url = "https://wowoutdoor.tk/room/message_select_query.php";
        String parameters = "roomNo="+roomNo;
        String method = "GET";

        //데이터 베이스에서 정보를 가져옴
        messageList = Util.httpConn(url, parameters, method);
        //-------------------------------------


        createApplyRecyclerview();

        //서비스에서 소켓채팅 실행
        Intent intent = new Intent(this, SocketService.class);
        Log.d(TAG,"[서비스 실행전]roomNo:"+roomNo);
        intent.putExtra("roomNo", roomNo);

        //바인드 서비스 시작
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG,"채팅방 onStart()");
        //onStart일때 알림메시지를 받을 수 없게 한다.
        AdminApplication.isNotification = false;

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"채팅방 onResume()");


    }

    void initializeView(){
        button_send = findViewById(R.id.button_send);
        image_back = findViewById(R.id.image_back);
        editText_message = findViewById(R.id.editText_message);
    }

    void registerListener(){
        button_send.setOnClickListener(this);
        image_back.setOnClickListener(this);
    }

    void createApplyRecyclerview(){
        /*리사이클러뷰 생성*/
        recyclerView_chat_message = findViewById(R.id.recyclerView_chat_message);
        recyclerView_chat_message.setHasFixedSize(true);

        /*리사이클러뷰 레이아웃 생성 및 적용*/
        LinearLayoutManager layoutManagerRoom = new LinearLayoutManager(this);
        layoutManagerRoom.setOrientation(LinearLayoutManager.VERTICAL);

        recyclerView_chat_message.setLayoutManager(layoutManagerRoom);

        /*리사이클러뷰에 adapter적용*/
        rvChatMessageAdapter = new RVChatMessageAdapter(this, messageList, R.layout.row_recyclerview_chat_message_receive, R.layout.row_recyclerview_chat_message_send);
        recyclerView_chat_message.setAdapter(rvChatMessageAdapter);

        //스크롤 제일 아래로
        recyclerView_chat_message.scrollToPosition(rvChatMessageAdapter.getItemCount()-1);

    }

    @Override
    public void onClick(View v) {
        //보내기 버튼 클릭
        if(v.getId() == R.id.button_send){
            Log.d(TAG,"버튼클릭");
            String sendMessage = editText_message.getText().toString();
            //보낼 메시지가 존재한다면
            if(!TextUtils.isEmpty(sendMessage)){
                //메시지 송신
//                socketClient.send(sendMessage);
                socketService.send(sendMessage);
                //메시지 초기화
                editText_message.setText("");
            }
        //뒤로가기 클릭
        }else if(v.getId() == R.id.image_back){
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"채팅방 onStop()");
        //onStop일때 알림메시지를 받을 수 있게한다.
        AdminApplication.isNotification = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"채팅방 onPause()");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"채팅방 onDestroy()");

        //메시지 리스트도 초기화
//        messageList.clear();
        unbindService(serviceConnection);

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG,"채팅방 onRestart()");
    }
}
