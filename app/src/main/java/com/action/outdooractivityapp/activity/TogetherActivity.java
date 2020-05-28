package com.action.outdooractivityapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.action.outdooractivityapp.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class TogetherActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Intent intent;
    private BottomNavigationView navView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeView();

        registerListener();

        Log.d(TAG,LoginActivity.userMap.toString());

    }

    void initializeView(){
        navView = findViewById(R.id.nav_view);
    }

    void registerListener(){
        navView.setOnNavigationItemSelectedListener(OnNavigationItemSelectedListener);
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
        Log.d(TAG,"메인 onResume()");
        /*하단 네비게이션 checked표시*/
        navView.getMenu().getItem(0).setChecked(true);

    }

}