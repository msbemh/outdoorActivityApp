package com.action.outdooractivityapp.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.popup.TakingImageProfilePopup;
import com.action.outdooractivityapp.urlConnection.UploadFile;
import com.action.outdooractivityapp.util.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ModifyProfileActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = "ModifyProfileActivity";

    private ImageView image_profile;
    private ImageView image_back;
    private ImageView image_check;
    private EditText edit_user_name;

    private Intent intent;
    private final int IMAGE_REQUEST_CODE = 0;

    private String flag="";
    private Uri uri;
    private Bitmap imageBitmap;
    private String currentPhotoPath;

    private String nickName = LoginActivity.userMap.get("nick_name").toString();
    private Map userMap = LoginActivity.userMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_profile);

        initializeView();

        registerListener();

        //imageView 둥글게 만들기
        image_profile.setBackground(new ShapeDrawable(new OvalShape()));
        image_profile.setClipToOutline(true);

        //현재 닉네임 가져오기
        edit_user_name.setText(nickName);

        //프로필 사진 있으면 보여주기
        if(userMap.get("profile_image") !=null
                && !"null".equals(userMap.get("profile_image").toString())
                && LoginActivity.profileImage != null){
            image_profile.setImageBitmap(LoginActivity.profileImage);
        //없으면 기본 프로필 사진 보여주기
        }else{
            image_profile.setImageResource(R.drawable.icon_profile_invert);
        }
    }

    void initializeView(){
        image_profile = findViewById(R.id.image_profile);
        image_back = findViewById(R.id.image_back);;
        image_check = findViewById(R.id.image_check);;
        edit_user_name = findViewById(R.id.edit_user_name);;
    }

    void registerListener(){
        image_profile.setOnClickListener(this);
        image_check.setOnClickListener(this);
        image_back.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        //image클릭
        if(v.getId() == R.id.image_profile){
            intent = new Intent(this, TakingImageProfilePopup.class);
            startActivityForResult(intent, IMAGE_REQUEST_CODE);
        //저장 버튼 클릭
        }else if(v.getId() == R.id.image_check){
            //userName에 값이 없을때
            if(edit_user_name.getText().toString().length() <= 0){
                Toast.makeText(this,"name이 없습니다. name을 입력해주세요.",Toast.LENGTH_SHORT).show();
            //userName에 값이 있을때
            }else{
                saveData();
            }

        //뒤로가기 버튼 클릭
        }else if(v.getId() == R.id.image_back){
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && requestCode == IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                //앨범이냐 카메라냐에 따라서 구분하기 위해서 & 초기화유무
                flag = data.getStringExtra("flag");

                //초기화일 경우
                if("initialize".equals(flag)){
                    uri = null;
                //초기화가 아닐경우
                }else{
                    String uriString = data.getStringExtra("imageUri");

                    //사진 촬영의 경우만 currentPhotoPath값 들어옴.
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
                    image_profile.setImageURI(uri);
                //없으면 기본 프로필 사진 보여주기
                }else{
                    image_profile.setImageResource(R.drawable.icon_profile_invert);
                }
                Log.d(TAG,"[TEST]확인"+LoginActivity.userMap);

            }else if(resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "사진 선택 취소", Toast.LENGTH_LONG).show();
            }
        }
    }


    void saveData(){
        //userMap의 nick_name 변경
        String editUserName = edit_user_name.getText().toString();
        LoginActivity.userMap.put("nick_name", editUserName);

        //------- user닉네임 수정 ----------------
        String url = "https://wowoutdoor.tk/user/nick_name_update_query.php";
        String parameters = "user_id="+LoginActivity.userMap.get("user_id").toString()+"&nick_name="+LoginActivity.userMap.get("nick_name").toString();
        String method = "POST";

        //데이터 베이스에서 정보를 가져옴
        List<Map> resultList = Util.httpConn(url, parameters, method);
        //이미지 경로 수정
        Log.d(TAG,"result:"+resultList.get(0).get("result"));
        //---------------------------------------------------

        //파일 업로드
        if(currentPhotoPath != null){
            uploadFile(currentPhotoPath);
        //파일 업로드 없을 경우
        }else{
            //프로필 사진 초기화일 경우
            if("initialize".equals(flag)){
                //------- user프로필 사진경로 null로 바꾸기 ----------------
                url = "https://wowoutdoor.tk/user/profile_image_update_query.php";
                parameters = "user_id="+LoginActivity.userMap.get("user_id").toString();
                method = "POST";

                //데이터 베이스에서 정보를 가져옴
                resultList = Util.httpConn(url, parameters, method);
                boolean result = Boolean.parseBoolean(resultList.get(0).get("result").toString());
                //이미지 경로 수정
                Log.d(TAG,"result:"+result);

                //로컬 유저 정보 수정
                if(result){
                    LoginActivity.userMap.put("profile_image", null);
                    LoginActivity.profileImage = null;
                }
                //---------------------------------------------------
            }
            finish();
        }

        Toast.makeText(getApplicationContext(),"변경이 완료됐습니다.", Toast.LENGTH_SHORT).show();
    }

    public void uploadFile(String filePath){
        String url = "https://wowoutdoor.tk/upload_file.php";
        try{
            UploadFile uploadFile = new UploadFile(ModifyProfileActivity.this);
            uploadFile.setPath(filePath);
            uploadFile.execute(url);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

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

}
