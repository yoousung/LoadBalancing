package com.example.demoproject_master;

// 스레드 간 데이터 전달을 위한 클래스
public class DataHolderDET {
    private static DataHolderDET instance;
    private String bboxdata;

    private DataHolderDET() {
        // Private constructor to prevent instantiation
    }

    public static synchronized DataHolderDET getInstance() {
        if (instance == null) {
            instance = new DataHolderDET();
        }
        return instance;
    }

    public String getBboxdata() {
        return bboxdata;
    }

    public void setBboxdata(String bboxdata) {
        this.bboxdata = bboxdata;
    }
}
