package com.action.outdooractivityapp.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.action.outdooractivityapp.AdminApplication;
import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.popup.TakingImageProfilePopup;
import com.action.outdooractivityapp.urlConnection.TrackingThumbnailUploadFile;
import com.action.outdooractivityapp.util.Util;

import java.io.IOException;
import java.util.Date;
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
    private double distance;
    private String startDate;
    private String endDate;
    private Bundle extras;
    private String thumbnail_image_route;

    private final int IMAGE_REQUEST_CODE = 0;
    private String flag="";
    private Uri uri;
    private Bitmap imageBitmap;
    private String currentPhotoPath;

    private String title;
    private boolean is_public = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking_info_input);

        initializeView();

        registerListener();

        /*data 받아오기*/
        extras = getIntent().getExtras();
        location = extras.getString("location");
        distance = extras.getDouble("distance");
        startDate = extras.getString("startDate");
        endDate = extras.getString("endDate");
        Log.d(TAG, "location:"+location);
        Log.d(TAG, "distance:"+distance);
        Log.d(TAG, "startDate:"+startDate);
        Log.d(TAG, "endDate:"+endDate);
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
            title = editText_title.getText().toString();
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
            if("공개".equals(radioButton.getText().toString())){
                is_public = true;
            }else if("비공개".equals(radioButton.getText().toString())){
                is_public = false;
            }

            //파일 업로드 (파일경로가 존재할경우에만)
            //이곳에서 데이터베이스에 다른 정보도 저장시킨다.
            //이곳에서 모든게 완료되면 finish시켜줌.
            if(currentPhotoPath != null){
                trackingThumbnailUploadFile(currentPhotoPath);
            //업로드할 파일이 없을때
            }else{
                //안드로이드 => http => 데이터베이스 에서 정보를 가져오기 위해서
                //url, paramters, method정보가 필요함.
                String url = "https://wowoutdoor.tk/tracking/tracking_insert_query.php";
                String parameters = "user_id="+ AdminApplication.userMap.get("user_id")+"&nick_name="+AdminApplication.userMap.get("nick_name")
                        +"&location="+location+"&title="+title+"&is_public="+is_public+"&thumbnail_image_route="+currentPhotoPath
                        +"&distance="+distance+"&start_date="+startDate+"&end_date="+endDate;
                String method = "POST";
                Log.d(TAG,"url:"+url+"?"+parameters);

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
                finish();
            }
            //-----------------------------------------------------------------------------------
        //이미지 클릭
        }else if(v.getId() == R.id.imageView_thumbnail){
            intent = new Intent(this, TakingImageProfilePopup.class);
            startActivityForResult(intent, IMAGE_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && requestCode == IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                //초기화유무
                flag = data.getStringExtra("flag");

                //초기화일 경우
                if("initialize".equals(flag)){
                    uri = null;
                //초기화가 아닐경우
                }else{
                    String uriString = data.getStringExtra("imageUri");

                    currentPhotoPath = data.getStringExtra("currentPhotoPath");
                    Log.d(TAG,"[넘어온 currentPhotoPath]:"+currentPhotoPath);
                    Log.d(TAG,"[넘어온 uri]:"+uriString);

                    uri = Uri.parse(uriString);

                    //uri => bitmap으로 변환
                    imageBitmap = Util.convertUriToBitmap(this,uri);


                    // 이미지를 상황에 맞게 회전시킨다
                    if(currentPhotoPath != null){
                        ExifInterface exif = null;
                        try {
                            exif = new ExifInterface(currentPhotoPath);
                            int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                            int exifDegree = exifOrientationToDegrees(exifOrientation);
                            Log.d(TAG,"exifDegree:"+exifDegree);
                            imageBitmap = rotate(imageBitmap, exifDegree);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                //프로필 사진 있으면 보여주기
                if(uri != null){
                    imageView_thumbnail.setImageURI(uri);
                //없으면 기본 프로필 사진 보여주기
                }else{
                    imageView_thumbnail.setImageResource(R.drawable.icon_profile_invert);
                }
                Log.d(TAG,"[TEST]확인"+AdminApplication.userMap);

            }else if(resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "사진 선택 취소", Toast.LENGTH_LONG).show();
            }
        }
    }

    public int exifOrientationToDegrees(int exifOrientation) {
        if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        }
        else if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        }
        else if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    public Bitmap rotate(Bitmap bitmap, int degrees) {
        if(degrees != 0 && bitmap != null){
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) bitmap.getWidth() / 2,
                    (float) bitmap.getHeight() / 2);

            try {
                Bitmap converted = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), m, true);
                if(bitmap != converted) {
                    bitmap.recycle();
                    bitmap = converted;
                }
            } catch(OutOfMemoryError ex) {
                // 메모리가 부족하여 회전을 시키지 못할 경우 그냥 원본을 반환합니다.
            }
        }
        return bitmap;
    }

    //파일 업로드 (파일경로가 존재할경우에만)
    //이곳에서 데이터베이스에 다른 정보도 저장시킨다.
    //이곳에서 모든게 완료되면 finish시켜줌.
    public void trackingThumbnailUploadFile(String filePath){
        String url = "https://wowoutdoor.tk/public_upload_file.php";
        try{
            TrackingThumbnailUploadFile trackingThumbnailUploadFile = new TrackingThumbnailUploadFile(this);
            trackingThumbnailUploadFile.setPath(filePath);
            trackingThumbnailUploadFile.setLocation(location);
            trackingThumbnailUploadFile.setTitle(title);
            trackingThumbnailUploadFile.setIsPublic(is_public);
            trackingThumbnailUploadFile.setStartDate(startDate);
            trackingThumbnailUploadFile.setEndDate(endDate);
            trackingThumbnailUploadFile.setDistance(distance);
            trackingThumbnailUploadFile.execute(url);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
