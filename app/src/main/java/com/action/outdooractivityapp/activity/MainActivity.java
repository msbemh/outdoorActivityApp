package com.action.outdooractivityapp.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.action.outdooractivityapp.AdminApplication;
import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.popup.TakingImageProfilePopup;
import com.action.outdooractivityapp.service.ForcedTerminationService;
import com.action.outdooractivityapp.util.Util;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import net.daum.mf.map.api.CameraUpdateFactory;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPointBounds;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, MapView.CurrentLocationEventListener {

    private static final String TAG = "MainActivity";
    private Intent intent;
    private BottomNavigationView navView;
    private final int MY_PERMISSIONS_REQUEST_LOCATION = 1;
    private final int MY_PERMISSIONS_REQUEST_CAMERA = 2;
    static final int REQUEST_TAKE_PHOTO = 3;
    public MapView mapView;
    private ImageView imageView_tracking_button;
    private ImageView imageView_camera;

    private String currentPhotoPath;
    private Uri photoURI;

    private ViewGroup mapViewContainer;

    private LocationManager locationManager;
    private boolean isFirst = true;
    private static boolean isMapReset = false;
    private String trackingStatus = "end";

    //트래킹 위치 정보
    private List<Map> trackingList = new ArrayList<Map>();
    //트래킹 거리 정보
    private List<Double> trackingDistanceList = new ArrayList<Double>();
    //트래킹 사진 정보(위치포함)
    private List<Map> trackingPhotoList = new ArrayList<Map>();
    private Location currentLocation;
    private Location startLocation;
    private Date startDate;
    private Date endDate;

    private MapPolyline polyline;

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
        imageView_camera = findViewById(R.id.imageView_camera);
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
        imageView_camera.setOnClickListener(this);
    }

    //권한 설정 결과
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA) {
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
        //카메라 권한 요청이였을 경우
        }else if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA) {
            //승인
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,"카메라 권한이 승인됨",Toast.LENGTH_LONG).show();
                //카메라 실행
                dispatchTakePictureIntent();
                galleryAddPic();
            //취소
            } else {
                Toast.makeText(this,"카메라 권한이 거절 되었습니다. 카메라를 이용하려면 권한을 승낙하여야 합니다.",Toast.LENGTH_LONG).show();
            }
        }
    }

    //카메라 촬영 결과
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //카메라 촬영 성공
        if(requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            //트래킹 중일때에만 사진 저장
            if("start".equals(trackingStatus)){
                //트래킹의 photoList에 저장
                if(currentPhotoPath != null){
                    double latitude = currentLocation.getLatitude();
                    double longitude = currentLocation.getLongitude();
                    Map map = new HashMap();
                    map.put("photo_image", currentPhotoPath);
                    map.put("latitude", latitude);
                    map.put("longitude", longitude);
                    trackingPhotoList.add(map);
                }
                //초기화
                currentPhotoPath = null;
                //로그 찍어보기
                for(Map map : trackingPhotoList){
                    Log.d(TAG, "photo_image:"+map.get("photo_image").toString());
                    Log.d(TAG, "latitude:"+map.get("latitude").toString());
                    Log.d(TAG, "longitude:"+map.get("longitude").toString());
                }
            }
        }
    }

    //카메라 촬영&저장 intent전송
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                //해당 경로(외부저장소)에 파일이 생성됨
                photoURI = FileProvider.getUriForFile(this,"com.action.outdooractivityapp.fileprovider",photoFile);
                Log.d(TAG,"[카메라 URI]:"+photoURI);
                //Data결과가 파일URI로 output하겠다는 의미
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                //사진촬영을 하겠다는 의미
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    //파일생성(timeStamp이용)
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile( imageFileName,  ".jpg", storageDir);

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        Log.d(TAG,"[새로 생성된 FILE]:"+image);
        Log.d(TAG,"[새로 생성된 FILE의 절대 경로]:"+currentPhotoPath);

        return image;
    }

    //갤러리에서 인식 안될떄 인식되게 하기
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
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
        //모든 경로 다시 생성
        createAllRoutine();
        //시작위치 다시 생성
        if(startLocation != null){
            startMarker(startLocation);
        }
        //카메라 마커 지도에 표시
        createCameraMarkerAll();
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
        //초기화
        reset();
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

                //시작위치 저장
                startLocation = currentLocation;

                //시작시간 저장
                startDate = new Date();
                Log.d(TAG,"startDate:"+startDate);

                //현재위치 정보
                double latitude = currentLocation.getLatitude();
                double longitude = currentLocation.getLongitude();

                //맵 다시 생서할때를 대비해서 위치정보 저장
                Map map = new HashMap();
                map.put("latitude",latitude);
                map.put("longitude",longitude);
                trackingList.add(map);
                Log.d(TAG, "trackingList.size():"+trackingList.size());

                //시작 위치에 Marker표시하기
                startMarker(startLocation);

                imageView_tracking_button.setImageResource(R.drawable.icon_video_pause);
                trackingStatus = "start";
            }
        //카메라 클릭
        }else if(v.getId() == R.id.imageView_camera){
            //카메라 권한 가져오기
            int permssionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            //카메라 권한 없을때
            if (permssionCheck!= PackageManager.PERMISSION_GRANTED) {
                //User에게 권한 요청
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_CAMERA);
            //카메라 권한 있을때
            }else{
                //카메라 촬영
                dispatchTakePictureIntent();
                galleryAddPic();
            }
        }
     }

    //카카오맵에서의 위치변화감지 (단점, 액티비티 안보이면 동작안함)
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


    //위치에 관한 리스너 (장점: 해당 액티비티 안보여도 동작함.)
    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            //여기서 위치값이 갱신되면 이벤트가 발생한다.
            //값은 Location 형태로 리턴되며 좌표 출력 방법은 다음과 같다.
            if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                //Gps 위치제공자에 의한 위치변화. 오차범위가 좁다.
                double longitude = location.getLongitude();       //경도
                double latitude = location.getLatitude();         //위도
                float accuracy = location.getAccuracy();          //신뢰도
                Log.d(TAG,"[리스너]longitude:"+longitude);
                Log.d(TAG,"[리스너]latitude:"+latitude);
                Log.d(TAG,"[리스너]accuracy:"+accuracy);

                //현재위치 갱신
                currentLocation = location;

                //트랙킹 중일 때에만 리스트에추가하고 경로생성함.
                if("start".equals(trackingStatus)) {

                    //맵 다시 생성할때를 대비해서 위치정보 저장
                    Map map = new HashMap();
                    map.put("latitude",latitude);
                    map.put("longitude",longitude);
                    trackingList.add(map);
                    Log.d(TAG, "[위치추가]");
                    Log.d(TAG, "trackingList.size():"+trackingList.size());

                    //경로 생성
                    createRoutine();

                    //거리 저장
                    createDistance();
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

    //거리 polyLine생성
    void createRoutine(){
        Log.d(TAG, "createRoutine()동작");
        Log.d(TAG, "!!!trackingList.size():"+trackingList.size());
        //------------------리스트 정보로 경로 만들어주기-------------------------------
        if(trackingList.size() > 1){
            //polyLine 생성
            polyline = new MapPolyline();
            polyline.setTag(1);
            polyline.setLineColor(Color.argb(128, 255, 51, 0)); // Polyline 컬러 지정.

            //현재위치와 바로전 위치 가져오기
            int size = trackingList.size();
            double before_latitude = Double.parseDouble(trackingList.get(size-2).get("latitude").toString());
            double before_longitude = Double.parseDouble(trackingList.get(size-2).get("longitude").toString());
            double current_latitude = Double.parseDouble(trackingList.get(size-1).get("latitude").toString());
            double current_longitude = Double.parseDouble(trackingList.get(size-1).get("longitude").toString());

            //polyLine에 추가
            polyline.addPoint(MapPoint.mapPointWithGeoCoord(before_latitude, before_longitude));
            polyline.addPoint(MapPoint.mapPointWithGeoCoord(current_latitude, current_longitude));

            //polyLine을 map에 추가
            mapView.addPolyline(polyline);

            //마지막 위치로 카메라 이동
            MapPoint mapPoint =  MapPoint.mapPointWithGeoCoord(current_latitude, current_longitude);
            mapView.moveCamera(CameraUpdateFactory.newMapPoint(mapPoint));
        //트래킹 중이지 않을때 카메라 현재위치로 이동시키기.
        }else{
            MapPoint mapPoint = convertLocationToMapPoint(currentLocation);
            mapView.moveCamera(CameraUpdateFactory.newMapPoint(mapPoint));
        }
        //-------------------------------------------------------------------------------------
    }

    //모든 경로 다시 생성
    void createAllRoutine(){
        //polyLine 생성
        polyline = new MapPolyline();
        polyline.setTag(1);
        polyline.setLineColor(Color.argb(128, 255, 51, 0)); // Polyline 컬러 지정.

        if(trackingList.size() > 1){
            for(Map mapItem : trackingList){
                double latitude = Double.parseDouble(mapItem.get("latitude").toString());
                double longitude = Double.parseDouble(mapItem.get("longitude").toString());
                polyline.addPoint(MapPoint.mapPointWithGeoCoord(latitude, longitude));
            }

            //polyLine을 map에 추가
            mapView.addPolyline(polyline);

            //마지막 위치로 카메라 이동
            int size = trackingList.size();
            double current_latitude = Double.parseDouble(trackingList.get(size-1).get("latitude").toString());
            double current_longitude = Double.parseDouble(trackingList.get(size-1).get("longitude").toString());
            MapPoint mapPoint =  MapPoint.mapPointWithGeoCoord(current_latitude, current_longitude);
            mapView.moveCamera(CameraUpdateFactory.newMapPoint(mapPoint));
        //트래킹 중이지 않을때 카메라 현재위치로 이동시키기.
        }else{
            MapPoint mapPoint = convertLocationToMapPoint(currentLocation);
            mapView.moveCamera(CameraUpdateFactory.newMapPoint(mapPoint));
        }
    }

    //카메라 마커 지도에 표시 (카메라창 갔다오면 OnStop => OnRestart 돼서 OnRestart에서 불러옴)
    void createCameraMarkerAll(){
        for(Map mapItem : trackingPhotoList){
            double latitude = Double.parseDouble(mapItem.get("latitude").toString());
            double longitude = Double.parseDouble(mapItem.get("longitude").toString());

            MapPOIItem customMarker = new MapPOIItem();
            customMarker.setItemName("사진");
            customMarker.setTag(1);
            MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude);
            customMarker.setMapPoint(mapPoint);
            customMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커타입을 커스텀 마커로 지정.
            customMarker.setCustomImageResourceId(R.drawable.icon_camera_marker); // 마커 이미지.
            customMarker.setCustomImageAutoscale(false); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
            customMarker.setCustomImageAnchor(0.5f, 1.0f); // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정 - 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.

            mapView.addPOIItem(customMarker);

        }

    }

    //이동 거리 추가
    void createDistance(){
        if(trackingList.size() > 1){
            //현재위치와 바로전 위치 가져오기
            int size = trackingList.size();
            double before_latitude = Double.parseDouble(trackingList.get(size-2).get("latitude").toString());
            double before_longitude = Double.parseDouble(trackingList.get(size-2).get("longitude").toString());
            double current_latitude = Double.parseDouble(trackingList.get(size-1).get("latitude").toString());
            double current_longitude = Double.parseDouble(trackingList.get(size-1).get("longitude").toString());
            double distance = Util.distanceByHaversine(before_latitude,before_longitude,current_latitude,current_longitude);
            Log.d(TAG,"distance:"+distance);
            trackingDistanceList.add(distance);
        }
    }

    //초기화
    void reset(){
        //트래킹리스트 초기화
        trackingList.clear();
        //경로 삭제
        mapView.removeAllPolylines();
        //시작위치 초기화
        startLocation = null;
        //모든 마커 삭제
        mapView.removeAllPOIItems();
        //시작 시간 초기화
        startDate = null;
        //끝 시간 초기화
        endDate = null;
        //이동거리 초기화
        trackingDistanceList.clear();
        //사진 트래킹 리스트 초기화
        trackingPhotoList.clear();

    }

    MapPoint convertLocationToMapPoint(Location location){
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        MapPoint mapPoint =  MapPoint.mapPointWithGeoCoord(latitude, longitude);
        return mapPoint;
    }

    void startMarker(Location location){
        MapPOIItem customMarker = new MapPOIItem();
        customMarker.setItemName("시작위치");
        customMarker.setTag(1);
        MapPoint mapPoint = convertLocationToMapPoint(location);
        customMarker.setMapPoint(mapPoint);
        customMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커타입을 커스텀 마커로 지정.
        customMarker.setCustomImageResourceId(R.drawable.icon_tracking_start); // 마커 이미지.
        customMarker.setCustomImageAutoscale(false); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
        customMarker.setCustomImageAnchor(0.5f, 1.0f); // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정 - 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.

        mapView.addPOIItem(customMarker);
    }

    //다이얼로그 열기
    void openDialog(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setIcon(R.drawable.icon_warning);
        dialog.setTitle("트래킹");
        dialog.setMessage("트래킹 정보를 저장하시겠습니까?");
        dialog.setPositiveButton("아니오",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //초기화
                reset();
            }
        });
        dialog.setNegativeButton("예",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //트래킹 정보 List<Map> => Json으로 변환
                        String location = Util.convertListMapToJsonString(trackingList);
                        Log.d(TAG,"location:"+location);

                        //사진 트래킹 정보 List<Map> => Json으로 변환
                        String photoListInfo = Util.convertListMapToJsonString(trackingPhotoList);
                        Log.d(TAG,"photoListInfo:"+photoListInfo);

                        //이동거리 계산
                        double distance = 0;
                        for(double distanceItem : trackingDistanceList){
                            distance += distanceItem;
                        }

                        //끝 시간 생성
                        endDate = new Date();
                        Log.d(TAG,"endDate:"+endDate);

                        //Date => String(yyyy-MM-dd HH:mm:ss)으로 변환하여 보내주기
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                        //트래킹 정보를 포함시켜서 저장 Activity로 이동
                        intent = new Intent(MainActivity.this, TrackingInfoInputActivity.class);
                        intent.putExtra("location",location);
                        intent.putExtra("startDate",format.format(startDate));
                        intent.putExtra("endDate",format.format(endDate));
                        intent.putExtra("distance",distance);
                        intent.putExtra("photoListInfo",photoListInfo);
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                        startActivity(intent);

                        //초기화
                        reset();
                    }
                });
        dialog.show();
    }
}
