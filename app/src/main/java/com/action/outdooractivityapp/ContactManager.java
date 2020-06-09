package com.action.outdooractivityapp;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;

public class ContactManager {

	private static final String LOG_TAG = "ContactManager";
	public static final int BROADCAST_PORT = 50001; // Socket on which packets are sent/received
	private static final int BROADCAST_INTERVAL = 10000; // Milliseconds
	private static final int BROADCAST_BUF_SIZE = 1024;
	private boolean BROADCAST = true;
	private boolean LISTEN = true;
	private HashMap<String, InetAddress> contacts;
	private InetAddress broadcastIP;
	
	public ContactManager(String name, InetAddress broadcastIP) {
		//등록된 InetAddress리스트
		contacts = new HashMap<String, InetAddress>();
		//InetAddress설정
		this.broadcastIP = broadcastIP;

		listen();

		broadcastName(name, broadcastIP);
	}

	//접속한 이용자 목록 get하기
	public HashMap<String, InetAddress> getContacts() {
		return contacts;
	}

	//접속한 이용자 추가하기
	public void addContact(String name, InetAddress address) {
		// If the contact is not already known to us, add it
		//이미 접속한 이용자가 아니라면
		if(!contacts.containsKey(name)) {
			Log.i(LOG_TAG, "Adding contact: " + name);
			//추가
			contacts.put(name, address);
			Log.i(LOG_TAG, "#Contacts: " + contacts.size());
			return;
		}
		Log.i(LOG_TAG, "Contact already exists: " + name);
		return;
	}

	//접속한 이용자 삭제하기
	public void removeContact(String name) {
		// If the contact is known to us, remove it
		//이미 접속한 이용자 라면
		if(contacts.containsKey(name)) {
			Log.i(LOG_TAG, "Removing contact: " + name);
			//삭제
			contacts.remove(name);
			Log.i(LOG_TAG, "#Contacts: " + contacts.size());
			return;
		}
		Log.i(LOG_TAG, "Cannot remove contact. " + name + " does not exist.");
		return;
	}

	//bye라고 보내주기
	public void bye(final String name) {
		// Sends a Bye notification to other devices
		Thread byeThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				try {
					Log.i(LOG_TAG, "Attempting to broadcast BYE notification!");
					//byt메시지 생성
					String notification = "BYE:"+name;
					byte[] message = notification.getBytes();
					DatagramSocket socket = new DatagramSocket();

					//브로드캐스트 설정
					socket.setBroadcast(true);

					//패킷 생성
					DatagramPacket packet = new DatagramPacket(message, message.length, broadcastIP, BROADCAST_PORT);
					//보내기
					socket.send(packet);
					Log.i(LOG_TAG, "Broadcast BYE notification!");
					//소켓연결 끊기
					socket.disconnect();
					socket.close();
					return;
				}catch(SocketException e) {
					
					Log.e(LOG_TAG, "SocketException during BYE notification: " + e);
				}catch(IOException e) {
					
					Log.e(LOG_TAG, "IOException during BYE notification: " + e);
				}
			}
		});
		byeThread.start();
	}

	//브로드캐스트 메시지 송신
	public void broadcastName(final String name, final InetAddress broadcastIP) {
		// Broadcasts the name of the device at a regular interval
		Log.i(LOG_TAG, "Broadcasting started!");
		Thread broadcastThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					//메시지 생성
					String request = "ADD:"+name;
					byte[] message = request.getBytes();

					//소켓 생성
					DatagramSocket socket = new DatagramSocket();

					//브로드캐스트 설정
					socket.setBroadcast(true);

					//패킷 생성
					DatagramPacket packet = new DatagramPacket(message, message.length, broadcastIP, BROADCAST_PORT);

					//전파하는 동안 패킷정보 보내기
					while(BROADCAST) {
						socket.send(packet);
						Log.i(LOG_TAG, "Broadcast packet sent: " + packet.getAddress().toString());
						Thread.sleep(BROADCAST_INTERVAL);
					}
					Log.i(LOG_TAG, "Broadcaster ending!");

					//소켓 연결 끊기
					socket.disconnect();
					socket.close();

					return;
				}catch(SocketException e) {
					Log.e(LOG_TAG, "SocketExceltion in broadcast: " + e);
					Log.i(LOG_TAG, "Broadcaster ending!");
					return;
				}catch(IOException e) {
					Log.e(LOG_TAG, "IOException in broadcast: " + e);
					Log.i(LOG_TAG, "Broadcaster ending!");
					return;
				}catch(InterruptedException e) {
					Log.e(LOG_TAG, "InterruptedException in broadcast: " + e);
					Log.i(LOG_TAG, "Broadcaster ending!");
					return;
				}
			}
		});
		//시작
		broadcastThread.start();
	}
	
	public void stopBroadcasting() {
		// Ends the broadcasting thread
		BROADCAST = false;
	}

	//접속 받기
	public void listen() {
		// Create the listener thread
		Log.i(LOG_TAG, "Listening started!");
		Thread listenThread = new Thread(new Runnable() {
			@Override
			public void run() {
				DatagramSocket socket;
				try {
					socket = new DatagramSocket(BROADCAST_PORT);
				}catch (SocketException e) {
					Log.e(LOG_TAG, "SocketExcepion in listener: " + e);
					return;
				}

				byte[] buffer = new byte[BROADCAST_BUF_SIZE];

				//계속 contact 접속 받기
				while(LISTEN) {
					listen(socket, buffer);
				}
				Log.i(LOG_TAG, "Listener ending!");

				//소켓 연결 끊기
				socket.disconnect();
				socket.close();
				return;
			}

			//contact 접속 받기
			public void listen(DatagramSocket socket, byte[] buffer) {
				try {
					//Listen in for new notifications
					Log.i(LOG_TAG, "Listening for a packet!");
					//패킷 받기
					DatagramPacket packet = new DatagramPacket(buffer, BROADCAST_BUF_SIZE);
					socket.setSoTimeout(15000);
					socket.receive(packet);

					//패킷 data 추출
					String data = new String(buffer, 0, packet.getLength());
					Log.i(LOG_TAG, "Packet received: " + data);
					String action = data.substring(0, 4);

					//접속 추가
					if(action.equals("ADD:")) {
						// Add notification received. Attempt to add contact
						Log.i(LOG_TAG, "Listener received ADD request");
						addContact(data.substring(4, data.length()), packet.getAddress());
					//접속 해제
					}else if(action.equals("BYE:")) {
						// Bye notification received. Attempt to remove contact
						Log.i(LOG_TAG, "Listener received BYE request");
						removeContact(data.substring(4, data.length()));
					//예외처리
					}else {
						// Invalid notification received
						Log.w(LOG_TAG, "Listener received invalid request: " + action);
					}
				}catch(SocketTimeoutException e) {
					Log.i(LOG_TAG, "No packet received!");
					if(LISTEN) {
						listen(socket, buffer);
					}
					return;
				}catch(SocketException e) {
					Log.e(LOG_TAG, "SocketException in listen: " + e);
					Log.i(LOG_TAG, "Listener ending!");
					return;
				}catch(IOException e) {
					Log.e(LOG_TAG, "IOException in listen: " + e);
					Log.i(LOG_TAG, "Listener ending!");
					return;
				}
			}
		});
		listenThread.start();
	}
	
	public void stopListening() {
		// Stops the listener thread
		LISTEN = false;
	}
}
