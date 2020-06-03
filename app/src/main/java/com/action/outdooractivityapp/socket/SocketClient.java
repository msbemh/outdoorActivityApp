package com.action.outdooractivityapp.socket;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.activity.LoginActivity;
import com.action.outdooractivityapp.activity.RoomChatActivity;
import com.action.outdooractivityapp.service.SocketService;
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
    private SocketService socketService;
    private final String NOTIFICATION_CHANNEL_ID = "channel_1";
    private int roomNo = -1;

    Socket socket;

    public SocketClient(SocketService socketService, int roomNo){
        this.socketService = socketService;
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

        //메시지 recyclerview 리로드
        RoomChatActivity.rvChatMessageAdapter.notifyDataSetChanged();

        //스크롤 제일 아래로
        RoomChatActivity.recyclerView_chat_message.scrollToPosition(RoomChatActivity.rvChatMessageAdapter.getItemCount()-1);

        //알림 띄워주기
        notificationGenerate();
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
                    socket.connect(new InetSocketAddress("13.125.70.176", 5001));
                    //집 private Ip
//                    socket.connect(new InetSocketAddress("192.168.219.187", 5001));

                    //---- 이곳에 PrintWriter을 통해서 유저정보와 room정보를 서버에 보내주자 ------
                    OutputStream os = socket.getOutputStream();
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));

                    String userId = LoginActivity.userMap.get("user_id").toString();

                    String userAndRoomInfo = userId+";"+roomNo;

                    writer.println(userAndRoomInfo);
                    writer.flush();
                    //---------------------------------------------------------------------------

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
                    Log.d(TAG, itemMap.get("writer").toString());
                    Log.d(TAG, itemMap.get("nickName").toString());
                    Log.d(TAG, itemMap.get("message").toString());
                    Log.d(TAG, itemMap.get("profileImage").toString());
                    Log.d(TAG, itemMap.get("creationDate").toString());
                }
                //---------------------------------------------------------


                //로컬 메시지 리스트에 메시지 입력
                Map map = resultList.get(0);
                RoomChatActivity.messageList.add(map);

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

    //알림메시지 발생시키기
    public void notificationGenerate() {
        NotificationManager notificationManager = (NotificationManager)socketService.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(socketService, RoomChatActivity.class);
        notificationIntent.putExtra("room_no",roomNo+"");
        Log.d(TAG,"[보내기전]roomNo:"+roomNo);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK) ;
        PendingIntent pendingIntent = PendingIntent.getActivity(socketService, 0, notificationIntent,  PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(socketService, NOTIFICATION_CHANNEL_ID)
                .setLargeIcon(BitmapFactory.decodeResource(socketService.getResources(), R.drawable.ic_launcher_foreground)) //BitMap 이미지 요구
                .setContentTitle("상태바 드래그시 보이는 타이틀")
                .setContentText("상태바 드래그시 보이는 서브타이틀")
                // 더 많은 내용이라서 일부만 보여줘야 하는 경우 아래 주석을 제거하면 setContentText에 있는 문자열 대신 아래 문자열을 보여줌
                //.setStyle(new NotificationCompat.BigTextStyle().bigText("더 많은 내용을 보여줘야 하는 경우..."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent) // 사용자가 노티피케이션을 탭시 ResultActivity로 이동하도록 설정
                .setAutoCancel(true);

        //OREO API 26 이상에서는 채널 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            builder.setSmallIcon(R.drawable.ic_launcher_foreground); //mipmap 사용시 Oreo 이상에서 시스템 UI 에러남
            CharSequence channelName  = "노티페케이션 채널";
            String description = "오레오 이상을 위한 것임";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName , importance);
            channel.setDescription(description);

            // 노티피케이션 채널을 시스템에 등록
            notificationManager.createNotificationChannel(channel);

        }else builder.setSmallIcon(R.mipmap.ic_launcher); // Oreo 이하에서 mipmap 사용하지 않으면 Couldn't create icon: StatusBarIcon 에러남

        //notification 생성
        Notification notification = builder.build();
        // 고유숫자로 노티피케이션 동작시킴
        notificationManager.notify(1234, notification);

    }


}
