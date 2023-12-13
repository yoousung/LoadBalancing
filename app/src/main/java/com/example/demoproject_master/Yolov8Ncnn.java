package com.example.demoproject_master;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.widget.ImageView;

public class Yolov8Ncnn {
    public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);
    public native String predict(ImageView imageView, Bitmap bitmap);
    static {
        System.loadLibrary("ncnnyolov8");
    }
}



