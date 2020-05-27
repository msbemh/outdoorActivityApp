package com.action.outdooractivityapp.popup;

import android.Manifest;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.activity.ModifyProfileActivity;
import com.action.outdooractivityapp.util.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TakingImageProfilePopup extends Activity implements View.OnClickListener {

    private View checkBox_album;
    private View checkBox_picture;
    private TextView text_initialize;
    private Bitmap imageBitmap;

    private final int PICK_FROM_ALBUM_CODE = 0;
    private final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    static final int REQUEST_TAKE_PHOTO = 2;

    private TextView text_back;

    private String currentPhotoPath;
    private Uri photoURI;
    private Uri albumURI;

    private final String TAG = "ChoiceImageProfilePopup";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //타이틀바 없애기
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_choice_way_of_taking_popup);

        //다이얼로그 크기조정
        dialogSizeSetting();

        initializeView();

        registerListener();
    }

    //dialog 크기 조정
    void dialogSizeSetting(){
        int width = getResources().getDimensionPixelSize(R.dimen.popup_width_350);
        int height = getResources().getDimensionPixelSize(R.dimen.popup_height_450);
        getWindow().getAttributes().width = width;
        getWindow().getAttributes().height = height;
    }

    @Override
    public void onClick(View v) {
        //앨범에서 사진가져오기 클릭
        if(v.getId() == R.id.checkBox_album){
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, PICK_FROM_ALBUM_CODE);
        //카메라 이용하기 클릭
        }else if(v.getId() == R.id.checkBox_picture){
            //카메라 권한 가져오기
            int permssionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            //카메라 권한 없을때
            if (permssionCheck!= PackageManager.PERMISSION_GRANTED) {
                //User에게 권한 요청
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_CAMERA);

            //카메라 권한 있을때
            }else{
                //카메라 촬영
                dispatchTakePictureIntent();
                galleryAddPic();
            }
        //뒤로가기 클릭
        }else if(v.getId() == R.id.text_back){
            finish();
        //초기화Text 클릭
        }else if(v.getId() == R.id.text_initialize){
            Intent intent = new Intent(TakingImageProfilePopup.this, ModifyProfileActivity.class);

            //취소 floag 보내기
            intent.putExtra("flag","initialize");

            setResult(RESULT_OK,intent);

            //데이터 받아서 보내주면 activity 없애기
            finish();

        }
    }

    //User가 권한을 허용,취소했을때 result값 받기
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        //카메라 권한 요청이였을 경우
        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA) {
            //승인
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,"카메라 권한이 승인됨",Toast.LENGTH_LONG).show();
                //카메라 실행
                dispatchTakePictureIntent();
                galleryAddPic();
            //취소
            } else {
                Toast.makeText(this,"카메라 권한이 거절 되었습니다. 카메라를 이용하려면 권한을 승낙하여야 합니다.",Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //앨범에서 사진 선택 onActivityResult
        if (data != null && requestCode == PICK_FROM_ALBUM_CODE && resultCode == RESULT_OK) {
            try{
                //data.getData()로 URI를 가져올 수 있음.
                Uri uri = data.getData();
                Log.d(TAG,"uri:"+uri);

                //Uri => Bitmap 변환
                imageBitmap = Util.convertUriToBitmap(this, uri);

                //전 activity에 보내기
                Intent intent = new Intent(TakingImageProfilePopup.this, ModifyProfileActivity.class);

                saveToExternalStorage();

                //저장된 경로(URI & 실제경로) 보내주기
                intent.putExtra("imageUri",albumURI.toString());
                intent.putExtra("currentPhotoPath",currentPhotoPath);
                intent.putExtra("flag","album");

                setResult(RESULT_OK,intent);

                //데이터 받아서 보내주면 activity 없애기
                finish();

            }catch(Exception e){
                e.printStackTrace();
            }
        //카메라 촬영 onActivityResult
        }else if(requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            //전 activitu에 보내기
            Intent intent = new Intent(TakingImageProfilePopup.this, ModifyProfileActivity.class);
            intent.putExtra("imageUri",photoURI.toString());
            intent.putExtra("currentPhotoPath",currentPhotoPath);
            intent.putExtra("flag","camera");
            setResult(RESULT_OK,intent);
            finish();
        }
    }

    //파일생성(timeStamp이용)
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile( imageFileName,  ".jpg", storageDir);
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        Log.d(TAG,"[새로 생성된 FILE]:"+image);
        Log.d(TAG,"[새로 생성된 FILE의 절대 경로]:"+currentPhotoPath);
        return image;
    }

    //카메라 촬영&저장 intent전송
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,"com.action.outdooractivityapp.fileprovider",photoFile);
                Log.d(TAG,"[카메라 URI]:"+photoURI);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    //갤러리에서 인식 안될떄 인식되게 하기
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    void initializeView(){
        //View 가져오기
        checkBox_album = findViewById(R.id.checkBox_album);
        checkBox_picture = findViewById(R.id.checkBox_picture);
        text_back = findViewById(R.id.text_back);
        text_initialize = findViewById(R.id.text_initialize);
    }

    void registerListener(){
        //Listener등록
        checkBox_album.setOnClickListener(this);
        checkBox_picture.setOnClickListener(this);
        text_back.setOnClickListener(this);
        text_initialize.setOnClickListener(this);

    }

    void saveToExternalStorage(){
        try {
            File albumFile = null;

            albumFile = createImageFile();

            if (albumFile != null) {
                albumURI = FileProvider.getUriForFile(this, "com.action.outdooractivityapp.fileprovider", albumFile);
                Log.d(TAG,"[앨범 URI]:"+albumURI);
            }
            FileOutputStream out = new FileOutputStream(albumFile);
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
