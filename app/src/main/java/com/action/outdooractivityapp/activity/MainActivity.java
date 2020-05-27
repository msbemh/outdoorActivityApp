package com.action.outdooractivityapp.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.action.outdooractivityapp.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG,LoginActivity.userMap.toString());

    }

    /*하단 네비게이션바 Listener*/
    private BottomNavigationView.OnNavigationItemSelectedListener OnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()){
                /*홈 선택*/
                case R.id.navigation_home:
                    intent = new Intent(MainActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                    startActivity(intent);
                    return true;
                /*평가하기 선택*/
                case R.id.navigation_evaluate:
//                    intent = new Intent(MainActivity.this, EvaluateMovieActivity.class);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
//                    startActivity(intent);
                    return true;
                /*평가하기 선택*/
                case R.id.navigation_my:
//                    intent = new Intent(MainActivity.this, MyPageActivity.class);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
//                    startActivity(intent);
                    return true;

            }
            return false;
        }
    };
}
