package com.action.outdooractivityapp.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.action.outdooractivityapp.AdminApplication;
import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.urlConnection.BringImageFile;
import com.action.outdooractivityapp.util.Util;

import net.daum.mf.map.api.CalloutBalloonAdapter;
import net.daum.mf.map.api.MapPOIItem;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CustomCalloutBalloonAdapter implements CalloutBalloonAdapter {

    private final View mCalloutBalloon;
    private Context context;
    private List<Map> trackingPhotoList;

    private String TAG = "CustomCalloutBalloonAdapter";

    public CustomCalloutBalloonAdapter(Context context, List<Map> trackingPhotoList) {
        this.context = context;
        this.trackingPhotoList = trackingPhotoList;
        mCalloutBalloon = ((Activity)context).getLayoutInflater().inflate(R.layout.custom_callout_balloon, null);
    }

    @Override
    public View getCalloutBalloon(MapPOIItem mapPOIItem) {
        Log.d(TAG,"name:"+mapPOIItem.getItemName());
        Log.d(TAG,"tag:"+mapPOIItem.getTag());
        int marker_tag = mapPOIItem.getTag();
        //tag(0):시작위치
        //tag(1~쭈욱):사진
        //시작위치일 경우
        if(marker_tag == 0){
            ((TextView) mCalloutBalloon.findViewById(R.id.textView_title)).setText(mapPOIItem.getItemName());
            ((TextView) mCalloutBalloon.findViewById(R.id.textView_time)).setText("날짜");
            ((ImageView) mCalloutBalloon.findViewById(R.id.imageView_photo)).setVisibility(View.GONE);
        //사진리스트일 경우
        }else if(marker_tag > 0){
            ((TextView) mCalloutBalloon.findViewById(R.id.textView_title)).setText(mapPOIItem.getItemName());
            ((TextView) mCalloutBalloon.findViewById(R.id.textView_time)).setText("날짜");
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
                ((ImageView) mCalloutBalloon.findViewById(R.id.imageView_photo)).setVisibility(View.GONE);;
            //이미지가 있을 경우
            }else{
                ((ImageView) mCalloutBalloon.findViewById(R.id.imageView_photo)).setVisibility(View.VISIBLE);;
                Log.d(TAG,"imageRoute:"+imageRoute);
                //절대경로 => 비트맵
                Bitmap bitmap = BitmapFactory.decodeFile(imageRoute);
                // 이미지를 상황에 맞게 회전시킨다
                if(imageRoute != null){
                    ExifInterface exif = null;
                    try {
                        exif = new ExifInterface(imageRoute);
                        int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                        Log.d(TAG,"exifOrientation:"+exifOrientation);
                        int exifDegree = Util.exifOrientationToDegrees(exifOrientation);
                        Log.d(TAG,"exifDegree:"+exifDegree);
                        bitmap = Util.rotate(bitmap, exifDegree);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                ((ImageView) mCalloutBalloon.findViewById(R.id.imageView_photo)).setImageBitmap(bitmap);
            }

        }

        return mCalloutBalloon;
    }

    @Override
    public View getPressedCalloutBalloon(MapPOIItem mapPOIItem) {
        return null;
    }
}
