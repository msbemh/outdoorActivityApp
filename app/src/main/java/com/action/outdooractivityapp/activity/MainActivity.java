package com.action.outdooractivityapp.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.action.outdooractivityapp.AdminApplication;
import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.service.ForcedTerminationService;
import com.action.outdooractivityapp.util.Util;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import net.daum.mf.map.api.CameraUpdateFactory;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPointBounds;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, MapView.CurrentLocationEventListener {

    private static final String TAG = "MainActivity";
    private Intent intent;
    private BottomNavigationView navView;
    private final int MY_PERMISSIONS_REQUEST_LOCATION = 1;
    public MapView mapView;
    private ImageView imageView_tracking_button;

    private ViewGroup mapViewContainer;

    private LocationManager locationManager;
    private boolean isFirst = true;
    private static boolean isMapReset = false;
    private String trackingStatus = "end";

    //
    private List<Map> trackingList = new ArrayList<Map>();
    private List<MapPolyline> polylineList = new ArrayList<MapPolyline>();
    private Location currentLocation;
    private MapPoint currentMapPoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "메인 onCreate()");
        setContentView(R.layout.activity_main);

        initializeView();

        registerListener();

        Log.d(TAG, AdminApplication.userMap.toString());

        //앱 강제 종료시켰을때 Destroy 동작 시키기 위해서 추가
        startService(new Intent(this, ForcedTerminationService.class));

        //카카오앱을 위한 해시값 얻기
        getAppKeyHash();

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        //위치권한이 허용되지 않았을 경우
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //위치허용 권한 요구하기
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            //이미 위치권한이 허용돼 있을 경우
        } else {
            //현재위치 설정
            Log.d(TAG, "내위치 설정");
            //카카오맵 트래킹 모드 시작
            mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1500, 5, locationListener);
        }

    }

    void initializeView() {
        navView = findViewById(R.id.nav_view);
        imageView_tracking_button = findViewById(R.id.imageView_tracking_button);
        //카카오지도
        mapView = new MapView(this);
        mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);
    }

    void registerListener() {
        navView.setOnNavigationItemSelectedListener(OnNavigationItemSelectedListener);
        //카카오지도
        mapView.setCurrentLocationEventListener(this);
        imageView_tracking_button.setOnClickListener(this);
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
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1500, 5, locationListener);
                    }

                //위치 사용 허가 안했을 때
                } else {
                    Log.d("TAG", "permission denied by user");
                    Util.toastText(this,"위치권한을 허용해야 트래킹기능을 사용할 수 있습니다.");
                }
                return;
            }
        }
    }


    /*하단 네비게이션바 Listener*/
    private BottomNavigationView.OnNavigationItemSelectedListener OnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()){
                /*홈 선택*/
                case R.id.navigation_home:
                    intent = new Intent(MainActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                    startActivity(intent);
                    return true;
                /*트래킹 게시판 선택*/
                case R.id.navigation_tracking_board:
                    intent = new Intent(MainActivity.this, TrackingBoardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                    startActivity(intent);
                    return true;
                /*함께하기 선택*/
                case R.id.navigation_together:
                    intent = new Intent(MainActivity.this, TogetherActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                    startActivity(intent);
                    return true;
                /*마이페이지이 선택*/
                case R.id.navigation_my:
                    intent = new Intent(MainActivity.this, MyPageActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                    startActivity(intent);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG,"메인 onRestart()");

        //카카오지도 view삭제
        mapViewContainer.removeView(mapView);
        //카카오맵 다시 추가
        mapView = new MapView(this);
        mapViewContainer.addView(mapView);
        //경로 삭제
        mapView.removeAllPolylines();
        //경로 다시 생성
        createRoutine();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"메인 onResume()");
        /*하단 네비게이션 checked표시*/
        navView.getMenu().getItem(0).setChecked(true);

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"메인 onStop()");
        //카카오지도 view 삭제
        mapViewContainer.removeView(mapView);
        //경로 삭제
        mapView.removeAllPolylines();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"메인 onDestroy()");
        Util.toastText(this, "메인 onDestroy()");
        //경로 초기화
        routineReset();
    }


    //카카오앱을 위한 해시값 얻기
    private void getAppKeyHash() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md;
                md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String something = new String(Base64.encode(md.digest(), 0));
                Log.d(TAG, something);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, e.toString());
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.imageView_tracking_button){
            //트래킹중인 상태
            if("start".equals(trackingStatus)){
                imageView_tracking_button.setImageResource(R.drawable.icon_video_start);
                trackingStatus = "end";
                //저장할지 물어보기
                openDialog();
            //트래킹중이지 않은 상태
            }else if("end".equals(trackingStatus)){
                //null 예외처리
                if(currentLocation == null){
                    Util.toastText(this,"아직 현재위치가 로딩되지 않았습니다.");
                    return;
                }
                //현재위치 리스트에 추가
                double latitude = currentLocation.getLatitude();
                double longitude = currentLocation.getLongitude();
                Map map = new HashMap();
                map.put("latitude",latitude);
                map.put("longitude",longitude);
                trackingList.add(map);

                imageView_tracking_button.setImageResource(R.drawable.icon_video_pause);
                trackingStatus = "start";
            }
        }
     }

    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float v) {
        Log.d(TAG, "onCurrentLocationUpdate");
//        Log.d(TAG, "현재위치:"+mapPoint.getMapPointGeoCoord());
//        Log.d(TAG, "현재위치:"+mapPoint.getMapPointGeoCoord().latitude);
//        Log.d(TAG, "현재위치:"+mapPoint.getMapPointGeoCoord().longitude);
//        Log.d(TAG, "현재위치v:"+v);
        if(isFirst){
            //카메라이동
            mapView.moveCamera(CameraUpdateFactory.newMapPoint(mapPoint));
            //현재위치 Location객체로 저장
            Location location = new Location("location");
            location.setLatitude(mapPoint.getMapPointGeoCoord().latitude);
            location.setLongitude(mapPoint.getMapPointGeoCoord().longitude);
            currentLocation = location;
            //처음 한번만 동작 되도록 잠그기
            isFirst = false;
        }
        currentMapPoint = mapPoint;
    }

    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {

    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {

    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {

    }


    //위치에 관한 리스너
    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            //여기서 위치값이 갱신되면 이벤트가 발생한다.
            //값은 Location 형태로 리턴되며 좌표 출력 방법은 다음과 같다.
            if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                //Gps 위치제공자에 의한 위치변화. 오차범위가 좁다.
                double longitude = location.getLongitude();    //경도
                double latitude = location.getLatitude();         //위도
                float accuracy = location.getAccuracy();        //신뢰도
                Log.d(TAG,"[리스너]longitude:"+longitude);
                Log.d(TAG,"[리스너]latitude:"+latitude);
                Log.d(TAG,"[리스너]accuracy:"+accuracy);
                currentLocation = location;

                //트랙킹 중일 때에만 리스트에추가하고 경로생성함.
                if("start".equals(trackingStatus)) {
                    //리스트에 추가
                    Map map = new HashMap();
                    map.put("latitude",latitude);
                    map.put("longitude",longitude);
                    trackingList.add(map);
                    //경로 생성
                    createRoutine();
                }

            } else {
                //Network 위치제공자에 의한 위치변화
                //Network 위치는 Gps에 비해 정확도가 많이 떨어진다.
                Log.d(TAG,"[리스너]동작안됨!!!");
            }
        }

        public void onProviderDisabled(String provider) {}

        public void onProviderEnabled(String provider) {}

        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    void createRoutine(){
        Log.d(TAG, "createRoutine()동작");
        //------------------리스트 정보로 경로 만들어주기-------------------------------
        MapPolyline polyline = new MapPolyline();
        polyline.setTag(1001);
        polyline.setLineColor(Color.argb(128, 255, 51, 0)); // Polyline 컬러 지정.

        //폴리라인 리스트에 저장
        polylineList.add(polyline);

        // Polyline 좌표 지정.
        for(Map mapItem : trackingList){
            double latitude = Double.parseDouble(mapItem.get("latitude").toString());
            double longitude = Double.parseDouble(mapItem.get("longitude").toString());
            polyline.addPoint(MapPoint.mapPointWithGeoCoord(latitude, longitude));
        }
        // Polyline 지도에 올리기.
        mapView.addPolyline(polyline);

        //트래킹 정보가 있을때 카메라이동
        if(trackingList.size() > 0){
            //가장 마지막 위치의 위도 경도 가져옴.
            double latitude = Double.parseDouble(trackingList.get(trackingList.size()-1).get("latitude").toString());
            double longitude = Double.parseDouble(trackingList.get(trackingList.size()-1).get("longitude").toString());
            //위도경도 => MapPoint로 변환
            MapPoint mapPoint =  MapPoint.mapPointWithGeoCoord(latitude, longitude);
            mapView.moveCamera(CameraUpdateFactory.newMapPoint(mapPoint));
        //트래킹 정보가 없을때 카메라이동
        }else{
            MapPoint mapPoint = convertLocationToMapPoint(currentLocation);
            mapView.moveCamera(CameraUpdateFactory.newMapPoint(mapPoint));
        }

        //-------------------------------------------------------------------------------------
    }

    //경로 초기화
    void routineReset(){
        //polyline 리스트 초기화
        polylineList.clear();
        //트래킹리스트 초기화
        trackingList.clear();

    }

    //맵에서 경로 없애기
    void removePolyLineToMap(){
        for(MapPolyline mapPolylineItem : polylineList){
            mapView.removePolyline(mapPolylineItem);
        }
    }

    MapPoint convertLocationToMapPoint(Location location){
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        MapPoint mapPoint =  MapPoint.mapPointWithGeoCoord(latitude, longitude);
        return mapPoint;
    }

    //다이얼로그 열기
    void openDialog(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setIcon(R.drawable.icon_warning);
        dialog.setTitle("트래킹");
        dialog.setMessage("트래킹 정보를 저장하시겠습니까?");
        dialog.setPositiveButton("아니오",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //경로 초기화
                routineReset();
            }
        });
        dialog.setNegativeButton("예",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //트래킹 정보 List<Map> => Json으로 변환
                        JSONArray jsonArray = new JSONArray();
                        for(Map mapItem : trackingList){
                            try {
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("longitude", mapItem.get("longitude"));
                                jsonObject.put("latitude", mapItem.get("latitude"));
                                jsonArray.put(jsonObject);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                        String location = jsonArray.toString();

                        //트래킹 정보를 포함시켜서 저장 Activity로 이동
                        intent = new Intent(MainActivity.this, TrackingInfoInputActivity.class);
                        intent.putExtra("location",location);
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                        startActivity(intent);

                        //경로 초기화
                        routineReset();
                    }
                });
        dialog.show();
    }
}
