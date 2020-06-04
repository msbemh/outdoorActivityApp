package com.action.outdooractivityapp.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.action.outdooractivityapp.activity.MainActivity;
import com.action.outdooractivityapp.activity.RoomChatActivity;
import com.action.outdooractivityapp.util.Util;

import java.util.List;
import java.util.Map;

public class RVRoomAdapter extends RecyclerView.Adapter<RVRoomAdapter.ViewHolder> {
    private Context context;
    private List<Map> items;
    private int itemLayout;
    private Intent intent;
    private AlertDialog dialog;
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

        String password = items.get(position).get("password").toString();
        final int roomNo = Integer.parseInt(items.get(position).get("room_no").toString());
        //패스워드 있는 방일때
        if(!TextUtils.isEmpty(password)){
            holder.image_key.setVisibility(View.VISIBLE);
            holder.itemView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    //-----------패스워드 확인 다이얼로그 생성------------------------------------------------------
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setIcon(R.drawable.icon_warning);
                    builder.setTitle("비밀번호");
                    builder.setMessage("방의 비밀번호를 입력해주세요.");
                    // EditText 삽입하기
                    final EditText editText = new EditText(context);
                    editText.setHint("비밀번호 입력");
                    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    builder.setView(editText);
                    builder.setCancelable(false);
                    builder.setPositiveButton("취소",null);
                    builder.setNegativeButton("확인", null);

                    dialog = builder.create();
                    dialog.show();
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Text 값 받아서 로그 남기기
                            String password = editText.getText().toString();
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
            holder.image_key.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    intent = new Intent(context, RoomChatActivity.class);
                    intent.putExtra("room_no",items.get(position).get("room_no").toString());

                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //연속으로 2번 눌러도 activity가 2개 생성되지 않도록 하기위해서 사용.
                    context.startActivity(intent);
                }
            });
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

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView text_room_no;
        TextView text_user_num;
        TextView text_writer;
        TextView text_title;
        ImageView image_key;
        ViewHolder(View itemView) {
            super(itemView);
            text_room_no = (TextView)itemView.findViewById(R.id.text_room_no);
            text_user_num = (TextView)itemView.findViewById(R.id.text_user_num);
            text_writer = (TextView)itemView.findViewById(R.id.text_writer);
            text_title = (TextView)itemView.findViewById(R.id.text_title);
            image_key = (ImageView)itemView.findViewById(R.id.image_key);
        }
    }
}
