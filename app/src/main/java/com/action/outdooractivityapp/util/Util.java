package com.action.outdooractivityapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.action.outdooractivityapp.activity.LoginActivity;
import com.action.outdooractivityapp.urlConnection.URLConnector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Util {

    private static String TAG = "Util";
    private static SharedPreferences preferences;
    private static SharedPreferences.Editor editor;

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

    //user정보 SharedPreferences에 저장
    public static void saveUser(Context context){

        preferences = context.getSharedPreferences("myApp", context.MODE_PRIVATE);
        editor = preferences.edit();

        //JSON으로 변환후 Shared Preferences를 이용하여 userMap을 저장.
        JSONArray jsonArray = new JSONArray();
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("userId", LoginActivity.userMap.get("user_id"));
                jsonObject.put("userPassword", LoginActivity.userMap.get("user_password"));
                jsonObject.put("nickName", LoginActivity.userMap.get("nick_name"));
                jsonObject.put("profileImage", LoginActivity.userMap.get("profile_image"));

                jsonArray.put(jsonObject);

            }catch (Exception e){
                e.printStackTrace();
            }

            //SharedPreference로 저장
            //List<userMap>를 String으로 변환하여 저장
            editor.putString("userList", jsonArray.toString());
            editor.commit();
    }

    //user정보 ShqredPreferences에서 불러오기
    public static void bringUser(Context context){
        //Shared Preferences에서 userList 가져오기
        SharedPreferences preferences = context.getSharedPreferences("myApp", context.MODE_PRIVATE);
        String userReviewListString = preferences.getString("userList","");
        try {
            //String => JSON으로 변경
            JSONArray jsonArray = new JSONArray(userReviewListString);
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            Map userMap = new HashMap();
            userMap.put("user_id", jsonObject.getString("userId"));
            userMap.put("user_password", jsonObject.optString("userPassword",""));
            userMap.put("nick_name", jsonObject.optString("nickName",""));
            userMap.put("profile_image", jsonObject.optString("profileImage",""));

            LoginActivity.userMap = userMap;


        } catch (JSONException e) {
            Log.e(TAG,e.getMessage());
            e.printStackTrace();
        }

        Log.d(TAG,"LoginActivity.userMap:"+LoginActivity.userMap);
    }

    //String => Date로 변경 (형식: yyyy-MM-dd HH:mm:ss)
    public static Date convertStringToDateTime(String dateString){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        Log.d(TAG,"dateString:"+dateString);
        try {
            if(dateString != null && !"".equals(dateString)){
                date = format.parse(dateString);
            }
            return date;
        }catch (Exception e){
            e.printStackTrace();
            Log.e(TAG,e.getMessage());
        }
        return date;
    }

    //Date => String로 변경 (형식: yyyy-MM-dd HH:mm:ss)
    public static String convertDateTimeToString(Date date){
        String dateString = "";
        if(date != null){
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dateString = format.format(date);
        }
        return dateString;
    }

    //String => Date로 변경 (형식: yyyy-MM-dd)
    public static Date convertStringToDate(String dateString){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date date = null;
        Log.d(TAG,"dateString:"+dateString);
        try {
            if(dateString != null && !"".equals(dateString)){
                date = format.parse(dateString);
            }
            return date;
        }catch (Exception e){
            e.printStackTrace();
            Log.e(TAG,e.getMessage());
        }
        return date;
    }

    //Date => String로 변경 (형식: yyyy-MM-dd)
    public static String convertDateToString(Date date){
        String dateString = "";
        if(date != null){
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            dateString = format.format(date);
        }
        return dateString;
    }

    //Toast 텍스트
    public static void toastText(Context context, String text){
        Toast.makeText(context.getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

}
