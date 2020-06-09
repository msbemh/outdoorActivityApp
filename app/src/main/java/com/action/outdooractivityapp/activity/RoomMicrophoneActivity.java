package com.action.outdooractivityapp.activity;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.rtp.AudioCodec;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.net.rtp.RtpStream;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import com.action.outdooractivityapp.R;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class RoomMicrophoneActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "RoomMicrophoneActivity";
    private Intent intent;
    private AudioStream audioStream;
    private AudioGroup audioGroup;

    private Button button_push_to_talk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_microphone);

        initializeView();

        registerListener();



//        try {
//            audioGroup = new AudioGroup();
//            audioGroup.setMode(AudioGroup.MODE_NORMAL);
//
//            audioStream = new AudioStream(InetAddress.getByAddress(new byte[]{(byte) 192, (byte) 168, (byte) 1, (byte) 4}));
//            audioStream.setCodec(AudioCodec.PCMU);
//            audioStream.setMode(RtpStream.MODE_NORMAL);
//            audioStream.associate(InetAddress.getByAddress(new byte[]{(byte) 192, (byte) 168, (byte) 1, (byte) 2}), 5004);
//            audioStream.join(audioGroup);
//
//            AudioManager Audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//            Audio.setMode(AudioManager.MODE_IN_COMMUNICATION);
//        }catch (SocketException e){
//            e.printStackTrace();
//        }catch (UnknownHostException e){
//            e.printStackTrace();
//        }catch (Exception e){
//            e.printStackTrace();
//        }

    }

    void initializeView(){
        button_push_to_talk = findViewById(R.id.button_push_to_talk);
    }

    void registerListener(){
        button_push_to_talk.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        //통신 클릭
        if(v.getId() == R.id.button_push_to_talk) {
            Log.d(TAG, "통신 클릭");

            String url = "http://........"; // your URL here
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mediaPlayer.setDataSource(url);
                mediaPlayer.prepare(); // might take long! (for buffering, etc)
                mediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
