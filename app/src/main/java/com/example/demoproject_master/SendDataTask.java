package com.example.demoproject_master;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.TextureView;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SendDataTask {

    private Context context;
    private String TAG = "SendTask";
    private String part[];
    private static String[] SERVER_IP_LIST = new String[2]; // Device ip : Device1, Device2
    public static final int PORT_NUMBER = 1357; // 송신을 위한 포트 넘버

    public SendDataTask(Context context) {
        this.context = context;
    }

    // socket 통신 설정하기
    public void set_socket(String ip_data){
        // IP출력
        Log.e(TAG, "IP DATA : "+ip_data);
        if(ip_data!=null) {
            part = ip_data.split(" ");

            if (part.length == 2) {
                // 쉼표로 구분된 경우
                Log.e(TAG, "Device1: " + part[0]);
                Log.e(TAG, "Device2: " + part[1]);
                SERVER_IP_LIST[0] = part[0];
                SERVER_IP_LIST[1] = part[1];
                Log.e(TAG, "IP : "+SERVER_IP_LIST[1]+","+Log.e(TAG, "IP : "+SERVER_IP_LIST[0]));
            } else {
                // 구분되지 않은 경우
                Log.e(TAG, "Device1: " + part[0]);
                SERVER_IP_LIST[0] = part[0];
                Log.e(TAG, "IP : "+SERVER_IP_LIST[0]);
            }
        } else {
            // ip_data가 null인 경우 처리
            Log.e(TAG, "ip_data is null.");
        }
    }

    // CameraPreview Device에 전송
    public static void sendBitmapOverNetwork(Bitmap resizedBitmap, int deviceIndex) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
                byte[] byteArray = stream.toByteArray();

                // IP 주소를 deviceIndex에 따라 선택합니다.
                String serverIp = SERVER_IP_LIST[deviceIndex];

                try {
                    Socket clientSocket = new Socket(serverIp, PORT_NUMBER);
                    BufferedOutputStream outToServer = new BufferedOutputStream(clientSocket.getOutputStream());

                    outToServer.write(byteArray);
                    outToServer.flush(); // 버퍼 비우기
                    clientSocket.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
