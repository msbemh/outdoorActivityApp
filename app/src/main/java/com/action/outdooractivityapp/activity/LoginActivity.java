package com.action.outdooractivityapp.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.action.outdooractivityapp.AdminApplication;
import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.urlConnection.BringImageFile;
import com.action.outdooractivityapp.urlConnection.URLConnector;
import com.action.outdooractivityapp.util.Util;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private Intent intent;
    private EditText idEdit;
    private EditText pwEdit;
    private Button button_login;
    private Button button_sign_up;

    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //view초기화
        initializeView();

        //리스너 등록
        registerListener();

        Log.d(TAG,"로그인 onCreate()");
    }

    void initializeView(){
        idEdit = findViewById(R.id.id_input);
        pwEdit = findViewById(R.id.pw_input);
        button_login = findViewById(R.id.button_login);
        button_sign_up = findViewById(R.id.button_sign_up);
    }

    void registerListener(){
        button_login.setOnClickListener(this);
        button_sign_up.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        /*로그인 시도*/
        if(v.getId() == R.id.button_login){
            //로그인 성공
            if(checkLogin(idEdit.getText().toString(), pwEdit.getText().toString())){
                Toast.makeText(this,"로그인 성공",Toast.LENGTH_SHORT).show();
                intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기

                //------이미지 파일 서버에서 Bitmap으로 가져오기-------
                BringImageFile bringImageFile = new BringImageFile(AdminApplication.userMap.get("profile_image").toString());

                bringImageFile.start();

                try{
                    bringImageFile.join();
                    //이미지 불러오기 완료되면 가져오기
                    Bitmap bitmap = bringImageFile.getBitmap();
                    AdminApplication.profileImage = bitmap;
                }
                catch(InterruptedException e){
                    e.printStackTrace();
                }
                //----------------------------------------------------
                Log.d(TAG,"[TEST]확인"+AdminApplication.userMap);
                startActivity(intent);
                finish();
            //로그인 실패
            }else{
                pwEdit.setText("");
                Toast.makeText(this,"아이디와 패스워드를 확인해주세요.",Toast.LENGTH_SHORT).show();
            }
        /*회원가입 시도*/
        }else if(v.getId() == R.id.button_sign_up){
            intent = new Intent(this, SignUpActivity.class);
            startActivity(intent);
        }
    }


    //로그인 체크
    boolean checkLogin(String idString, String pwString){
        //안드로이드 => http => 데이터베이스 에서 정보를 가져오기 위해서
        //url, paramters, method정보가 필요함.
        String url = "https://wowoutdoor.tk/user/user_login_check.php";
        String parameters = "user_id="+idString+"&user_pw="+pwString;
        String method = "POST";

        if(TextUtils.isEmpty(idString) || TextUtils.isEmpty(pwString)){
            Toast.makeText(this,"아이디와 패스워드를 입력해주세요.",Toast.LENGTH_SHORT).show();
            return false;
        }

        //데이터 베이스에서 정보를 가져옴
        List<Map> resultList = Util.httpConn(url, parameters, method);
        //cnt : 0  => 작성한 id,pw와 일치하는 정보가 없음.
        //cnt : 1  => 작성한 id,pw와 일치하는 정보가 있음.
        Log.d(TAG,"resultList.get(0):"+resultList.get(0));
        int cnt = Integer.parseInt(resultList.get(0).get("cnt").toString());

        if(cnt>0){
            //로그인 성공
            AdminApplication.userMap = resultList.get(0);
            //SharedPreferences에 저장
            Util.saveUser(this);
            return true;
        }else{
            //로그인 실패
            return false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG,"로그인 onStart()");

        //SharedPreferences에서 로그인 정보를 가져온다.
        //[이유]한번 로그인하면, 로그아웃전까지 세션유지시키기 위해서.
        Util.bringUser(this);
        Log.d(TAG,"userMap:"+AdminApplication.userMap);

        //다시 로그인 체크
        if(AdminApplication.userMap.size() > 0){
            String userId = AdminApplication.userMap.get("user_id").toString();
            String userPassword = AdminApplication.userMap.get("user_password").toString();
            if(checkLogin(userId, userPassword)){
                intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //재생성 하지않고 해당 activity를 제일 위로 올리기

                //------이미지 파일 서버에서 Bitmap으로 가져오기-------
                BringImageFile bringImageFile = new BringImageFile(AdminApplication.userMap.get("profile_image").toString());

                bringImageFile.start();

                try{
                    bringImageFile.join();
                    //이미지 불러오기 완료되면 가져오기
                    Bitmap bitmap = bringImageFile.getBitmap();
                    AdminApplication.profileImage = bitmap;
                }
                catch(InterruptedException e){
                    e.printStackTrace();
                }
                //----------------------------------------------------
                Log.d(TAG,"[TEST]확인"+AdminApplication.userMap);
                Toast.makeText(this,"이미 로그인한 상태입니다.",Toast.LENGTH_SHORT).show();
                startActivity(intent);
                finish();
                //로그인 실패
            }else{
                pwEdit.setText("");
                Toast.makeText(this,"로그아웃 됐습니다. 다시 로그인해주세요.",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"로그인 onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"로그인 onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"로그인 onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"로그인 onDestroy()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG,"로그인 onRestart()");
    }

}
