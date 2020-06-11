package com.action.outdooractivityapp.activity;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.util.Util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class RoomMicrophoneActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "RoomMicrophoneActivity";
    private Intent intent;
    private Bundle extras;
    private int roomNo = -1;
    private AudioStream audioStream;
    private AudioGroup audioGroup;
    private Button button_push_to_talk;
    private Button button_push_to_talk_pause;

    private String message;

    //유저관리 서버 Port
    private static int SERVER_USER_PORT = 50000;
    //마이크 서버 Port
    private static int SERVER_MIC_PORT = 50001;
    //나와 녹음 통신할 Port가져오기
    private static int SERVER_GET_MY_PORT = 50002;

    private Thread receiveThread;
    private DatagramSocket userSocket;
    private int communicationPort = -1;

    //유저접속 on/off
    private boolean receive = false;
    //마이크 on/off
    private boolean mic = false;
    //스피커 on/off
    private boolean speakers = false;
    private boolean test_receive = false;

    //마이크 관련 변수
    private static final int SAMPLE_RATE = 8000; // Hertz
    private static final int SAMPLE_INTERVAL = 20; // Milliseconds
    private static final int SAMPLE_SIZE = 2; // Bytes
    private static final int BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2; //Bytes

    //집
//    private String serverIP = "192.168.219.165";
    //아마존
    private String serverIP = "13.125.70.176";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_microphone);

        initializeView();

        registerListener();

        //방번호 받기
        /*data 받아오기*/
        extras = getIntent().getExtras();
        roomNo = extras.getInt("room_no",-1);
        Log.d(TAG, "roomNo:"+roomNo);


        //서버에 나에 대한 정보 등록하기
        message = "ADD:"+LoginActivity.userMap.get("user_id")+";"+LoginActivity.userMap.get("nick_name")+";"+roomNo;
        sendMessage(message, SERVER_USER_PORT);

//        //받기 테스트
//        receiveThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                test_receive = true;
//                try {
////                    //소켓 생성
////                    DatagramSocket socket =  new DatagramSocket(2000);
//                    while (test_receive){
//                        //받기
//                        byte[] buffer = new byte[512];
//                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
//                        userSocket.receive(packet);
//
//                        Log.d(TAG,"server ip : "+packet.getAddress() + " , server port : "+ packet.getPort());
//                        Log.d(TAG,"수신된 데이터 : "+ new String(packet.getData()).trim());
//                    }
//                    userSocket.disconnect();
//                    userSocket.close();
//                } catch (SocketException e) {
//                    e.printStackTrace();
//                } catch (UnknownHostException e){
//                    e.printStackTrace();
//                } catch (IOException e){
//                    e.printStackTrace();
//                }
//            }
//        });


    }

    void initializeView(){
        button_push_to_talk = findViewById(R.id.button_push_to_talk);
        button_push_to_talk_pause = findViewById(R.id.button_push_to_talk_pause);
    }

    void registerListener(){
        button_push_to_talk.setOnClickListener(this);
        button_push_to_talk_pause.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        //통신 클릭
        if(v.getId() == R.id.button_push_to_talk) {
            Log.d(TAG, "통신 클릭");
//            //보내기 테스트
//            Thread sendThread = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        message = "hello";
//                        //유저관리 요청 상대 IP
//                        InetAddress address = InetAddress.getByName(serverIP);
//                        //소켓 생성
//                        DatagramSocket socket =  new DatagramSocket();
//                        //message 구성
//                        byte[] data = message.getBytes();
//                        //패킷 생성
//                        DatagramPacket packet = new DatagramPacket(data, data.length, address, 50002);
//                        //보내기
//                        socket.send(packet);
//                        Log.d(TAG, "Sent message( " + message + " ) to " + serverIP);
//
//                        socket.disconnect();
//                        socket.close();
//                    } catch (SocketException e) {
//                        e.printStackTrace();
//                    } catch (UnknownHostException e){
//                        e.printStackTrace();
//                    } catch (IOException e){
//                        e.printStackTrace();
//                    }
//                }
//            });
//            sendThread.start();

            startMic();

        //통신 중단 클릭
        }else if(v.getId() == R.id.button_push_to_talk_pause){
            muteMic();
            muteSpeakers();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {

        receive = false;

        //서버에 나에 대한 정보 끊기
        message = "END:"+LoginActivity.userMap.get("user_id")+";"+LoginActivity.userMap.get("nick_name")+";"+roomNo;
        sendMessage(message, SERVER_USER_PORT);

        super.onDestroy();
        Log.d(TAG,"무전기 onDestroy()");
        Util.toastText(this, "무전기 onDestroy()");
    }

    //메시지로 정보 보내기
    private void sendMessage(final String message, final int port) {
        Thread replyThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //유저관리 요청 상대 IP
                    InetAddress address = InetAddress.getByName(serverIP);
                    //소켓 생성
                    userSocket =  new DatagramSocket();

                    //message 구성
                    byte[] data = message.getBytes();
                    //패킷 생성
                    DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
                    //보내기
                    userSocket.send(packet);

                    Log.d(TAG, "Sent message( " + message + " ) to " + serverIP);

                    //받기
                    Log.d(TAG,"유저관리 소켓에서 데이터 받는중...");
                    userSocket.receive(packet);
                    String communicationPortString = new String(packet.getData(), 0, packet.getLength(),"UTF-8");
                    communicationPort = Integer.parseInt(communicationPortString);
                    Log.d(TAG,"communicationPort:"+communicationPort);

                    //준비완료 됐다고 받기
//                    userSocket.receive(packet);
//                    String ready = new String(packet.getData(), 0, packet.getLength(),"UTF-8");
//                    Log.d(TAG,"ready:"+ready);
//                    if("ready".equals(ready)){
//                        startSpeakers();
//                    }
                    startSpeakers();
                    //                    receiveThread.start();

                }catch(UnknownHostException e) {
                    Log.e(TAG, "Failure. UnknownHostException in sendMessage: " + serverIP);
                }catch(SocketException e) {
                    Log.e(TAG, "Failure. SocketException in sendMessage: " + e);
                }catch(IOException e) {
                    Log.e(TAG, "Failure. IOException in sendMessage: " + e);
                }
            }
        });
        replyThread.start();
    }

    //마이크 시작
    public void startMic() {
        // Creates the thread for capturing and transmitting audio
        mic = true;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Create an instance of the AudioRecord class
                Log.i(TAG, "Send thread started. Thread id: " + Thread.currentThread().getId());

                //오디오 Record 생성
                AudioRecord audioRecorder = new AudioRecord (MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)*10);
                //버퍼 생성
                int bytes_read = 0;
                int bytes_sent = 0;
                byte[] buf = new byte[BUF_SIZE];

                try {
                    // Create a socket and start recording
                    Log.i(TAG, "Packet destination: " + serverIP);
                    //마이크 요청 상대 IP
                    InetAddress address = InetAddress.getByName(serverIP);
                    //소켓 생성
                    DatagramSocket socket = new DatagramSocket();

                    //녹음 시작
                    audioRecorder.startRecording();
                    //녹음 반복
                    while(mic) {
                        // Capture audio from the mic and transmit it
                        //녹음 데이터 읽기
                        bytes_read = audioRecorder.read(buf, 0, BUF_SIZE);
                        //패킷 생성
                        DatagramPacket packet = new DatagramPacket(buf, bytes_read, address, communicationPort);
                        //보내기
                        socket.send(packet);
                        bytes_sent += bytes_read;
                        Log.i(TAG, "Total bytes sent: " + bytes_sent);
                        Thread.sleep(SAMPLE_INTERVAL, 0);
                    }
                    // Stop recording and release resources
                    //녹음 중단
                    audioRecorder.stop();
                    audioRecorder.release();
                    //소켓 끊기
                    socket.disconnect();
                    socket.close();
                    mic = false;
                    return;
                } catch(InterruptedException e) {
                    Log.e(TAG, "InterruptedException: " + e.toString());
                    mic = false;
                }catch(SocketException e) {
                    Log.e(TAG, "SocketException: " + e.toString());
                    mic = false;
                }catch(UnknownHostException e) {
                    Log.e(TAG, "UnknownHostException: " + e.toString());
                    mic = false;
                } catch(IOException e) {
                    Log.e(TAG, "IOException: " + e.toString());
                    mic = false;
                }
            }
        });
        thread.start();
    }

    //스피커 시작
    public void startSpeakers() {
        // Creates the thread for receiving and playing back audio
        if(!speakers) {
            speakers = true;
            Thread receiveThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    // Create an instance of AudioTrack, used for playing back audio
                    Log.i(TAG, "Receive thread started. Thread id: " + Thread.currentThread().getId());

                    try {
                        //스피커 요청 상대 IP
                        InetAddress address = InetAddress.getByName(serverIP);
                        //소켓생성
                        DatagramSocket socket = new DatagramSocket();
                        //message 구성
                        String message = "connect";
                        byte[] data = message.getBytes();
                        //패킷 생성
                        DatagramPacket packet = new DatagramPacket(data, data.length, address, communicationPort);
                        //보내기
                        Log.i(TAG, "connect 보내기");
                        socket.send(packet);
                        //받기
                        Log.i(TAG, "connect 대기중...");
                        socket.receive(packet);
                        String info = new String(packet.getData(), 0, packet.getLength(),"UTF-8");
                        Log.d(TAG, "[스피커]"+info);

                        //오디오 트랙 생성
                        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                                AudioFormat.ENCODING_PCM_16BIT, BUF_SIZE, AudioTrack.MODE_STREAM);
                        //트랙 실행
                        track.play();

                        int bytes_receive = 0;
                        while(speakers) {
                            //버퍼 생성
                            byte[] buf = new byte[BUF_SIZE];
                            // Play back the audio received from packets
                            //패킷 생성
                            packet = new DatagramPacket(buf, BUF_SIZE);
                            //받기
                            Log.d(TAG,"스피커 받을준비!!!");

                            socket.receive(packet);
                            Log.i(TAG, "Packet received: " + packet.getLength());
//                            bytes_receive += buf.length;
                            Log.i(TAG, "Total bytes receive: " + bytes_receive);
                            //트랙에 데이터 쓰기
                            track.write(packet.getData(), 0, BUF_SIZE);
                        }
                        // Stop playing back and release resources
                        //소켓 끊기
                        socket.disconnect();
                        socket.close();
                        //트랙 끊고 비우기
                        track.stop();
                        track.flush();
                        track.release();
                        speakers = false;
                        return;
                    }catch(SocketException e) {
                        Log.e(TAG, "SocketException: " + e.toString());
                        speakers = false;
                    }catch(IOException e) {
                        Log.e(TAG, "IOException: " + e.toString());
                        speakers = false;
                    }
                }
            });
            receiveThread.start();
        }
    }

    //마이크 끝내기
    public void muteMic() {
        mic = false;
    }

    //스피커 끝내기
    public void muteSpeakers() {
        speakers = false;
    }


}
