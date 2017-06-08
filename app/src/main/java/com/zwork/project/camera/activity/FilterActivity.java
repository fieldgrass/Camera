package com.zwork.project.camera.activity;

import android.app.Activity;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.zwork.project.camera.R;
import com.zwork.project.camera.utils.GPUImageFilterTools;
import com.zwork.project.camera.utils.RoundImageView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;


public class FilterActivity extends Activity implements
        View.OnClickListener, GPUImageView.OnPictureSavedListener {
//    private static final int REQUEST_PICK_IMAGE = 1;
    private GPUImageFilter mFilter;
    private GPUImageFilterTools.FilterAdjuster mFilterAdjuster;
    private GPUImageView mGPUImageView;
    private LinearLayout imageLL;
    private int[] filterString = {R.string.text_filter_normal,R.string.text_filter_in1977,R.string.text_filter_amaro,
            R.string.text_filter_brannan, R.string.text_filter_early_bird,R.string.text_filter_hefe, R.string.text_filter_hudson,
            R.string.text_filter_inkwell,R.string.text_filter_lomofi, R.string.text_filter_lord_kelvin,
            R.string.text_filter_early_bird,R.string.text_filter_rise,R.string.text_filter_sierra,
            R.string.text_filter_sutro,R.string.text_filter_toaster,R.string.text_filter_valencia,
            R.string.text_filter_walden,R.string.text_filter_xproii};
    //			"Contrast", "Brightness", "Sepia", "Vignette", "ToneCurve",
//			"Lookup (Amatorka)" };
    private int[] images = { R.mipmap.filter_normal,
            R.mipmap.filter_in1977, R.mipmap.filter_amaro,
            R.mipmap.filter_brannan, R.mipmap.filter_early_bird,
            R.mipmap.filter_hefe, R.mipmap.filter_hudson,
            R.mipmap.filter_inkwell, R.mipmap.filter_lomofi,
            R.mipmap.filter_lord_kelvin, R.mipmap.filter_nashville,
            R.mipmap.filter_rise, R.mipmap.filter_sierra,
            R.mipmap.filter_sutro, R.mipmap.filter_toaster,
            R.mipmap.filter_valencia, R.mipmap.filter_walden,
            R.mipmap.filter_xproii };
    private GPUImageFilterTools.FilterList filters = new GPUImageFilterTools.FilterList();
    private List<ImageView> selectList = new ArrayList<>();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        initView();
        initData();

//        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
//        photoPickerIntent.setType("image/*");
//        startActivityForResult(photoPickerIntent, REQUEST_PICK_IMAGE);

        Uri u =getIntent().getData();
        if(u!=null)
            handleImage(u);

    }

    private void initView(){
        //((SeekBar) findViewById(R.id.seekBar)).setOnSeekBarChangeListener(this);
        //	findViewById(R.id.button_choose_filter).setOnClickListener(this);
        findViewById(R.id.button_save).setOnClickListener(this);

        mGPUImageView = (GPUImageView) findViewById(R.id.gpuimage);
        imageLL = (LinearLayout) findViewById(R.id.images_layout);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;

        imageLL.removeAllViews();
        for (int i = 0; i < images.length; i++) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_image,
                    null);
            RoundImageView imageview = (RoundImageView) view
                    .findViewById(R.id.image_iv);
            TextView textView = (TextView) view.findViewById(R.id.image_tv);
            ImageView selectIV = (ImageView) view
                    .findViewById(R.id.filter_select);
            selectList.add(selectIV);
            imageview.setImageDrawable(getResources().getDrawable(images[i]));
            textView.setText(getResources().getString(filterString[i]));
            view.setOnClickListener(new ImageItemClick(i));
            imageLL.addView(view);

        }

        selectList.get(0).setVisibility(View.VISIBLE);


    }

    private void initData(){
        filters.addFilter("default", GPUImageFilterTools.FilterType.I_1977);
        filters.addFilter("1977", GPUImageFilterTools.FilterType.I_1977);
        filters.addFilter("Amaro", GPUImageFilterTools.FilterType.I_AMARO);
        filters.addFilter("Brannan", GPUImageFilterTools.FilterType.I_BRANNAN);
        filters.addFilter("Earlybird", GPUImageFilterTools.FilterType.I_EARLYBIRD);
        filters.addFilter("Hefe", GPUImageFilterTools.FilterType.I_HEFE);
        filters.addFilter("Hudson", GPUImageFilterTools.FilterType.I_HUDSON);
        filters.addFilter("Inkwell", GPUImageFilterTools.FilterType.I_INKWELL);
        filters.addFilter("Lomo", GPUImageFilterTools.FilterType.I_LOMO);
        filters.addFilter("LordKelvin", GPUImageFilterTools.FilterType.I_LORDKELVIN);
        filters.addFilter("Nashville", GPUImageFilterTools.FilterType.I_NASHVILLE);
        filters.addFilter("Rise", GPUImageFilterTools.FilterType.I_NASHVILLE);
        filters.addFilter("Sierra", GPUImageFilterTools.FilterType.I_SIERRA);
        filters.addFilter("sutro", GPUImageFilterTools.FilterType.I_SUTRO);
        filters.addFilter("Toaster", GPUImageFilterTools.FilterType.I_TOASTER);
        filters.addFilter("Valencia", GPUImageFilterTools.FilterType.I_VALENCIA);
        filters.addFilter("Walden", GPUImageFilterTools.FilterType.I_WALDEN);
        filters.addFilter("Xproll", GPUImageFilterTools.FilterType.I_XPROII);
        filters.addFilter("Contrast", GPUImageFilterTools.FilterType.CONTRAST);
        filters.addFilter("Brightness", GPUImageFilterTools.FilterType.BRIGHTNESS);
        filters.addFilter("Sepia", GPUImageFilterTools.FilterType.SEPIA);
        filters.addFilter("Vignette", GPUImageFilterTools.FilterType.VIGNETTE);
        filters.addFilter("ToneCurve", GPUImageFilterTools.FilterType.TONE_CURVE);
        filters.addFilter("Lookup (Amatorka)", GPUImageFilterTools.FilterType.LOOKUP_AMATORKA);
    }


    public class ImageItemClick implements View.OnClickListener {
        int clickPosition;
        public ImageItemClick(int position) {
            clickPosition = position;
        }


        @Override
        public void onClick(View v) {
            if (clickPosition == 0) {
                switchFilterTo(new GPUImageFilter());
            } else {
                GPUImageFilter filter = GPUImageFilterTools
                        .createFilterForType(FilterActivity.this,
                                filters.filters.get(clickPosition));
                switchFilterTo(filter);
            }
            for (int i = 0; i < selectList.size(); i++) {
                if (i == clickPosition) {
                    selectList.get(i).setVisibility(View.VISIBLE);
                } else {
                    selectList.get(i).setVisibility(View.INVISIBLE);
                }
            }

            mGPUImageView.requestRender();
        }

    }

//    @Override
//    protected void onActivityResult(final int requestCode,
//                                    final int resultCode, final Intent data) {
//        switch (requestCode) {
//            case REQUEST_PICK_IMAGE:
//                if (resultCode == RESULT_OK) {
//                    handleImage(data.getData());
//                } else {
//                    finish();
//                }
//
//                break;
//
//            default:
//                super.onActivityResult(requestCode, resultCode, data);
//                break;
//        }
//    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.button_save:
                saveImage();
                break;

            default:
                break;
        }

    }

    @Override
    public void onPictureSaved(final Uri uri) {
        Toast.makeText(this, "Saved: " + uri.toString(), Toast.LENGTH_SHORT)
                .show();
    }

    private void saveImage() {
        String fileName = System.currentTimeMillis() + ".jpg";
        mGPUImageView.saveToPictures("Camera", fileName, this);
    }

    private void switchFilterTo(final GPUImageFilter filter) {
        if (mFilter == null
                || (filter != null && !mFilter.getClass().equals(
                filter.getClass()))) {
            mFilter = filter;
            mGPUImageView.setFilter(mFilter);
            mFilterAdjuster = new GPUImageFilterTools.FilterAdjuster(mFilter);
        }
    }


    private void handleImage(final Uri selectedImage) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                    getContentResolver(), selectedImage);
            float width = bitmap.getWidth();
            float height = bitmap.getHeight();
            float ratio = width / height;
            mGPUImageView.setRatio(ratio);
            mGPUImageView.setImage(selectedImage);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
