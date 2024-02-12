package com.example.demoproject_master;

import android.graphics.Bitmap;

class DataHolderDET {
    private static DataHolderDET instance;
    private String bboxdata;

    private DataHolderDET() {
    }

    public static synchronized DataHolderDET getInstance() {
        if (instance == null) {
            instance = new DataHolderDET();
        }
        return instance;
    }

    public void setBboxdata(String bboxdata) {
        this.bboxdata = bboxdata;
    }

    public String getBboxdata() {
        return bboxdata;
    }
}

class DataHolderSEG {
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
