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

    public native boolean heteroGen(ImageView imageView,
                                    Bitmap bitmap,
                                    String data);

    static {
        System.loadLibrary("ncnntotal");
    }
}
