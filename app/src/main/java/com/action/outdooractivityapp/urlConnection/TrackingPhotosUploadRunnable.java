package com.action.outdooractivityapp.urlConnection;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.action.outdooractivityapp.AdminApplication;
import com.action.outdooractivityapp.util.Util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrackingPhotosUploadRunnable implements Runnable {

    Context context;

    HttpURLConnection conn = null;
    DataOutputStream dos = null;

    String lineEnd = "\r\n";

    //HTTP Request Body의 끝은 "--" 와 boundary 를 붙여서 파라미터들을 구분
    String twoHyphones = "--";

    //HTTP Request Header에 boundary를 설정하면
    //HTTP Request Body에서 각 파라미터들을 구분해주는 역할을 한다.
    String boundary = "*****";


    int bytesRead, bytesAvailable, bufferSize;
    byte[] buffer;
    int maxBufferSize = 1024;
    int serverResponseCode;

    private static final String TAG = "PhotosUploadRunnable";

    //파일경로 리스트
    private List<Map> uploadFilePathList;
    private String urlString;
    //업로드 성공(true)/실패(false)
    private boolean result = true;
    //서버의 이미지 저장경로
    private List<String> serverImageRouteList = new ArrayList<String>();

    public TrackingPhotosUploadRunnable(Context context){
        this.context = context;
    }

    //클라이언트 상의 File경로를 이용하여,
    //클라이언트 상의 File을 복사하여 새로운 File만듦
    public void setPaths(List<Map> uploadFilePathList){
        this.uploadFilePathList = uploadFilePathList;
    }

    //서버 url 받아오기
    public void setUrl(String urlString){
        this.urlString = urlString;
    }

    //결과 보내주기
    public boolean getResult(){
        return this.result;
    }

    //서버 이미지 경로 리스트 보내주기
    public List<String> getServerImageRouteList(){
        return serverImageRouteList;
    }

    //백그라운드에서 실행
    @Override
    public void run() {
        //업로드 함수 실행
        uploadFileFunc(urlString);
    }

    //업로드 함수 실행
    void uploadFileFunc(String urlString){
        for(Map mapItem : uploadFilePathList){
            //클라이언트 파일 경로
            String uploadFilePath = mapItem.get("photo_image").toString();

            //새로 만들 파일이름을 클라이언트 파일경로로 설정
            String fileName = uploadFilePath;
            //원본 파일과 같은 파일을 만들기
            //File객체는 디렉터리 일수 도 있고, File 일수 도 있다.
            File sourceFile = new File(uploadFilePath);

            if(!sourceFile.isFile()){ //해당 위치의 파일이 있는지 검사
                Log.d(TAG,"sourceFile("+fileName+") is Not A File");
                return;
            }else{
                Log.d(TAG,"sourceFile("+fileName+") is A File");
                try {
                    FileInputStream fileInputStream = new FileInputStream(sourceFile);
                    URL url = new URL(urlString);
                    Log.d(TAG,"urlString:"+urlString);

                    //Open a HTTP connection to the URL
                    //HTTP Header설정
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setUseCaches(false);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Connection","Keep-Alive");
                    conn.setRequestProperty("ENCTYPE","multipart/form-data");
                    conn.setRequestProperty("Content-Type","multipart/form-data; boundary="+boundary);
                    conn.setRequestProperty("uploaded_file",fileName);
                    Log.d(TAG,"fileName:"+fileName);

                    //HTTP Body 설정 (1)
                    dos = new DataOutputStream(conn.getOutputStream());
                    dos.writeBytes(twoHyphones + boundary + lineEnd); //Body 파라미터 구분
                    dos.writeBytes("Content-Disposition: form-data; name='data'"+lineEnd);
                    dos.writeBytes(lineEnd);
                    dos.writeBytes("newImage");
                    dos.writeBytes(lineEnd);

                    //HTTP Body 설정 (2)
                    dos.writeBytes(twoHyphones + boundary + lineEnd); //Body 파라미터 구분
                    dos.writeBytes("Content-Disposition: form-data; name='user_id'"+ lineEnd);
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(AdminApplication.userMap.get("user_id").toString());
                    dos.writeBytes(lineEnd);

                    //-------------소스파일을 byte로 읽어서 서버로 보내주자.-----------------
                    //HTTP Body 설정 (3)
                    dos.writeBytes(twoHyphones + boundary + lineEnd); //Body 파라미터 구분
                    dos.writeBytes("Content-Disposition: form-data; name='uploaded_file'; fileName='"+fileName+"'"+lineEnd);
                    dos.writeBytes(lineEnd);

                    bytesAvailable = fileInputStream.available();

                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];

                    //입력스트림으로 부터 0부터 bufferSize만큼 바이트를 읽어서 buffer[]에 저장
                    //읽은 바이트수 리턴
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                    while (bytesRead > 0){
                        //buffer[]에서 0부터 bufferSize만큼 출력하기
                        dos.write(buffer, 0, bufferSize);
                        //아래는 다시 buffer만들기
                        bytesAvailable = fileInputStream.available();
                        bufferSize =  Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    }

                    dos.writeBytes(lineEnd);
                    dos.writeBytes(twoHyphones + boundary + lineEnd); //Body 끝
                    //-------------------------------------------------------------------------



                    //----------------- 서버에서의 응답을 받자 ----------------------------------
                    serverResponseCode = conn.getResponseCode();
                    String serverResponseMessage = conn.getResponseMessage();

                    Log.d(TAG,"[UploadImageToServer] HTTP Response is :"+serverResponseMessage+":"+serverResponseCode);

                    if(serverResponseCode != 200){
                        result = false;
                    }

                    //결과 확인
                    BufferedReader rd = null;

                    //서버에서 echo로 보내준 Text출력하기
                    rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    String line = null;
                    while((line = rd.readLine()) != null){
                        Log.d(TAG,"Upload State:"+line);
                        serverImageRouteList.add(line);
                    }
                    //-------------------------------------------------------------------------

                    //close the streams
                    fileInputStream.close();
                    dos.flush(); //남은 버퍼 출력하고 비우기
                    dos.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

    }
}
