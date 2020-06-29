package com.action.outdooractivityapp.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.urlConnection.BringImageFile;

import net.daum.mf.map.api.CameraUpdateFactory;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import java.util.List;
import java.util.Map;

public class RVTrackingPhotoListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private List<Map> items;
    private int itemLayoutTrackingPhotoList;
    private MapView mapView;
    private static final String TAG = "RVTrackingPhotoAdapter";

    public RVTrackingPhotoListAdapter(Context context, List<Map> items, int itemLayoutTrackingPhotoList, MapView mapView) {
        this.context = context;
        this.items = items;
        this.itemLayoutTrackingPhotoList = itemLayoutTrackingPhotoList;
        this.mapView = mapView;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        Log.d(TAG,"onCreateViewHolder");
        View view = LayoutInflater.from(parent.getContext()).inflate(itemLayoutTrackingPhotoList, parent, false);
        return new ViewHolderTrackingBoard(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        Log.d(TAG,"onBindViewHolder");
        //데이터 추출
        final int tracking_no = Integer.parseInt(items.get(position).get("tracking_no").toString());
        final double latitude = Double.parseDouble(items.get(position).get("latitude").toString());
        final double longitude =  Double.parseDouble(items.get(position).get("longitude").toString());
        String photoImage = items.get(position).get("photo_image").toString();
        int tag = Integer.parseInt(items.get(position).get("tag").toString());

        //데이터 셋팅
        ViewHolderTrackingBoard viewHolder = (ViewHolderTrackingBoard) holder;

        //------이미지 파일 서버에서 Bitmap으로 가져오기-------
        Log.d(TAG,"photoImage:"+photoImage);
        if(!"null".equals(photoImage) && !TextUtils.isEmpty(photoImage) && photoImage != null){
            BringImageFile bringImageFile = new BringImageFile(photoImage);

            bringImageFile.start();
            try{
                bringImageFile.join();
                //이미지 불러오기 완료되면 가져오기
                Bitmap bitmap = bringImageFile.getBitmap();
                Log.d(TAG,"bitmap:"+bitmap);
                viewHolder.imageView_photo.setImageBitmap(bitmap);
            }
            catch(InterruptedException e){
                e.printStackTrace();
            }
        }else{
            //기본 이미지 보여주기
            viewHolder.imageView_photo.setImageResource(R.drawable.background);
        }
        //----------------------------------------------------

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //해당 위치로 map카메라 이동
                //마지막 위치로 카메라 이동
                MapPoint mapPoint =  MapPoint.mapPointWithGeoCoord(latitude, longitude);
                mapView.moveCamera(CameraUpdateFactory.newMapPoint(mapPoint));
            }
        });


    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "items.size():"+items.size());
        if(items == null){
            return 0;
        }
        return items.size();

    }

    public class ViewHolderTrackingBoard extends RecyclerView.ViewHolder {
        ImageView imageView_photo;
        ViewHolderTrackingBoard(View itemView) {
            super(itemView);
            imageView_photo = (ImageView)itemView.findViewById(R.id.imageView_photo);
        }
    }

}
