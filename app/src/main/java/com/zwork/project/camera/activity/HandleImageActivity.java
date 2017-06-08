package com.zwork.project.camera.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.zwork.project.camera.R;
import com.zwork.project.camera.utils.StickerView;
import com.zwork.project.camera.utils.ImageUtil;

import java.io.IOException;

public class HandleImageActivity extends Activity {

    ImageView imageView;
    StickerView stickerView;
    Bitmap alphaBitmap;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //状态栏透明
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_handle_image);
        initView();
    }

    private void initView(){
        imageView = (ImageView) findViewById(R.id.imageView);

        String path=getIntent().getStringExtra("filePath");

        if(stickerView==null){stickerView = new StickerView(this);} //防止restart后重复定义
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.image);
        params.addRule(RelativeLayout.ALIGN_TOP, R.id.image);
        ((ViewGroup)imageView.getParent()).addView(stickerView, params);
        alphaBitmap= BitmapFactory.decodeFile(path);
        alphaBitmap= ImageUtil.comp(alphaBitmap);
        imageView.setImageBitmap(alphaBitmap);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cover);
        stickerView.setWaterMark(bitmap);
    }

    public void nextPhoto(View view){
        dialog = ProgressDialog.show(this,"","加载中...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap newbmp = Bitmap.createBitmap(alphaBitmap.getWidth(), alphaBitmap.getHeight(), Bitmap.Config.ARGB_4444);
                Canvas cv = new Canvas(newbmp);
                cv.drawBitmap(alphaBitmap, 0, 0, null);
                cv.drawBitmap(stickerView.getBitmap(), 0,0, null);
                cv.save(Canvas.ALL_SAVE_FLAG);
                cv.restore();
                alphaBitmap = ImageUtil.compressImage(newbmp);
                try {
                    ImageUtil.saveBitmap(alphaBitmap,"temp.png");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                handler.sendEmptyMessage(0);
            }
        }).start();

    }

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            dialog.dismiss();
            Intent toNextPho=new Intent(HandleImageActivity.this,NextPhotoActivity.class);
            float[] point = stickerView.getmPoints();
            toNextPho.putExtra("point",point);
            startActivity(toNextPho);
        }
    };
}
