package com.action.outdooractivityapp.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.action.outdooractivityapp.AdminApplication;
import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.adapter.CustomCalloutBalloonViewAdapter;
import com.action.outdooractivityapp.adapter.RVTrackingBoardAdapter;
import com.action.outdooractivityapp.adapter.RVTrackingPhotoListAdapter;
import com.action.outdooractivityapp.urlConnection.BringImageFile;
import com.action.outdooractivityapp.util.Util;

import net.daum.mf.map.api.CameraUpdateFactory;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPointBounds;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapReverseGeoCoder;
import net.daum.mf.map.api.MapView;

import java.util.List;
import java.util.Map;

public class TrackingBoardViewActivity extends AppCompatActivity implements View.OnClickListener, MapReverseGeoCoder.ReverseGeoCodingResultListener {

    private static final String TAG = "TrackingViewActivity";
    private Intent intent;
    private Bundle extras;
    private int tracking_no;
    private String writer;

    private List<Map> trackingLocationList;
    private List<Map> trackingPhotoList;
    private List<Map> trackingList;
    private Map trackingInfo;
    private ImageView image_back;
    private ImageView image_delete;
    private ImageView image_check;
    private TextView textView_title;
    private TextView textView_start_location;
    private TextView textView_start_time;
    private TextView textView_distance;
    private TextView textView_move_time;
    private TextView textView_average_speed;
    private TextView textView_difficult;
    private ImageView image_profile;
    private TextView textView_user_id;
    private TextView textView_nick_name;

    public MapView mapView;
    private ViewGroup mapViewContainer;

    private RecyclerView recyclerView_photo_list;
    public LinearLayoutManager layoutManagerTrackingPhotoList;
    public RVTrackingPhotoListAdapter rvTrackingPhotoListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking_board_view);

        //트래킹 게시판 번호 받기
        /*data 받아오기*/
        extras = getIntent().getExtras();
        tracking_no = extras.getInt("tracking_no", -1);
        writer = extras.getString("user_id");
        Log.d(TAG, "tracking_no:"+tracking_no);
        Log.d(TAG, "writer:"+writer);


        //--------해당번호의 트래킹 사진 정보 가져오기---------
        String url = "https://wowoutdoor.tk/tracking/tracking_photo_select_query.php";
        String parameters = "tracking_no="+tracking_no;
        String method = "GET";

        //데이터 베이스에서 트래킹 사진 정보를 가져옴
        trackingPhotoList = Util.httpConn(url, parameters, method);
        //-------------------------------------

        //--------해당번호의 트래킹 위치 정보 가져오기---------
        url = "https://wowoutdoor.tk/tracking/tracking_select_info.php";
        parameters = "tracking_no="+tracking_no;
        method = "GET";

        //데이터 베이스에서 트래킹위치 정보를 가져옴
        trackingLocationList = Util.httpConn(url, parameters, method);
        //-------------------------------------

        //데이터 확인
        Util.checkLogListMap(TAG, trackingPhotoList);

        initializeView();

        registerListener();

        //지도에 선긋기
        createPolyline();

        //사진들 마커 표시하기
        createPhotoMarker();


        //작성자만 삭제와 수정 가능하게 하기
        if(writer.equals(AdminApplication.userMap.get("user_id").toString())){
            image_delete.setVisibility(View.VISIBLE);
            image_check.setVisibility(View.VISIBLE);
        }else{
            image_delete.setVisibility(View.GONE);
            image_check.setVisibility(View.GONE);
        }

        //해당번호의 트래킹 게시판 정보 가져오기
        settingInfo();

        createApplyRecyclerview();

    }

    void initializeView(){
        image_back = findViewById(R.id.image_back);
        image_delete = findViewById(R.id.image_delete);
        image_check = findViewById(R.id.image_check);
        textView_title = findViewById(R.id.textView_title);
        textView_start_location = findViewById(R.id.textView_start_location);
        textView_start_time = findViewById(R.id.textView_start_time);
        textView_distance = findViewById(R.id.textView_distance);
        textView_move_time = findViewById(R.id.textView_move_time);
        textView_average_speed = findViewById(R.id.textView_average_speed);
        textView_difficult = findViewById(R.id.textView_difficult);
        image_profile = findViewById(R.id.image_profile);
        textView_user_id = findViewById(R.id.textView_user_id);
        textView_nick_name = findViewById(R.id.textView_nick_name);
        //카카오지도
        mapView = new MapView(this);
        mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);
        //커스텀 말풍선 적용
        mapView.setCalloutBalloonAdapter(new CustomCalloutBalloonViewAdapter(this, trackingPhotoList));

    }

    void registerListener(){
        image_back.setOnClickListener(this);
        image_delete.setOnClickListener(this);
        image_check.setOnClickListener(this);
    }

    void createApplyRecyclerview(){
        /*리사이클러뷰 생성*/
        recyclerView_photo_list = findViewById(R.id.recyclerView_photo_list);
        recyclerView_photo_list.setHasFixedSize(true);

        /*리사이클러뷰 레이아웃 생성 및 적용*/
        layoutManagerTrackingPhotoList = new LinearLayoutManager(this);
        layoutManagerTrackingPhotoList.setOrientation(LinearLayoutManager.HORIZONTAL);

        recyclerView_photo_list.setLayoutManager(layoutManagerTrackingPhotoList);

        /*리사이클러뷰에 adapter적용*/
        rvTrackingPhotoListAdapter = new RVTrackingPhotoListAdapter(this, trackingPhotoList, R.layout.row_recyclerview_tracking_photo_list, mapView);
        Util.checkLogListMap(TAG, trackingPhotoList);
        recyclerView_photo_list.setAdapter(rvTrackingPhotoListAdapter);
    }

    void createPolyline() {
        MapPolyline polyline = new MapPolyline();
        polyline.setTag(1000);
        polyline.setLineColor(Color.argb(128, 255, 51, 0)); // Polyline 컬러 지정.

        // Polyline 좌표 지정.
        for(Map map : trackingLocationList){
            double latitude = Double.parseDouble(map.get("latitude").toString());
            double longitude = Double.parseDouble(map.get("longitude").toString());
            polyline.addPoint(MapPoint.mapPointWithGeoCoord(latitude, longitude));
        }

        //시작위치 넣기
        startMarker(trackingLocationList.get(0));

        //도착위치 넣기
        int size = trackingLocationList.size();
        endMarker(trackingLocationList.get(size-1));

        // Polyline 지도에 올리기.
        mapView.addPolyline(polyline);

        //지도 화면에 추가된 모든 Polyline들이 화면에 나타나도록 지도 화면 중심과 확대/축소 레벨을 자동을 조정한다.
        MapPointBounds mapPointBounds = new MapPointBounds(polyline.getMapPoints());
        int padding = 100;
        mapView.moveCamera(CameraUpdateFactory.newMapPointBounds(mapPointBounds, padding));
    }

    //사진 마커 표시하기
    void createPhotoMarker(){
        for(Map mapItem : trackingPhotoList){
            double latitude = Double.parseDouble(mapItem.get("latitude").toString());
            double longitude = Double.parseDouble(mapItem.get("longitude").toString());
            int tag = Integer.parseInt(mapItem.get("tag").toString());

            MapPOIItem customMarker = new MapPOIItem();
            customMarker.setItemName("사진");
            customMarker.setTag(tag);
            MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude);
            customMarker.setMapPoint(mapPoint);
            customMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커타입을 커스텀 마커로 지정.
            customMarker.setCustomImageResourceId(R.drawable.icon_camera_marker); // 마커 이미지.
            customMarker.setCustomImageAutoscale(false); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
            customMarker.setCustomImageAnchor(0.5f, 1.0f); // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정 - 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.

            mapView.addPOIItem(customMarker);

        }
    }


    @Override
    public void onClick(View v) {
        //뒤로가기 클릭
        if(v.getId() == R.id.image_back){
            finish();
            //삭제 클릭
        }else if(v.getId() == R.id.image_delete){
            Log.d(TAG,"삭제클릭");
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setIcon(R.drawable.icon_warning);
            dialog.setTitle("삭제");
            dialog.setMessage("해당 글을 삭제하시겠습니까?");
            dialog.setPositiveButton("아니오",null);
            dialog.setNegativeButton("예",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //안드로이드 => http => 데이터베이스 에서 정보를 가져오기 위해서
                            //url, paramters, method정보가 필요함.
                            String url = "https://wowoutdoor.tk/tracking/tracking_delete_query.php";
                            String parameters = "tracking_no="+tracking_no;
                            String method = "POST";
                            Log.d(TAG,"parameters:"+parameters);

                            //데이터 베이스에서 정보를 가져옴
                            List<Map> resultList = Util.httpConn(url, parameters, method);
                            //result : true  => 트래킹글 삭제 성공
                            //result : false => 트래킹글 삭제 실패
                            boolean result = Boolean.parseBoolean(resultList.get(0).get("result").toString());

                            if(result){
                                Util.toastText(TrackingBoardViewActivity.this,"삭제가 완료됐습니다.");
                            }else{
                                Util.toastText(TrackingBoardViewActivity.this,"삭제에 실패했습니다.");
                            }
                            finish();
                        }
                    });
            dialog.show();
            //체크(수정) 클릭
        }else if(v.getId() == R.id.image_check){

        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"트래킹 Board 뷰 onDestroy()");
        //카카오지도 view 삭제
        mapViewContainer.removeView(mapView);
    }

    void startMarker(Map map){
        MapPOIItem customMarker = new MapPOIItem();
        customMarker.setItemName("시작위치");
        customMarker.setTag(0);
        MapPoint mapPoint = convertMapToMapPoint(map);
        customMarker.setMapPoint(mapPoint);
        customMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커타입을 커스텀 마커로 지정.
        customMarker.setCustomImageResourceId(R.drawable.icon_tracking_start); // 마커 이미지.
        customMarker.setCustomImageAutoscale(false); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
        customMarker.setCustomImageAnchor(0.5f, 1.0f); // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정 - 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.

        mapView.addPOIItem(customMarker);
    }

    void endMarker(Map map){
        MapPOIItem customMarker = new MapPOIItem();
        customMarker.setItemName("도착위치");
        customMarker.setTag(0);
        MapPoint mapPoint = convertMapToMapPoint(map);
        customMarker.setMapPoint(mapPoint);
        customMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커타입을 커스텀 마커로 지정.
        customMarker.setCustomImageResourceId(R.drawable.icon_tracking_end); // 마커 이미지.
        customMarker.setCustomImageAutoscale(false); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
        customMarker.setCustomImageAnchor(0.5f, 1.0f); // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정 - 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.

        mapView.addPOIItem(customMarker);
    }

    MapPoint convertMapToMapPoint(Map map){
        double latitude = Double.parseDouble(map.get("latitude").toString());
        double longitude = Double.parseDouble(map.get("longitude").toString());
        MapPoint mapPoint =  MapPoint.mapPointWithGeoCoord(latitude, longitude);
        return mapPoint;
    }

    //해당번호의 트래킹 게시판 정보 가져오기
    void settingInfo(){
        //--------해당번호의 트래킹 게시판 정보 가져오기---------
        String url = "https://wowoutdoor.tk/tracking/tracking_select_query.php";
        String parameters = "tracking_no="+tracking_no;
        String method = "GET";
        Log.d(TAG,"url:"+url+"?"+parameters);
        //데이터 베이스에서 트래킹위치 정보를 가져옴
        trackingList = Util.httpConn(url, parameters, method);

        //데이터 보관
        String title = trackingList.get(0).get("title").toString();
        MapPoint startMapPoint = convertMapToMapPoint(trackingLocationList.get(0));
        int size = trackingLocationList.size();
        MapPoint endMapPoint = convertMapToMapPoint(trackingLocationList.get(size-1));
        String startDate = trackingList.get(0).get("trans_start_date").toString();
        String endDate = trackingList.get(0).get("trans_end_date").toString();
        double distance = Double.parseDouble(trackingList.get(0).get("trans_distance").toString());
        double speed = Double.parseDouble(trackingList.get(0).get("speed").toString());
        int takenSecond = Integer.parseInt(trackingList.get(0).get("taken_second").toString());
        String difficult = trackingList.get(0).get("difficult").toString();
        difficult = convertEnglishToHangul(difficult);
        String profileImage = trackingList.get(0).get("profile_image").toString();
        String writer = trackingList.get(0).get("user_id").toString();
        String nickName = trackingList.get(0).get("nick_name").toString();

        //걸린초 => 00:00:00 형식으로 변형
        String time = convertSecondToTime(takenSecond);

        //시작위치 역 Geo코딩
        MapReverseGeoCoder reverseGeoCoder = new MapReverseGeoCoder(getString(R.string.kakao_map_key), startMapPoint, this, this);
        reverseGeoCoder.startFindingAddress();

        //------이미지 파일 서버에서 Bitmap으로 가져오기-------
        Log.d(TAG,"profileImage:"+profileImage);
        if(!"null".equals(profileImage) && !TextUtils.isEmpty(profileImage) && profileImage != null){
            BringImageFile bringImageFile = new BringImageFile(profileImage);

            bringImageFile.start();

            try{
                bringImageFile.join();
                //이미지 불러오기 완료되면 가져오기
                Bitmap bitmap = bringImageFile.getBitmap();
                image_profile.setImageBitmap(bitmap);
            }
            catch(InterruptedException e){
                e.printStackTrace();
            }
        }else{
            //기본 프로필 이미지 보여주기
            image_profile.setImageResource(R.drawable.icon_profile_invert);
        }
        //----------------------------------------------------

        //------------------데이터 세팅-------------------------
        textView_title.setText(title);
        textView_start_time.setText(startDate);
        textView_distance.setText(distance+"km");
        textView_move_time.setText(time);
        textView_average_speed.setText(speed+"km/h");
        textView_difficult.setText(difficult);
        textView_user_id.setText(writer);
        textView_nick_name.setText(nickName);
        //-------------------------------------------------------
    }

    //걸린초 => 00:00:00 형식으로 변형
    String convertSecondToTime(int taken_second){
        int hour = taken_second/3600;
        int minute = (taken_second%3600)/60;
        int second = taken_second%60;

        String hourString = "";
        String minuteString = "";
        String secondString = "";

        if(hour<10){
            hourString = "0"+hour;
        }else{
            hourString = hour+"";
        }
        if(minute<10){
            minuteString = "0"+minute;
        }else{
            minuteString = minute+"";
        }
        if(second<10){
            secondString = "0"+second;
        }else{
            secondString = second+"";
        }

        String time = hourString+":"+minuteString+":"+secondString;
        return time;
    }

    @Override
    public void onReverseGeoCoderFoundAddress(MapReverseGeoCoder mapReverseGeoCoder, String s) {
        // 주소를 찾은 경우.
        textView_start_location.setText(s);
    }

    @Override
    public void onReverseGeoCoderFailedToFindAddress(MapReverseGeoCoder mapReverseGeoCoder) {
        // 호출에 실패한 경우.
    }

    String convertEnglishToHangul(String difficult){
        if("easy".equals(difficult)){
            return "쉬움";
        }else if("usual".equals(difficult)){
            return "보통";
        }else if("hard".equals(difficult)){
            return "어려움";
        }
        return "없음";
    }
}
