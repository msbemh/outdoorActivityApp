package com.action.outdooractivityapp.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.util.Util;

import net.daum.mf.map.api.CameraUpdateFactory;
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

    private List<Map> trackingList;
    private Map trackingInfo;
    private ImageView image_back;

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
        Log.d(TAG, "tracking_no:"+tracking_no);

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

    }

    void initializeView(){
        image_back = findViewById(R.id.image_back);
        //카카오지도
        //카카오지도 view 삭제
        mapView = new MapView(this);
        mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);
    }

    void registerListener(){
        image_back.setOnClickListener(this);
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
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"트래킹 Board 뷰 onDestroy()");
        //카카오지도 view 삭제
        mapViewContainer.removeView(mapView);

    }
}
