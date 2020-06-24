package com.action.outdooractivityapp.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.action.outdooractivityapp.AdminApplication;
import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.util.Util;

import net.daum.mf.map.api.CameraUpdateFactory;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPointBounds;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapView;

import java.util.List;
import java.util.Map;

public class TrackingBoardViewActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "TrackingViewActivity";
    private Intent intent;
    private Bundle extras;
    private int tracking_no;
    private String writer;

    private List<Map> trackingList;
    private Map trackingInfo;
    private ImageView image_back;
    private ImageView image_delete;
    private ImageView image_check;

    public MapView mapView;
    private ViewGroup mapViewContainer;

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

        initializeView();

        registerListener();

        //--------해당번호의 트래킹 게시판 정보 가져오기---------
        String url = "https://wowoutdoor.tk/tracking/tracking_select_info.php";
        String parameters = "tracking_no="+tracking_no;
        String method = "GET";

        //데이터 베이스에서 정보를 가져옴
        trackingList = Util.httpConn(url, parameters, method);
        //-------------------------------------
        //지도에 선긋기
        createPolyline();

        //작성자만 삭제와 수정 가능하게 하기
        if(writer.equals(AdminApplication.userMap.get("user_id").toString())){
            image_delete.setVisibility(View.VISIBLE);
            image_check.setVisibility(View.VISIBLE);
        }else{
            image_delete.setVisibility(View.GONE);
            image_check.setVisibility(View.GONE);
        }

    }

    void initializeView(){
        image_back = findViewById(R.id.image_back);
        image_delete = findViewById(R.id.image_delete);
        image_check = findViewById(R.id.image_check);
        //카카오지도
        mapView = new MapView(this);
        mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);
    }

    void registerListener(){
        image_back.setOnClickListener(this);
        image_delete.setOnClickListener(this);
        image_check.setOnClickListener(this);
    }

    void createPolyline() {
        MapPolyline polyline = new MapPolyline();
        polyline.setTag(1000);
        polyline.setLineColor(Color.argb(128, 255, 51, 0)); // Polyline 컬러 지정.

        // Polyline 좌표 지정.
        for(Map map : trackingList){
            double latitude = Double.parseDouble(map.get("latitude").toString());
            double longitude = Double.parseDouble(map.get("longitude").toString());
            polyline.addPoint(MapPoint.mapPointWithGeoCoord(latitude, longitude));
        }

        //시작위치 넣기
        startMarker(trackingList.get(0));

        //도착위치 넣기
        int size = trackingList.size();
        endMarker(trackingList.get(size-1));

        // Polyline 지도에 올리기.
        mapView.addPolyline(polyline);

        //지도 화면에 추가된 모든 Polyline들이 화면에 나타나도록 지도 화면 중심과 확대/축소 레벨을 자동을 조정한다.
        MapPointBounds mapPointBounds = new MapPointBounds(polyline.getMapPoints());
        int padding = 100;
        mapView.moveCamera(CameraUpdateFactory.newMapPointBounds(mapPointBounds, padding));
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
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"트래킹 Board 뷰 onDestroy()");
        //카카오지도 view 삭제
        mapViewContainer.removeView(mapView);
    }

    void startMarker(Map map){
        MapPOIItem customMarker = new MapPOIItem();
        customMarker.setItemName("시작위치");
        customMarker.setTag(1);
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
        customMarker.setTag(2);
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
}
