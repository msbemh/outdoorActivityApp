package com.action.outdooractivityapp.socket;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.action.outdooractivityapp.activity.LoginActivity;
import com.action.outdooractivityapp.activity.RoomChatActivity;
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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SocketClient extends AsyncTask<String, String, String> {

    private String TAG = "SocketClient";
    private int roomNo = -1;

    Socket socket;

    public SocketClient(int roomNo){
        this.roomNo = roomNo;
    }

    //동적으로 메시지UI를 변경하기 위해 AsyncTask사용
    @Override
    protected String doInBackground(String... strings) {
        Log.d(TAG,"doInBackground동작");
        startClient();
        return null;
    }

    //메시지를 수신받을때 동작해서 UI변경
    @Override
    protected void onProgressUpdate(String... strings) {
        super.onProgressUpdate();
        Log.d(TAG,"onProgressUpdate");
        RoomChatActivity.rvChatMessageAdapter.notifyDataSetChanged();
    }

    public void startClient() {
        // connect()와 read() 메소드는 블로킹 되기 때문에 별도의 스레드를 생성해서 처리합니다.
        Thread thread = new Thread() {
            @Override
            public void run() {
                socket = new Socket();
                try {
                    Log.d(TAG,"[서버에 연결 요청 중]");
                    //서버쪽 public IP
                    //아마존 EC2 Ip
//                    socket.connect(new InetSocketAddress("13.125.70.176", 5001));
                    //집 private Ip
                    socket.connect(new InetSocketAddress("192.168.219.187", 5001));

                    //---- 이곳에 PrintWriter을 통해서 유저정보와 room정보를 서버에 보내주자 ------
                    OutputStream os = socket.getOutputStream();
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));

                    String userId = LoginActivity.userMap.get("user_id").toString();

                    String userAndRoomInfo = userId+";"+roomNo;

                    writer.println(userAndRoomInfo);
                    writer.flush();
                    //----------------------------------------------------

                    Log.d(TAG,"[서버와 연결 완료]");
                } catch (IOException e) {
                    Log.d(TAG,"[서버와 통신 안됨]");

                    if(!socket.isClosed()) {
                        stopClient();
                    }
                    return;
                }
                receive();
            }
        };
        thread.start();
    }

    public void stopClient() {
        if(socket!=null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //메시지 받는 부분
    void receive() {
        while(true) {
            try {
                InputStream is = socket.getInputStream();
                //------Reader 테스트------
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String result = reader.readLine();
                System.out.println("[메시지 수신] message : " + result);
                //socket이 끊긴경우 thread를 중지시키자.
                if(result == null) {throw new IOException();}
                //-----------------------

                //----- byte 단위로 읽기 -----
//				byte[] arr = new byte[100];
//				int readByteCnt = is.read(arr);
//				if (readByteCnt == -1) {throw new IOException();}
//				System.out.println("[메시지 수신]readByteCnt : " + readByteCnt);
//				String message = new String(arr, 0, readByteCnt, "UTF-8");
//				System.out.println("[메시지 수신] : " + message);
                //-----------------------

                //--------client, message정보 json데이터 가져오기--------
                List<Map> resultList = new ArrayList<Map>();
                JSONArray jsonArray = null;
                try {
                    if(!TextUtils.isEmpty(result)){
                        jsonArray = new JSONArray(result);
                        for(int i=0; i<jsonArray.length(); i++){
                            resultList.add(Util.jsonToMap(jsonArray.getJSONObject(i)));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                for(Map itemMap : resultList){
                    Log.d(TAG, itemMap.get("userId").toString());
                    Log.d(TAG, itemMap.get("nickName").toString());
                    Log.d(TAG, itemMap.get("message").toString());
                    Log.d(TAG, itemMap.get("profileImage").toString());
                }
                //---------------------------------------------------------


                //로컬 메시지 리스트에 메시지 입력
                Map map = resultList.get(0);
                RoomChatActivity.messageList.add(0, map);

                //메시지에대한 RecyclerView UI 보여주기
                publishProgress();
            } catch (IOException e) {
                Log.d(TAG,"서버와 통신 안됨");
                stopClient();
                break;
            }

        }
    }

    public void send(final String message) {
        // write() 메소드는 블로킹 되기 때문에 별도의 스레드에서 실행합니다.
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    //----- byte 단위로 쓰기 -----
//					byte[] arr = message.getBytes("UTF-8");
//
//					OutputStream os = socket.getOutputStream();
//					os.write(arr);
//					os.flush();
                    //------------------------

                    //------writer 테스트------
                    OutputStream os = socket.getOutputStream();
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
                    writer.println(message);
                    writer.flush();
                    //-----------------------
                } catch(Exception e) {
                    Log.d(TAG,"서버와 통신 안됨");
                    stopClient();
                }
            }
        };
        thread.start();
    }


}
