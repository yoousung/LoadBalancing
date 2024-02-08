package com.example.demoproject_master;

public class DataHolderDET {
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
