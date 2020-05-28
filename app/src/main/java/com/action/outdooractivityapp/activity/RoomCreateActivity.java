package com.action.outdooractivityapp.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.action.outdooractivityapp.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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
        }
    }
}
