package com.action.outdooractivityapp.socket;

import android.os.AsyncTask;
import android.util.Log;

import com.action.outdooractivityapp.AdminApplication;
import com.action.outdooractivityapp.activity.RoomChatActivity;
import com.action.outdooractivityapp.service.RadioCommunicationService;
import com.action.outdooractivityapp.service.SocketService;

import java.util.Map;

public class SocketUDPClient extends AsyncTask<String, Map, String> {

    private String TAG = "SocketUDPClient";
    private int roomNo = -1;
    private RadioCommunicationService radioCommunicationService;

    public SocketUDPClient(RadioCommunicationService radioCommunicationService, int roomNo){
        this.radioCommunicationService = radioCommunicationService;
        this.roomNo = roomNo;
    }

    @Override
    protected String doInBackground(String... strings) {
        return null;
    }

    //메시지를 수신받을때 동작해서 UI변경
    @Override
    protected void onProgressUpdate(Map... map) {
        super.onProgressUpdate();
        Log.d(TAG,"onProgressUpdate");

    }

}
