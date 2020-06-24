package com.action.outdooractivityapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.action.outdooractivityapp.AdminApplication;
import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.adapter.RVTrackingBoardAdapter;
import com.action.outdooractivityapp.util.Util;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;
import java.util.Map;

public class TrackingBoardPrivateActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener {

    private static final String TAG = "TrackingBoardPrivate";
    private Intent intent;
    private BottomNavigationView navView;

    private RecyclerView recyclerView_tracking_board;
    public LinearLayoutManager layoutManagerTrackingBoard;
    public RVTrackingBoardAdapter rvTrackingBoardAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    private List<Map> trackingList;

    private ImageView image_back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking_private_board);

        initializeView();

        registerListener();

        createApplyRecyclerview();
    }

    void initializeView(){
        navView = findViewById(R.id.nav_view);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        image_back = findViewById(R.id.image_back);
    }

    void registerListener(){
        swipeRefreshLayout.setOnRefreshListener(this);
        image_back.setOnClickListener(this);
    }

    void createApplyRecyclerview(){
        /*리사이클러뷰 생성*/
        recyclerView_tracking_board = findViewById(R.id.recyclerView_tracking_board);
        recyclerView_tracking_board.setHasFixedSize(true);

        /*리사이클러뷰 레이아웃 생성 및 적용*/
        layoutManagerTrackingBoard = new LinearLayoutManager(this);
        layoutManagerTrackingBoard.setOrientation(LinearLayoutManager.VERTICAL);

        recyclerView_tracking_board.setLayoutManager(layoutManagerTrackingBoard);

        //DB에서 트래킹 게시판 리스트 가져오기
        String url = "https://wowoutdoor.tk/tracking/tracking_select_query.php";
        String parameters = "is_public=false"+"&user_id="+AdminApplication.userMap.get("user_id");
        Log.d(TAG,"url:"+url+"?"+parameters);
        String method = "GET";

        //데이터 베이스에서 정보를 가져옴
        trackingList = Util.httpConn(url, parameters, method);

        /*리사이클러뷰에 adapter적용*/
        rvTrackingBoardAdapter = new RVTrackingBoardAdapter(this, trackingList, R.layout.row_recyclerview_tracking_board);
        recyclerView_tracking_board.setAdapter(rvTrackingBoardAdapter);
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        trackingList.clear();

        //DB에서 트래킹 게시판 리스트 가져오기
        String url = "https://wowoutdoor.tk/tracking/tracking_select_query.php";
        String parameters = "is_public=false"+"&user_id="+ AdminApplication.userMap.get("user_id");;
        String method = "GET";

        //데이터 베이스에서 정보를 가져옴
        List<Map> tempList = Util.httpConn(url, parameters, method);
        trackingList.addAll(tempList);
        //리사이클러뷰 notify
        rvTrackingBoardAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"트래킹 개인 게시판 onResume()");
    }

    @Override
    public void onRefresh() {
        trackingList.clear();

        //DB에서 트래킹 게시판 리스트 가져오기
        String url = "https://wowoutdoor.tk/tracking/tracking_select_query.php";
        String parameters = "is_public=false"+"&user_id="+ AdminApplication.userMap.get("user_id");
        String method = "GET";

        //데이터 베이스에서 정보를 가져옴
        List<Map> tempList = Util.httpConn(url, parameters, method);
        trackingList.addAll(tempList);
        //리사이클러뷰 notify
        rvTrackingBoardAdapter.notifyDataSetChanged();

        //리프레쉬 모양 없애기
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onClick(View v) {
        //뒤로가기 클릭
        if(v.getId() == R.id.image_back){
            finish();
        }
    }
}
