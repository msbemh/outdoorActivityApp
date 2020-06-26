package com.action.outdooractivityapp.adapter;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.action.outdooractivityapp.R;

import net.daum.mf.map.api.CalloutBalloonAdapter;
import net.daum.mf.map.api.MapPOIItem;

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
        int tag = mapPOIItem.getTag();
        //tag(0):시작위치
        //tag(1~쭈욱):사진
        if(tag == 0){
            ((TextView) mCalloutBalloon.findViewById(R.id.textView_title)).setText(mapPOIItem.getItemName());
            ((TextView) mCalloutBalloon.findViewById(R.id.textView_time)).setText("날짜");
            ((ImageView) mCalloutBalloon.findViewById(R.id.imageView_photo)).setVisibility(View.GONE);
        }else if(tag > 0){
            ((TextView) mCalloutBalloon.findViewById(R.id.textView_title)).setText(mapPOIItem.getItemName());
            ((TextView) mCalloutBalloon.findViewById(R.id.textView_time)).setText("날짜");
            String imageRoute = null;
            for(Map mapItem : trackingPhotoList){
                int order = Integer.parseInt(mapItem.get("order").toString());
                if(tag == order){
                    imageRoute = mapItem.get("photo_image").toString();
                }
            }
            //이미지가 없을 경우
            if(imageRoute == null){
                ((ImageView) mCalloutBalloon.findViewById(R.id.imageView_photo)).setImageResource(R.drawable.background);
            //이미지가 있을 경우
            }else{
                ((ImageView) mCalloutBalloon.findViewById(R.id.imageView_photo)).setImageResource(R.drawable.background);
            }

        }

        return mCalloutBalloon;
    }

    @Override
    public View getPressedCalloutBalloon(MapPOIItem mapPOIItem) {
        return null;
    }
}
