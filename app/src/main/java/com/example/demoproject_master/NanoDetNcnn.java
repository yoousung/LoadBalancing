package com.example.demoproject_master;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.widget.ImageView;

public class NanoDetNcnn {
    public native boolean loadModel(AssetManager mgr,
                                    int modelid,
                                    int cpugpu);
    public native boolean predict(ImageView imageView,
                                  Bitmap bitmap);
    public native boolean draw_Bbox(ImageView imageView,
                                    Bitmap bitmap,
                                    String data1,
                                    String data2);

    static {
        System.loadLibrary("nanodetncnn");
    }
}
