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
import java.net.UnknownHostException;

public class ReceiveCallActivity extends Activity {

	private static final String LOG_TAG = "ReceiveCall";
	private static final int BROADCAST_PORT = 50002;
	private static final int BUF_SIZE = 1024;
	private String contactIp;
	private String contactName;
	private boolean LISTEN = true;
	private boolean IN_CALL = false;
	private AudioCall call;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_receive_call);

		//intent 정보 받기
		Intent intent = getIntent();
		contactName = intent.getStringExtra(MainActivity2.EXTRA_CONTACT);
		contactIp = intent.getStringExtra(MainActivity2.EXTRA_IP);

		//전화요청 Text 넣기
		TextView textView = (TextView) findViewById(R.id.textViewIncomingCall);
		textView.setText("Incoming call: " + contactName);

		//전화 종료버튼
		final Button endButton = (Button) findViewById(R.id.buttonEndCall1);
		endButton.setVisibility(View.INVISIBLE);

		//리스너 시작
		startListener();
		
		// ACCEPT BUTTON
		// 수락 버튼
		Button acceptButton = (Button) findViewById(R.id.buttonAccept);
		acceptButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					// Accepting call. Send a notification and start the call
					//수락하기
					sendMessage("ACC:");

					//IP정보 가져오기
					InetAddress address = InetAddress.getByName(contactIp);
					Log.i(LOG_TAG, "Calling " + address.toString());

					IN_CALL = true;

					//통화 시작
					call = new AudioCall(address);
					call.startCall();

					// Hide the buttons as they're not longer required
					//수락버튼 비활성화
					Button accept = (Button) findViewById(R.id.buttonAccept);
					accept.setEnabled(false);

					//거절버튼 비활성화
					Button reject = (Button) findViewById(R.id.buttonReject);
					reject.setEnabled(false);

					//종료버튼 활성화
					endButton.setVisibility(View.VISIBLE);
				}catch(UnknownHostException e) {
					Log.e(LOG_TAG, "UnknownHostException in acceptButton: " + e);
				}catch(Exception e) {
					Log.e(LOG_TAG, "Exception in acceptButton: " + e);
				}
			}
		});
		
		// REJECT BUTTON
		// 거절버튼
		Button rejectButton = (Button) findViewById(R.id.buttonReject);
		rejectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Send a reject notification and end the call
				sendMessage("REJ:");
				endCall();
			}
		});
		
		// END BUTTON
		endButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				endCall();
			}
		});
	}
	
	private void endCall() {
		// End the call and send a notification
		stopListener();
		if(IN_CALL) {
			call.endCall();
		}
		sendMessage("END:");
		finish();
	}

	//전화요청 받는 리스너 시작
	private void startListener() {
		// Creates the listener thread
		LISTEN = true;
		Thread listenThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Log.i(LOG_TAG, "Listener started!");
					//소켓 생성
					DatagramSocket socket = new DatagramSocket(BROADCAST_PORT);
					socket.setSoTimeout(1500);

					//버퍼생성
					byte[] buffer = new byte[BUF_SIZE];
					//패킷 생성
					DatagramPacket packet = new DatagramPacket(buffer, BUF_SIZE);
					while(LISTEN) {
						try {
							Log.i(LOG_TAG, "Listening for packets");

							//받기
							socket.receive(packet);

							//데이터 추출
							String data = new String(buffer, 0, packet.getLength());
							Log.i(LOG_TAG, "Packet received from "+ packet.getAddress() +" with contents: " + data);
							String action = data.substring(0, 4);

							//종료
							if(action.equals("END:")) {
								// End call notification received. End call
								endCall();
							//예외처리
							}else {
								// Invalid notification received.
								Log.w(LOG_TAG, packet.getAddress() + " sent invalid message: " + data);
							}
						}
						catch(IOException e) {
							
							Log.e(LOG_TAG, "IOException in Listener " + e);
						}
					}
					Log.i(LOG_TAG, "Listener ending");

					//소켓 끊기
					socket.disconnect();
					socket.close();
					return;
				}catch(SocketException e) {
					
					Log.e(LOG_TAG, "SocketException in Listener " + e);
					endCall();
				}
			}
		});
		listenThread.start();
	}

	//요청받기 리스너 끊기
	private void stopListener() {
		// Ends the listener thread
		LISTEN = false;
	}

	//전화요청에 대한 대답
	private void sendMessage(final String message) {
		// Creates a thread for sending notifications
		Thread replyThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					//IP설정
					InetAddress address = InetAddress.getByName(contactIp);
					//message설정
					byte[] data = message.getBytes();
					//소켓 생성
					DatagramSocket socket = new DatagramSocket();
					//패킷 생성
					DatagramPacket packet = new DatagramPacket(data, data.length, address, BROADCAST_PORT);
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
		getMenuInflater().inflate(R.menu.receive_call, menu);
		return true;
	}

}
