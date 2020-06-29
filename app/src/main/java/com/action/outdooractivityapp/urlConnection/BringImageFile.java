package com.action.outdooractivityapp.urlConnection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;


import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BringImageFile extends Thread {

    private String URL;
    private Bitmap bitmap;
    private String TAG = "BringImageFile";

    @Override
    public void run() {
        bringImageFile();
    }

    public BringImageFile(String url){
        this.URL = "https://wowoutdoor.tk/"+url;
        Log.d(TAG,"URL:"+URL);
    }

    private void bringImageFile() {
        try {
            URL url = new URL(URL);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            if (conn != null) {
                //전송모드 설정 - 기본적인 설정
                conn.setConnectTimeout(10000);
                conn.setDoInput(true);
                conn.connect();

                InputStream is = conn.getInputStream(); //inputStream 값 가져오기

                bitmap = BitmapFactory.decodeStream(is); // Bitmap으로 반환
                Log.d(TAG,"bitmap:"+bitmap.getByteCount());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public Bitmap getBitmap(){
        return this.bitmap;
    }

}
