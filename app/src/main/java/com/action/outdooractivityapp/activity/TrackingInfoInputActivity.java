package com.action.outdooractivityapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.action.outdooractivityapp.AdminApplication;
import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.util.Util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class TrackingInfoInputActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = "TrackingInfoActivity";
    private Intent intent;
    private EditText editText_title;
    private RadioGroup radioGroup_public_private;
    private ImageView imageView_thumbnail;
    private ImageView image_back;
    private ImageView image_check;

    private String location;
    private Bundle extras;
    private String thumbnail_image_route;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking_info_input);

        initializeView();

        registerListener();

        /*data 받아오기*/
        extras = getIntent().getExtras();
        location = extras.getString("location");
        Log.d(TAG, "location:"+location);
    }

    void initializeView(){
        editText_title = findViewById(R.id.editText_title);
        radioGroup_public_private = findViewById(R.id.radioGroup_public_private);
        imageView_thumbnail = findViewById(R.id.imageView_thumbnail);
        image_back = findViewById(R.id.image_back);
        image_check = findViewById(R.id.image_check);
    }

    void registerListener(){
        editText_title.setOnClickListener(this);
        radioGroup_public_private.setOnClickListener(this);
        imageView_thumbnail.setOnClickListener(this);
        image_back.setOnClickListener(this);
        image_check.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        //뒤로가기 클릭
        if(v.getId() == R.id.image_back) {
            finish();
        //저장하기 클릭
        }else if(v.getId() == R.id.image_check){
            //---------------------------트래킹정보 DB에 저장시키기---------------------------
            //제목
            String title = editText_title.getText().toString();
            //섬네일 이미지 경로
            thumbnail_image_route ="";

            //제목 빈값 체크
            if(TextUtils.isEmpty(title)){
                Util.toastText(this, "제목을 입력해주세요.");
                return;
            }

            //라디오 그룹 선택된 ID가져오기
            int selected_radio_id = radioGroup_public_private.getCheckedRadioButtonId();
            Log.d(TAG,"selected_radio_id:"+selected_radio_id);

            //라디오 그룹 체크했는지 검사 (아무것도 체크하지 않으면 -1임)
            if(selected_radio_id == -1){
                Util.toastText(this, "공개여부를 선택해주세요.");
                return;
            }

            //선택된 라디오 Text에 따라서 is_public(true,false) 정하기
            RadioButton radioButton = findViewById(selected_radio_id);
            boolean is_public = false;
            if("공개".equals(radioButton.getText().toString())){
                is_public = true;
            }else if("비공개".equals(radioButton.getText().toString())){
                is_public = false;
            }

            //안드로이드 => http => 데이터베이스 에서 정보를 가져오기 위해서
            //url, paramters, method정보가 필요함.
            String url = "https://wowoutdoor.tk/tracking/tracking_insert_query.php";
            String parameters = "user_id="+ AdminApplication.userMap.get("user_id")+"&nick_name="+AdminApplication.userMap.get("nick_name")
                    +"&location="+location+"&title="+title+"&is_public="+is_public+"&thumbnail_image_route="+thumbnail_image_route;
            String method = "POST";
            Log.d(TAG,"parameters:"+parameters);

            //데이터 베이스에서 정보를 가져옴
            List<Map> resultList = Util.httpConn(url, parameters, method);
            //result : true  => 트래킹정보 저장 성공
            //result : false => 트래킹정보 저장 실패
            boolean result = Boolean.parseBoolean(resultList.get(0).get("result").toString());

            if(result){
                Util.toastText(this,"저장이 완료됐습니다.");
            }else{
                Util.toastText(this,"저장에 실패했습니다.");
            }
            //-----------------------------------------------------------------------------------
        //이미지 클릭
        }else if(v.getId() == R.id.image_check){


        }
    }
}
