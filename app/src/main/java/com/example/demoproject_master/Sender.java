package com.example.demoproject_master;

import android.graphics.Bitmap;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class Sender implements Runnable {
    private final ArrayList<String> iplist;
    private final Bitmap bmp;

    public Sender(Bitmap bmp, ArrayList<String> ip_list) {
        this.bmp = bmp;
        this.iplist = ip_list;
    }

    @Override
    public void run() {
        try (Socket clientSocket = new Socket(iplist.get(0), 1300);
             Socket clientSocket2 = new Socket(iplist.get(1), 1301)) {
            // 192.168.43.32 samsung s22
            // 192.168.43.77 samsung s8+
            // 192.168.43.103 samsung s7+

            BufferedOutputStream outToServer = new BufferedOutputStream(clientSocket.getOutputStream());
            BufferedOutputStream outToServer2 = new BufferedOutputStream(clientSocket2.getOutputStream());

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 70, stream);

            byte[] byteArray = stream.toByteArray();
            bmp.recycle();

            outToServer.write(byteArray);
            outToServer2.write(byteArray);
            outToServer.flush(); // 버퍼 비우기
            outToServer2.flush(); // 버퍼 비우기
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}