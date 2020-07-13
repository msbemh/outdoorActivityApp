package com.action.outdooractivityapp.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.service.LocationSharingService;
import com.action.outdooractivityapp.util.Util;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.odsay.odsayandroidsdk.API;
import com.odsay.odsayandroidsdk.ODsayData;
import com.odsay.odsayandroidsdk.ODsayService;
import com.odsay.odsayandroidsdk.OnResultCallbackListener;

import org.json.JSONException;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetDirectionsActivity extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        OnResultCallbackListener{

    private GoogleMap mMap;
    private String TAG = "GetDirectionsActivity";
    private Button button_get_directions;
    private EditText editText_start_select;
    private EditText editText_end_select;
    private final int AUTOCOMPLETE_REQUEST_CODE = 0;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    //location 권한이 허용되지 않을때, 디폴트 위치 정보
    private final LatLng mDefaultLocation = new LatLng(37.56, 126.97);
    private static final int DEFAULT_ZOOM = 15;
    // The entry point to the Places API.
    private PlacesClient mPlacesClient;
    //검색 시작(0), 도착(1)
    private int select_flag = -1;

    //Geocoder
    private Geocoder geocoder;

    private LatLng startPos;
    private LatLng endPos;
    private Polyline polyline;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private ImageView image_public_transport;
    private ImageView image_driving;
    private ImageView image_walking;
    private ImageView image_bicycle;
    private String mode = "driving";

    private TextView textView_total_time;
    private TextView textView_total_distance;

    private ODsayService odsayService;

    private static final int COLOR_BLACK_ARGB = 0xff000000;
    private static final int COLOR_WHITE_ARGB = 0xffffffff;
    private static final int COLOR_GREEN_ARGB = 0xff388E3C;
    private static final int COLOR_PURPLE_ARGB = 0xff81C784;
    private static final int COLOR_ORANGE_ARGB = 0xffF57F17;
    private static final int COLOR_BLUE_ARGB = 0xFFCC008C;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_directions);

        TextView button_get_directions = (Button) findViewById(R.id.button_get_directions);
        button_get_directions.setOnClickListener(this);

        initializeView();

        registerListener();

        geocoder = new Geocoder(this);
        //PlacesClient 생성
        Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        mPlacesClient = Places.createClient(this);

        //내위치 인지를 위해서 추가
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    Log.d(TAG,"현재 나의 위치로 이동하자!"+location);
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    LatLng latLng =  new LatLng(latitude, longitude);
                    //내위치로 카메라 이동
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom( latLng, DEFAULT_ZOOM));
                }
            }
        });
    }


    void initializeView() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        editText_start_select = findViewById(R.id.editText_start_select);
        editText_end_select = findViewById(R.id.editText_end_select);
        image_public_transport = findViewById(R.id.image_public_transport);
        image_driving = findViewById(R.id.image_driving);
        image_walking = findViewById(R.id.image_walking);
        image_bicycle = findViewById(R.id.image_bicycle);
        textView_total_time = findViewById(R.id.textView_total_time);
        textView_total_distance = findViewById(R.id.textView_total_distance);

        // 싱글톤 생성, Key 값을 활용하여 객체 생성
        odsayService = ODsayService.init(this, "WGDe7oPYSppfG0fDCDC8D6PlFUzTO/xs58qYuiIxYYI");
        // 서버 연결 제한 시간(단위(초), default : 5초)
        odsayService.setReadTimeout(5000);
        // 데이터 획득 제한 시간(단위(초), default : 5초)
        odsayService.setConnectionTimeout(5000);

    }

    void registerListener() {
        editText_start_select.setOnClickListener(this);
        editText_end_select.setOnClickListener(this);
        image_public_transport.setOnClickListener(this);
        image_driving.setOnClickListener(this);
        image_walking.setOnClickListener(this);
        image_bicycle.setOnClickListener(this);
    }

    //권한 요청 결과
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE : {
                //위치 사용 허가했을 때
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (mMap != null) {
                        //내위치 활성화
                        mMap.setMyLocationEnabled(true);
                        //내위치 버튼 활성화
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);
                    }
                //위치 사용 허가 안했을 때
                } else {
                    Log.d(TAG, "permission denied by user");
                    Util.toastText(this,"위치권한을 허용해야합니다.");
                    finish();
                }
            }
            break;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //위치권한 존재할떄
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                Log.d(TAG, "위치활성화!");
                //내위치 활성화
                mMap.setMyLocationEnabled(true);
                //내위치 버튼 활성화
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                mMap.setOnMyLocationButtonClickListener(this);
                mMap.setOnMyLocationClickListener(this);
            }
            //위치권한 없을때 권한 요청
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onClick(View v) {
        //길찾기 버튼 클릭
        if(v.getId() == R.id.button_get_directions){
            //대중교통 길찾기 일때, Google Map API 이용
            if("transit".equals(mode)){
                double startLatitude = startPos.latitude;
                double startLongitude = startPos.longitude;
                double endLatitude = endPos.latitude;
                double endLongitude = endPos.longitude;
                Log.d(TAG,"startLatitude:"+startLatitude);
                Log.d(TAG,"startLongitude:"+startLongitude);
                Log.d(TAG,"endLatitude:"+endLatitude);
                Log.d(TAG,"endLongitude:"+endLongitude);

                // API 호출
                odsayService.requestSearchPubTransPath(startLongitude+"", startLatitude+"", endLongitude+"", endLatitude+"",
                        "0","0","0", this);
                //---------------------------------------------------
            //자동차 길찾기 일때, Naver API 이용
            }else if("driving".equals(mode)){
                //http통신으로 길찾기 json 데이터 받아오기
                String url = "https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving";
                double startLatitude = startPos.latitude;
                double startLongitude = startPos.longitude;
                double endLatitude = endPos.latitude;
                double endLongitude = endPos.longitude;
                LatLng startLatLng = new LatLng(startLatitude, startLongitude);
                LatLng endLatLng = new LatLng(endLatitude, endLongitude);

                String parameters = "start="+startLongitude+","+startLatitude
                        +"&goal="+endLongitude+","+endLatitude
                        +"&option=trafast";
                String method = "GET";
                Map headrInfoMap = new HashMap();
                headrInfoMap.put("X-NCP-APIGW-API-KEY-ID","dtkviawsm8");
                headrInfoMap.put("X-NCP-APIGW-API-KEY","d5LTKKgjW8Sujoq095AtScnfcrjKHAjwq3w20VXn");

                Map resultMap = Util.httpConnMap(url, parameters, method, headrInfoMap);
                //데이터 뽑기
                Log.d(TAG,"resultMap:"+resultMap);

                //결과 code 확인
                int code = Integer.parseInt(resultMap.get("code").toString());
                //code
                //0: 길찾기 성공, 1: 출발지와 도착지가 동일, 2: 출발지 또는 도착지가 도로 주변이 아닌 경우, 3:자동차 길찾기 결과 제공 불가
                //4: 경유지가 도로 주변이 아닌 경우, 5: 요청 경로가 매우 긴 경우(경유지를 포함한 직선거리의 합이 1500km이상인 경우)
                if(code == 0){
                    //맵 클리어
                    mMap.clear();

                    Map routesMap = (Map)resultMap.get("route");
                    List trafastList = (List)(routesMap.get("trafast"));
                    Map summaryMap = (Map)((Map)trafastList.get(0)).get("summary");
                    List bboxList = (List)summaryMap.get("bbox");
                    List pathList = (List)((Map)trafastList.get(0)).get("path");


                    //총 걸린시간
                    int duration = Integer.parseInt(summaryMap.get("duration").toString());
                    String totalTime = "";
                    int hour = (duration/(1000*60*60))%24;
                    int min = (duration/(1000*60))%60;
                    int second = (duration/(1000))%60;
                    if(hour != 0){
                        totalTime += hour+"시간";
                    }
                    if(min != 0){
                        totalTime += min+"분";
                    }
                    if(second != 0){
                        totalTime += second+"초";
                    }

                    //총 거리
                    int distance = Integer.parseInt(summaryMap.get("distance").toString());
                    textView_total_time.setText(totalTime);
                    textView_total_distance.setText(distance+"m");

                    PolylineOptions polylineOptions = new PolylineOptions();
                    polylineOptions.clickable(true);
                    polylineOptions.add(startLatLng);
                    //경로 선긋기 위한 데이터 뽑기
                    for(int i=0; i<pathList.size(); i++){
                        List locationList = (List)pathList.get(i);
                        Log.d(TAG, "locationList:"+locationList);
                        double longitude = Double.parseDouble(locationList.get(0).toString());
                        double latitude = Double.parseDouble(locationList.get(1).toString());
                        Log.d(TAG, "latitude:"+latitude);
                        Log.d(TAG, "longitude:"+longitude);
                        LatLng latLng = new LatLng(latitude, longitude);
                        Log.d(TAG, "latLng:"+latLng);
                        polylineOptions.add(latLng);
                    }
                    polylineOptions.add(endLatLng);

                    //선 긋기
                    polyline = mMap.addPolyline(polylineOptions);

                    //카메라 조정을 위한 데이터 뽑기
                    List leftBottomList = (List)bboxList.get(0);
                    List rightTopList = (List)bboxList.get(1);

                    double leftBottomLongitude = Double.parseDouble(leftBottomList.get(0).toString());
                    double leftBottomLatitude = Double.parseDouble(leftBottomList.get(1).toString());
                    double rightTopLongitude = Double.parseDouble(rightTopList.get(0).toString());
                    double rightTopLatitude = Double.parseDouble(rightTopList.get(1).toString());

                    LatLng northeastLatLng = new LatLng(leftBottomLatitude, leftBottomLongitude);
                    LatLng southwestLatLng = new LatLng(rightTopLatitude, rightTopLongitude);

                    LatLngBounds latLngBounds = new LatLngBounds(northeastLatLng, southwestLatLng);
                    //카메라 이동
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100));

                    //마커 추가하기
                    //시작 마커 추가
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.zIndex(10);
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    markerOptions.title("출발");
                    markerOptions.position(startLatLng);
                    mMap.addMarker(markerOptions);
                    //시작 마커 추가
                    markerOptions = new MarkerOptions();
                    markerOptions.zIndex(10);
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    markerOptions.title("도착");
                    markerOptions.position(endLatLng);
                    mMap.addMarker(markerOptions);

                }else{
                    Util.toastText(this, "길찾기 실패");
                }
            //보행자 길찾기
            }else if("walking".equals(mode)){
                //http통신으로 길찾기 json 데이터 받아오기
                String url = "https://apis.openapi.sk.com/tmap/routes/pedestrian";
                double startLatitude = startPos.latitude;
                double startLongitude = startPos.longitude;
                double endLatitude = endPos.latitude;
                double endLongitude = endPos.longitude;
                LatLng startLatLng = new LatLng(startLatitude, startLongitude);
                LatLng endLatLng = new LatLng(endLatitude, endLongitude);

                String parameters = "version=1"
                        +"&startX="+ startLongitude
                        +"&startY="+ startLatitude
                        +"&endX="+ endLongitude
                        +"&endY="+ endLatitude
                        +"&startName=출발"
                        +"&endName=도착"
                        +"&appkey=l7xx92a34c3fe9414957ae36d28263736b95";
                String method = "GET";

                Map resultMap = Util.httpConnMap(url, parameters, method);
                //데이터 뽑기
                Log.d(TAG,"resultMap:"+resultMap);

                //결과성공시 type에 FeatureCollection값이 존재
                if(resultMap.get("type") != null){
                    //맵 클리어
                    mMap.clear();

                    List featuresList =  (List)resultMap.get("features");
                    Map propertiesMap = (Map)((Map)featuresList.get(0)).get("properties");
                    int totalDistance = Integer.parseInt(propertiesMap.get("totalDistance").toString());
                    int totalTime = Integer.parseInt(propertiesMap.get("totalTime").toString());

                    //총 걸린시간
                    String totalTimeString = "";
                    int hour = (totalTime/(60*60))%24;
                    int min = (totalTime/(60))%60;
                    int second = (totalTime)%60;
                    if(hour != 0){
                        totalTimeString += hour+"시간";
                    }
                    if(min != 0){
                        totalTimeString += min+"분";
                    }
                    if(second != 0){
                        totalTimeString += second+"초";
                    }

                    textView_total_time.setText(totalTimeString);
                    textView_total_distance.setText(totalDistance+"m");

                    PolylineOptions polylineOptions = new PolylineOptions();
                    polylineOptions.clickable(true);
                    polylineOptions.add(startLatLng);
                    //경로 선긋기 위한 데이터 뽑기
                    //동서남북 최댓값 뽑기
                    double leftMax = startLongitude;
                    double rightMax = startLongitude;
                    double topMax = startLatitude;
                    double bottomMax = startLatitude;
                    for(int i=0; i<featuresList.size(); i++){
                        Map geometryMap = (Map)((Map)featuresList.get(i)).get("geometry");
                        String type = geometryMap.get("type").toString();
                        //점일때
                        if("Point".equals(type)){
                            List coordinatesList = (List)geometryMap.get("coordinates");
                            double longitude = Double.parseDouble(coordinatesList.get(0).toString());
                            double latitude = Double.parseDouble(coordinatesList.get(1).toString());
                            Log.d(TAG, "latitude:"+latitude);
                            Log.d(TAG, "longitude:"+longitude);
                            LatLng latLng = new LatLng(latitude, longitude);
                            polylineOptions.add(latLng);

                            //동서남북 최댓값 뽑기
                            if(leftMax > longitude){
                                leftMax = longitude;
                            }
                            if(rightMax < longitude){
                                rightMax = longitude;
                            }
                            if(topMax < latitude){
                                topMax = latitude;
                            }
                            if(bottomMax > latitude){
                                bottomMax = latitude;
                            }
                            Log.d(TAG, "leftMax:"+leftMax);
                            Log.d(TAG, "rightMax:"+rightMax);
                            Log.d(TAG, "topMax:"+topMax);
                            Log.d(TAG, "bottomMax:"+bottomMax);
                            Log.d(TAG, "------------------------");
                        //선일때
                        }else if("LineString".equals(type)){
                            List coordinatesList = (List)geometryMap.get("coordinates");
                            for(int j=1; j<coordinatesList.size(); j++){
                                List coordinatesValueList = (List)coordinatesList.get(j);
                                double longitude = Double.parseDouble(coordinatesValueList.get(0).toString());
                                double latitude = Double.parseDouble(coordinatesValueList.get(1).toString());
                                Log.d(TAG, "latitude:"+latitude);
                                Log.d(TAG, "longitude:"+longitude);
                                LatLng latLng = new LatLng(latitude, longitude);
                                polylineOptions.add(latLng);

                                //동서남북 최댓값 뽑기
                                if(leftMax > longitude){
                                    leftMax = longitude;
                                }
                                if(rightMax < longitude){
                                    rightMax = longitude;
                                }
                                if(topMax < latitude){
                                    topMax = latitude;
                                }
                                if(bottomMax > latitude){
                                    bottomMax = latitude;
                                }
                                Log.d(TAG, "leftMax:"+leftMax);
                                Log.d(TAG, "rightMax:"+rightMax);
                                Log.d(TAG, "topMax:"+topMax);
                                Log.d(TAG, "bottomMax:"+bottomMax);
                                Log.d(TAG, "------------------------");
                            }
                        }
                    }
                    polylineOptions.add(endLatLng);

                    //선 긋기
                    polyline = mMap.addPolyline(polylineOptions);

                    LatLng northeastLatLng = new LatLng(topMax, rightMax);
                    LatLng southwestLatLng = new LatLng(bottomMax, leftMax);

                    LatLngBounds latLngBounds = new LatLngBounds(southwestLatLng, northeastLatLng);
                    //카메라 이동
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 120));

                    //마커 추가하기
                    //시작 마커 추가
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.zIndex(10);
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    markerOptions.title("출발");
                    markerOptions.position(startLatLng);
                    mMap.addMarker(markerOptions);
                    //시작 마커 추가
                    markerOptions = new MarkerOptions();
                    markerOptions.zIndex(10);
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    markerOptions.title("도착");
                    markerOptions.position(endLatLng);
                    mMap.addMarker(markerOptions);

                //결과실패시 type이라는 key가 존재하지 않음
                //error라는 key가 존재함.
                }else{
                    Util.toastText(this, "[보행자]길찾기 실패");
                }

            }
        //시작 위치 검색 클릭
        }else if(v.getId() == R.id.editText_start_select){
            Log.d(TAG,"시작 위치 검색 클릭");
            //Arrays.asList()에는 배열을 만들어 넣으면 List로 반환을 해준다.
            //하지만 원소를 추가할 수 없다. 단지 배열의 주소를 갖고 있을 뿐이다.
            //원본 배열을 바꿔야 Arrays.asList()의 원소가 추가되거나 삭제될 수 있을 뿐이다.
            List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG,Place.Field.TYPES);

            //검색 시작
            select_flag = 0;

            //자동완성 intent시작하기
            Intent intent = new Autocomplete.IntentBuilder(
                    AutocompleteActivityMode.OVERLAY, fields)
                    .build(this);
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
        //도착 위치 검색 클릭
        }else if(v.getId() == R.id.editText_end_select){
            Log.d(TAG,"도착 위치 검색 클릭");
            List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG,Place.Field.TYPES);

            //도착 시작
            select_flag = 1;

            //자동완성 intent시작하기
            Intent intent = new Autocomplete.IntentBuilder(
                    AutocompleteActivityMode.OVERLAY, fields)
                    .build(this);
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
        //대중교통 선택
        }else if(v.getId() == R.id.image_public_transport){
            mode = "transit";
            image_public_transport.setBackgroundColor(Color.GRAY);
            image_driving.setBackgroundColor(Color.WHITE);
            image_walking.setBackgroundColor(Color.WHITE);
            image_bicycle.setBackgroundColor(Color.WHITE);
        //운전 선택
        }else if(v.getId() == R.id.image_driving){
            mode = "driving";
            image_public_transport.setBackgroundColor(Color.WHITE);
            image_driving.setBackgroundColor(Color.GRAY);
            image_walking.setBackgroundColor(Color.WHITE);
            image_bicycle.setBackgroundColor(Color.WHITE);
        //걷기 선택
        }else if(v.getId() == R.id.image_walking){
            mode = "walking";
            image_public_transport.setBackgroundColor(Color.WHITE);
            image_driving.setBackgroundColor(Color.WHITE);
            image_walking.setBackgroundColor(Color.GRAY);
            image_bicycle.setBackgroundColor(Color.WHITE);
        //자전거 선택
        }else if(v.getId() == R.id.image_bicycle){
            mode = "bicycling";
            image_public_transport.setBackgroundColor(Color.WHITE);
            image_driving.setBackgroundColor(Color.WHITE);
            image_walking.setBackgroundColor(Color.WHITE);
            image_bicycle.setBackgroundColor(Color.GRAY);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //장소 검색에 대한 결과 얻어옴
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                double latitude = place.getLatLng().latitude;
                double longitude = place.getLatLng().longitude;
                Log.d(TAG,"place:"+place);

                //camera position의 위도 경도 위치로 camera이동
                LatLng latLng =  new LatLng(latitude, longitude);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));

                //주소이름
                String addressName = place.getName();
                //시작위치 검색
                if(select_flag == 0){
                    editText_start_select.setText(addressName);
                    startPos = latLng;
                //도착위치 검색
                }else if(select_flag == 1){
                    editText_end_select.setText(addressName);
                    endPos = latLng;
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Handle the error.
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.i(TAG, status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }

    //위도경도를 이용하여 주소가져오기(geocoder이용)
    String bringAddressUsingLatLng(LatLng latLng){
        List<Address> list = null;
        String address = "";
        try {
            list = geocoder.getFromLocation(
                    latLng.latitude, // 위도
                    latLng.longitude, // 경도
                    2); // 얻어올 값의 개수
            Log.d(TAG,"list:"+list);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("test", "입출력 오류 - 서버에서 주소변환시 에러발생");
        }
        if (list != null) {
            if (list.size()==0) {
                Log.d(TAG,"해당되는 주소 정보는 없습니다");
            } else {
                Log.d(TAG,list.get(0).toString());
                address = list.get(0).getAddressLine(0);
            }
        }
        return address;
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    //오디세이 길찾기 호출 성공 시 실행
    @Override
    public void onSuccess(ODsayData odsayData, API api) {
        // API Value 는 API 호출 메소드 명을 따라갑니다.
        if (api == API.SEARCH_PUB_TRANS_PATH) {
            //맵 클리어
            mMap.clear();

            double startLatitude = startPos.latitude;
            double startLongitude = startPos.longitude;
            double endLatitude = endPos.latitude;
            double endLongitude = endPos.longitude;
            LatLng startLatLng = new LatLng(startLatitude, startLongitude);
            LatLng endLatLng = new LatLng(endLatitude, endLongitude);

            Map resultMap = (Map)odsayData.getMap().get("result");
            Log.d(TAG,"resultMap:"+resultMap);
            List pathList = (List)resultMap.get("path");
            Map firstPathMap = (Map)pathList.get(0);
            Map infoMap = (Map)firstPathMap.get("info");
            //시간 [분]
            int totalTime = Integer.parseInt(infoMap.get("totalTime").toString());
            //거리 [m]
            int totalDistance = Integer.parseInt(infoMap.get("totalDistance").toString());
            String totalTimeString ="";

            int hour = totalTime/(60);
            int min = totalTime%60;
            if(hour != 0){
                totalTimeString += hour+"시간";
            }
            if(min != 0){
                totalTimeString += min+"분";
            }

            textView_total_time.setText(totalTimeString);
            textView_total_distance.setText(totalDistance+"m");

            int pathType = Integer.parseInt(firstPathMap.get("pathType").toString());

            //지하철, 버스, 도보 마다 Polyline 색상 변화시키기.
            PolylineOptions polylineOptions1 = new PolylineOptions();
            polylineOptions1.clickable(true);
            polylineOptions1.color(COLOR_GREEN_ARGB);
            PolylineOptions polylineOptions2 = new PolylineOptions();
            polylineOptions2.clickable(true);
            polylineOptions2.color(COLOR_BLUE_ARGB);


            //동서남북 최댓값 뽑기
            double leftMax = startLongitude;
            double rightMax = startLongitude;
            double topMax = startLatitude;
            double bottomMax = startLatitude;

            //pathType
            // 1: 지하철
            // 2: 버스
            // 3 :지하철 + 버스
            List subPathList = (List)firstPathMap.get("subPath");
            Log.d(TAG,"subPathList.size():"+subPathList.size());
            for(int i=0; i<subPathList.size(); i++){
                Map subPathMap = (Map)subPathList.get(i);
                int trafficType = Integer.parseInt(subPathMap.get("trafficType").toString());

                //trafficType
                // 1: 지하철
                // 2: 버스
                // 3: 도보

                //지하철
                if(trafficType == 1){
                    List laneList = (List)subPathMap.get("lane");
                    for(int j=0; j<laneList.size(); j++){
                        String name = ((Map)laneList.get(j)).get("name").toString();
                        String subwayCode = ((Map)laneList.get(j)).get("subwayCode").toString();
                        Log.d(TAG,"subwayCode:"+subwayCode);
                    }
                    Map passStopMap = (Map)subPathMap.get("passStopList");
                    List stationsList = (List)passStopMap.get("stations");
                    for(int j=0; j<stationsList.size(); j++){
                        Map stationMap = (Map)stationsList.get(j);
                        double longitude = Double.parseDouble(stationMap.get("x").toString());
                        double latitude = Double.parseDouble(stationMap.get("y").toString());
                        Log.d(TAG,"latitude:"+latitude);
                        Log.d(TAG,"longitude:"+longitude);
                        LatLng latLng = new LatLng(latitude, longitude);
                        polylineOptions1.add(latLng);

                        //동서남북 최댓값 뽑기
                        if(leftMax > longitude){
                            leftMax = longitude;
                        }
                        if(rightMax < longitude){
                            rightMax = longitude;
                        }
                        if(topMax < latitude){
                            topMax = latitude;
                        }
                        if(bottomMax > latitude){
                            bottomMax = latitude;
                        }
                        Log.d(TAG, "leftMax:"+leftMax);
                        Log.d(TAG, "rightMax:"+rightMax);
                        Log.d(TAG, "topMax:"+topMax);
                        Log.d(TAG, "bottomMax:"+bottomMax);
                        Log.d(TAG, "------------------------");
                    }
                //버스
                }else if(trafficType == 2){
                    List laneList = (List)subPathMap.get("lane");
                    for(int j=0; j<laneList.size(); j++){
                        String busNo = ((Map)laneList.get(j)).get("busNo").toString();
                        Log.d(TAG,"busNo:"+busNo);
                    }
                    Map passStopMap = (Map)subPathMap.get("passStopList");
                    List stationsList = (List)passStopMap.get("stations");
                    for(int j=0; j<stationsList.size(); j++){
                        Map stationMap = (Map)stationsList.get(j);
                        double longitude = Double.parseDouble(stationMap.get("x").toString());
                        double latitude = Double.parseDouble(stationMap.get("y").toString());
                        Log.d(TAG,"latitude:"+latitude);
                        Log.d(TAG,"longitude:"+longitude);
                        LatLng latLng = new LatLng(latitude, longitude);
                        polylineOptions2.add(latLng);

                        //동서남북 최댓값 뽑기
                        if(leftMax > longitude){
                            leftMax = longitude;
                        }
                        if(rightMax < longitude){
                            rightMax = longitude;
                        }
                        if(topMax < latitude){
                            topMax = latitude;
                        }
                        if(bottomMax > latitude){
                            bottomMax = latitude;
                        }
                        Log.d(TAG, "leftMax:"+leftMax);
                        Log.d(TAG, "rightMax:"+rightMax);
                        Log.d(TAG, "topMax:"+topMax);
                        Log.d(TAG, "bottomMax:"+bottomMax);
                        Log.d(TAG, "------------------------");
                    }
                //도보
                }else if(trafficType == 3){
                    Log.d(TAG, "------------------------");
                    Log.d(TAG, "보행자");
                    double walkingStartLongitude;
                    double walkingStartLatitude;
                    double walkingEndLongitude;
                    double walkingEndLatitude;

                    //처음 subPath라면
                    if(i == 0){
                        //polyLine옵션 생성
                        PolylineOptions polylineOptions3 = new PolylineOptions();
                        polylineOptions3.clickable(true);
                        polylineOptions3.color(COLOR_BLACK_ARGB);

                        //사작 위치 가져오기
                        walkingStartLongitude = startLongitude;
                        walkingStartLatitude = startLatitude;

                        Log.d(TAG, "walkingStartLongitude:"+walkingStartLongitude);
                        Log.d(TAG, "walkingStartLatitude:"+walkingStartLatitude);
                        LatLng latLng = new LatLng(walkingStartLatitude, walkingStartLongitude);
                        polylineOptions3.add(latLng);

                        //다음 subPath에서 위치정보 가져오기
                        Map nextSubPathMap = (Map)subPathList.get(i+1);
                        walkingEndLongitude = Double.parseDouble(nextSubPathMap.get("startX").toString());
                        walkingEndLatitude = Double.parseDouble(nextSubPathMap.get("startY").toString());

                        Log.d(TAG, "walkingEndLongitude:"+walkingEndLongitude);
                        Log.d(TAG, "walkingEndLatitude:"+walkingEndLatitude);
                        latLng = new LatLng(walkingEndLatitude, walkingEndLongitude);
                        polylineOptions3.add(latLng);

                        //보행자 선 긋기
                        polyline = mMap.addPolyline(polylineOptions3);
                    //마지막 subPath라면
                    }else if(i == subPathList.size()-1){
                        //polyLine옵션 생성
                        PolylineOptions polylineOptions3 = new PolylineOptions();
                        polylineOptions3.clickable(true);
                        polylineOptions3.color(COLOR_BLACK_ARGB);

                        //전 subPath에서에서 위치정보 가져오기
                        Map beforeSubPathMap = (Map)subPathList.get(i-1);
                        walkingStartLongitude = Double.parseDouble(beforeSubPathMap.get("endX").toString());
                        walkingStartLatitude = Double.parseDouble(beforeSubPathMap.get("endY").toString());

                        Log.d(TAG, "walkingStartLongitude:"+walkingStartLongitude);
                        Log.d(TAG, "walkingStartLatitude:"+walkingStartLatitude);
                        LatLng latLng = new LatLng(walkingStartLatitude, walkingStartLongitude);
                        polylineOptions3.add(latLng);

                        //도착 위치정보 가져오기
                        walkingEndLongitude = endLongitude;
                        walkingEndLatitude = endLatitude;

                        Log.d(TAG, "walkingEndLongitude:"+walkingEndLongitude);
                        Log.d(TAG, "walkingEndLatitude:"+walkingEndLatitude);
                        latLng = new LatLng(walkingEndLatitude, walkingEndLongitude);
                        polylineOptions3.add(latLng);

                        //보행자 선 긋기
                        polyline = mMap.addPolyline(polylineOptions3);
                    //중간 subPath라면
                    }else{
                        //polyLine옵션 생성
                        PolylineOptions polylineOptions3 = new PolylineOptions();
                        polylineOptions3.clickable(true);
                        polylineOptions3.color(COLOR_BLACK_ARGB);

                        //전 subPath에서에서 위치정보 가져오기
                        Map beforeSubPathMap = (Map)subPathList.get(i-1);
                        walkingStartLongitude = Double.parseDouble(beforeSubPathMap.get("endX").toString());
                        walkingStartLatitude = Double.parseDouble(beforeSubPathMap.get("endY").toString());

                        Log.d(TAG, "walkingStartLongitude:"+walkingStartLongitude);
                        Log.d(TAG, "walkingStartLatitude:"+walkingStartLatitude);
                        LatLng latLng = new LatLng(walkingStartLatitude, walkingStartLongitude);
                        polylineOptions3.add(latLng);

                        //다음 subPath에서 위치정보 가져오기
                        Map nextSubPathMap = (Map)subPathList.get(i+1);
                        walkingEndLongitude = Double.parseDouble(nextSubPathMap.get("startX").toString());
                        walkingEndLatitude = Double.parseDouble(nextSubPathMap.get("startY").toString());

                        Log.d(TAG, "walkingEndLongitude:"+walkingEndLongitude);
                        Log.d(TAG, "walkingEndLatitude:"+walkingEndLatitude);
                        latLng = new LatLng(walkingEndLatitude, walkingEndLongitude);
                        polylineOptions3.add(latLng);

                        //보행자 선 긋기
                        polyline = mMap.addPolyline(polylineOptions3);
                    }
                }
            }
            //지하철 선 긋기
            if(polylineOptions1 != null){
                polyline = mMap.addPolyline(polylineOptions1);
            }
            //버스 선 긋기
            if(polylineOptions2 != null){
                polyline = mMap.addPolyline(polylineOptions2);
            }

            LatLng northeastLatLng = new LatLng(topMax, rightMax);
            LatLng southwestLatLng = new LatLng(bottomMax, leftMax);

            LatLngBounds latLngBounds = new LatLngBounds(southwestLatLng, northeastLatLng);
            //카메라 이동
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 140));

            //마커 추가하기
            //시작 마커 추가
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.zIndex(10);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            markerOptions.title("출발");
            markerOptions.position(startLatLng);
            mMap.addMarker(markerOptions);
            //시작 마커 추가
            markerOptions = new MarkerOptions();
            markerOptions.zIndex(10);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            markerOptions.title("도착");
            markerOptions.position(endLatLng);
            mMap.addMarker(markerOptions);
        }
    }
    //오디세이 길찾기 호출 실패 시 실행
    @Override
    public void onError(int i, String s, API api) {
        if (api == API.SEARCH_PUB_TRANS_PATH) {
            Util.toastText(this,"오디세이 API 길찾기 실패");
        }
    }
}
