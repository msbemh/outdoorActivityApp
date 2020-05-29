package com.action.outdooractivityapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.activity.LoginActivity;
import com.action.outdooractivityapp.activity.RoomChatActivity;

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
//        holder.image_profile.setImageBitmap(LoginActivity.profileImage);
//        holder.textView_nick_name.setText(0+"");
        holder.textView_message.setText(items.get(position).get("message").toString());
//        holder.textView_time.setText(items.get(position).get("writer").toString());

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
