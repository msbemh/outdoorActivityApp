package com.action.outdooractivityapp.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Movie;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.activity.LoginActivity;
import com.action.outdooractivityapp.activity.MainActivity;
import com.action.outdooractivityapp.activity.RoomChatActivity;
import com.action.outdooractivityapp.util.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RVRoomAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private List<Map> items;
    private int itemLayoutRoom;
    private int itemLayoutLoading;
    private Intent intent;
    private AlertDialog dialog;
    private static final String TAG = "RVRoomAdapter";

    //페이징 로딩관련
    private static final int VIEW_TYPE_LOADING = 0;
    private static final int VIEW_TYPE_NORMAL = 1;
    private boolean isLoaderVisible = false;

    public RVRoomAdapter(Context context, List<Map> items, int itemLayoutRoom, int itemLayoutLoading) {
        this.context = context;
        this.items = items;
        this.itemLayoutRoom = itemLayoutRoom;
        this.itemLayoutLoading = itemLayoutLoading;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        Log.d(TAG,"onCreateViewHolder");
        View view = null;

        //viewType이 0이면, 로딩바 view
        if(viewType == 0){
            view = LayoutInflater.from(parent.getContext()).inflate(itemLayoutLoading, parent, false);
            return new ViewHolderLoading(view);
        //viewType이 1이면, 방 view
        }else{
            view = LayoutInflater.from(parent.getContext()).inflate(itemLayoutRoom, parent, false);
            return new ViewHolderRoom(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        Log.d(TAG,"onBindViewHolder");
        //로딩바가 존재할때
        if (items.get(position).get("loading") !=null && Boolean.parseBoolean(items.get(position).get("loading").toString()) == true) {
            Log.d(TAG,"View 로딩바 바인딩!");
//            ViewHolderLoading holderLoading = (ViewHolderLoading) holder;
        //로딩바가 존재하지 않을때
        } else {
            Log.d(TAG,"View Room 바인딩!");
            ViewHolderRoom holderRoom = (ViewHolderRoom) holder;
            holderRoom.text_room_no.setText(items.get(position).get("room_no").toString());
            holderRoom.text_user_num.setText(0+"");
            holderRoom.text_writer.setText(items.get(position).get("writer").toString());
            holderRoom.text_title.setText(items.get(position).get("title").toString());

            String password = items.get(position).get("password").toString();
            final int roomNo = Integer.parseInt(items.get(position).get("room_no").toString());
            //패스워드 있는 방일때
            if(!TextUtils.isEmpty(password)){
                holderRoom.image_key.setVisibility(View.VISIBLE);
                holderRoom.itemView.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        //-----------패스워드 확인 다이얼로그 생성------------------------------------------------------
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setIcon(R.drawable.icon_warning);
                        builder.setTitle("비밀번호");
                        builder.setMessage("방의 비밀번호를 입력해주세요.");
                        //xml파일에서 view가져오기
                        builder.setView(R.layout.dialog_password_check);
                        builder.setCancelable(false);
                        builder.setPositiveButton("취소",null);
                        builder.setNegativeButton("확인", null);

                        dialog = builder.create();
                        dialog.show();
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Text 값 받아서 로그 남기기
                                EditText editText_password = dialog.findViewById(R.id.editText_password);
                                String password = editText_password.getText().toString();
                                //-----------------http로 DB와 연동하여 해당방의 비밀번호가 맞는지 확인--------------------
                                String url = "https://wowoutdoor.tk/room/room_password_check.php";
                                String parameters = "password="+password+"&roomNo="+roomNo;
                                String method = "GET";

                                if(TextUtils.isEmpty(password)){
                                    Util.toastText(context,"패스워드를 입력해주세요");
                                    return;
                                }

                                //데이터 베이스에서 정보를 가져옴
                                List<Map> resultList = Util.httpConn(url, parameters, method);
                                //cnt : 0  => 방의 비밀번호가 다름
                                //cnt : 1  => 방의 비밀번호가 맞음
                                int cnt = Integer.parseInt(resultList.get(0).get("cnt").toString());

                                if(cnt == 0){
                                    editText_password.setText("");
                                    Util.toastText(context,"패스워드가 틀립니다.");
                                }else if(cnt == 1){
                                    intent = new Intent(context, RoomChatActivity.class);
                                    intent.putExtra("room_no",items.get(position).get("room_no").toString());

                                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //연속으로 2번 눌러도 activity가 2개 생성되지 않도록 하기위해서 사용.
                                    context.startActivity(intent);
                                    dialog.dismiss();
                                }
                                //-------------------------------------------------------------------------------------
                            }
                        });
                        //---------------------------------------------------------------------------------------------
                    }
                });
                //패스워드 없는 방일때
            }else{
                holderRoom.image_key.setVisibility(View.GONE);
                holderRoom.itemView.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        intent = new Intent(context, RoomChatActivity.class);
                        intent.putExtra("room_no",items.get(position).get("room_no").toString());

                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //연속으로 2번 눌러도 activity가 2개 생성되지 않도록 하기위해서 사용.
                        context.startActivity(intent);
                    }
                });
            }
        }

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

    @Override
    public int getItemViewType(int position) {
        //로딩바가 존재할때
        if (isLoaderVisible) {
            //position이 마지막을 경우 VIEW_TYPE_LOADING(0)반환 => 로딩바 view만들기
            return position == items.size() - 1 ? VIEW_TYPE_LOADING : VIEW_TYPE_NORMAL;
        //로딩바가 존재하지 않을때
        } else {
            //그냥 VIEW_TYPE_NORMAL(1)반환 => 방 view만들기
            return VIEW_TYPE_NORMAL;
        }
    }

    public class ViewHolderRoom extends RecyclerView.ViewHolder {
        TextView text_room_no;
        TextView text_user_num;
        TextView text_writer;
        TextView text_title;
        ImageView image_key;
        ViewHolderRoom(View itemView) {
            super(itemView);
            text_room_no = (TextView)itemView.findViewById(R.id.text_room_no);
            text_user_num = (TextView)itemView.findViewById(R.id.text_user_num);
            text_writer = (TextView)itemView.findViewById(R.id.text_writer);
            text_title = (TextView)itemView.findViewById(R.id.text_title);
            image_key = (ImageView)itemView.findViewById(R.id.image_key);
        }
    }

    public class ViewHolderLoading extends RecyclerView.ViewHolder {
        ViewHolderLoading(View itemView) {
            super(itemView);
        }
    }

    public void addItem(Map map) {
        Log.d(TAG,"addItem()동작");
        items.add(0, map);
        notifyItemInserted(0);
    }

    public void addItems(List<Map> roomList) {
        Log.d(TAG,"addItems()동작");
        for(Map itemMap : roomList){
            Log.d(TAG,"itemMap:"+itemMap.get("title"));
        }
        items.addAll(roomList);
        notifyDataSetChanged();
    }

    public void removeLoading() {
        Log.d(TAG,"removeLoading()동작");
        isLoaderVisible = false; //로딩바 없음 표시
        int position = items.size() -1;
        if( items.size() > 0){
            items.remove(position);
            notifyItemRemoved(position);
        }

    }

    public void addLoading() {
        Log.d(TAG,"addLoading()동작");
        isLoaderVisible = true; //로딩바 있음 표시
        Map map = new HashMap();
        map.put("loading",true);
        items.add(map);
        notifyItemInserted(items.size() - 1);
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }
}
