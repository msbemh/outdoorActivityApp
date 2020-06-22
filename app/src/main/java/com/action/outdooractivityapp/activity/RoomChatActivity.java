package com.action.outdooractivityapp.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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
import com.action.outdooractivityapp.service.LocationSharingService;
import com.action.outdooractivityapp.service.RadioCommunicationService;
import com.action.outdooractivityapp.service.SocketService;
import com.action.outdooractivityapp.util.Util;

import net.daum.mf.map.api.MapView;

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
    private ImageView image_chat;
    private ImageView image_map;
    private ImageView image_microphone;
    private ImageView image_user_list;

    public static List<Map> messageList = new ArrayList<Map>();

    public static RecyclerView recyclerView_chat_message;
    public static RVChatMessageAdapter rvChatMessageAdapter;
    private int roomNo = -1;

    SocketService socketService; // 채팅 서비스 객체
    RadioCommunicationService radioCommunicationService; // 무전기 서비스 객체
    LocationSharingService locationSharingService; // 무전기 서비스 객체

    private final int MY_PERMISSIONS_REQUEST_LOCATION = 1;
    private LocationManager locationManager;


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

    //위치공유 서비스와 연결되는 부분
    ServiceConnection locationSharingServiceConnection = new ServiceConnection() {
        // 서비스와 연결되었을 때 호출되는 메서드
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            locationSharingService = ((LocationSharingService.MyBinder) service).getService();
        }

        // 서비스와 연결이 끊겼을 때 호출되는 메서드
        @Override
        public void onServiceDisconnected(ComponentName name) {
            locationSharingServiceConnection = null;
            locationSharingService = null;
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
        roomNo = extras.getInt("room_no", -1);
        Log.d(TAG, "roomNo:"+roomNo);

        //--------메시지 리스트 받아오기---------
        String url = "https://wowoutdoor.tk/room/message_select_query.php";
        String parameters = "roomNo="+roomNo;
        String method = "GET";

        //데이터 베이스에서 정보를 가져옴
        messageList = Util.httpConn(url, parameters, method);
        //-------------------------------------


        createApplyRecyclerview();

        //채팅 바인드 서비스 시작
        Intent intent = new Intent(this, SocketService.class);
        Log.d(TAG,"[채팅 서비스 실행전]roomNo:"+roomNo);
        intent.putExtra("roomNo", roomNo);
        bindService(intent, chatServiceConnection, Context.BIND_AUTO_CREATE);


        //무전기 바인드 서비스 시작
        intent = new Intent(this, RadioCommunicationService.class);
        Log.d(TAG,"[무전기 서비스 실행전]roomNo:"+roomNo);
        intent.putExtra("roomNo", roomNo);
        bindService(intent, radioServiceConnection, Context.BIND_AUTO_CREATE);


        //나의 위치 1.5초 또는 1m 변경 될때마다 갱신 시키기.(LastLocation얻을때 값이 반영 돼있음.)
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1500, 1, locationListener);
        }


        //위치허용 권한 요구하기
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
        //이미 위치권한이 허용되어 있는 경우
        }else{
            //위치공유 서비스 시작
            intent = new Intent(this, LocationSharingService.class);
            Log.d(TAG,"[위치공유 서비스 실행전]roomNo:"+roomNo);
            intent.putExtra("roomNo", roomNo);
            bindService(intent, locationSharingServiceConnection, Context.BIND_AUTO_CREATE);
        }



    }

    //권한 설정 결과
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                //위치 사용 허가했을 때
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //위치공유 서비스 시작
                    intent = new Intent(this, LocationSharingService.class);
                    Log.d(TAG,"[위치공유 서비스 실행전]roomNo:"+roomNo);
                    intent.putExtra("roomNo", roomNo);
                    bindService(intent, locationSharingServiceConnection, Context.BIND_AUTO_CREATE);
                //위치 사용 허가 안했을 때
                } else {
                    Log.d(TAG, "위치권한을 허용해야합니다.");
                    Util.toastText(this,"위치권한을 허용해야합니다.");
                    finish();
                }
                return;
            }
        }
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
        image_chat = findViewById(R.id.image_chat);
        image_map = findViewById(R.id.image_map);
        image_microphone = findViewById(R.id.image_microphone);
        image_user_list = findViewById(R.id.image_user_list);
    }

    void registerListener(){
        button_send.setOnClickListener(this);
        image_back.setOnClickListener(this);
        image_chat.setOnClickListener(this);
        image_map.setOnClickListener(this);
        image_microphone.setOnClickListener(this);
        image_user_list.setOnClickListener(this);
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
                socketService.send(sendMessage);
                //메시지 초기화
                editText_message.setText("");
            }
        //뒤로가기 클릭
        }else if(v.getId() == R.id.image_back){
            finish();
        //마이크 클릭
        }else if(v.getId() == R.id.image_microphone){
            intent = new Intent(this, RoomMicrophoneActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
            intent.putExtra("room_no", roomNo);
            startActivity(intent);
        //위치공유맵 클릭
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
        Util.toastText(this,"채팅방 onDestroy()");
        Log.d(TAG,"채팅방 onDestroy()");

        //메시지 리스트도 초기화
//        messageList.clear();
        //채팅 서비스 unbind시키기
        unbindService(chatServiceConnection);
        //무전기 서비스 unbind시키기
        unbindService(radioServiceConnection);
        //위치 공유 서비스 unbind시키기
        unbindService(locationSharingServiceConnection);
        //현재위치 받아오기 중지시키기
        locationManager.removeUpdates(locationListener);

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG,"채팅방 onRestart()");
    }

    //위치에 관한 리스너
    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            //여기서 위치값이 갱신되면 이벤트가 발생한다.
            //값은 Location 형태로 리턴되며 좌표 출력 방법은 다음과 같다.
            if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                //Gps 위치제공자에 의한 위치변화. 오차범위가 좁다.
                double longitude = location.getLongitude();    //경도
                double latitude = location.getLatitude();         //위도
                float accuracy = location.getAccuracy();        //신뢰도
                Log.d(TAG,"[리스너]longitude:"+longitude);
                Log.d(TAG,"[리스너]latitude:"+latitude);
                Log.d(TAG,"[리스너]accuracy:"+accuracy);
            } else {
                //Network 위치제공자에 의한 위치변화
                //Network 위치는 Gps에 비해 정확도가 많이 떨어진다.
                Log.d(TAG,"[리스너]동작안됨!!!");
            }
        }

        public void onProviderDisabled(String provider) {}

        public void onProviderEnabled(String provider) {}

        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };


}
