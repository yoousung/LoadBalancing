package com.example.demoproject_master;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.widget.ImageView;

public class Ncnn {
    // nanodet
    public native boolean loadModel_nanodet(AssetManager mgr, int modelid, int cpugpu);
    public native boolean loadModel_yolov8(AssetManager mgr, int modelid, int cpugpu);

    // yolov8
    public native boolean predict(ImageView imageView, Bitmap bitmap, String input);

//    public native boolean draw_Bbox(ImageView imageView,
//                                Bitmap bitmap,
//                                String data1,
//                                String data2);

    static {
        System.loadLibrary("ncnntotal");
    }
}
