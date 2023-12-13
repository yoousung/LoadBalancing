package com.example.demoproject_master;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.widget.ImageView;

public class Ncnn {
    // nanodet
    public native boolean loadModel_nanodet(AssetManager mgr, int modelid, int cpugpu);
    public native boolean loadModel_yolov8(AssetManager mgr, int modelid, int cpugpu);

    // yolov8
    public native boolean predict_nanodet(ImageView imageView, Bitmap bitmap);
    public native String predict_yolov8(ImageView imageView, Bitmap bitmap);

    static {
        System.loadLibrary("ncnntotal");
    }
}
