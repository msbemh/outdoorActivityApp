package com.action.outdooractivityapp.socket;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public class SocketClient {

    private String TAG = "SocketClient";

    Socket socket;

    public void startClient() {
        // connect()와 read() 메소드는 블로킹 되기 때문에 별도의 스레드를 생성해서 처리합니다.
        Thread thread = new Thread() {
            @Override
            public void run() {
                socket = new Socket();
                try {
                    Log.d(TAG,"[서버에 연결 요청 중]");
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

    void stopClient() {
        if(socket!=null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void receive() {
        while(true) {
            byte[] arr = new byte[100];
            try {
                InputStream is = socket.getInputStream();
                int readByteCnt = is.read(arr);
                if (readByteCnt == -1) {throw new IOException();}
                String message = new String(arr, 0, readByteCnt, "UTF-8");
                Log.d(TAG,"[메시지 수신] : " + message);
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
