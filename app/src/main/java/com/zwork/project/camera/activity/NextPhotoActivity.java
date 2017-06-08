package com.zwork.project.camera.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.zwork.project.camera.R;
import com.zwork.project.camera.camera.CameraPreview;
import com.zwork.project.camera.utils.ImageUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class NextPhotoActivity extends Activity implements Camera.PreviewCallback{
    private Camera mCamera;
    private CameraPreview mPreview;
    private FrameLayout preview;
    private View focusIndex;
    final public static String TAG="FirstPhoActivityError";

    private float pointX,pointY;
    private float oldDist = 1f;
    private Handler handler = new Handler();

    private Bitmap compareBitmap;
    Button btn0;
    private float[] mP;

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

        setContentView(R.layout.activity_next_photo);

        focusIndex = findViewById(R.id.focus_index);
        startCamera();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(cThread==null){
            cThread=new CThread();
            cThread.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        flag=false;
        releaseCamera();
        preview.removeView(mPreview);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        startCamera();
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            Log.d(TAG, "Error opening camera: " + e.getMessage());// Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private void startCamera(){
        mCamera=getCameraInstance();
        initParameters();
        mPreview=new CameraPreview(this,mCamera);
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        ImageView imageView= (ImageView) findViewById(R.id.alphaPic);
        btn0= (Button) findViewById(R.id.btn0);
        String fileName="/sdcard/Pictures/temp.png" ;
        Uri uri = Uri.fromFile(new File(fileName));
        imageView.setImageURI(uri);
        imageView.setAlpha(0.5f);
        compareBitmap = BitmapFactory.decodeFile(fileName);
        mP=getIntent().getFloatArrayExtra("point");
        flag=true;

        preview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getPointerCount() == 1) {
                    pointX=event.getX();
                    pointY=event.getY();
                    pointFocus((int)pointX,(int)pointY);

                    RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(focusIndex.getLayoutParams());
                    layout.setMargins((int) pointX - 60, (int) pointY - 60, 0, 0);
                    focusIndex.setLayoutParams(layout);
                    focusIndex.setVisibility(View.VISIBLE);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            focusIndex.setVisibility(View.INVISIBLE);
                        }
                    }, 800);


                } else {
                    switch (event.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_POINTER_DOWN:
                            oldDist = getFingerSpacing(event);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            float newDist = getFingerSpacing(event);
                            if (newDist > oldDist) {
                                handleZoom(true, mCamera);
                            } else if (newDist < oldDist) {
                                handleZoom(false, mCamera);
                            }
                            oldDist = newDist;
                            break;
                    }
                }
                return true;
            }
        });

    }

    public void releaseCamera(){
        if(mCamera!=null){
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }


    public void initParameters(){
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPictureSize(2560,1440);
        parameters.setPreviewSize(1920,1080);
//        List<Camera.Size> list = parameters.getSupportedPreviewSizes();
//        for(int i=0;i<list.size();i++)
//            Log.d("123",list.get(i).width+" "+list.get(i).height);

        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        mCamera.setParameters(parameters);
    }

    public void doCapture(View view){
        mCamera.takePicture(null, null, mPicture);
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = ImageUtil.getOutputMediaFile(ImageUtil.MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                Bitmap bitmap= BitmapFactory.decodeByteArray(data , 0, data.length);
                Matrix matrix = new Matrix();
                matrix.setRotate(90);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos))
                {
                    fos.flush();
                    fos.close();
                }
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }

            Intent toGallery = new Intent(NextPhotoActivity.this,GalleryActivity.class);
            toGallery.putExtra("WhichActivity","gallery");
            startActivity(toGallery);
            NextPhotoActivity.this.finish();
        }
    };


    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void handleZoom(boolean isZoomIn, Camera camera) {
        Camera.Parameters params = camera.getParameters();
        if (params.isZoomSupported()) {
            int maxZoom = params.getMaxZoom();
            int zoom = params.getZoom();
            if (isZoomIn && zoom < maxZoom) {
                zoom++;
            } else if (zoom > 0) {
                zoom--;
            }
            params.setZoom(zoom);
            camera.setParameters(params);
        } else {
            Log.i(TAG, "zoom not supported");
        }
    }

    //定点对焦的代码
    private void pointFocus(int x, int y) {
        mCamera.cancelAutoFocus();
        Camera.Parameters parameters = mCamera.getParameters();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            showPoint(x, y);
        }
        mCamera.setParameters(parameters);
        autoFocus();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void showPoint(int x, int y) {
        Camera.Parameters parameters = mCamera.getParameters();
        Camera.Size previewSize = parameters.getPreviewSize();
        if (parameters.getMaxNumMeteringAreas() > 0) {
            List<Camera.Area> areas = new ArrayList<>();
            //xy变换了
            int rectY = -x * 2000 / previewSize.width + 1000;
            int rectX = y * 2000 / previewSize.height - 1000;

            int left = rectX < -900 ? -1000 : rectX - 100;
            int top = rectY < -900 ? -1000 : rectY - 100;
            int right = rectX > 900 ? 1000 : rectX + 100;
            int bottom = rectY > 900 ? 1000 : rectY + 100;
            Rect area1 = new Rect(left, top, right, bottom);
            areas.add(new Camera.Area(area1, 800));
            parameters.setMeteringAreas(areas);
        }

        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    }


    //实现自动对焦
    private void autoFocus() {
        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (mCamera == null) {
                    return;
                }
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (success) {
                            initParameters();
                        }
                    }
                });
            }
        };
    }



    private boolean flag;
    private CThread cThread;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        int width = parameters.getPreviewSize().width;
        int height = parameters.getPreviewSize().height;

        YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 50, out);

        byte[] bytes = out.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        Matrix matrix = new Matrix();
        matrix.setRotate(90);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        //Log.d("cms", bitmap.toString());
        float ans = isEquals(bitmap,compareBitmap);
        Log.d("cms",ans+"");
        if(ans>=0.001f){
            btn0.setClickable(true);
            btn0.setVisibility(View.VISIBLE);
        }else{
            btn0.setVisibility(View.INVISIBLE);
        }
        bitmap.recycle();
        bitmap=null;
    }

    class CThread extends Thread{

        public void run(){

            while(flag){
                mCamera.setOneShotPreviewCallback(NextPhotoActivity.this);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }

    }

    public float isEquals(Bitmap b1,Bitmap b2) {
        int haha = 0,sum = 0;
        int xLeft = 0, xRight = 0, yTop = 0, yBottom = 0;
        int tempNum1 = 0, tempNum2 = 0;
        int xCount = b1.getWidth();
        int yCount = b1.getHeight();

        tempNum1 = (int)Math.min(mP[0], mP[2]);
        tempNum2 = (int)Math.min(mP[4], mP[6]);
        xLeft = Math.min(tempNum1, tempNum2);
        xLeft = Math.max(0, xLeft);

        tempNum1 = (int)Math.max(mP[0], mP[2]);
        tempNum2 = (int)Math.max(mP[4], mP[6]);
        xRight = Math.max(tempNum1, tempNum2);
        xRight = Math.min(xCount, xRight);

        tempNum1 = (int)Math.min(mP[1], mP[3]);
        tempNum2 = (int)Math.min(mP[5], mP[7]);
        yBottom = Math.min(tempNum1, tempNum2);
        yBottom = Math.max(0, yBottom);

        tempNum1 = (int)Math.max(mP[1], mP[3]);
        tempNum2 = (int)Math.max(mP[5], mP[7]);
        yTop = Math.max(tempNum1, tempNum2);
        yTop = Math.min(yCount, yTop);

//        Log.d("cms",b2.getWidth()+" "+b2.getHeight());
//        Log.d("cms",b1.getWidth()+" "+b1.getHeight());
        if (b1.getWidth() == b2.getWidth()
                && b1.getHeight() == b2.getHeight()) {
            for (int x = 0; x < xLeft; x++) {
                for (int y = 0; y < yTop; y++) {
                    if (b1.getPixel(x, y) == b2.getPixel(x, y)) {
                        haha++;
                    }
                    sum++;
                }
            }
            for (int x = xRight; x < xCount; x++) {
                for (int y = 0; y < yTop; y++) {
                    if (b1.getPixel(x, y) == b2.getPixel(x, y)) {
                        haha++;
                    }
                    sum++;
                }
            }
            for (int y = yTop; y < yCount; y++) {
                for (int x = 0; x < xCount; x++) {
                    if (b1.getPixel(x, y) == b2.getPixel(x, y)) {
                        haha++;
                    }
                    sum++;
                }
            }
            for (int y = 0; y < yBottom; y++) {
                for (int x = xLeft; x < xRight; x++) {
                    if (b1.getPixel(x, y) == b2.getPixel(x, y)) {
                        haha++;
                    }
                    sum++;
                }
            }
        }
        //Log.d("cms","sum:"+sum+" haha:"+haha);

        float ret = (float)haha/sum;

        return ret;
    }

}
