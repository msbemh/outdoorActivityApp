package com.action.outdooractivityapp.activity;

import android.content.Intent;
import android.graphics.Movie;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.action.outdooractivityapp.AdminApplication;
import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.adapter.RVRoomAdapter;
import com.action.outdooractivityapp.util.Util;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import net.daum.mf.map.api.MapView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TogetherActivity extends AppCompatActivity implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "TogetherActivity";
    private Intent intent;
    private BottomNavigationView navView;
    private ImageView image_plus;
    private List<Map> roomList;

    private RecyclerView recyclerView_room;
    public RVRoomAdapter rvRoomAdapter;
    public LinearLayoutManager layoutManagerRoom;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar_first;

    //페이징 관련
    private boolean isLoading = false;
    private boolean isLastPage = false;
    public static final int PAGE_START = 0; //시작하자마자 1증가시킴
    //1페이지에 몇개의 item이 있을지 정하기
    private static final int PAGE_SIZE = 10;
    private int currentPage = PAGE_START;
    private int totalPage;
    private int itemCount = 0;

    //request 관련 변수
    private final int CREATE_ROOM_REQUEST_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG,"함께해요 onCreate()");

        setContentView(R.layout.activity_together);

        initializeView();

        registerListener();

        createApplyRecyclerview();

        Log.d(TAG, AdminApplication.userMap.toString());

    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.image_plus){
            Log.d(TAG,"플러스 버튼 클릭");
            intent = new Intent(this, RoomCreateActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
            startActivityForResult(intent, CREATE_ROOM_REQUEST_CODE);
        }
    }

    /*데이터 받아오기*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //방생성 결과
        if (requestCode == CREATE_ROOM_REQUEST_CODE && resultCode == RESULT_OK) {
            Map map = new HashMap();
            int room_no = Integer.parseInt(data.getStringExtra("room_no"));
            map.put("room_no", room_no);
            map.put("password",data.getStringExtra("password"));
            map.put("writer",data.getStringExtra("writer"));
            map.put("title",data.getStringExtra("title"));
            rvRoomAdapter.addItem(map);
            recyclerView_room.smoothScrollToPosition(0);

            //방으로 이동
            intent = new Intent(this, RoomChatActivity.class);
            intent.putExtra("room_no", room_no+"");

            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //연속으로 2번 눌러도 activity가 2개 생성되지 않도록 하기위해서 사용.
            startActivity(intent);
        }
    }

    void initializeView(){
        navView = findViewById(R.id.nav_view);
        image_plus = findViewById(R.id.image_plus);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        progressBar_first = findViewById(R.id.progressBar_first);
    }

    void registerListener(){
        navView.setOnNavigationItemSelectedListener(OnNavigationItemSelectedListener);
        image_plus.setOnClickListener(this);
        swipeRefreshLayout.setOnRefreshListener(this);
    }

    void createApplyRecyclerview(){
        /*리사이클러뷰 생성*/
        recyclerView_room = findViewById(R.id.recyclerView_room);
        recyclerView_room.setHasFixedSize(true);

        /*리사이클러뷰 레이아웃 생성 및 적용*/
        layoutManagerRoom = new LinearLayoutManager(this);
        layoutManagerRoom.setOrientation(LinearLayoutManager.VERTICAL);

        recyclerView_room.setLayoutManager(layoutManagerRoom);

        //처음엔 영화리스트없애고 [처음로딩중] 모양 보여주기
        progressBar_first.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setVisibility(View.GONE);

        //데이터베이스에서 방 SELECT하기
        String url = "https://wowoutdoor.tk/room/room_select_query.php";
        String method = "GET";

        //데이터 베이스에서 정보를 가져옴
        roomList = Util.httpConn(url, null, method);

        //총 페이지개수 계산
        totalPage = roomList.size() / PAGE_SIZE;
        if(roomList.size() % PAGE_SIZE > 0){
            totalPage += 1;
        }

        /*리사이클러뷰에 adapter적용*/
        rvRoomAdapter = new RVRoomAdapter(this, new ArrayList<Map>(), R.layout.row_recyclerview_room, R.layout.row_recyclerview_loading);
        recyclerView_room.setAdapter(rvRoomAdapter);

        /* 리사이클러뷰 스크롤 리스너 등록 */
        recyclerView_room.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //화면에 보이는 마지막 item 위치
                int lastVisibleItemPosition = layoutManagerRoom.findLastVisibleItemPosition();

                //로딩된 item 총 갯수
                int itemTotalCount = rvRoomAdapter.getItemCount() - 1;

                if(lastVisibleItemPosition == itemTotalCount){
                    Log.d(TAG,"마지막위치");
                    //로딩중이지 않고, 마지막페이지도 아닐때
                    if (!isLoading && !isLastPage) {
                        loadMoreItem();
                    }
                }

            }
        });

        loadMoreItem();

    }

    void loadMoreItem(){
        isLoading = true;
        //현재 어느 페이지까지 왔는지 계산.
        currentPage++;
        //데이터 세팅
        getItem();
    }

    void getItem(){
        new Handler().postDelayed(new Runnable() {
            final List<Map> items = new ArrayList<>();
            @Override
            public void run() {
                //현재 페이지에 맞는 개수만큼 방 가져오기
                //1페이지당 10개(부족하면 부족한만큼만 가져온다)
                if(roomList.size() > itemCount){
                    int criterion = itemCount; //기준
                    for (int i = criterion; i < criterion+10; i++) {
                        itemCount++;
                        items.add(roomList.get(i));
                        if(roomList.size() <= itemCount){
                            break;
                        }
                    }
                }
                //다음 페이지로 넘어갈때 로딩표시 없애기
                if (currentPage != PAGE_START) rvRoomAdapter.removeLoading();
                Log.d(TAG,"items:"+items);
                rvRoomAdapter.addItems(items);

                //마지막페이지가 아니라면
                if (currentPage < totalPage) {
                    //로딩표시 추가하기
                    rvRoomAdapter.addLoading();
                } else {
                    //마지막페이지라고 인식
                    isLastPage = true;
                }
                //로딩중 초기화 인식
                isLoading = false;

                //로딩이 완료되면 [처음로딩중] 모양 없애고 방 리스트 보여주기
                swipeRefreshLayout.setRefreshing(false);
                progressBar_first.setVisibility(View.GONE);
                swipeRefreshLayout.setVisibility(View.VISIBLE);
            }
        }, 1500);
    }

    /*하단 네비게이션바 Listener*/
    private BottomNavigationView.OnNavigationItemSelectedListener OnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()){
                /*홈 선택*/
                case R.id.navigation_home:
                    intent = new Intent(TogetherActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                    startActivity(intent);
                    return true;
                /*트래킹 게시판 선택*/
                case R.id.navigation_tracking_board:
                    intent = new Intent(TogetherActivity.this, TrackingBoardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                    startActivity(intent);
                    return true;
                /*함께하기 선택*/
                case R.id.navigation_together:
                    intent = new Intent(TogetherActivity.this, TogetherActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                    startActivity(intent);
                    return true;
                /*마이페이지이 선택*/
                case R.id.navigation_my:
                    intent = new Intent(TogetherActivity.this, MyPageActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                    startActivity(intent);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"함께해요 onResume()");

        /*하단 네비게이션 checked표시*/
        navView.getMenu().getItem(2).setChecked(true);


    }

    //swipeRefreshLayout의 refresh
    @Override
    public void onRefresh() {
        roomList.clear();

        //데이터베이스에서 방 SELECT하기
        String url = "https://wowoutdoor.tk/room/room_select_query.php";
        String method = "GET";

        //데이터 베이스에서 정보를 가져옴
        List<Map> tempList = Util.httpConn(url, null, method);
        Log.d(TAG,"roomList:"+roomList);
        roomList.addAll(tempList);

        //페이징관련 값들 초기화
        itemCount = 0;
        currentPage = PAGE_START;
        isLastPage = false;
        rvRoomAdapter.clear();

        loadMoreItem();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"함께하기 onDestroy()");
        Util.toastText(this, "함께하기 onDestroy()");
    }
}
