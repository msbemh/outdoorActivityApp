package com.action.outdooractivityapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;

public class MainActivity2 extends Activity {

	static final String TAG = "MainActivity2";
	private static final int LISTENER_PORT = 50003;
	private static final int BUF_SIZE = 1024;
	private ContactManager contactManager;
	private String displayName;
	private boolean STARTED = false;
	private boolean IN_CALL = false;
	private boolean LISTEN = false;

	private final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1234;
	
	public final static String EXTRA_CONTACT = "hw.dt83.udpchat.CONTACT";
	public final static String EXTRA_IP = "hw.dt83.udpchat.IP";
	public final static String EXTRA_DISPLAYNAME = "hw.dt83.udpchat.DISPLAYNAME";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main2);
		
		Log.i(TAG, "UDPChat started");
		
		//시작 버튼
		final Button btnStart = (Button) findViewById(R.id.buttonStart);
		btnStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				Log.i(TAG, "Start button pressed");
				STARTED = true;

				//이름 Edit
				EditText displayNameText = (EditText) findViewById(R.id.editTextDisplayName);
				displayName = displayNameText.getText().toString();

				//시작버튼, 이름 Edit 비활성화
				displayNameText.setEnabled(false);
				btnStart.setEnabled(false);

				//설명구 띄워주기
				TextView text = (TextView) findViewById(R.id.textViewSelectContact);
				text.setVisibility(View.VISIBLE);

				//사람목록 띄워주기
				Button updateButton = (Button) findViewById(R.id.buttonUpdate);
				updateButton.setVisibility(View.VISIBLE);

				//전화걸기
				Button callButton = (Button) findViewById(R.id.buttonCall);
				callButton.setVisibility(View.VISIBLE);

				//사람목록 scroll띄워주기
				ScrollView scrollView = (ScrollView) findViewById(R.id.scrollView);
				scrollView.setVisibility(View.VISIBLE);

				//컨텍트 메니저 생성
				contactManager = new ContactManager(displayName, getBroadcastIp());
				//리스너 시작
				startCallListener();
			}
		});

		//오디오 권한 요구하기
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},MY_PERMISSIONS_REQUEST_READ_CONTACTS);
		}

		// UPDATE BUTTON
		// Updates the list of reachable devices
		// 사용자 목록 띄워주기 버튼
		final Button btnUpdate = (Button) findViewById(R.id.buttonUpdate);
		btnUpdate.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//사용자 목록 업데이트
				updateContactList();
			}
		});
		
		// CALL BUTTON
		// Attempts to initiate an audio chat session with the selected device
		//전화걸기 버튼
		final Button btnCall = (Button) findViewById(R.id.buttonCall);
		btnCall.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {

				//라디오그룹의 선택된 pos 가져오기
				RadioGroup radioGroup = (RadioGroup) findViewById(R.id.contactList);
				int selectedButton = radioGroup.getCheckedRadioButtonId();

				//선택된 라디오 버튼이 없다면
				if(selectedButton == -1) {
					// If no device was selected, present an error message to the user
					Log.w(TAG, "Warning: no contact selected");
					//대화상자 생성
					final AlertDialog alert = new AlertDialog.Builder(MainActivity2.this).create();
					//대화상자 설정
					alert.setTitle("Oops");
					alert.setMessage("You must select a contact first");
					alert.setButton(-1, "OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					       alert.dismiss();
						}
					});
					//대화상자 Show
					alert.show();
					return;
				}

				// Collect details about the selected contact
				//선택된 라디오 버튼 가져오기
				RadioButton radioButton = (RadioButton) findViewById(selectedButton);
				//선택된 라디오 버튼 Text 가져오기
				String contact = radioButton.getText().toString();
				//Text에 대한 ip 가져오기
				InetAddress ip = contactManager.getContacts().get(contact);
				IN_CALL = true;
				
				// Send this information to the MakeCallActivity and start that activity
				//intent 설정
				Intent intent = new Intent(MainActivity2.this, MakeCallActivity.class);
				//contact 사용자 이름 정보 intent에 추가
				intent.putExtra(EXTRA_CONTACT, contact);
				//ip 정보 intent에 추가
				String address = ip.toString();
				address = address.substring(1, address.length());
				intent.putExtra(EXTRA_IP, address);
				//사용자 이름 정보 intent에 추가
				intent.putExtra(EXTRA_DISPLAYNAME, displayName);
				startActivity(intent);
			}
		});
	}

	//사용자 목록 업데이트
	private void updateContactList() {
		// Create a copy of the HashMap used by the ContactManager
		// contactManager에서 연결된 사람목록 가져오기
		HashMap<String, InetAddress> contacts = contactManager.getContacts();

		// Create a radio button for each contact in the HashMap
		// 라디오 그룹
		RadioGroup radioGroup = (RadioGroup) findViewById(R.id.contactList);
		// 라디오 요소 없애기
		radioGroup.removeAllViews();

		//연결된 사람 목록 보여주기
		for(String name : contacts.keySet()) {
			//라디오 버튼생성
			RadioButton radioButton = new RadioButton(getBaseContext());
			radioButton.setText(name);
			radioButton.setTextColor(Color.BLACK);
			//라디오 그룹에 추가
			radioGroup.addView(radioButton);
		}
		//라디오 그룹 체크 초기화
		radioGroup.clearCheck();
	}

	//IP얻기
	private InetAddress getBroadcastIp() {
		try {
			//와이파이의 IP얻기
			WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			int ipAddress = wifiInfo.getIpAddress();
			String addressString = toBroadcastIp(ipAddress);
			Log.d(TAG,"addressString:"+addressString);

			InetAddress broadcastAddress = InetAddress.getByName(addressString);
			return broadcastAddress;
		}catch(UnknownHostException e) {
			Log.e(TAG, "UnknownHostException in getBroadcastIP: " + e);
			return null;
		}
		
	}

	//int형 ip를 String으로 변환
	private String toBroadcastIp(int ip) {
		// Returns converts an IP address in int format to a formatted string
		return (ip & 0xFF) + "." +
				((ip >> 8) & 0xFF) + "." +
				((ip >> 16) & 0xFF) + "." +
				"255";
	}

	//전화요청 받는 리스너
	private void startCallListener() {
		// Creates the listener thread
		LISTEN = true;
		Thread listener = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				try {
					// Set up the socket and packet to receive
					Log.i(TAG, "Incoming call listener started");
					//소켓 생성
					DatagramSocket socket = new DatagramSocket(LISTENER_PORT);
					socket.setSoTimeout(1000);

					//패킷 생성
					byte[] buffer = new byte[BUF_SIZE];
					DatagramPacket packet = new DatagramPacket(buffer, BUF_SIZE);

					while(LISTEN) {
						// Listen for incoming call requests
						try {
							Log.i(TAG, "Listening for incoming calls");
							socket.receive(packet);

							//패킷으로부터 데이터 추출
							String data = new String(buffer, 0, packet.getLength());
							Log.i(TAG, "Packet received from "+ packet.getAddress() +" with contents: " + data);
							String action = data.substring(0, 4);

							//전화요청 받으면
							if(action.equals("CAL:")) {
								// Received a call request. Start the ReceiveCallActivity
								String address = packet.getAddress().toString();
								String name = data.substring(4, packet.getLength());
								
								Intent intent = new Intent(MainActivity2.this, ReceiveCallActivity.class);
								intent.putExtra(EXTRA_CONTACT, name);
								intent.putExtra(EXTRA_IP, address.substring(1, address.length()));
								IN_CALL = true;
								//LISTEN = false;
								//stopCallListener();
								startActivity(intent);
							//예외 처리
							}else {
								// Received an invalid request
								Log.w(TAG, packet.getAddress() + " sent invalid message: " + data);
							}
						}catch(Exception e) {
							//e.printStackTrace();
						}
					}
					Log.i(TAG, "Call Listener ending");

					//소켓 끊기
					socket.disconnect();
					socket.close();
				}catch(SocketException e) {
					e.printStackTrace();
					Log.e(TAG, "SocketException in listener " + e);
				}
			}
		});
		listener.start();
	}
	
	private void stopCallListener() {
		// Ends the listener thread
		LISTEN = false;
	}
	
	@Override
	public void onPause() {
		
		super.onPause();
		//시작버튼 눌렀었다면
		if(STARTED) {
			//접속자 리스트에서 삭제
			contactManager.bye(displayName);
			//접속메니저 끄기
			contactManager.stopBroadcasting();
			contactManager.stopListening();
			//STARTED = false;
		}
		//call 리스너 끄기
		stopCallListener();
		Log.i(TAG, "App paused!");
	}
	
	@Override
	public void onStop() {
		super.onStop();
		Log.i(TAG, "App stopped!");
		//call 리스너 끄기
		stopCallListener();

		//전화중이 아니라면
		if(!IN_CALL) {
			//액티비티 끝
			finish();
		}
	}
	
	@Override
	public void onRestart() {
		super.onRestart();
		Log.i(TAG, "App restarted!");
		IN_CALL = false;
		STARTED = true;
		//연결 매니저 다시 시작
		contactManager = new ContactManager(displayName, getBroadcastIp());
		//콜 리스너 시작
		startCallListener();
	}

	//권한 설정 결과
	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				} else {
					Log.d("TAG", "permission denied by user");
				}
				return;
			}
		}
	}
}
