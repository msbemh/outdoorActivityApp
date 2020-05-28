package com.action.outdooractivityapp.urlConnection;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadFile extends AsyncTask<String, String, String> {

    Context context;
    ProgressDialog progressDialog;

    //원본 파일경로
    String fileName;

    //원본 파일과 같은 파일을 만들기
    //File객체는 디렉터리 일수 도 있고, File 일수 도 있다.
    File sourceFile;

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

    private static final String TAG = "UploadFile";

    public UploadFile(Context context){
        this.context = context;
    }

    //파일 실제 경로와
    //원본파일과 같은 파일 세팅
    public void setPath(String uploadFilePath){
        this.fileName = uploadFilePath;
        this.sourceFile = new File(uploadFilePath);
    }

    //AsyncTask 동작되기 전에 실행
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Loading...");
        progressDialog.setMessage("Image uploading...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setIndeterminate(false);
        progressDialog.show();
    }

    //AsyncTask 백그라운드에서 실행
    @Override
    protected String doInBackground(String... strings) {
        if(!sourceFile.isFile()){ //해당 위치의 파일이 있는지 검사
            Log.d(TAG,"sourceFile("+fileName+") is Not A File");
            return null;
        }else{
            String success = "Success";
            Log.d(TAG,"sourceFile("+fileName+") is A File");
            try {
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(strings[0]);
                Log.d(TAG,"strings[0]:"+strings[0]);

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
                dos.writeBytes("Content-Disposition: form-data; name='uploaded_file'; fileName='"+fileName+"'"+lineEnd);
                dos.writeBytes(lineEnd);

                //-------------소스파일을 byte로 읽어서 서버로 보내주자.-----------------
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0){
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize =  Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }
                //-------------------------------------------------------------------------

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphones + boundary + lineEnd); //Body 끝

                //----------------- 서버에서의 응답을 받자 ----------------------------------
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.d(TAG,"[UploadImageToServer] HTTP Response is :"+serverResponseMessage+":"+serverResponseCode);

                if(serverResponseCode == 200){

                }

                //결과 확인
                BufferedReader rd = null;

                //서버에서 echo로 보내준 Text출력하기
                rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                String line = null;
                while((line = rd.readLine()) != null){
                    Log.d(TAG,"Upload State:"+line);
                }
                //-------------------------------------------------------------------------

                //close the streams
                fileInputStream.close();
                dos.flush(); //남은 버퍼 출력하고 비우기
                dos.close();
            }catch (Exception e){
                e.printStackTrace();
            }
            return success;
        }
    }

    //doInBackground메소드가 종료된 후 호출됨.
    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        //프로그래스 대화상장 끄기
        progressDialog.dismiss();
        //프로필 변경 Activity 끄기
        ((Activity)context).finish();
    }

}
