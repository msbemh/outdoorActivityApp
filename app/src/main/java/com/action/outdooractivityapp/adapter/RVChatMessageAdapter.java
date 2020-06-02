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

public class RVChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private List<Map> items;
    private int itemLayoutReceive;
    private int itemLayoutSend;
    private Intent intent;
    private static final String TAG = "RVChatMessageAdapter";

    public RVChatMessageAdapter(Context context, List<Map> items, int itemLayoutReceive, int itemLayoutSend) {
        this.context = context;
        this.items = items;
        this.itemLayoutReceive = itemLayoutReceive;
        this.itemLayoutSend = itemLayoutSend;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        Log.d(TAG,"onCreateViewHolder");
        View view = null;

        //viewType이 0이면, 나의 메시지 view
        if(viewType == 0){
            view = LayoutInflater.from(parent.getContext()).inflate(itemLayoutSend, parent, false);
            return new ViewHolderSend(view);
        //viewType이 1이면, 상대방의 메시지 view
        }else{
            view = LayoutInflater.from(parent.getContext()).inflate(itemLayoutReceive, parent, false);
            return new ViewHolderReceive(view);
        }

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        Log.d(TAG,"onBindViewHolder");

        String writer = items.get(position).get("writer").toString();
        String userId = LoginActivity.userMap.get("user_id").toString();

        //나의 메시지라면
        if(userId.equals(writer)){
            ViewHolderSend holderSend = (ViewHolderSend) holder;
            holderSend.textView_message.setText(items.get(position).get("message").toString());
            holderSend.textView_time.setText(items.get(position).get("creationDate").toString());
        //나의 메시지가 아니라면
        }else{
            ViewHolderReceive holderReceive = (ViewHolderReceive) holder;
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
                    holderReceive.image_profile.setImageBitmap(bitmap);
                }
                catch(InterruptedException e){
                    e.printStackTrace();
                }
            }else{
                //기본 프로필 이미지 보여주기
                holderReceive.image_profile.setImageResource(R.drawable.icon_profile_invert);
            }

            //----------------------------------------------------
            holderReceive.textView_nick_name.setText(items.get(position).get("nickName").toString());
            holderReceive.textView_message.setText(items.get(position).get("message").toString());
            holderReceive.textView_time.setText(items.get(position).get("creationDate").toString());
        }


    }

    @Override
    public int getItemCount() {
        if(items == null){
            return 0;
        }
        return items.size();

    }

    @Override
    public int getItemViewType(int position) {
        String writer = items.get(position).get("writer").toString();
        String userId = LoginActivity.userMap.get("user_id").toString();
        //나의 메시지라면 0
        if(userId.equals(writer)){
            return 0;
        //나의 메시지가 아니라면 1
        }else{
            return 1;
        }
    }

    //받는 메시지 view holder
    public class ViewHolderReceive extends RecyclerView.ViewHolder {
        CircleImageView image_profile;
        TextView textView_nick_name;
        TextView textView_message;
        TextView textView_time;
        ViewHolderReceive(View itemView) {
            super(itemView);
            image_profile = (CircleImageView)itemView.findViewById(R.id.image_profile);
            textView_nick_name = (TextView)itemView.findViewById(R.id.textView_nick_name);
            textView_message = (TextView)itemView.findViewById(R.id.textView_message);
            textView_time = (TextView)itemView.findViewById(R.id.textView_time);
        }
    }

    //보내는 메시지 view holder
    public class ViewHolderSend extends RecyclerView.ViewHolder {
        TextView textView_message;
        TextView textView_time;
        ViewHolderSend(View itemView) {
            super(itemView);
            textView_message = (TextView)itemView.findViewById(R.id.textView_message);
            textView_time = (TextView)itemView.findViewById(R.id.textView_time);
        }
    }
}
