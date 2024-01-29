package com.example.demoproject_master;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.Socket;

public class SocketThread implements Runnable {
    private String serverIp;
    private Bitmap bmp;

    public SocketThread(Bitmap bmp, String ip_data) {
        this.bmp = bmp;
        this.serverIp = ip_data;
    }

    @Override
    public void run() {
        try {
            Log.e("Stream", serverIp);
            Socket clientSocket = new Socket(serverIp, 1357);

            // Send image to server
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