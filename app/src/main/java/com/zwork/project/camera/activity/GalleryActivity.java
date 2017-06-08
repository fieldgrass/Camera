package com.zwork.project.camera.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zwork.project.camera.R;
import com.zwork.project.camera.adapter.GalleryAdapter;
import com.zwork.project.camera.model.ImageModel;
import com.zwork.project.camera.utils.RecyclerItemClickListener;
import com.zwork.project.camera.utils.ImageUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class GalleryActivity extends Activity{

    GalleryAdapter mAdapter;
    RecyclerView mRecyclerView;
    String select;
    Button shareBtn;
    TextView toolBarTitle;

    ArrayList<ImageModel> data = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        shareBtn= (Button) findViewById(R.id.do_share);
        toolBarTitle= (TextView) findViewById(R.id.toolbar_title);
        select=getIntent().getStringExtra("WhichActivity");

        if(select.equals("share")){
            toolBarTitle.setText("分享");
            shareBtn.setVisibility(View.INVISIBLE);
        }

        List<String> imgPaths= ImageUtil.getImagePathFromSD();

        int size=imgPaths.size();
        for (int i=0;i<size;i++){

            ImageModel imageModel = new ImageModel();
            imageModel.setName("Image "+i);
            imageModel.setUrl(imgPaths.get(size-1-i));
            data.add(imageModel);

        }

        mRecyclerView= (RecyclerView) findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this,3));
        mRecyclerView.setHasFixedSize(true);

        mAdapter=new GalleryAdapter(this,data);
        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this,
                new RecyclerItemClickListener.OnItemClickListener() {

                    @Override
                    public void onItemClick(View view, int position) {
                        if(select.isEmpty()) return;
                        String fileP=data.get(position).getUrl();
                        Uri uri = Uri.fromFile(new File(fileP));
                        if(select.equals("gallery")) {
                            Intent intent = new Intent(GalleryActivity.this, ImagePageActivity.class);
                            intent.putParcelableArrayListExtra("data", data);
                            intent.putExtra("pos", position);
                            startActivity(intent);
                        }else if(select.equals("FirstPhoto")){
                            Intent toHandle=new Intent(GalleryActivity.this,HandleImageActivity.class);
                            toHandle.putExtra("filePath",fileP);
                            startActivity(toHandle);
                        }else if(select.equals("filter")){
                            Intent intent = new Intent(GalleryActivity.this, FilterActivity.class);
                            intent.setData(uri);
                            startActivity(intent);
                        }else if(select.equals("share")){
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            File file = new File(fileP);
                            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                            shareIntent.setType("image/jpeg");
                            startActivity(Intent.createChooser(shareIntent, GalleryActivity.this.getTitle()));
                        }

                    }

                    @Override
                    public void onLongClick(View view, final int position) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(GalleryActivity.this);
                        builder.setMessage("确认删除该相片？");
                        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                File f=new File(data.get(position).getUrl());
                                if(f.exists()) f.delete();
                                mAdapter.removeData(position);
                                dialog.cancel();
                            }
                        });
                        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        AlertDialog alert = builder.create();
                        alert.show();

                    }
                }));

    }


    public void doShare(View view){
        Intent toShare = new Intent(this,GalleryActivity.class);
        toShare.putExtra("WhichActivity","share");
        startActivity(toShare);
    }
}
