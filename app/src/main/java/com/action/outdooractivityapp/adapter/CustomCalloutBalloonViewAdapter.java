package com.action.outdooractivityapp.adapter;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.abstact.Loader;
import com.action.outdooractivityapp.urlConnection.BringImageFile;
import com.action.outdooractivityapp.util.Util;

import net.daum.mf.map.api.CalloutBalloonAdapter;
import net.daum.mf.map.api.MapPOIItem;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CustomCalloutBalloonViewAdapter implements CalloutBalloonAdapter {

    private final View mCalloutBalloon;
    private Context context;
    private List<Map> trackingPhotoList;

    private String TAG = "CustomCalloutBalloonViewAdapter";

    public CustomCalloutBalloonViewAdapter(Context context, List<Map> trackingPhotoList) {
        this.context = context;
        this.trackingPhotoList = trackingPhotoList;
        mCalloutBalloon = ((Activity)context).getLayoutInflater().inflate(R.layout.custom_callout_balloon, null);
    }

    @Override
    public View getCalloutBalloon(MapPOIItem mapPOIItem) {
        Log.d(TAG,"name:"+mapPOIItem.getItemName());
        Log.d(TAG,"tag:"+mapPOIItem.getTag());
        int marker_tag = mapPOIItem.getTag();

        TextView textView_title = ((TextView) mCalloutBalloon.findViewById(R.id.textView_title));
        TextView textView_time = ((TextView) mCalloutBalloon.findViewById(R.id.textView_time));
        ImageView imageView_photo = ((ImageView) mCalloutBalloon.findViewById(R.id.imageView_photo));

        //tag(0):시작위치
        //tag(1~쭈욱):사진
        //시작위치일 경우
        if(marker_tag == 0){
            textView_title.setText(mapPOIItem.getItemName());
            textView_time.setText("날짜");
            imageView_photo.setVisibility(View.GONE);
        //사진리스트일 경우
        }else if(marker_tag > 0){
            textView_title.setText(mapPOIItem.getItemName());
            textView_time.setText("날짜");
            imageView_photo.setVisibility(View.VISIBLE);
            String imageRoute = null;
            for(Map mapItem : trackingPhotoList){
                int tag = Integer.parseInt(mapItem.get("tag").toString());
                //마커의 태그와 trackingPhotoList에 저장된 tag가 같을경우
                if(marker_tag == tag){
                    imageRoute = mapItem.get("photo_image").toString();
                }
            }

            //이미지가 없을 경우
            if(imageRoute == null){

            //이미지가 있을 경우
            }else{
                //사진 가져오기
                //------이미지 파일 서버에서 Bitmap으로 가져오기-------
                BringImageFile bringImageFile = new BringImageFile(imageRoute);
                bringImageFile.start();
                try {
                    bringImageFile.join();
                    //이미지 불러오기 완료되면 가져오기
                    Bitmap bitmap = bringImageFile.getBitmap();
                    imageView_photo.setImageBitmap(bitmap);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
                //----------------------------------------------------
            }

        }

        return mCalloutBalloon;
    }

    @Override
    public View getPressedCalloutBalloon(MapPOIItem mapPOIItem) {
        return null;
    }
}
