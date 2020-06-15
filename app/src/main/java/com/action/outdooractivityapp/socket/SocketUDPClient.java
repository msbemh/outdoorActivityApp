package com.action.outdooractivityapp.socket;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import com.action.outdooractivityapp.AdminApplication;
import com.action.outdooractivityapp.service.RadioCommunicationService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class SocketUDPClient extends AsyncTask<String, String, String> {

    private String TAG = "SocketUDPClient";
    private int roomNo = -1;
    private RadioCommunicationService radioCommunicationService;

    private String message;
    private final int BUF_SIZE = 1024;

    //유저관리 서버 Port
    private static int SERVER_USER_PORT = 50000;
    //마이크 접속 서버 Port
    private static int AUDIO_MANAGE_ACCEPT_PORT = 50001;
    //마이크 접속 관리 서버 Port
    private static int AUDIO_MANAGE_PORT = 50002;

    private DatagramSocket userSocket = null;
    private int communicationPort = -1;
    private DatagramSocket micManageAcceptSocket = null;

    //마이크 on/off
    private boolean mic = false;
    //스피커 on/off
    private boolean speakers = false;
    //마이크 사용 여부 on/off
    private boolean micAvailable = false;

    //마이크 관련 변수
    private static final int SAMPLE_RATE = 8000; // Hertz
    private static final int SAMPLE_INTERVAL = 20; // Milliseconds
    private static final int SAMPLE_SIZE = 2; // Bytes
    private static final int AUDIO_BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2; //Bytes

    //집
//    private String serverIP = "192.168.219.165";
    //아마존
    private String serverIP = "13.125.70.176";


    public SocketUDPClient(RadioCommunicationService radioCommunicationService, int roomNo){
        this.radioCommunicationService = radioCommunicationService;
        this.roomNo = roomNo;
    }

    @Override
    protected String doInBackground(String... strings) {
        //서버에 나에 대한 정보 등록하기
        message = "ADD:"+ AdminApplication.userMap.get("user_id")+";"+AdminApplication.userMap.get("nick_name")+";"+roomNo;
        userManage(message, SERVER_USER_PORT);
        return null;
    }

    //메시지를 수신받을때 동작해서 UI변경
    @Override
    protected void onProgressUpdate(String... strings) {
        super.onProgressUpdate();
        Log.d(TAG,"onProgressUpdate동작!!");


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

                    //client생성 완료 체크
                    Log.d(TAG,"client생성 완료 체크 얻어오는중...");
                    userSocket.receive(packet);
                    String clientCreateCheck = new String(packet.getData(), 0, packet.getLength(),"UTF-8");
                    Log.d(TAG,"clientCreateCheck : "+clientCreateCheck);

                    //마이크 관리 접속 시키기
                    micManageAccept();

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
                        InetAddress  address = InetAddress.getByName(serverIP);
                        DatagramSocket socket = new DatagramSocket();

                        //녹음 시작
                        audioRecorder.startRecording();
                        //녹음 반복
                        while(mic) {
                            //녹음 중단
                            if(communicationPort == -1){
                                break;
                            }
                            //녹음 데이터 읽기
                            audioRecorder.read(buf, 0, AUDIO_BUF_SIZE);
                            //녹음 데이터 서버로 전송
                            Log.d(TAG, "communicationPort: " + communicationPort);
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

    public void muteMicAvailable(){
        micAvailable = false;
    }

    //서버에 연결 끊는다고 알리기
    public void sendCommunicationEnd(){
        //서버에 나에 대한 정보 끊기
        Thread endThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    message = "END:"+AdminApplication.userMap.get("user_id")+";"+AdminApplication.userMap.get("nick_name")+";"+roomNo;
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
                    //마이크관리 접속 소켓 닫기
                    micManageAcceptSocket.disconnect();
                    micManageAcceptSocket.close();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    //유저관리 소켓 닫기
                    userSocket.disconnect();
                    userSocket.close();
                    //마이크관리 접속 소켓 닫기
                    micManageAcceptSocket.disconnect();
                    micManageAcceptSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    //유저관리 소켓 닫기
                    userSocket.disconnect();
                    userSocket.close();
                    //마이크관리 접속 소켓 닫기
                    micManageAcceptSocket.disconnect();
                    micManageAcceptSocket.close();
                }
            }
        });
        endThread.start();

        //마이크, 스피커, 마이크 이용가능여부 중단시키기
        muteMic();
        muteSpeakers();
        muteMicAvailable();
    }

    //마이크 사용 관리 접속 & 마이크 이용가능 여부 데이터받기
    void micManageAccept(){
        micAvailable = true;
        Thread micManageAcceptThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //마이크 사용 관리 접속 시키기
                    String message = AdminApplication.userMap.get("user_id")+";"+roomNo;
                    byte[] data = message.getBytes();
                    InetAddress address = InetAddress.getByName(serverIP);
                    micManageAcceptSocket = new DatagramSocket();
                    DatagramPacket packet = new DatagramPacket(data, data.length, address, AUDIO_MANAGE_ACCEPT_PORT);
                    micManageAcceptSocket.send(packet);

                    //마이크 이용가능 여부 데이터 받기
                    while(micAvailable){
                        //이용 가능여부 결과 받기
                        Log.d(TAG,"이용 가능 여부 받는중...");
                        byte[] buffer = new byte[BUF_SIZE];
                        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length, address, AUDIO_MANAGE_ACCEPT_PORT);
                        micManageAcceptSocket.receive(receivePacket);
                        String result = new String(receivePacket.getData(), 0, receivePacket.getLength(),"UTF-8");
                        Log.d(TAG,"result:"+result);

                        //마이크 동작 가능
                        if("connectSuccess".equals(result)){
                            //무전기 통신 버튼 UI변경을 위해서 브로드캐스트 송신
                            Intent intent = new Intent(AdminApplication.AUDIO_COMMUNICATION_CHANGED);
                            intent.putExtra("result","connectSuccess");
                            radioCommunicationService.sendBroadcast(intent);
                            Log.d(TAG,"connectSuccess");
                            //마이크 시작
                            startMic();
                        //마이크 동작 불가처리
                        }else if("connectFail".equals(result)){
                            //무전기 통신 버튼 UI변경을 위해서 브로드캐스트 송신
                            Intent intent = new Intent(AdminApplication.AUDIO_COMMUNICATION_CHANGED);
                            intent.putExtra("result","connectFail");
                            Log.d(TAG,"connectFail");
                            radioCommunicationService.sendBroadcast(intent);
                        //해제완료(마이크 동작 가능)
                        } else if("endSuccess".equals(result)){
                            //무전기 통신 버튼 UI변경을 위해서 브로드캐스트 송신
                            Intent intent = new Intent(AdminApplication.AUDIO_COMMUNICATION_CHANGED);
                            intent.putExtra("result","endSuccess");
                            Log.d(TAG,"endSuccess");
                            radioCommunicationService.sendBroadcast(intent);
                        //해제실패(마이크 동작 불가처리)
                        } else if("endFail".equals(result)){
                            //무전기 통신 버튼 UI변경을 위해서 브로드캐스트 송신
                            Intent intent = new Intent(AdminApplication.AUDIO_COMMUNICATION_CHANGED);
                            intent.putExtra("result","endFail");
                            Log.d(TAG,"endFail");
                            radioCommunicationService.sendBroadcast(intent);
                        }

                    }

                }catch(UnknownHostException e) {
                    Log.e(TAG, "Failure. UnknownHostException in sendMessage: " + serverIP);
                }catch(SocketException e) {
                    Log.e(TAG, "Failure. SocketException in sendMessage: " + e);
                }catch(IOException e) {
                    Log.e(TAG, "Failure. IOException in sendMessage: " + e);
                }
            }
        });
        micManageAcceptThread.start();
    }


    //마이크 이용가능한지 체크(이용가능하면 마이크 실행)
    public void micCheckAndGo(){
        Thread micCheckAndGoThread = new Thread(new Runnable() {
            @Override
            public void run() {
                DatagramSocket audioManageSocket = null;
                try {
                    //마이크 동작해도 될지 서버에 요청후 응답받기
                    String message = "ADD:"+AdminApplication.userMap.get("user_id")+";"+roomNo;
                    byte[] data = message.getBytes();
                    InetAddress address = InetAddress.getByName(serverIP);
                    audioManageSocket = new DatagramSocket();
                    DatagramPacket packet = new DatagramPacket(data, data.length, address, AUDIO_MANAGE_PORT);
                    //이용 가능여부 송출
                    audioManageSocket.send(packet);

                    //소켓 끊기
                    audioManageSocket.disconnect();
                    audioManageSocket.close();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    //소켓 끊기
                    if(audioManageSocket != null){
                        audioManageSocket.disconnect();
                        audioManageSocket.close();
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                    //소켓 끊기
                    if(audioManageSocket != null){
                        audioManageSocket.disconnect();
                        audioManageSocket.close();
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    //소켓 끊기
                    if(audioManageSocket != null){
                        audioManageSocket.disconnect();
                        audioManageSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    //소켓 끊기
                    if(audioManageSocket != null){
                        audioManageSocket.disconnect();
                        audioManageSocket.close();
                    }
                }
            }
        });
        micCheckAndGoThread.start();
    }

    //마이크사용중인 방 해제&체크
    public void micEndCheck(){
        Thread micEndCheckThread = new Thread(new Runnable() {
            @Override
            public void run() {
                DatagramSocket audioManageSocket = null;
                try {
                    //마이크 동작해도 될지 서버에 요청후 응답받기
                    String message = "END:"+AdminApplication.userMap.get("user_id")+";"+roomNo;
                    byte[] data = message.getBytes();
                    InetAddress address = InetAddress.getByName(serverIP);
                    audioManageSocket = new DatagramSocket();
                    DatagramPacket packet = new DatagramPacket(data, data.length, address, AUDIO_MANAGE_PORT);
                    audioManageSocket.send(packet);

                    //소켓 끊기
                    audioManageSocket.disconnect();
                    audioManageSocket.close();

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    //소켓 끊기
                    if(audioManageSocket != null){
                        audioManageSocket.disconnect();
                        audioManageSocket.close();
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                    //소켓 끊기
                    if(audioManageSocket != null){
                        audioManageSocket.disconnect();
                        audioManageSocket.close();
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    //소켓 끊기
                    if(audioManageSocket != null){
                        audioManageSocket.disconnect();
                        audioManageSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    //소켓 끊기
                    if(audioManageSocket != null){
                        audioManageSocket.disconnect();
                        audioManageSocket.close();
                    }
                }
            }
        });
        micEndCheckThread.start();
    }


}
