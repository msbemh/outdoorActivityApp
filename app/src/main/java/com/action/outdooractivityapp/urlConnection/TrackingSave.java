package com.action.outdooractivityapp.urlConnection;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.action.outdooractivityapp.AdminApplication;
import com.action.outdooractivityapp.util.Util;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TrackingSave extends AsyncTask<String, String, Boolean> {

    Context context;
    ProgressDialog progressDialog;
    private boolean finalResult = true;
    private String thumbnailUrl;
    private String trackingPhotosUrl;

    private static final String TAG = "TrackingSave";

    //트래킹 관련 변수
    private String location;
    private String title;
    private boolean is_public;
    private String photoPath;
    private String photoPaths;
    private double distance;
    private String startDate;
    private String endDate;
    private String difficult;
    private String photoListInfo;
    private int tracking_no;
    private String serverImageRoute;
    private boolean result;
    private List<Map> photoList;

    public TrackingSave(Context context){
        this.context = context;
    }

    public void setLocation(String location){
        this.location = location;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public void setIsPublic(boolean is_public){
        this.is_public = is_public;
    }

    public void setStartDate(String startDate){
        this.startDate = startDate;
    }

    public void setEndDate(String endDate){
        this.endDate =endDate;
    }

    public void setDistance(double distance){
        this.distance = distance;
    }

    public void setDifficult(String difficult){
        this.difficult = difficult;
    }

    public void setThumbnailUrl(String thumbnailUrl){
        this.thumbnailUrl = thumbnailUrl;
    };

    public void setTrackingPhotosUrl(String trackingPhotosUrl){
        this.trackingPhotosUrl = trackingPhotosUrl;
    };

    public void setPath(String photoPath){
        this.photoPath = photoPath;
    }

    public void setPhotoListInfo(String photoListInfo){
        this.photoListInfo = photoListInfo;
    }
    

    //AsyncTask 동작되기 전에 실행
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("저장중...");
        progressDialog.setMessage("저장중...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setIndeterminate(false);
        progressDialog.show();
    }

    //AsyncTask 백그라운드에서 실행
    @Override
    protected Boolean doInBackground(String... strings) {
        try {
            //섬네일 이미지가 있을때
            if(TextUtils.isEmpty(photoPath)){
                Log.d(TAG,"serverImageRoute:"+serverImageRoute);
            //섬네일 이미지가 없을때
            }else{
                //섬네일 이미지 업로드
                ThumbnailUploadRunnable thumbnailUploadRunnable = new ThumbnailUploadRunnable(context);
                thumbnailUploadRunnable.setPath(photoPath);
                thumbnailUploadRunnable.setUrl(thumbnailUrl);
                Thread thread = new Thread(thumbnailUploadRunnable);
                thread.start();
                thread.join();
                //서버의 이미지 경로
                serverImageRoute = thumbnailUploadRunnable.getServerImageRoute();
                Log.d(TAG,"serverImageRoute:"+serverImageRoute);
                //결과 체크
                result = thumbnailUploadRunnable.getResult();
                Log.d(TAG,"result:"+result);
                checkResult(result);
            }

            //섬네일 이미지경로와 트래킹 정보 INSERT
            result = trackingInfoAndThumbNailInsert(serverImageRoute);
            //결과 체크
            checkResult(result);

            //jsonString => List<Map> 변경
            photoList = Util.convertJsonStringToListMap(photoListInfo);
            
            //------------------ 로그 확인 ------------------------
//            for(Map mapItem : photoList){
//                Iterator<String> iteratorK = mapItem.keySet().iterator();
//                while (iteratorK.hasNext()) {
//                    String key = iteratorK.next();
//                    String value = mapItem.get(key).toString();
//                    Log.d(TAG,"[key]:" + key + ", [value]:" + value);
//                }
//
//                Log.d(TAG,"latitude:"+mapItem.get("latitude").toString());
//                Log.d(TAG,"longitude:"+mapItem.get("longitude").toString());
//                Log.d(TAG,"order:"+mapItem.get("order").toString());
//                Log.d(TAG,"photo_image:"+mapItem.get("photo_image").toString());
//            }
            //----------------------------------------------------------
            
            //사진리스트가 존재하지 않을때
            if(photoList.size() == 0){

            //사진리스트가 존재할때
            }else{
                TrackingPhotosUploadRunnable trackingPhotosUploadRunnable = new TrackingPhotosUploadRunnable(context);

                trackingPhotosUploadRunnable.setPaths(photoList);
                trackingPhotosUploadRunnable.setUrl(trackingPhotosUrl);
                Thread thread = new Thread(trackingPhotosUploadRunnable);
                thread.start();
                thread.join();
                //결과 체크
                result = trackingPhotosUploadRunnable.getResult();
                checkResult(result);

                //------------------ 로그 확인 ------------------------
                for(Map mapItem : photoList){
                    Iterator<String> iteratorK = mapItem.keySet().iterator();
                    while (iteratorK.hasNext()) {
                        String key = iteratorK.next();
                        String value = mapItem.get(key).toString();
                        Log.d(TAG,"[key]:" + key + ", [value]:" + value);
                    }
                }
                //----------------------------------------------------------
                
                //트래킹 이미지 파일들 INSERT
                result = trackingPhotosInsert();
                //결과 체크
                checkResult(result);
                //트래킹 저장설정 창 끝내기
            }
            ((Activity)context).finish();
        }  catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }
    
    //섬네일 이미지경로와 트래킹 정보 INSERT
    boolean trackingInfoAndThumbNailInsert(String serverImageRoute){
        //안드로이드 => http => 데이터베이스 에서 정보를 가져오기 위해서
        //url, paramters, method정보가 필요함.
        String url = "https://wowoutdoor.tk/tracking/tracking_insert_query.php";
        String parameters = "user_id="+ AdminApplication.userMap.get("user_id")+"&nick_name="+AdminApplication.userMap.get("nick_name")
                +"&location="+location+"&title="+title+"&is_public="+is_public+"&thumbnail_image_route="+serverImageRoute
                +"&distance="+distance+"&start_date="+startDate+"&end_date="+endDate+"&difficult="+difficult;
        String method = "POST";
        Log.d(TAG,"url:"+url+"?"+parameters);
        Log.d(TAG,"parameters:"+parameters);

        //데이터 베이스에서 정보를 가져옴
        List<Map> resultList = Util.httpConn(url, parameters, method);
        //result : true  => 트래킹정보 저장 성공
        //result : false => 트래킹정보 저장 실패
        boolean result = Boolean.parseBoolean(resultList.get(0).get("result").toString());
        //INSERT해서 나온 PK값 가져오기
        tracking_no = Integer.parseInt(resultList.get(0).get("tracking_no").toString());
        Log.d(TAG,"tracking_no:"+tracking_no);
        
        return result;
    }

    //트래킹 이미지 파일들 INSERT
    boolean trackingPhotosInsert(){
        boolean result = true;
        //List<Map> => JsonString으로 변환
        String photoListJsonString = Util.convertListMapToJsonString(photoList);
        //url, paramters, method정보가 필요함.
        String url = "https://wowoutdoor.tk/tracking/tracking_photo_insert_query.php";
        String parameters = "tracking_no="+ tracking_no+"&photoListJsonString="+photoListJsonString;
        String method = "POST";
        Log.d(TAG,"photoListJsonString:"+photoListJsonString);
        Log.d(TAG,"url:"+url+"?"+parameters);
        Log.d(TAG,"parameters:"+parameters);

        //데이터 베이스에서 정보를 가져옴
        List<Map> resultList = Util.httpConn(url, parameters, method);
        //result : true  => 트래킹정보 저장 성공
        //result : false => 트래킹정보 저장 실패
        result = Boolean.parseBoolean(resultList.get(0).get("result").toString());
        //결과 체크
        if(!result){
            result = false;
        }
        return result;
    }
    
    //트랜잭션처럼 한번이라도 실패한게 있는지 체크하기
    void checkResult(boolean result){
        if(!result){
            finalResult = false;
        }
    }

    //doInBackground메소드가 종료된 후 호출됨.
    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if(finalResult){
            Util.toastText(context,"저장이 완료됐습니다.");
        }else{
            Util.toastText(context,"저장에 실패했습니다.");
        }
        //프로그래스 대화상장 끄기
        progressDialog.dismiss();
    }

}
