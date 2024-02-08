package com.example.demoproject_master;

import android.graphics.Bitmap;

public class DataHolderSEG {
    private static final DataHolderSEG instance = new DataHolderSEG();
    private Bitmap segData;

    private DataHolderSEG() {
    }

    public static DataHolderSEG getInstance() {
        return instance;
    }

    public void setSegdata(Bitmap segData) {
        this.segData = segData;
    }

    public Bitmap getSegdata() {
        return segData;
    }
}

