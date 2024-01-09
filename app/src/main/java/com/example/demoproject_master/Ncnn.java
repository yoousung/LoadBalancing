package com.example.demoproject_master;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.widget.ImageView;

import java.util.ArrayList;

public class Ncnn {
    public native boolean loadModel(AssetManager mgr,
                                    int modelid,
                                    int cpugpu);

    public native boolean homoGen(ImageView imageView,
                                  Bitmap bitmap,
                                  boolean[] opt);

    public native boolean heteroGenDet(ImageView imageView,
                                    Bitmap bitmap,
                                    String data);

    public native boolean heteroGenSeg(ImageView imageView,
                                    Bitmap bitmap,
                                    Bitmap data);

    public native boolean predict(ImageView imageView, Bitmap bitmap,  boolean[] opt);

    public native boolean draw_Bbox(ImageView imageView,
                                    Bitmap bitmap,
                                    String data);

    public native boolean draw_Seg(ImageView imageView,
                                   Bitmap bitmap,
                                   Bitmap segbitmap);

    static {
        System.loadLibrary("ncnntotal");
    }
}
