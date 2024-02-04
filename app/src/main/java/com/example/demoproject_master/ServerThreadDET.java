package com.example.demoproject_master;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

// DET 소켓 통신
// Device1 socket (DET)
public class ServerThreadDET implements Runnable {

    private final Handler uiHandler;
    private final TextView device1_state;
    private final int serverPort;
    public static final int MESSAGE_DET_DATA = 1;

    // "volatile" 추가

    public ServerThreadDET(int serverPort, Handler uiHandler, TextView device1_state){
        this.serverPort = serverPort;
        this.uiHandler = uiHandler;
        this.device1_state = device1_state;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void run(){
        try {
            ServerSocket serverSocket = new ServerSocket(serverPort);
            Log.i("ServerThread", "Server listening on port " + serverPort);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Log.i("ServerThread", "Client connected: " + clientSocket.getInetAddress());

                BufferedInputStream inFromClient = new BufferedInputStream(clientSocket.getInputStream());
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] data = new byte[1024]; // 1024 = 1KB 크기 버퍼
                int bytesRead;

                while ((bytesRead = inFromClient.read(data)) != -1) {
                    byteArrayOutputStream.write(data, 0, bytesRead);
                }
                byte[] receivedData = byteArrayOutputStream.toByteArray();
                String receivedText = new String(receivedData, StandardCharsets.UTF_8);

                uiHandler.post(() -> {
                    if (receivedText.equals("off")) {
                        device1_state.setText("off");
                    } else {
                        device1_state.setText("on");
                    }
                });

                Message message = uiHandler.obtainMessage(MESSAGE_DET_DATA, receivedText);
                uiHandler.sendMessage(message);

                clientSocket.close();
            }
        } catch (IOException e) {
            device1_state.setText("off");
            e.printStackTrace();
        }
    }
}
