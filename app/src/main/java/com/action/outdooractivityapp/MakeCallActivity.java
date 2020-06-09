package com.action.outdooractivityapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class MakeCallActivity extends Activity {

	private static final String LOG_TAG = "MakeCall";
	private static final int BROADCAST_PORT = 50002;
	private static final int BUF_SIZE = 1024;
	private String displayName;
	private String contactName;
	private String contactIp;
	private boolean LISTEN = true;
	private boolean IN_CALL = false;
	private AudioCall call;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_make_call);
		
		Log.i(LOG_TAG, "MakeCallActivity started!");

		//intent로 넘어온 값
		Intent intent = getIntent();
		displayName = intent.getStringExtra(MainActivity2.EXTRA_DISPLAYNAME);
		contactName = intent.getStringExtra(MainActivity2.EXTRA_CONTACT);
		contactIp = intent.getStringExtra(MainActivity2.EXTRA_IP);

		//전화 연결중 Text표시
		TextView textView = (TextView) findViewById(R.id.textViewCalling);
		textView.setText("Calling: " + contactName);

		//전화요청결과 리스너
		startListener();
		//전화요청
		makeCall();

		//통화 종료
		Button endButton = (Button) findViewById(R.id.buttonEndCall);
		endButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Button to end the call has been pressed
				endCall();
			}
		});
	}

	//전화요청
	private void makeCall() {
		// Send a request to start a call
		//전화요청
		sendMessage("CAL:"+displayName, 50003);
	}

	//통화 끝
	private void endCall() {
		// Ends the chat sessions
		//전화요청 결과 리스너 끝내기
		stopListener();
		if(IN_CALL) {
			//통화 끝
			call.endCall();
		}
		//전화요청 결과 리스너 끝내기
		sendMessage("END:", BROADCAST_PORT);
		finish();
	}

	//전화요청 결과 리스너
	private void startListener() {
		// Create listener thread
		LISTEN = true;
		Thread listenThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Log.i(LOG_TAG, "Listener started!");
					//소켓 생성
					DatagramSocket socket = new DatagramSocket(BROADCAST_PORT);
					socket.setSoTimeout(15000);

					//패킷 생성
					byte[] buffer = new byte[BUF_SIZE];
					DatagramPacket packet = new DatagramPacket(buffer, BUF_SIZE);
					while(LISTEN) {
						try {
							//소켓으로 패킷받기
							Log.i(LOG_TAG, "Listening for packets");
							socket.receive(packet);

							//패킷 데이터 추출
							String data = new String(buffer, 0, packet.getLength());
							Log.i(LOG_TAG, "Packet received from "+ packet.getAddress() +" with contents: " + data);
							String action = data.substring(0, 4);
							//받아들임
							if(action.equals("ACC:")) {
								// Accept notification received. Start call
								//오디오 통화 시작
								call = new AudioCall(packet.getAddress());
								call.startCall();
								IN_CALL = true;
							//거절
							}else if(action.equals("REJ:")) {
								// Reject notification received. End call
								//오디오 통화 끄기
								endCall();
							//끝
							}else if(action.equals("END:")) {
								// End call notification received. End call
								//오디오 통화 끄기
								endCall();
							//예외처리
							}else {
								// Invalid notification received
								Log.w(LOG_TAG, packet.getAddress() + " sent invalid message: " + data);
							}
						}catch(SocketTimeoutException e) {
							if(!IN_CALL) {
								Log.i(LOG_TAG, "No reply from contact. Ending call");
								//오디오 통화 끄기
								endCall();
								return;
							}
						}catch(IOException e) {

						}
					}
					Log.i(LOG_TAG, "Listener ending");

					//소켓 연결 끊기
					socket.disconnect();
					socket.close();
					return;
				}catch(SocketException e) {
					Log.e(LOG_TAG, "SocketException in Listener");
					//오디오 통화 끄기
					endCall();
				}
			}
		});
		listenThread.start();
	}

	//전화요청 결과 리스너 끝내기
	private void stopListener() {
		// Ends the listener thread
		LISTEN = false;
	}

	//전화요청
	private void sendMessage(final String message, final int port) {
		// Creates a thread used for sending notifications
		Thread replyThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					//전화요청 상대 IP
					InetAddress address = InetAddress.getByName(contactIp);
					//message 구성
					byte[] data = message.getBytes();
					//소켓 생성
					DatagramSocket socket = new DatagramSocket();
					//패킷 생성
					DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
					//보내기
					socket.send(packet);

					Log.i(LOG_TAG, "Sent message( " + message + " ) to " + contactIp);
					//소켓 끊기
					socket.disconnect();
					socket.close();
				}catch(UnknownHostException e) {
					Log.e(LOG_TAG, "Failure. UnknownHostException in sendMessage: " + contactIp);
				}catch(SocketException e) {
					Log.e(LOG_TAG, "Failure. SocketException in sendMessage: " + e);
				}catch(IOException e) {
					Log.e(LOG_TAG, "Failure. IOException in sendMessage: " + e);
				}
			}
		});
		replyThread.start();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.make_call, menu);
		return true;
	}

}
