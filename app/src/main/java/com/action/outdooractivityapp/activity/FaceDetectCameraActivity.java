package com.action.outdooractivityapp.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.adapter.CustomCalloutBalloonAdapter;

import net.daum.mf.map.api.MapView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;

public class FaceDetectCameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    private static final String TAG = "MainActivity";
    private Mat matInput;
    private Mat matResult;
    private CameraBridgeViewBase cameraBridgeViewBase;
    private Button button_capture;
    private Button button_mask;

    public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);

    public native void Detect(long matAddrInput, long matAddrResult);

    public native long loadCascade(String cascadeFileName );
    public native void detect(long cascadeClassifier_face,
                              long cascadeClassifier_eye, long matAddrInput, long matAddrResult, int maskFlag);
    public long cascadeClassifier_face = 0;
    public long cascadeClassifier_eye = 0;
    private int maskFlag = 0;

    //영상에 검출된 이미지와 최종 영상을 저장하는 코드간에 동기화를 맞추기 위해서 세마포어를 사용함.
    //세마포어: 여러 프로세스가 공유데이터를 동시에 접근하지 못하게 할 수 있음.
    private final Semaphore writeLock = new Semaphore(1);

    public void getWriteLock() throws InterruptedException {
        writeLock.acquire();
    }

    public void releaseWriteLock() {
        writeLock.release();
    }

    /* loadLibrary는 클래스패스상의 디렉터리에 라이브러리가 있다고 인식함. */
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_facedetect_camera);

        initializeView();

        registerListener();

        Log.d(TAG, "onCreate 동작");
        cameraBridgeViewBase = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);
        cameraBridgeViewBase.setCameraIndex(0); // front-camera(1),  back-camera(0)
    }

    void initializeView() {
        button_capture = findViewById(R.id.button_capture);
        button_mask = findViewById(R.id.button_mask);
    }

    void registerListener() {
        button_capture.setOnClickListener(this);
        button_mask.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        //캡처 클릭시
        if(v.getId() == R.id.button_capture){
            try {
                //세마포어 잠그기
                getWriteLock();

                //빈 파일 생성
                File file;
                try{
                    file = createImageFile();
                }catch (Exception e){
                    e.printStackTrace();
                    Log.d(TAG,e.getMessage());
                    return;
                }
                String filename = file.toString();
                Log.d(TAG,"filename:"+filename);

                //BGR을 RGBA로 변형
                Imgproc.cvtColor(matResult, matResult, Imgproc.COLOR_BGR2RGBA);

                //Imgcodecs.imwrite는 파일 저장
                //Imgcodecs.imread는 파일 불러오기
                boolean result  = Imgcodecs.imwrite( filename, matResult);

                //성공여부 확인
                if ( result ) Log.d(TAG, "SUCESS");
                else Log.d(TAG, "FAIL");

                //미디어 스캔을 통하여 새로 생성된 파일을 인지할 수 있도록 한다.
                Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(Uri.fromFile(file));
                sendBroadcast(mediaScanIntent);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //세마포어 풀기
            releaseWriteLock();
        //마스크 클릭시
        }else if(v.getId() == R.id.button_mask){
            maskFlag++;
            //maskFlag
            //0 : 원본
            //1 : 선글라스
            if(maskFlag == 2){
                maskFlag = 0;
            }
        //카메라 뒤집기
        }else if(v.getId() == R.id.button_camera_reverse){
            //카메라 뒤집기
            cameraBridgeViewBase.disableView();
            //후면 카메라동작
            cameraBridgeViewBase.setCameraIndex(1);
            cameraBridgeViewBase.enableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d(TAG, "onCameraViewStarted 동작");
    }

    @Override
    public void onCameraViewStopped() {
        Log.d(TAG, "onCameraViewStopped 동작");
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        try {
            //세마포어 잠그기
            getWriteLock();

            matInput = inputFrame.rgba();
            if ( matResult == null )
                matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());
//          ConvertRGBtoGray(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
//            Detect(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
//          detectFace();

            //haar cascade 하기
            //화면 뒤집기 (양수: 수평 뒤집기, 0: 수직 뒤집기, 음수: 모두 뒤집기)
//            Core.flip(matInput, matInput, -1);
            detect(cascadeClassifier_face, cascadeClassifier_eye, matInput.getNativeObjAddr(), matResult.getNativeObjAddr(), maskFlag);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //세마포어 풀기기
        releaseWriteLock();

        return matResult;

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        Log.d(TAG, "onPointerCaptureChanged 동작");
    }

    //로딩되면 콜백받는 객체
    //OpenCV로딩 되면 콜백 받기위해서 추가
    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            Log.d(TAG, "BaseLoaderCallback의 onManagerConnected 동작");
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:{
                    Log.d(TAG, "enableView 동작");
                    cameraBridgeViewBase.enableView();
                }
                break;
                default: {
                    Log.d(TAG, "onManagerConnected 동작");
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause 동작");
        if (cameraBridgeViewBase != null)
            cameraBridgeViewBase.disableView();
    }

    @Override
    public void onResume(){
        super.onResume();

        Log.d(TAG, "onResume 동작");
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, baseLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy 동작");
        if (cameraBridgeViewBase != null)
            cameraBridgeViewBase.disableView();
    }

    private void read_cascade_file(){
        //copyFile 메소드는 Assets에서 해당 파일을 가져와
        //외부 저장소 특정위치에 저장하도록 구현된 메소드입니다.
        copyFile("haarcascade_frontalface_alt.xml");
        copyFile("haarcascade_eye_tree_eyeglasses.xml");
        copyFile("haarcascade_eye_tree_eyeglasses.xml");

        Log.d(TAG, "read_cascade_file:");

        //loadCascade 메소드는 외부 저장소의 특정 위치에서 해당 파일을 읽어와서
        //CascadeClassifier 객체로 로드합니다.
        cascadeClassifier_face = loadCascade( "haarcascade_frontalface_alt.xml");
        Log.d(TAG, "read_cascade_file:");
        cascadeClassifier_eye = loadCascade( "haarcascade_eye_tree_eyeglasses.xml");
    }

    private void copyFile(String filename) {
        String baseDir = Environment.getExternalStorageDirectory().getPath();
        String pathDir = baseDir + File.separator + filename;

        AssetManager assetManager = this.getAssets();

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            Log.d( TAG, "copyFile :: 다음 경로로 파일복사 "+ pathDir);
            inputStream = assetManager.open(filename);
            outputStream = new FileOutputStream(pathDir);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            inputStream = null;
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        } catch (Exception e) {
            Log.d(TAG, "copyFile :: 파일 복사 중 예외 발생 "+e.toString() );
        }
    }

    //파일생성(timeStamp이용)
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File storageDir = new File(Environment.getExternalStorageDirectory() + "/Images/");
        //해당 경로 폴더가 없으면 생성(상위 디렉토리 없으면 같이 생성)
//        path.mkdirs();
//        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File file = File.createTempFile( imageFileName,  ".jpg", storageDir);

        // Save a file: path for use with ACTION_VIEW intents
        String currentPhotoPath = file.getAbsolutePath();
        Log.d(TAG,"[storageDir]:"+storageDir.getAbsolutePath());
        Log.d(TAG,"[imageFileName]:"+imageFileName);
        Log.d(TAG,"[새로 생성된 FILE]:"+file);
        Log.d(TAG,"[새로 생성된 FILE의 절대 경로]:"+currentPhotoPath);

        return file;
    }


    //----------------여기서부턴 퍼미션 관련 메소드--------------------------------------------------
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        //Collections.singletonList에는 요소가 1개만 들어갈 수 있다.
        //cameraBridgeViewBase요소 1개만 추가.
        return Collections.singletonList(cameraBridgeViewBase);
    }

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;

    //카메라, 외부저장소 읽기 쓰기 권한이 있을 경우
    //cameraBridgeViewBase에 카메라 권한 수여.
    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase: cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();

                read_cascade_file();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean havePermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //카메라, 외부저장소 읽기 쓰기 권한이 없을경우
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //권한 요청
                requestPermissions(new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE}, CAMERA_PERMISSION_REQUEST_CODE);
                //권한 no have
                havePermission = false;
            }
        }
        //권한 있을 경우
        if (havePermission) {
            //카메라View에 카메라, 외부저장소 읽기 쓰기 권한 주기
            onCameraPermissionGranted();
        }
    }

    //권한 선택 결과
    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //카메라, 외부저장소 읽기 쓰기 권한 허가한 경우
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
                && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
            //카메라View에 카메라 권한 주기
            onCameraPermissionGranted();
        //카메라 권한 허가하지 않은경우
        }else{
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //카메라 권한 선택 대화상자
    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder( this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id){
                //카메라, 외부저장소 읽기 쓰기 권한 요청
                requestPermissions(new String[]{Manifest.permission.CAMERA
                        ,Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ,Manifest.permission.READ_EXTERNAL_STORAGE}, CAMERA_PERMISSION_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                //끝내기
                finish();
            }
        });
        builder.create().show();
    }


}
