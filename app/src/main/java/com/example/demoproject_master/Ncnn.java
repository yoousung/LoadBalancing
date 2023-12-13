package com.example.demoproject_master;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.widget.ImageView;

public class Ncnn {
//    public native boolean Bitmap2Rgb(ImageView imageView,
//                                     Bitmap bitmap);

    // nanodet
    public native boolean loadModel_nanodet(AssetManager mgr, int modelid, int cpugpu);
    public native boolean loadModel_yolov8(AssetManager mgr, int modelid, int cpugpu);

    // yolov8
    public native boolean predict_nanodet(ImageView imageView, Bitmap bitmap);
    public native boolean predict_yolov8(ImageView imageView, Bitmap bitmap);

//    public native boolean draw_Bbox(ImageView imageView,
//                                Bitmap bitmap,
//                                String data1,
//                                String data2);

    static {
        System.loadLibrary("ncnntotal");
    }
}
