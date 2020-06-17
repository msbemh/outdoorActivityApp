package com.action.outdooractivityapp.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.action.outdooractivityapp.AdminApplication;
import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.service.LocationSharingService;
import com.action.outdooractivityapp.util.Util;

import net.daum.mf.map.api.CameraUpdateFactory;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LocationSharingMap extends AppCompatActivity implements View.OnClickListener, MapView.CurrentLocationEventListener {

    private static final String TAG = "LocationSharingMap";
    private final int MY_PERMISSIONS_REQUEST_LOCATION = 1;
    private Intent intent;
    private Bundle extras;
    private int roomNo = -1;

    private ImageView image_back;
    private ImageView image_chat;
    private ImageView image_map;
    private ImageView image_microphone;

    public MapView mapView;
    public MapPoint currentMapPoint;
    private boolean isCameraMove = true;

    private LocationSharingService locationSharingService;

    private List<MapPOIItem> locationList = new ArrayList<MapPOIItem>();

    //위치공유 서비스와 연결되는 부분
    ServiceConnection locationSharingServiceConnection = new ServiceConnection() {
        // 서비스와 연결되었을 때 호출되는 메서드
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            locationSharingService = ((LocationSharingService.MyBinder) service).getService();
        }

        // 서비스와 연결이 끊겼을 때 호출되는 메서드
        @Override
        public void onServiceDisconnected(ComponentName name) {
            locationSharingServiceConnection = null;
            locationSharingService = null;
        }
    };

    //위치공유 서비스의 브로드캐스트 수신받는곳
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"위치공유 브로드캐스트 리시버 동작");
            List<Map> resultList = (List<Map>)intent.getSerializableExtra("result");
            Log.d(TAG,"resultList:"+resultList);

            //지도에 마커 삭제
            for(MapPOIItem mapPOIItem : locationList){
                mapView.removePOIItem(mapPOIItem);
            }
            locationList.clear();

            //지도에 표시할 마커 생성하기
            int i =0;
            for(Map itemMap : resultList){
                //데이터 추출
                String nickName = itemMap.get("nickName").toString();
                double longitude = Double.parseDouble(itemMap.get("longitude").toString());
                double latitude = Double.parseDouble(itemMap.get("latitude").toString());

                //마커 세팅
                MapPOIItem marker = new MapPOIItem();
                marker.setItemName(nickName);
                marker.setTag(0);
                //위치정보로 MapPoint생성
                MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude);
                marker.setMapPoint(mapPoint);
                marker.setMarkerType(MapPOIItem.MarkerType.RedPin); // 기본으로 제공하는 BluePin 마커 모양.
                marker.setSelectedMarkerType(MapPOIItem.MarkerType.RedPin); // 마커를 클릭했을때, 기본으로 제공하는 RedPin 마커 모양.
                locationList.add(marker);
            }

            //지도에 마커 추가
            for(MapPOIItem mapPOIItem : locationList){
                mapView.addPOIItem(mapPOIItem);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_sharing_map);

        //방번호 받기
        /*data 받아오기*/
        extras = getIntent().getExtras();
        roomNo = extras.getInt("room_no",-1);
        Log.d(TAG, "roomNo:"+roomNo);

        initializeView();

        registerListener();

        registerBroadcast();

        //위치허용 권한 요구하기
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
        }else{
            //현재위치 설정
            Log.d(TAG, "내위치 설정");
            mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving);

            //위치공유 서비스 바인드시키기
            intent = new Intent(this, LocationSharingService.class);
            Log.d(TAG,"[위치공유 서비스 실행전]roomNo:"+roomNo);
            intent.putExtra("roomNo", roomNo);
            bindService(intent, locationSharingServiceConnection, Context.BIND_AUTO_CREATE);
        }

    }

    //권한 설정 결과
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                //위치 사용 허가했을 때
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //현재위치 설정
                    Log.d(TAG, "내위치 설정");
                    mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving);

                    //위치공유 서비스 바인드시키기
                    intent = new Intent(this, LocationSharingService.class);
                    Log.d(TAG,"[위치공유 서비스 실행전]roomNo:"+roomNo);
                    intent.putExtra("roomNo", roomNo);
                    bindService(intent, locationSharingServiceConnection, Context.BIND_AUTO_CREATE);
                //위치 사용 허가 안했을 때
                } else {
                    Log.d("TAG", "permission denied by user");
                    Util.toastText(this,"위치권한을 허용해야합니다.");
                    finish();
                }
                return;
            }
        }
    }

    void initializeView(){
        image_back = findViewById(R.id.image_back);
        image_chat = findViewById(R.id.image_chat);
        image_map = findViewById(R.id.image_map);
        image_microphone = findViewById(R.id.image_microphone);
        //카카오지도
        mapView = new MapView(this);
        ViewGroup mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);

    }

    void registerListener(){
        image_back.setOnClickListener(this);
        image_chat.setOnClickListener(this);
        image_map.setOnClickListener(this);
        image_microphone.setOnClickListener(this);
        //카카오지도
        mapView.setCurrentLocationEventListener(this);
    }

    public void registerBroadcast(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(AdminApplication.LOCATION_SHARE_BROAD_CAST);
        registerReceiver(broadcastReceiver, filter);
    }

    public void unregisterBroadcast(){
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onClick(View v) {
        //뒤로가기 클릭
        if(v.getId() == R.id.image_back){
            finish();
        //마이크 클릭
        }else if(v.getId() == R.id.image_microphone){
            intent = new Intent(this, RoomMicrophoneActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
            intent.putExtra("room_no", roomNo);
            startActivity(intent);
        //위치공유맵 클릭
        }else if(v.getId() == R.id.image_map){
            intent = new Intent(this, LocationSharingMap.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
            intent.putExtra("room_no", roomNo);
            startActivity(intent);
        //채팅방 클릭
        }else if(v.getId() == R.id.image_chat){
            intent = new Intent(this, RoomChatActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
            intent.putExtra("room_no", roomNo);
            startActivity(intent);
        }
    }



    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float v) {
        Log.d(TAG, "onCurrentLocationUpdate");
        Log.d(TAG, "현재위치:"+mapPoint.getMapPointGeoCoord());
        Log.d(TAG, "현재위치:"+mapPoint.getMapPointGeoCoord().latitude);
        Log.d(TAG, "현재위치:"+mapPoint.getMapPointGeoCoord().longitude);
        currentMapPoint = mapPoint;
        if(isCameraMove){
            //카메라이동
            mapView.moveCamera(CameraUpdateFactory.newMapPoint(currentMapPoint));
            isCameraMove = false;
        }

    }

    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {
        Log.d(TAG, "onCurrentLocationDeviceHeadingUpdate");
    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {
        Log.d(TAG, "onCurrentLocationUpdateFailed");
    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {
        Log.d(TAG, "onCurrentLocationUpdateCancelled");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //위치 공유 서비스 unbind시키기
        unbindService(locationSharingServiceConnection);
        //브로드캐스트 리시버 해제
        unregisterBroadcast();
    }
}
