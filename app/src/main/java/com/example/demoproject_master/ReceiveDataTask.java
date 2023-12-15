package com.example.demoproject_master;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ReceiveDataTask {

    private Context context;
    private String TAG = "ReceiveTask";
    public static final int PORT[] = {13579, 2468}; // 결과값 리턴을 위한 포트 넘버
    private String state_connecting[] = {"off","off"};
    private String Bbox_data;
    private String pre_Bbox_data = "";

    public ReceiveDataTask(Context context) {
        this.context = context;
    }

    public void receive_state(int port_index){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(PORT[port_index]);
                    Socket clientSocket = serverSocket.accept();

                    // 클라이언트로부터 데이터를 수신하는 부분
                    BufferedInputStream inFromClient = new BufferedInputStream(clientSocket.getInputStream());
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] data = new byte[1024]; // 1024 = 1KB 크기 버퍼

                    int bytesRead;

                    while ((bytesRead = inFromClient.read(data)) != -1) {
                        byteArrayOutputStream.write(data, 0, bytesRead);
                    }

                    byte[] byteArray2 = byteArrayOutputStream.toByteArray();

                    // 데이터를 수신받았다면
                    if (byteArray2 != null) {
                        String receivedText = new String(byteArray2, StandardCharsets.UTF_8);

                        // 양측에서 받아오는 BBOX데이터
                        Bbox_data = receivedText;

                        Log.e(TAG, "Receive : "+Bbox_data);

                        // 핸드폰 연결 확인
                        if(receivedText.equals("off")) {
                            state_connecting[port_index] = "off";
                            Log.e(TAG, "State : "+state_connecting[port_index]);
                        } else {state_connecting[port_index] = "on";}

                    } else {
                        Log.e(TAG, "Received data is null.");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void set_state(TextView textView1, TextView textView2){
        textView1.setText(state_connecting[0]);
        textView2.setText(state_connecting[1]);
    }

    public String getBboxdata(){
        return Bbox_data;
    }
}
