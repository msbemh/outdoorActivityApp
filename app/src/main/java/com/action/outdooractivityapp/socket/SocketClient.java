package com.action.outdooractivityapp.socket;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.action.outdooractivityapp.activity.RoomChatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class SocketClient extends AsyncTask<String, String, String> {

    private String TAG = "SocketClient";

    Socket socket;

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
                    socket.connect(new InetSocketAddress("13.125.70.176", 5001));
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
            byte[] arr = new byte[100];
            try {
                InputStream is = socket.getInputStream();
                int readByteCnt = is.read(arr);
                if (readByteCnt == -1) {throw new IOException();}
                String message = new String(arr, 0, readByteCnt, "UTF-8");
                Log.d(TAG,"[메시지 수신] : " + message);

                //로컬 메시지 리스트에 메시지 입력
                Map map = new HashMap();
                map.put("message",message);
                RoomChatActivity.messageList.add(0, map);
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
                    byte[] arr = message.getBytes("UTF-8");
                    OutputStream os = socket.getOutputStream();
                    os.write(arr);
                    os.flush();
                } catch(Exception e) {
                    Log.d(TAG,"서버와 통신 안됨");
                    stopClient();
                }
            }
        };
        thread.start();
    }


}
