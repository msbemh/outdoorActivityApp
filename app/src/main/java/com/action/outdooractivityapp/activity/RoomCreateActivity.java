package com.action.outdooractivityapp.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.util.Util;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;
import java.util.Map;

public class RoomCreateActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "RoomCreateActivity";
    private Intent intent;
    private EditText editText_title;
    private EditText editText_password;
    private Button button_create_room;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_create);

        initializeView();

        registerListener();
    }

    void initializeView(){
        editText_title = findViewById(R.id.editText_title);
        editText_password = findViewById(R.id.editText_password);
        button_create_room = findViewById(R.id.button_create_room);
    }

    void registerListener(){
        button_create_room.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.button_create_room){
            Log.d(TAG,"방 생성 클릭");

            String titleString = editText_title.getText().toString();
            String passwordString = editText_password.getText().toString();

            //데이터베이스에 방 생성하기
            String url = "https://wowoutdoor.tk/room/room_insert_query.php";
            String parameters = "user_id="+LoginActivity.userMap.get("user_id").toString();
            parameters += "&title="+titleString;
            String method = "POST";

            if(TextUtils.isEmpty(titleString)){
                Toast.makeText(this,"제목을 입력해주세요.",Toast.LENGTH_SHORT).show();
                return;
            }

            //비밀번호 있으면 parameters에 추가
            if(!TextUtils.isEmpty(passwordString)){
                parameters += "&password="+passwordString;
            }

            //데이터 베이스에서 정보를 가져옴
            List<Map> resultList = Util.httpConn(url, parameters, method);
            Log.d(TAG,"resultList:"+resultList);
            boolean result = Boolean.parseBoolean(resultList.get(0).get("result").toString());

            if(result){
                Toast.makeText(this,"방생성이 완료됐습니다.",Toast.LENGTH_SHORT).show();
                intent = new Intent(this, TogetherActivity.class);
                //점수를 전activity에 돌려주기
                intent.putExtra("room_no", resultList.get(0).get("room_no").toString());
                intent.putExtra("password", resultList.get(0).get("password").toString());
                intent.putExtra("creation_date", resultList.get(0).get("creation_date").toString());
                intent.putExtra("writer", resultList.get(0).get("writer").toString());
                intent.putExtra("title", resultList.get(0).get("title").toString());
                setResult(RESULT_OK, intent);
                finish();
            }else{
                Toast.makeText(this,"방생성에 실패했습니다.",Toast.LENGTH_SHORT).show();
            }

        }
    }
}
