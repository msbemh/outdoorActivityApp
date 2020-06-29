package com.action.outdooractivityapp.abstact;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.action.outdooractivityapp.R;
import com.action.outdooractivityapp.util.Util;

public abstract class Loader extends AsyncTask<String, String, String> {

    private Context context;
    private ProgressDialog progressDialog;
    private boolean finalResult = true;
    private String TAG = "Loader";
    private String text = "";
    private Bitmap bitmap;
    private View view;

    public Loader(Context context, String text){
        this.context = context;
        this.text = text;
    }

    protected abstract void run();

    //AsyncTask 동작되기 전에 실행
    @Override
    protected void onPreExecute() {
        Log.d(TAG,"onPreExecute 동작");
        super.onPreExecute();
        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle(text);
        progressDialog.setMessage(text);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setIndeterminate(false);
        progressDialog.show();
    }

    @Override
    protected String doInBackground(String... strings) {
        Log.d(TAG,"백그라운드 동작");
        run();
        return null;
    }

    //메시지를 수신받을때 동작해서 UI변경
    @Override
    protected void onProgressUpdate(String... strings) {
        super.onProgressUpdate();
        Log.d(TAG, "onProgressUpdate");
    }

    //doInBackground메소드가 종료된 후 호출됨.
    @Override
    protected void onPostExecute(String string) {
        Log.d(TAG,"onPostExecute 동작");
        super.onPostExecute(string);
        if(finalResult){
            Util.toastText(context,"저장이 완료됐습니다.");
        }else{
            Util.toastText(context,"저장에 실패했습니다.");
        }

        if(bitmap != null){
            ImageView imageView = (ImageView) this.view;
            imageView.setImageResource(R.drawable.ic_launcher_background);
        }

        //프로그래스 대화상장 끄기
        progressDialog.dismiss();
    }

    public void setBitmap(Bitmap bitmap){
        this.bitmap = bitmap;
    }

    public void setView(View view){
        this.view = view;
    }
}
