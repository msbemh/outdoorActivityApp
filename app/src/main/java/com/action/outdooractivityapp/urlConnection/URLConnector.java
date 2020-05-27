package com.action.outdooractivityapp.urlConnection;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class URLConnector extends Thread {

    private static final String TAG = "URLConnector";

    private String result;
    private String URL;
    private String method;
    private String parameters;

    public URLConnector(String url, String method){
        this.URL = url;
        this.method = method;
    }

    public URLConnector(String url, String method, String parameters){
        this.URL = url;
        this.method = method;
        this.parameters = parameters;
    }

    @Override
    public void run() {
        final String output = request();
        result = output;
    }

    public String getResult(){
        return result;
    }

    private String request() {
        StringBuilder output = new StringBuilder();
        try {
            URL url = null;
            if("GET".equals(method)){
                //넘겨줄 파라미터가 있을때
                if(TextUtils.isEmpty(parameters)){
                    url = new URL(URL);
                //넘겨줄 파라미터가 없을때
                }else{
                    url = new URL(URL+"?"+parameters);
                }
            }else if("POST".equals(method)){
                url = new URL(URL);
            }
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            if (conn != null) {
                //전송모드 설정 - 기본적인 설정
                conn.setConnectTimeout(10000);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);

                //서버로 데이터 전송
                if("POST".equals(method)){
                    conn.setDoOutput(true);
                    OutputStreamWriter outStream = new OutputStreamWriter(conn.getOutputStream(),"UTF-8");
                    PrintWriter writer = new PrintWriter(outStream);
                    writer.write(parameters);
                    writer.flush();
                    writer.close();
                }else if("GET".equals(method)){
                    conn.setDoOutput(false);
                }


                //서버에서 데이터 전송 받기
                int resCode = conn.getResponseCode();
                if (resCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "HTTP_OK");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(),"UTF-8")) ;
                    String line = null;
                    while((line = reader.readLine()) != null) {
                        output.append(line + "\n");
                        Log.d(TAG, "line:"+line);
                    }
                    reader.close();
                    conn.disconnect();
                }
            }
        } catch(Exception ex) {
            Log.e("SampleHTTP", "Exception in processing response.", ex);
            ex.printStackTrace();
        }

        return output.toString();
    }
}
