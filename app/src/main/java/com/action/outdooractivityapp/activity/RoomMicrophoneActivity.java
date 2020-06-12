package com.action.outdooractivityapp.activity;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
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
    private Button button_push_to_talk;
    private Button button_push_to_talk_pause;

    private String message;

    //유저관리 서버 Port
    private static int SERVER_USER_PORT = 50000;

    private DatagramSocket userSocket;
    private int communicationPort = -1;

    //마이크 on/off
    private boolean mic = false;
    //스피커 on/off
    private boolean speakers = false;

    //마이크 관련 변수
    private static final int SAMPLE_RATE = 8000; // Hertz
    private static final int SAMPLE_INTERVAL = 20; // Milliseconds
    private static final int SAMPLE_SIZE = 2; // Bytes
    private static final int AUDIO_BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2; //Bytes

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
        userManage(message, SERVER_USER_PORT);

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
            //마이크 녹음 시작
            startMic();
            //통신 중단 클릭
        }else if(v.getId() == R.id.button_push_to_talk_pause){
            muteMic();
//            muteSpeakers();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //서버에 나에 대한 정보 끊기
        Thread endThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    message = "END:"+LoginActivity.userMap.get("user_id")+";"+LoginActivity.userMap.get("nick_name")+";"+roomNo;
                    //UDP 통신 설정
                    InetAddress address = InetAddress.getByName(serverIP);
                    byte[] data = message.getBytes();
                    DatagramPacket packet = new DatagramPacket(data, data.length, address, SERVER_USER_PORT);

                    //서버로 송신
                    userSocket.send(packet);
                    userManage(message, SERVER_USER_PORT);

                    //유저관리 소켓 닫기
                    userSocket.disconnect();
                    userSocket.close();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    //유저관리 소켓 닫기
                    userSocket.disconnect();
                    userSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    //유저관리 소켓 닫기
                    userSocket.disconnect();
                    userSocket.close();
                }
            }
        });
        endThread.start();

        //마이크, 시피커 중단시키기
        muteMic();
        muteSpeakers();

        Log.d(TAG,"무전기 onDestroy()");
        Util.toastText(this, "무전기 onDestroy()");
    }

    //유저 서버에 등록
    private void userManage(final String message, final int port) {
        Thread replyThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //UDP 통신 설정
                    InetAddress address = InetAddress.getByName(serverIP);
                    userSocket =  new DatagramSocket();
                    byte[] data = message.getBytes();
                    DatagramPacket packet = new DatagramPacket(data, data.length, address, port);

                    //서버로 송신
                    userSocket.send(packet);

                    //서버 Client 와 나와 연동할 Port가져오기
                    Log.d(TAG,"오디오데이터 통신할 Port얻오는중...");
                    userSocket.receive(packet);
                    String communicationPortString = new String(packet.getData(), 0, packet.getLength(),"UTF-8");
                    communicationPort = Integer.parseInt(communicationPortString);
                    Log.d(TAG,"포트결과 : "+communicationPort);

                    //스피커 동작시키기
                    startSpeakers();
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
        //마이크가 꺼져있을 때만 마이크 시작
        if(!mic){
            mic = true;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //오디오 Record 생성
                        AudioRecord audioRecorder = new AudioRecord (MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE,
                                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                                AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)*10);

                        //오디오 UDP 통신 설정
                        byte[] buf = new byte[AUDIO_BUF_SIZE];
                        InetAddress address = InetAddress.getByName(serverIP);
                        DatagramSocket socket = new DatagramSocket();

                        //녹음 시작
                        audioRecorder.startRecording();
                        //녹음 반복
                        while(mic) {
                            //녹음 데이터 읽기
                            audioRecorder.read(buf, 0, AUDIO_BUF_SIZE);
                            //녹음 데이터 서버로 전송
                            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, communicationPort);
                            socket.send(packet);

                            Log.d(TAG, "오디오 데이터 보낸 패킷: " + packet.getLength());
                            Thread.sleep(SAMPLE_INTERVAL, 0);
                        }

                        //녹음 중단
                        audioRecorder.stop();
                        audioRecorder.release();
                        //소켓 끊기
                        socket.disconnect();
                        socket.close();
                        mic = false;
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
    }

    //스피커 시작
    public void startSpeakers() {
        //스피커가 꺼져있을때만 다시 동작함.
        if(!speakers) {
            speakers = true;
            Thread receiveThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //서버의 Client객체와 나와 connect 시키기
                        InetAddress address = InetAddress.getByName(serverIP);
                        DatagramSocket socket = new DatagramSocket();
                        String message = "connect";
                        byte[] connectData = message.getBytes();
                        DatagramPacket packet = new DatagramPacket(connectData, connectData.length, address, communicationPort);

                        Log.i(TAG, "[서버 Client]connect 대기중...");
                        socket.send(packet);
                        socket.receive(packet);
                        Log.i(TAG, "[서버 Client]connected");


                        //오디오 트랙 생성
                        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                                AudioFormat.ENCODING_PCM_16BIT, AUDIO_BUF_SIZE, AudioTrack.MODE_STREAM);
                        //트랙 실행
                        track.play();

                        while(speakers) {
                            //버퍼 생성
                            byte[] buf = new byte[AUDIO_BUF_SIZE];
                            // Play back the audio received from packets
                            //패킷 생성
                            packet = new DatagramPacket(buf, AUDIO_BUF_SIZE);
                            //받기
                            Log.d(TAG,"스피커 받을준비!!!");

                            socket.receive(packet);
                            Log.d(TAG, "오디오 데이터 받은 패킷: " + packet.getLength());
                            //트랙에 데이터 쓰기
                            track.write(packet.getData(), 0, AUDIO_BUF_SIZE);
                        }

                        //소켓 끊기
                        socket.disconnect();
                        socket.close();
                        //트랙 끊고 비우기
                        track.stop();
                        track.flush();
                        track.release();
                        speakers = false;
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
