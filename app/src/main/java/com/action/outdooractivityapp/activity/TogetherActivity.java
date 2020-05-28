package com.action.outdooractivityapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.action.outdooractivityapp.R;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_together);

        initializeView();

        registerListener();

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

    }


}
