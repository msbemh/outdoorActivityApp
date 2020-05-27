package com.action.outdooractivityapp.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.util.Util;

import java.util.List;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {

    private Intent intent;
    private EditText idEidt;
    private EditText pwEidt;
    private EditText pwReEidt;
    private TextView idCheckText;
    private EditText nameEdit;
    private Button button_sign_up;
    private Button button_cancel;

    private static final String TAG = "SignUpActivity";

    /*ID중복 체크*/
    boolean idCheck = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        //view초기화
        initializeView();

        //리스너 등록
        registerListener();

        /*아이디 중복체크를 focus없어질때 하기위해서*/
        idEidt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
            if (!hasFocus) {
                /*아이디 중복 체크*/
                if(checkId()){
                    idCheckText.setVisibility(View.VISIBLE);
                    idCheckText.setText("사용 가능한 아이디입니다.");
                    idCheckText.setTextColor(getResources().getColor(R.color.colorGreen));
                    idCheck = true;
                }else{
                    idCheckText.setVisibility(View.VISIBLE);
                    idCheckText.setText("사용 불가능한 아이디입니다.");
                    idCheckText.setTextColor(Color.RED);
                    idCheck = false;
                }
            }
            }
        });
    }

    void initializeView(){
        button_sign_up = findViewById(R.id.button_sign_up);
        button_cancel = findViewById(R.id.button_cancel);
        idEidt = findViewById(R.id.input_id_sign_up);
        pwEidt = findViewById(R.id.input_pw_sign_up);
        pwReEidt = findViewById(R.id.input_re_pw_sign_up);
        idCheckText = findViewById(R.id.id_check_result);
        nameEdit = findViewById(R.id.input_name_sign_up);
    }

    void registerListener(){
        button_sign_up.setOnClickListener(this);
        button_cancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        /*회원가입 시도*/
        if(v.getId() == R.id.button_sign_up){
            checkSignUp();
        /*취소*/
        }else if(v.getId() == R.id.button_cancel){
            finish();
        }
    }

    void checkSignUp(){
        String idString = idEidt.getText().toString();
        String nameString = nameEdit.getText().toString();
        String pwString = pwEidt.getText().toString();
        String pwReString = pwReEidt.getText().toString();

        if(TextUtils.isEmpty(idString) || TextUtils.isEmpty(pwString) || TextUtils.isEmpty(pwReString)){
            Toast.makeText(this,"아이디, 패스워드, 패스워드 재입력 모두 입력 해주세요.",Toast.LENGTH_SHORT).show();
        }else if(!idCheck){
            Toast.makeText(this,"아이디가 중복됐습니다.",Toast.LENGTH_SHORT).show();
        }else if(!pwString.equals(pwReString)){
            Toast.makeText(this,"패스워드가 일치하지 않습니다.",Toast.LENGTH_SHORT).show();
        }else{
            //회원가입 성공
            if(sign_up()){
                Toast.makeText(this,"회원가입이 완료됐습니다.",Toast.LENGTH_SHORT).show();
                finish();
            //회원가입 실패
            }else{
                Toast.makeText(this,"회원가입에 실패했습니다.",Toast.LENGTH_SHORT).show();
            }


        }
    }

    boolean checkId(){
        String idString = idEidt.getText().toString();

        //안드로이드 => http => 데이터베이스 에서 정보를 가져오기 위해서
        //url, paramters, method정보가 필요함.
        String url = "https://wowoutdoor.tk/user/sign_up_overlap_check.php";
        String parameters = "user_id="+idString;
        String method = "GET";

        if(TextUtils.isEmpty(idString)){
            Toast.makeText(this,"아이디를 입력해주세요.",Toast.LENGTH_SHORT).show();
            return false;
        }

        //데이터 베이스에서 정보를 가져옴
        List<Map> resultList = Util.httpConn(url, parameters, method);
        //cnt : 0  => 작성한 id와 일치하는 정보가 없음.
        //cnt : 1  => 작성한 id와 일치하는 정보가 있음.
        int cnt = Integer.parseInt(resultList.get(0).get("cnt").toString());

        //ID 사용 가능
        if(cnt == 0){
            return true;
        //ID 사용 불가능
        }else{
            return false;
        }
    }

    boolean sign_up(){
        String idString = idEidt.getText().toString();
        String nameString = nameEdit.getText().toString();
        String pwString = pwEidt.getText().toString();

        //안드로이드 => http => 데이터베이스 에서 정보를 가져오기 위해서
        //url, paramters, method정보가 필요함.
        String url = "https://wowoutdoor.tk/user/sign_up_query.php";
        String parameters = "user_id="+idString+"&nick_name="+nameString+"&user_password="+pwString;
        String method = "POST";

        if(TextUtils.isEmpty(idString)){
            Toast.makeText(this,"아이디를 입력해주세요.",Toast.LENGTH_SHORT).show();
            return false;
        }

        //데이터 베이스에서 정보를 가져옴
        List<Map> resultList = Util.httpConn(url, parameters, method);
        //result : true  => 회원가입 성공
        //result : false => 회원가입 실패
        boolean result = Boolean.parseBoolean(resultList.get(0).get("result").toString());

        //회원가입 성공
        if(result){
            return true;
        //회원가입 실패
        }else{
            return false;
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("song","회원가입 onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("song","회원가입 onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("song","회원가입 onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("song","회원가입 onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("song","회원가입 onDestroy()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("song","회원가입 onRestart()");
    }
}
