package com.action.outdooractivityapp.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.action.outdooractivityapp.AdminApplication;
import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.adapter.RVMapUserAdapter;
import com.action.outdooractivityapp.adapter.RVRoomUserAdapter;
import com.action.outdooractivityapp.service.SocketService;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;

import java.util.List;
import java.util.Map;

public class TogetherUserListActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "TogetherUserList";
    private Intent intent;
    private Bundle extras;

    private ImageView image_back;
    private ImageView image_chat;
    private ImageView image_map;
    private ImageView image_microphone;
    private ImageView image_user_list;

    private int roomNo = -1;

    private SocketService socketService; // 채팅 서비스 객체

    private RecyclerView recyclerView_room_user_list;
    public LinearLayoutManager layoutManagerRoomUserList;
    public static RVRoomUserAdapter rvRoomUserAdapter;

    //채팅 서비스와 연결되는 부분
    ServiceConnection chatServiceConnection = new ServiceConnection() {
        // 서비스와 연결되었을 때 호출되는 메서드
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            socketService = ((SocketService.MyBinder) service).getService();
        }

        // 서비스와 연결이 끊겼을 때 호출되는 메서드
        @Override
        public void onServiceDisconnected(ComponentName name) {
            chatServiceConnection = null;
            socketService = null;
        }
    };

    //같은방 유저리스트 브로드캐스트 수신받는곳
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"같은방 유저리스트 브로드캐스트 리시버 동작");

            //유저위치정보 리스트 받아오기
//            List<Map> resultList = (List<Map>)intent.getSerializableExtra("resultList");
//            Log.d(TAG,"resultList:"+resultList);

            //유저목록 리스트 recyclerview 리로드
            rvRoomUserAdapter.notifyDataSetChanged();

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_together_user_list);

        initializeView();

        registerListener();

        createApplyRecyclerview();

        registerBroadcast();

        //방번호 받기
        /*data 받아오기*/
        extras = getIntent().getExtras();
        roomNo = extras.getInt("room_no", -1);
        Log.d(TAG, "roomNo:"+roomNo);

        //채팅 서비스와 바인드시키기
        intent = new Intent(this, SocketService.class);
        Log.d(TAG,"[채팅 서비스와 바인드]roomNo:"+roomNo);
        intent.putExtra("roomNo", roomNo);
        bindService(intent, chatServiceConnection, Context.BIND_AUTO_CREATE);
    }

    void initializeView(){
        image_back = findViewById(R.id.image_back);
        image_chat = findViewById(R.id.image_chat);
        image_map = findViewById(R.id.image_map);
        image_microphone = findViewById(R.id.image_microphone);
        image_user_list = findViewById(R.id.image_user_list);
    }

    void registerListener(){
        image_back.setOnClickListener(this);
        image_chat.setOnClickListener(this);
        image_map.setOnClickListener(this);
        image_microphone.setOnClickListener(this);
        image_user_list.setOnClickListener(this);
    }

    void createApplyRecyclerview(){
        /*리사이클러뷰 생성*/
        recyclerView_room_user_list = findViewById(R.id.recyclerView_room_user_list);
        recyclerView_room_user_list.setHasFixedSize(true);

        /*리사이클러뷰 레이아웃 생성 및 적용*/
        layoutManagerRoomUserList = new LinearLayoutManager(this);
        layoutManagerRoomUserList.setOrientation(LinearLayoutManager.VERTICAL);

        recyclerView_room_user_list.setLayoutManager(layoutManagerRoomUserList);

        /*리사이클러뷰에 adapter적용*/
        rvRoomUserAdapter = new RVRoomUserAdapter(this, SocketService.roomUserList, R.layout.row_recyclerview_room_user);
        recyclerView_room_user_list.setAdapter(rvRoomUserAdapter);
    }

    public void registerBroadcast(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(AdminApplication.SAME_ROOM_USER_LIST);
        registerReceiver(broadcastReceiver, filter);
    }

    public void unregisterBroadcast(){
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onClick(View v) {
        //뒤로가기 클릭
        if(v.getId() == R.id.image_back) {
            finish();
        //채팅 클릭
        }else if(v.getId() == R.id.image_chat){
            intent = new Intent(this, RoomChatActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
            intent.putExtra("room_no", roomNo);
            startActivity(intent);
        //마이크 클릭
        }else if(v.getId() == R.id.image_microphone){
            intent = new Intent(this, RoomMicrophoneActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
            intent.putExtra("room_no", roomNo);
            startActivity(intent);
        //위치공유 클릭
        }else if(v.getId() == R.id.image_map){
            intent = new Intent(this, LocationSharingMap.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
            intent.putExtra("room_no", roomNo);
            startActivity(intent);
        //유저리스트 클릭
        }else if(v.getId() == R.id.image_user_list){
            intent = new Intent(this, TogetherUserListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
            intent.putExtra("room_no", roomNo);
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //채팅 서비스 unbind시키기
        unbindService(chatServiceConnection);
        //유저 리스트 브로드캐스트 해제
        unregisterBroadcast();
    }
}
