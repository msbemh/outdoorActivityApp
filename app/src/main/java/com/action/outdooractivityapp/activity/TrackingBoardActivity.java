package com.action.outdooractivityapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.adapter.RVTrackingBoardAdapter;
import com.action.outdooractivityapp.util.Util;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;
import java.util.Map;

public class TrackingBoardActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "TrackingBoardActivity";
    private Intent intent;
    private BottomNavigationView navView;

    private RecyclerView recyclerView_tracking_board;
    public LinearLayoutManager layoutManagerTrackingBoard;
    public RVTrackingBoardAdapter rvTrackingBoardAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    private List<Map> trackingList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking_board);

        initializeView();

        registerListener();

        createApplyRecyclerview();
    }

    void initializeView(){
        navView = findViewById(R.id.nav_view);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    void registerListener(){
        navView.setOnNavigationItemSelectedListener(OnNavigationItemSelectedListener);
        swipeRefreshLayout.setOnRefreshListener(this);
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
        String method = "GET";

        //데이터 베이스에서 정보를 가져옴
        trackingList = Util.httpConn(url, null, method);

        /*리사이클러뷰에 adapter적용*/
        rvTrackingBoardAdapter = new RVTrackingBoardAdapter(this, trackingList, R.layout.row_recyclerview_tracking_board);
        recyclerView_tracking_board.setAdapter(rvTrackingBoardAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"트래킹게시판 onResume()");
        /*하단 네비게이션 checked표시*/
        navView.getMenu().getItem(1).setChecked(true);
    }

    /*하단 네비게이션바 Listener*/
    private BottomNavigationView.OnNavigationItemSelectedListener OnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()){
                /*홈 선택*/
                case R.id.navigation_home:
                    intent = new Intent(TrackingBoardActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                    startActivity(intent);
                    return true;
                /*트래킹 게시판 선택*/
                case R.id.navigation_tracking_board:
                    intent = new Intent(TrackingBoardActivity.this, TrackingBoardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                    startActivity(intent);
                    return true;
                /*함께하기 선택*/
                case R.id.navigation_together:
                    intent = new Intent(TrackingBoardActivity.this, TogetherActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                    startActivity(intent);
                    return true;
                /*마이페이지이 선택*/
                case R.id.navigation_my:
                    intent = new Intent(TrackingBoardActivity.this, MyPageActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                    startActivity(intent);
                    return true;
            }
            return false;
        }
    };

    @Override
    public void onRefresh() {
        trackingList.clear();

        //DB에서 트래킹 게시판 리스트 가져오기
        String url = "https://wowoutdoor.tk/tracking/tracking_select_query.php";
        String method = "GET";

        //데이터 베이스에서 정보를 가져옴
        List<Map> tempList = Util.httpConn(url, null, method);
        trackingList.addAll(tempList);
        //리사이클러뷰 notify
        rvTrackingBoardAdapter.notifyDataSetChanged();

        //리프레쉬 모양 없애기
        swipeRefreshLayout.setRefreshing(false);
    }
}
