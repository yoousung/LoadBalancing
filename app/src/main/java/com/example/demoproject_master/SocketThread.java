package com.example.demoproject_master;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.Socket;

public class SocketThread implements Runnable{

    private Bitmap bmp;

    //TODO 원하는 IP로 수정
    private String serverIp = "192.168.43.103"; // 전송하고자 하는 디바이스 IP로 변경

    public SocketThread(Bitmap bmp) {
        this.bmp = bmp;
    }

    @Override
    public void run() {
        try {
            //Log.e("Stream", "Start Stream");
            Socket clientSocket = new Socket(serverIp, 1357);

            // 서버로 이미지 전송
            BufferedOutputStream outToServer = new BufferedOutputStream(clientSocket.getOutputStream());
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 70, stream);

            // byte array and recycle call for better performance
            byte[] byteArray = stream.toByteArray();
            bmp.recycle();

            outToServer.write(byteArray);
            outToServer.flush(); // 버퍼 비우기
            clientSocket.close();

            //Log.e(StateSingleton.getInstance().TAG, "Seccess!");
        }catch(Exception e){
            Log.e(StateSingleton.getInstance().TAG, "SocketThread runs on an error!");
        }
    }
}