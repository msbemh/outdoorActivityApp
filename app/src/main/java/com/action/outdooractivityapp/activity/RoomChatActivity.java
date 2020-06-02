package com.action.outdooractivityapp.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.adapter.RVChatMessageAdapter;
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
    private SocketClient socketClient;
    private ImageView image_back;
    private EditText editText_message;


    public static List<Map> messageList = new ArrayList<Map>();

    private RecyclerView recyclerView_chat_message;
    public static RVChatMessageAdapter rvChatMessageAdapter;
    private int roomNo = -1;

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
        Log.d(TAG, "size:"+messageList.size());
        for(Map itemMap : messageList){
            Log.d(TAG, itemMap.get("messageNo").toString());
        }

        //-------------------------------------


        createApplyRecyclerview();

        //소켓 연결
        socketClient = new SocketClient(roomNo);
        socketClient.execute();
//        socketClient.startClient();

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
//                sendMessage = LoginActivity.userMap.get("user_id").toString()+"|"+room_no+"|"+sendMessage;
                socketClient.send(sendMessage);
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

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"채팅방 onPause()");


    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"채팅방 onStop()");
        Log.d(TAG,"채팅방 onDestroy()");
        //소켓종료
        socketClient.stopClient();
        //AsyncTask종료
        if(socketClient.getStatus() == AsyncTask.Status.RUNNING){
            Log.d(TAG,"AsyncTask강제종료");
            socketClient.cancel(true);
        }
        //메시지 리스트도 초기화
        messageList.clear();

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG,"채팅방 onRestart()");
    }
}
