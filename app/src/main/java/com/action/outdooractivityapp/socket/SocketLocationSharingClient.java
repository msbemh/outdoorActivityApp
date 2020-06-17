package com.action.outdooractivityapp.socket;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.action.outdooractivityapp.AdminApplication;
import com.action.outdooractivityapp.service.LocationSharingService;
import com.action.outdooractivityapp.util.Util;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SocketLocationSharingClient extends AsyncTask<String, Map, String> {

    private String TAG = "SocketLocationSharingClient";
    private LocationSharingService locationSharingService;
    private int roomNo = -1;

    private Socket socket;
    private LocationManager locationManager;

    public SocketLocationSharingClient(LocationSharingService locationSharingService, int roomNo) {
        this.locationSharingService = locationSharingService;
        this.roomNo = roomNo;
    }

    @Override
    protected String doInBackground(String... strings) {
        locationManager = (LocationManager) locationSharingService.getSystemService(Context.LOCATION_SERVICE);
        startClient();
        return null;
    }

    //메시지를 수신받을때 동작해서 UI변경
    @Override
    protected void onProgressUpdate(Map... map) {
        super.onProgressUpdate();
        Log.d(TAG, "onProgressUpdate");

    }

    public void startClient() {
        // connect()와 read() 메소드는 블로킹 되기 때문에 별도의 스레드를 생성해서 처리합니다.
        Thread thread = new Thread() {
            @Override
            public void run() {
                socket = new Socket();
                try {
                    Log.d(TAG, "[서버에 연결 요청 중]");
                    //서버쪽 public IP
                    //아마존 EC2 Ip
//                    socket.connect(new InetSocketAddress("13.125.70.176", 5002));
                    //집 private Ip
                    socket.connect(new InetSocketAddress("192.168.219.165", 5002));

                    //---- 이곳에 PrintWriter을 통해서 유저정보, room정보, 위치정보를 서버에 보내주자 ------
                    OutputStream os = socket.getOutputStream();
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));

                    String userId = AdminApplication.userMap.get("user_id").toString();
                    String nickName = AdminApplication.userMap.get("nick_name").toString();
                    String dataInfo = userId + ";" + roomNo + ";" + nickName;

                    writer.println(dataInfo);
                    writer.flush();
                    //-------------------------------------------------------------------------------------
                    Log.d(TAG, "[위치공유 서버와 연결 완료]");
                } catch (IOException e) {
                    Log.d(TAG, "[위치공유 서버와 통신 안됨]");

                    if (!socket.isClosed()) {
                        stopClient();
                    }
                    return;
                }
                //위치정보 받는 Thread 동작시키기.
                receive();
                //위치정보 보내는 Thread 동작시키기
                send();
            }
        };
        thread.start();
    }

    public void stopClient() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //위치정보 받는 Thread 동작시키기.
    void receive() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        InputStream is = socket.getInputStream();
                        //------Reader 테스트------
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                        String result = reader.readLine();
                        System.out.println("[메시지 수신] result : " + result);
                        //socket이 끊긴경우 thread를 중지시키자.
                        if (result == null) {
                            throw new IOException();
                        }
                        //-----------------------

                        //--------위치정보 json데이터 가져오기--------
                        List<Map> resultList = new ArrayList<Map>();
                        JSONArray jsonArray = null;
                        try {
                            if (!TextUtils.isEmpty(result)) {
                                jsonArray = new JSONArray(result);
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    resultList.add(Util.jsonToMap(jsonArray.getJSONObject(i)));
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        for (Map itemMap : resultList) {
                            Log.d(TAG, itemMap.get("userId").toString());
                            Log.d(TAG, itemMap.get("nickName").toString());
                            Log.d(TAG, itemMap.get("roomNo").toString());
                            Log.d(TAG, itemMap.get("longitude").toString());
                            Log.d(TAG, itemMap.get("latitude").toString());
                        }
                        //---------------------------------------------------------
                        //지도에 같은방 사람들의 위치를 표시해주기 위해서 브로드캐스트 송신
                        Intent intent = new Intent(AdminApplication.LOCATION_SHARE_BROAD_CAST);
                        intent.putExtra("result",  (Serializable) resultList);
                        locationSharingService.sendBroadcast(intent);
//                publishProgress(map);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d(TAG, "[receive]서버와 통신 안됨");
                        stopClient();
                        break;
                    }

                }
            }
        };
        thread.start();
    }

    public void send() {
        // write() 메소드는 블로킹 되기 때문에 별도의 스레드에서 실행합니다.
        Thread thread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        //현재 위치 정보 가져오기
                        //위치권한이 존재할 경우
                        double longitude = 0;
                        double latitude = 0;
                        if (ActivityCompat.checkSelfPermission(locationSharingService, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            Log.d(TAG, "location:" + location);
                            if(location == null){
                                Thread.sleep(1500);
                                continue;
                            }
                            longitude = location.getLongitude();
                            latitude = location.getLatitude();
                            Log.d(TAG, "longitude:" + longitude);
                            Log.d(TAG, "latitude:" + latitude);

                            //------writer------
                            OutputStream os = socket.getOutputStream();
                            PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));

                            String userId = AdminApplication.userMap.get("user_id").toString();
                            String nickName = AdminApplication.userMap.get("nick_name").toString();
                            String dataInfo = userId + ";" + roomNo + ";" + nickName + ";" + longitude + ";" + latitude;
                            writer.println(dataInfo);
                            writer.flush();
                        }


                        //슬립주기
                        Thread.sleep(1500);
                        //-----------------------
                    } catch (Exception e) {

                        e.printStackTrace();
                        Log.d(TAG, "[send]e:"+e);
                        Log.d(TAG, "[send]서버와 통신 안됨");
                        stopClient();
                        break;
                    }
                }
            }
        };
        thread.start();
    }

}
