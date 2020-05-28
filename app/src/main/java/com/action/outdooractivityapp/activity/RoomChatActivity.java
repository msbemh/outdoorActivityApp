package com.action.outdooractivityapp.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.action.outdooractivityapp.R;

public class RoomChatActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "RoomChatActivity";
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_chat);

        initializeView();

        registerListener();
    }

    void initializeView(){
    }

    void registerListener(){
    }

    @Override
    public void onClick(View v) {

    }
}
