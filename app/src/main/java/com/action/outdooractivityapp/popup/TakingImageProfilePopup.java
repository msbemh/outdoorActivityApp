package com.action.outdooractivityapp.popup;

import android.Manifest;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.activity.ModifyProfileActivity;
import com.action.outdooractivityapp.service.LocationSharingService;
import com.action.outdooractivityapp.util.Util;

import net.daum.mf.map.api.MapView;

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
    private static final int MY_PERMISSIONS_REQUEST_READ = 3;

    private TextView text_back;

    private String currentPhotoPath;
    private Uri photoURI;
    private Uri albumURI;

    private String timeStamp;
    private String imageFileName;
    private File storageDir;

    private static final String TAG = "ChoiceImageProfilePopup";

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


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ);
        }else {
            Log.d(TAG,"읽기 허용되있음");
        }
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

                //-----------------------------------------------------------------------------------------------------------
                //Uri를 실제경로로 변경
                String imagePath = getRealPathFromURI(this, uri);
                Log.d(TAG,"imagePath:"+imagePath);

                // 이미지를 상황에 맞게 회전시킨다
                if(imagePath != null){
                    ExifInterface exif = null;
                    try {
                        exif = new ExifInterface(imagePath);
                        int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                        Log.d(TAG,"exifOrientation:"+exifOrientation);
                        int exifDegree = Util.exifOrientationToDegrees(exifOrientation);
                        Log.d(TAG,"exifDegree:"+exifDegree);
                        imageBitmap = Util.rotate(imageBitmap, exifDegree);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //------------------------------------------------------------------------------------------------------------------
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
            intent.putExtra("imageFileName",imageFileName+timeStamp+".jpg");
            intent.putExtra("flag","camera");
            setResult(RESULT_OK,intent);
            finish();
        }
    }

    //파일생성(timeStamp이용)
    private File createImageFile() throws IOException {
        // Create an image file name
        timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        imageFileName = "JPEG_" + timeStamp + "_";
        storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile( imageFileName,  ".jpg", storageDir);

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        Log.d(TAG,"[storageDir]:"+storageDir.getAbsolutePath());
        Log.d(TAG,"[imageFileName]:"+imageFileName);
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
                //해당 경로(외부저장소)에 파일이 생성됨
                photoURI = FileProvider.getUriForFile(this,"com.action.outdooractivityapp.fileprovider",photoFile);
                Log.d(TAG,"[카메라 URI]:"+photoURI);
                //Data결과가 파일URI로 output하겠다는 의미
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                //사진촬영을 하겠다는 의미
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

    private String getRealPathFromURI(Uri contentUri) {
        int column_index=0;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if(cursor.moveToFirst()){
            column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        }

        return cursor.getString(column_index);
    }

    //앨범으로 가져온 경우, 이곳에서 외부저장소에 저장
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
            out.flush();
            out.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    //----------------------------------Uri를 실제경로로 변환시키기---------------------------------------
    public static String getRealPathFromURI(final Context context, final Uri uri) {

        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {

            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/"
                            + split[1];
                } else {
                    String SDcardpath = getRemovableSDCardPath(context).split("/Android")[0];
                    return SDcardpath +"/"+ split[1];
                }
            }

            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/my_downloads"),
                        Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }

            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] { split[1] };

                return getDataColumn(context, contentUri, selection,
                        selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }


    public static String getRemovableSDCardPath(Context context) {
        File[] storages = ContextCompat.getExternalFilesDirs(context, null);
        if (storages.length > 1 && storages[0] != null && storages[1] != null)
            return storages[1].toString();
        else
            return "";
    }


    public static String getDataColumn(Context context, Uri uri,
                                       String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };

        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG,"읽기 허용되있음");
                cursor = context.getContentResolver().query(uri, projection,
                        selection, selectionArgs, null);
                if (cursor != null && cursor.moveToFirst()) {
                    final int index = cursor.getColumnIndexOrThrow(column);
                    return cursor.getString(index);
                }
            }else{
                Log.d(TAG,"읽기 허용 안됨");
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }



    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri
                .getAuthority());
    }


    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri
                .getAuthority());
    }


    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri
                .getAuthority());
    }


    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri
                .getAuthority());
    }
//------------------------------------------------------------------------------------------------


}
