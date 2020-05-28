package com.action.outdooractivityapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.adapter.RVRoomAdapter;
import com.action.outdooractivityapp.util.Util;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;
import java.util.Map;

public class TogetherActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "TogetherActivity";
    private Intent intent;
    private BottomNavigationView navView;
    private ImageView image_plus;
    private List<Map> roomList;

    private RecyclerView recyclerView_room;
    public RVRoomAdapter rvRoomAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG,"함께해요 onCreate()");

        setContentView(R.layout.activity_together);

        initializeView();

        registerListener();

        createApplyRecyclerview();

        Log.d(TAG,LoginActivity.userMap.toString());

    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.image_plus){
            Log.d(TAG,"플러스 버튼 클릭");
            intent = new Intent(this, RoomCreateActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
            startActivity(intent);
        }
    }

    void initializeView(){
        navView = findViewById(R.id.nav_view);
        image_plus = findViewById(R.id.image_plus);
    }

    void registerListener(){
        navView.setOnNavigationItemSelectedListener(OnNavigationItemSelectedListener);
        image_plus.setOnClickListener(this);
    }

    void createApplyRecyclerview(){
        /*리사이클러뷰 생성*/
        recyclerView_room = findViewById(R.id.recyclerView_room);
        recyclerView_room.setHasFixedSize(true);

        /*리사이클러뷰 레이아웃 생성 및 적용*/
        LinearLayoutManager layoutManagerRoom = new LinearLayoutManager(this);
        layoutManagerRoom.setOrientation(LinearLayoutManager.VERTICAL);

        recyclerView_room.setLayoutManager(layoutManagerRoom);

        //데이터베이스에서 방 SELECT하기
        String url = "https://wowoutdoor.tk/room/room_select_query.php";
        String method = "GET";

        //데이터 베이스에서 정보를 가져옴
        roomList = Util.httpConn(url, null, method);

        /*리사이클러뷰에 adapter적용*/
        rvRoomAdapter = new RVRoomAdapter(this, roomList, R.layout.row_recyclerview_room);
        recyclerView_room.setAdapter(rvRoomAdapter);

    }

    /*하단 네비게이션바 Listener*/
    private BottomNavigationView.OnNavigationItemSelectedListener OnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()){
                /*홈 선택*/
                case R.id.navigation_home:
                    intent = new Intent(TogetherActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                    startActivity(intent);
                    return true;
                /*함께하기 선택*/
                case R.id.navigation_together:
                    intent = new Intent(TogetherActivity.this, TogetherActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                    startActivity(intent);
                    return true;
                /*마이페이지이 선택*/
                case R.id.navigation_my:
                    intent = new Intent(TogetherActivity.this, MyPageActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                    startActivity(intent);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"함께해요 onResume()");

        /*하단 네비게이션 checked표시*/
        navView.getMenu().getItem(1).setChecked(true);

        //데이터베이스에서 방 SELECT하기
        String url = "https://wowoutdoor.tk/room/room_select_query.php";
        String method = "GET";

        //데이터 베이스에서 정보를 가져옴
        roomList = Util.httpConn(url, null, method);
        Log.d(TAG,"roomList:"+roomList);

        //방리스트 item 다시 변화시키기
//        rvRoomAdapter.notifyDataSetChanged();
        /*리사이클러뷰에 adapter적용*/
        rvRoomAdapter = new RVRoomAdapter(this, roomList, R.layout.row_recyclerview_room);
        recyclerView_room.setAdapter(rvRoomAdapter);

    }


}
