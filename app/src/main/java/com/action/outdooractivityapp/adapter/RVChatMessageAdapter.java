package com.action.outdooractivityapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.activity.LoginActivity;
import com.action.outdooractivityapp.activity.RoomChatActivity;
import com.action.outdooractivityapp.urlConnection.BringImageFile;

import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class RVChatMessageAdapter extends RecyclerView.Adapter<RVChatMessageAdapter.ViewHolder> {
    private Context context;
    private List<Map> items;
    private int itemLayout;
    private Intent intent;
    private static final String TAG = "RVChatMessageAdapter";

    public RVChatMessageAdapter(Context context, List<Map> items, int itemLayout) {
        this.context = context;
        this.items = items;
        this.itemLayout = itemLayout;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        Log.d(TAG,"onCreateViewHolder");
        View v = LayoutInflater.from(parent.getContext()).inflate(itemLayout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        Log.d(TAG,"onBindViewHolder");

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
                holder.image_profile.setImageBitmap(bitmap);
            }
            catch(InterruptedException e){
                e.printStackTrace();
            }
        }else{
            //기본 프로필 이미지 보여주기
            holder.image_profile.setImageResource(R.drawable.icon_profile_invert);
        }

        //----------------------------------------------------
        holder.textView_nick_name.setText(items.get(position).get("nickName").toString());
        holder.textView_message.setText(items.get(position).get("message").toString());
        holder.textView_time.setText(items.get(position).get("creationDate").toString());

    }

    @Override
    public int getItemCount() {
        if(items == null){
            return 0;
        }
        return items.size();

    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView image_profile;
        TextView textView_nick_name;
        TextView textView_message;
        TextView textView_time;
        ViewHolder(View itemView) {
            super(itemView);
            image_profile = (CircleImageView)itemView.findViewById(R.id.image_profile);
            textView_nick_name = (TextView)itemView.findViewById(R.id.textView_nick_name);
            textView_message = (TextView)itemView.findViewById(R.id.textView_message);
            textView_time = (TextView)itemView.findViewById(R.id.textView_time);
        }
    }
}
