package com.action.outdooractivityapp.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.action.outdooractivityapp.AdminApplication;
import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.urlConnection.BringImageFile;
import com.action.outdooractivityapp.util.Util;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class MyPageActivity extends AppCompatActivity implements View.OnClickListener {

    private Intent intent;
    private BottomNavigationView navView;

    private TextView text_user_name;

    private ImageView image_profile;
    private Button button_modify_profile;
    private Button button_logout;
    private FrameLayout frameLayout_my_tracking;
    private TextView text_my_tracking_count;

    private static final String TAG = "MyPage";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"마이페이지 onCreate()");
        setContentView(R.layout.activity_my_page);

        //view초기화
        initializeView();
        //리스너 등록
        registerListener();
    }

    @Override
    public void onClick(View v) {
        //프로필 수정 버튼 클릭
        if(v.getId() == R.id.button_modify_profile){
            intent = new Intent(this, ModifyProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //연속으로 2번 눌러도 activity가 2개 생성되지 않도록 하기위해서 사용.
            startActivity(intent);
        //로그아웃 버튼 클릭
        }else if(v.getId() == R.id.button_logout){
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setIcon(R.drawable.icon_warning);
            dialog.setTitle("로그아웃");
            dialog.setMessage("로그아웃 하시겠습니까?");
            dialog.setPositiveButton("아니오",null);
            dialog.setNegativeButton("예",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            intent = new Intent(MyPageActivity.this,LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); //현재 TASK 비우기, 새로운 TASK만들기
                            //local에서 유저 정보 삭제
                            AdminApplication.userMap.clear();

                            //SharedPreferences에서도 유저 정보 삭제
                            SharedPreferences pref = getSharedPreferences("myApp", MODE_PRIVATE);
                            SharedPreferences.Editor editor = pref.edit();
                            editor.remove("userList");
                            editor.commit();

                            Log.d(TAG,"[로그아웃 확인]"+AdminApplication.userMap);
                            startActivity(intent);
                            finish();
                        }
                    });
            dialog.show();
        //나의 트래킹 리스트 클릭
        }else if(v.getId() == R.id.frameLayout_my_tracking){
            intent = new Intent(this, TrackingBoardPrivateActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //연속으로 2번 눌러도 activity가 2개 생성되지 않도록 하기위해서 사용.
            startActivity(intent);
        }
    }

    void initializeView(){
        navView = findViewById(R.id.nav_view);
        text_user_name = findViewById(R.id.text_user_name);
        button_modify_profile = findViewById(R.id.button_modify_profile);
        image_profile = findViewById(R.id.image_profile);
        button_logout = findViewById(R.id.button_logout);
        frameLayout_my_tracking = findViewById(R.id.frameLayout_my_tracking);
        text_my_tracking_count = findViewById(R.id.text_my_tracking_count);
    }

    void registerListener(){
        navView.setOnNavigationItemSelectedListener(OnNavigationItemSelectedListener);
        button_modify_profile.setOnClickListener(this);
        button_logout.setOnClickListener(this);
        frameLayout_my_tracking.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"마이페이지 onResume()");

        /*하단 네비게이션 checked표시*/
        navView.getMenu().getItem(3).setChecked(true);

        //나의 네임 표시
        text_user_name.setText(AdminApplication.userMap.get("nick_name").toString()+"님");

        //프로필 사진 있으면 보여주기
        if(AdminApplication.userMap.get("profile_image") !=null
                && !"null".equals(AdminApplication.userMap.get("profile_image").toString())
                && AdminApplication.profileImage != null){
            image_profile.setImageBitmap(AdminApplication.profileImage);
            //------이미지 파일 서버에서 Bitmap으로 가져오기-------
//            String profileImage = AdminApplication.userMap.get("profile_image").toString();
//            Log.d(TAG,"profileImage:"+profileImage);
//                BringImageFile bringImageFile = new BringImageFile(profileImage);
//                bringImageFile.start();
//                try{
//                    bringImageFile.join();
//                    //이미지 불러오기 완료되면 가져오기
//                    Bitmap bitmap = bringImageFile.getBitmap();
//                    image_profile.setImageBitmap(bitmap);
//                }catch(InterruptedException e){
//                    e.printStackTrace();
//                }
            //----------------------------------------------------
        //없으면 기본 프로필 사진 보여주기
        }else{
            image_profile.setImageResource(R.drawable.icon_profile);
        }

        //------------나의 트래킹 글 개수보여주기----------
        //DB에서 트래킹 게시판 리스트 가져오기
        String url = "https://wowoutdoor.tk/tracking/tracking_select_count_query.php";
        String parameters = "is_public=false"+"&user_id="+ AdminApplication.userMap.get("user_id");;
        String method = "GET";
        Log.d(TAG,"url:"+url+"?"+parameters);
        //데이터 베이스에서 정보를 가져옴
        List<Map> resultList = Util.httpConn(url, parameters, method);
        int count = Integer.parseInt(resultList.get(0).get("cnt").toString());

        text_my_tracking_count.setText(count+"");
        //----------------------------------------------


    }


    /*하단 네비게이션바 Listener*/
    private BottomNavigationView.OnNavigationItemSelectedListener OnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()){
                /*메인 선택*/
                case R.id.navigation_home:
                    intent = new Intent(MyPageActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                    startActivity(intent);
                    return true;
                /*트래킹 게시판 선택*/
                case R.id.navigation_tracking_board:
                    intent = new Intent(MyPageActivity.this, TrackingBoardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                    startActivity(intent);
                    return true;
                /*함께하기 선택*/
                case R.id.navigation_together:
                    intent = new Intent(MyPageActivity.this, TogetherActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                    startActivity(intent);
                    return true;
                /*마이페이지 선택*/
                case R.id.navigation_my:
                    intent = new Intent(MyPageActivity.this, MyPageActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기
                    startActivity(intent);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"마이페이지 onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"마이페이지 onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"마이페이지 onDestroy()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG,"마이페이지 onRestart()");
    }
}
