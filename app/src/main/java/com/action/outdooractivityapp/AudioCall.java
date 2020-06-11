package com.action.outdooractivityapp;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class AudioCall {

    private static final String LOG_TAG = "AudioCall";
    private static final int SAMPLE_RATE = 8000; // Hertz
    private static final int SAMPLE_INTERVAL = 20; // Milliseconds
    private static final int SAMPLE_SIZE = 2; // Bytes
    private static final int BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2; //Bytes

    private InetAddress address; // Address to call
    private int port = 50000; // Port the packets are addressed to

    private boolean mic = false; // Enable mic?
    private boolean speakers = false; // Enable speakers?

    //생성자 address주소 가져옴
    public AudioCall(InetAddress address) {
        this.address = address;
    }

    //전화 시작하기
    public void startCall() {
        //마이크 시작
        startMic();
        //스피커 시작
        startSpeakers();
    }

    //전화 끝내기
    public void endCall() {
        Log.i(LOG_TAG, "Ending call!");
        muteMic();
        muteSpeakers();
    }

    //마이크 끝내기
    public void muteMic() {
        mic = false;
    }

    //스피커 끝내기
    public void muteSpeakers() {
        speakers = false;
    }

    //마이크 시작
    public void startMic() {
        // Creates the thread for capturing and transmitting audio
        mic = true;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Create an instance of the AudioRecord class
                Log.i(LOG_TAG, "Send thread started. Thread id: " + Thread.currentThread().getId());

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
                    Log.i(LOG_TAG, "Packet destination: " + address.toString());

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
                        DatagramPacket packet = new DatagramPacket(buf, bytes_read, address, port);
                        //보내기
                        socket.send(packet);
                        bytes_sent += bytes_read;
                        Log.i(LOG_TAG, "Total bytes sent: " + bytes_sent);
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
                    Log.e(LOG_TAG, "InterruptedException: " + e.toString());
                    mic = false;
                }catch(SocketException e) {
                    Log.e(LOG_TAG, "SocketException: " + e.toString());
                    mic = false;
                }catch(UnknownHostException e) {
                    Log.e(LOG_TAG, "UnknownHostException: " + e.toString());
                    mic = false;
                } catch(IOException e) {
                    Log.e(LOG_TAG, "IOException: " + e.toString());
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
                    Log.i(LOG_TAG, "Receive thread started. Thread id: " + Thread.currentThread().getId());

                    //오디오 트랙 생성
                    AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, BUF_SIZE, AudioTrack.MODE_STREAM);
                    //트랙 실행
                    track.play();
                    try {
                        // Define a socket to receive the audio
                        //소켓생성
                        DatagramSocket socket = new DatagramSocket(port);
                        //버퍼 생성
                        byte[] buf = new byte[BUF_SIZE];
                        //스피커 반복
                        while(speakers) {
                            // Play back the audio received from packets
                            //패킷 생성
                            DatagramPacket packet = new DatagramPacket(buf, BUF_SIZE);
                            //받기
                            socket.receive(packet);
                            Log.i(LOG_TAG, "Packet received: " + packet.getLength());
                            //트랙에 데이터 쓰기
                            track.write(packet.getData(), 0, BUF_SIZE);
                        }
                        // Stop playing back and release resources
                        //소켓 끊기
                        socket.disconnect();
                        socket.close();

                        track.stop();
                        track.flush();
                        track.release();
                        speakers = false;
                        return;
                    }
                    catch(SocketException e) {

                        Log.e(LOG_TAG, "SocketException: " + e.toString());
                        speakers = false;
                    }
                    catch(IOException e) {

                        Log.e(LOG_TAG, "IOException: " + e.toString());
                        speakers = false;
                    }
                }
            });
            receiveThread.start();
        }
    }
}