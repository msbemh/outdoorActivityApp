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
import com.action.outdooractivityapp.activity.RoomChatActivity;
import com.action.outdooractivityapp.activity.TrackingBoardViewActivity;
import com.action.outdooractivityapp.urlConnection.BringImageFile;

import net.daum.mf.map.api.CameraUpdateFactory;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import java.util.List;
import java.util.Map;

public class RVTrackingBoardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private List<Map> items;
    private int itemLayoutTrackingBoard;
    private Intent intent;
    private static final String TAG = "RVTrackingBoardAdapter";

    public RVTrackingBoardAdapter(Context context, List<Map> items, int itemLayoutTrackingBoard) {
        this.context = context;
        this.items = items;
        this.itemLayoutTrackingBoard = itemLayoutTrackingBoard;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        Log.d(TAG,"onCreateViewHolder");
        View view = LayoutInflater.from(parent.getContext()).inflate(itemLayoutTrackingBoard, parent, false);
        return new ViewHolderTrackingBoard(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        Log.d(TAG,"onBindViewHolder");
        //데이터 추출
//        String title = items.get(position).get("title").toString();
        String user_id = items.get(position).get("user_id").toString();
        String nick_name = items.get(position).get("nick_name").toString();
        String location = items.get(position).get("location").toString();
        String create_date = items.get(position).get("create_date").toString();
        final int tracking_no = Integer.parseInt(items.get(position).get("tracking_no").toString());

        //데이터 셋팅
        ViewHolderTrackingBoard viewHolder = (ViewHolderTrackingBoard) holder;
//        viewHolder.textView_title.setText(title);
        viewHolder.textView_user_id.setText(user_id);
        viewHolder.textView_nick_name.setText(nick_name);
        viewHolder.textView_creation_date.setText(create_date);

        //------이미지 파일 서버에서 Bitmap으로 가져오기-------
//        String thumbnailImage = items.get(position).get("imageView_tracking_thumbnail").toString();
//        Log.d(TAG,"thumbnailImage:"+thumbnailImage);
//        if(!"null".equals(thumbnailImage) && !TextUtils.isEmpty(thumbnailImage) && thumbnailImage != null){
//            BringImageFile bringImageFile = new BringImageFile(thumbnailImage);
//
//            bringImageFile.start();
//            try{
//                bringImageFile.join();
//                //이미지 불러오기 완료되면 가져오기
//                Bitmap bitmap = bringImageFile.getBitmap();
//                viewHolder.imageView_tracking_thumbnail.setImageBitmap(bitmap);
//            }
//            catch(InterruptedException e){
//                e.printStackTrace();
//            }
//        }else{
//            //기본 프로필 이미지 보여주기
//            viewHolder.imageView_tracking_thumbnail.setImageResource(R.drawable.icon_route);
//        }
        //----------------------------------------------------

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent(context, TrackingBoardViewActivity.class);
                intent.putExtra("tracking_no", tracking_no);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //연속으로 2번 눌러도 activity가 2개 생성되지 않도록 하기위해서 사용.
                context.startActivity(intent);
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
        ImageView imageView_tracking_thumbnail;
        TextView textView_title;
        TextView textView_user_id;
        TextView textView_nick_name;
        TextView textView_creation_date;
        ViewHolderTrackingBoard(View itemView) {
            super(itemView);
            imageView_tracking_thumbnail = (ImageView)itemView.findViewById(R.id.imageView_tracking_thumbnail);
            textView_title = (TextView)itemView.findViewById(R.id.textView_title);
            textView_user_id = (TextView)itemView.findViewById(R.id.textView_user_id);
            textView_nick_name = (TextView)itemView.findViewById(R.id.textView_nick_name);
            textView_creation_date = (TextView)itemView.findViewById(R.id.textView_creation_date);
        }
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }
}
