package com.example.demoproject_master;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class SocketThread implements Runnable {
    private final ArrayList<String> iplist;
    private final Bitmap bmp;

    public SocketThread(Bitmap bmp, ArrayList<String> ip_list) {
        this.bmp = bmp;
        this.iplist = ip_list;
    }

    @Override
    public void run() {
        try {
            // TODO : multithreading
            for (String ip : iplist) {
                Socket clientSocket = new Socket(ip, 1357);

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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}