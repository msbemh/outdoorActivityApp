package com.action.outdooractivityapp.socket;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.action.outdooractivityapp.AdminApplication;
import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.activity.LoginActivity;
import com.action.outdooractivityapp.activity.RoomChatActivity;
import com.action.outdooractivityapp.activity.TogetherActivity;
import com.action.outdooractivityapp.service.SocketService;
import com.action.outdooractivityapp.urlConnection.BringImageFile;
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

public class SocketClient extends AsyncTask<String, Map, String> {

    private String TAG = "SocketClient";
    private SocketService socketService;
    private final String MESSAGES_CHANEL = "message_channel_1";
    private final int NEW_MESSAGE_ID = 1;
    private NotificationManager notificationManager;
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
    protected void onProgressUpdate(Map... map) {
        super.onProgressUpdate();
        Log.d(TAG,"onProgressUpdate");

        //메시지 recyclerview 리로드
        RoomChatActivity.rvChatMessageAdapter.notifyDataSetChanged();

        //스크롤 제일 아래로
        RoomChatActivity.recyclerView_chat_message.scrollToPosition(RoomChatActivity.rvChatMessageAdapter.getItemCount()-1);

        //해당 액티비티 밖에 있을때 notification을 수신할 수 있게한다
        if(AdminApplication.isNotification){
            notificationGenerate(map[0]);
        }

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
                publishProgress(map);
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
    public void notificationGenerate(Map map) {
        //알림채널 만들기
        createMessageNotificationChannel(socketService);

        //-----------------Pending intent 만들기-----------------------------
        Intent intent = new Intent(socketService, RoomChatActivity.class);
        intent.putExtra("room_no",roomNo+"");
        //pendingIntent에 백스택으로 있을 activity를 추가하가위해서 사용
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(socketService);
        stackBuilder.addParentStack(RoomChatActivity.class);
        stackBuilder.addNextIntent(intent);

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        //-----------------------------------------------------------------

        //------이미지 파일 서버에서 Bitmap으로 가져오기-------
        Bitmap bitmap = null;
        String profileImage = map.get("profileImage").toString();
        //bitmap이 null일때
        if(TextUtils.isEmpty(profileImage) || "null".equals(profileImage)){

        //bitmap에 리소스가 존재할때
        }else{
            BringImageFile bringImageFile = new BringImageFile(map.get("profileImage").toString());
            bringImageFile.start();

            try{
                bringImageFile.join();
                //이미지 불러오기 완료되면 가져오기
                bitmap = bringImageFile.getBitmap();
            }
            catch(InterruptedException e){
                e.printStackTrace();
            }
        }
        //----------------------------------------------------

        //build설정
        NotificationCompat.Builder builder = new NotificationCompat.Builder(socketService, MESSAGES_CHANEL)
                //알림 작은 아이콘
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(map.get("nickName").toString())
                .setContentText(map.get("message").toString())
                // 더 많은 내용이라서 일부만 보여줘야 하는 경우 아래 주석을 제거하면 setContentText에 있는 문자열 대신 아래 문자열을 보여줌
                //.setStyle(new NotificationCompat.BigTextStyle().bigText("더 많은 내용을 보여줘야 하는 경우..."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent) // 사용자가 노티피케이션을 탭시 ResultActivity로 이동하도록 설정
                .setAutoCancel(true);
        //BitMap 이미지 요구
        if(bitmap != null){
            builder.setLargeIcon(bitmap);
        }else{
            builder.setLargeIcon(BitmapFactory.decodeResource(socketService.getResources(), R.drawable.icon_profile_invert));
        }

        //notification 생성
        Notification notification = builder.build();
        // 고유숫자로 노티피케이션 동작시킴
        notificationManager.notify(roomNo, notification);

    }

    //알림채널 만들기
    public void createMessageNotificationChannel(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //알림채널 이름
            CharSequence channelName  = context.getString(R.string.messages_channel_name);
            //알림채널 부가설명
            String description = "오레오 이상의 버전에서 지원";
            //알림채널 중요도
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(MESSAGES_CHANEL, channelName , importance);
            channel.setDescription(description);

            // 노티피케이션 채널을 시스템에 등록
            notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
