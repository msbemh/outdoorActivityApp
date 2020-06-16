package com.action.outdooractivityapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.action.outdooractivityapp.R;

import net.daum.mf.map.api.MapView;

public class LocationSharingMap extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "LocationSharingMap";
    private Intent intent;
    private Bundle extras;
    private int roomNo = -1;

    private ImageView image_back;
    private ImageView image_chat;
    private ImageView image_map;
    private ImageView image_microphone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_sharing_map);

        //방번호 받기
        /*data 받아오기*/
        extras = getIntent().getExtras();
        roomNo = extras.getInt("room_no",-1);
        Log.d(TAG, "roomNo:"+roomNo);

        initializeView();

        registerListener();
    }

    void initializeView(){
        image_back = findViewById(R.id.image_back);
        image_chat = findViewById(R.id.image_chat);
        image_map = findViewById(R.id.image_map);
        image_microphone = findViewById(R.id.image_microphone);
        //카카오지도
        MapView mapView = new MapView(this);
        ViewGroup mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);
    }

    void registerListener(){
        image_back.setOnClickListener(this);
        image_chat.setOnClickListener(this);
        image_map.setOnClickListener(this);
        image_microphone.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        //뒤로가기 클릭
        if(v.getId() == R.id.image_back){
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
        }
    }
}
