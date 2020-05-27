package com.action.outdooractivityapp.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.action.outdooractivityapp.urlConnection.URLConnector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Util {

    //데이터베이스에서 정보 가져오기(Android => http => mysql)
    public static List<Map> httpConn(String url, String parameters, String method){

        List<Map> list = new ArrayList<Map>();

        URLConnector urlConnector = new URLConnector(url, method, parameters);

        urlConnector.start();

        try{
            urlConnector.join();
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }

        //데이터베이스 결과 가져오기
        String result = urlConnector.getResult();
        JSONArray jsonArray = null;
        try {
            if(!TextUtils.isEmpty(result)){
                jsonArray = new JSONArray(result);
                for(int i=0; i<jsonArray.length(); i++){
                    list.add(jsonToMap(jsonArray.getJSONObject(i)));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return list;

    }

    //JSONObject를 Map<String, Object>으로 변환
    public static Map<String, Object> jsonToMap(JSONObject json) throws JSONException {
        Map<String, Object> retMap = new HashMap<String, Object>();

        if(json != JSONObject.NULL) {
            retMap = toMap(json);
        }
        return retMap;
    }

    //JSONObject를 Map<String, Object>으로 변환
    public static Map<String, Object> toMap(JSONObject object) throws JSONException {
        Map<String, Object> map = new HashMap<String, Object>();

        //JSONObejct의 key들을 뽑아 Iterator만들기
        Iterator<String> keysItr = object.keys();
        while(keysItr.hasNext()) {
            String key = keysItr.next(); //key
            Object value = object.get(key); //value

            //값이 JSONArray면 List로 변환
            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            //값이 JSONObject면 Map으로 다시 변환
            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }

            //Map으로 만들기
            map.put(key, value);
        }
        return map;
    }

    //JSONArray를 List<Object>로 변환
    public static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<Object>();
        for(int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }

    //URI => Bitmap 변경
    public static Bitmap convertUriToBitmap(Context context, Uri uri){

        Bitmap imageBitmap = null;

        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            imageBitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
        }catch (IOException e){
            e.printStackTrace();
        }

        return imageBitmap;
    }

}
