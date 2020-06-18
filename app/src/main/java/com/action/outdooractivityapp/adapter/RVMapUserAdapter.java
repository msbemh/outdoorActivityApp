package com.action.outdooractivityapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.urlConnection.BringImageFile;

import net.daum.mf.map.api.CameraUpdateFactory;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import java.util.List;
import java.util.Map;

public class RVMapUserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private List<Map> items;
    private int itemLayoutMapUser;
    private Intent intent;
    private static final String TAG = "RVMapUserAdapter";
    private MapView mapView;

    public RVMapUserAdapter(Context context, List<Map> items, int itemLayoutMapUser, MapView mapView) {
        this.context = context;
        this.items = items;
        this.itemLayoutMapUser = itemLayoutMapUser;
        this.mapView = mapView;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        Log.d(TAG,"onCreateViewHolder");
        View view = LayoutInflater.from(parent.getContext()).inflate(itemLayoutMapUser, parent, false);
        return new ViewHolderMapUser(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        Log.d(TAG,"onBindViewHolder");
        //데이터 추출
        String user_id = items.get(position).get("userId").toString();
        String nick_name = items.get(position).get("nickName").toString();
        final double longitude = Double.parseDouble(items.get(position).get("longitude").toString());
        final double latitude = Double.parseDouble(items.get(position).get("latitude").toString());

        //데이터 셋팅
        ViewHolderMapUser viewHolder = (ViewHolderMapUser) holder;
        viewHolder.textView_user_id.setText(user_id);
        viewHolder.textView_nick_name.setText(nick_name);

        //------이미지 파일 서버에서 Bitmap으로 가져오기-------
        String profileImage = items.get(position).get("profileImage").toString();
        Log.d(TAG,"profileImage:"+profileImage);
        if(!"null".equals(profileImage) && !TextUtils.isEmpty(profileImage) && profileImage != null){
            BringImageFile bringImageFile = new BringImageFile(profileImage);

            bringImageFile.start();
            try{
                bringImageFile.join();
                //이미지 불러오기 완료되면 가져오기
                Bitmap bitmap = bringImageFile.getBitmap();
                viewHolder.image_profile.setImageBitmap(bitmap);
            }
            catch(InterruptedException e){
                e.printStackTrace();
            }
        }else{
            //기본 프로필 이미지 보여주기
            viewHolder.image_profile.setImageResource(R.drawable.icon_profile_invert);
        }
        //----------------------------------------------------

        viewHolder.itemView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //지도 카메라 이동
                Log.d(TAG, "longitude:"+longitude);
                Log.d(TAG, "latitude:"+latitude);
                MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude);
                //카메라이동
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

    public class ViewHolderMapUser extends RecyclerView.ViewHolder {
        ImageView image_profile;
        TextView textView_user_id;
        TextView textView_nick_name;
        ViewHolderMapUser(View itemView) {
            super(itemView);
            image_profile = (ImageView)itemView.findViewById(R.id.image_profile);
            textView_user_id = (TextView)itemView.findViewById(R.id.textView_user_id);
            textView_nick_name = (TextView)itemView.findViewById(R.id.textView_nick_name);
        }
    }


    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }
}
