package com.example.demoproject_master;

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

    @Override
    public void run(){
        try {
            // 서버 소켓 생성
            ServerSocket serverSocket = new ServerSocket(serverPort);
            Log.i("ServerThread", "Server listening on port " + serverPort);

            while (true) {
                // 클라이언트의 연결을 기다림
                Socket clientSocket = serverSocket.accept();
                Log.i("ServerThread", "Client connected: " + clientSocket.getInetAddress());

                // 클라이언트로부터 문자열 데이터 수신신
                BufferedInputStream inFromClient = new BufferedInputStream(clientSocket.getInputStream());
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] data = new byte[1024]; // 1024 = 1KB 크기 버퍼
                int bytesRead;

                while ((bytesRead = inFromClient.read(data)) != -1) {
                    byteArrayOutputStream.write(data, 0, bytesRead);
                }
                byte[] receivedData = byteArrayOutputStream.toByteArray();

                // 수신받은 데이터 출력
                String receivedText = new String(receivedData, StandardCharsets.UTF_8);

                // UI 업데이트를 메인 스레드에서 처리
                uiHandler.post(() -> {
                    if (receivedText.equals("off")) {
                        device1_state.setText("off");
                    } else {
                        device1_state.setText("on");
                    }
                });
                // main으로 데이터 전송
                Message message = uiHandler.obtainMessage(MESSAGE_DET_DATA, receivedText);
                uiHandler.sendMessage(message);

                // 클라이언트 소켓 닫기
                clientSocket.close();
            }
        } catch (IOException e) {
            device1_state.setText("off");
            e.printStackTrace();
        }
    }
}
