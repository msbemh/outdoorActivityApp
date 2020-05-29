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
import com.action.outdooractivityapp.activity.RoomChatActivity;

import java.util.List;
import java.util.Map;

public class RVRoomAdapter extends RecyclerView.Adapter<RVRoomAdapter.ViewHolder> {
    private Context context;
    private List<Map> items;
    private int itemLayout;
    private Intent intent;
    private static final String TAG = "RVRoomAdapter";

    public RVRoomAdapter(Context context, List<Map> items, int itemLayout) {
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
        holder.text_room_no.setText(items.get(position).get("room_no").toString());
        holder.text_user_num.setText(0+"");
        holder.text_writer.setText(items.get(position).get("writer").toString());
        holder.text_title.setText(items.get(position).get("title").toString());

        holder.itemView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                intent = new Intent(context, RoomChatActivity.class);
                intent.putExtra("room_no",items.get(position).get("room_no").toString());

                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //연속으로 2번 눌러도 activity가 2개 생성되지 않도록 하기위해서 사용.
                context.startActivity(intent);
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View v) {
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        if(items == null){
            return 0;
        }
        return items.size();

    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView text_room_no;
        TextView text_user_num;
        TextView text_writer;
        TextView text_title;
        ViewHolder(View itemView) {
            super(itemView);
            text_room_no = (TextView)itemView.findViewById(R.id.text_room_no);
            text_user_num = (TextView)itemView.findViewById(R.id.text_user_num);
            text_writer = (TextView)itemView.findViewById(R.id.text_writer);
            text_title = (TextView)itemView.findViewById(R.id.text_title);
        }
    }
}
