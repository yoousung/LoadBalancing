package com.example.demoproject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CameraPreview extends AppCompatActivity {

    private String TAG="CameraPreview";
    private ReceiveDataTask receiveDataTaskTask;
    //private SendDataTask sendDataTask = new SendDataTask(CameraPreview.this);
    private ImageView detectView;
    private ImageView receiveimg;
    private int port_index;
    private ExecutorService executorService;
    private NanoDetNcnn nanodetncnn = new NanoDetNcnn();
    private int current_model = 0;
    private int current_cpugpu = 1;
    int corePoolSize = 2;
    int maximumPoolSize = 4;
    long keepAliveTime = 1;
    private int nThreads;
    private Bitmap receiveBitmap;
    private final Object bitmapLock = new Object();
    private String bboxdata =" ";
    private String pre_bboxdata = "";

    // 데이터 송신
    private final String master_IP = "192.168.43.1";
    //private final String master_IP = "192.168.43.91"

    private final int PORT[] = {13579, 2468}; // 결과값 송신을 위한 포트
    private boolean sendRunning = false;
    private Handler handler = new Handler();

    private Runnable sendRunnable = new Runnable() {
        @Override
        public void run() {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    send_connect(port_index);
                }
            });
            handler.postDelayed(this, 100);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        initsetting();

        // 수신부
        receiveDataTaskTask = new ReceiveDataTask(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS);
        receiveDataTaskTask.setDataReceivedCallback(callback);
        receiveDataTaskTask.startReceiving();

        startSendingResults();
        ConnectServer();
        reload();
    }

    private void reload() {
        // 모델 : 객체 인식 모델
        boolean ret_init_model = nanodetncnn.loadModel(getAssets(), current_model, current_cpugpu);
        if (!ret_init_model)
        {
            Log.e(TAG, "nanodetncnn failed");
        }
    }

    private void initsetting(){
        //화면 계속 켜진 상태로 유지
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        port_index = getIntent().getIntExtra("port_index_key",-1);
        nThreads = Runtime.getRuntime().availableProcessors();
        executorService = Executors.newFixedThreadPool(nThreads);
        detectView = findViewById(R.id.detectView);
        receiveimg = findViewById(R.id.receiveImageView);
    }

    public void setImageBitmap(Bitmap bitmap) {
        detectView.setImageBitmap(bitmap);
    }

    @Override
    protected void onPause() {
        super.onPause();
        send_disconnect(port_index);
        DisconnectServer();
        handler.removeCallbacksAndMessages(null);
    }

    // 카메라 프리뷰 재시작 했을 때
    @Override
    protected void onResume() {
        super.onResume();
        if (receiveDataTaskTask == null /*|| receiveTask.getStatus() != AsyncTask.Status.RUNNING*/) {
            send_connect(port_index);
            ConnectServer();
        }
    }

    // 카메라 프리뷰 파괴되었을 때
    @Override
    protected void onDestroy() {
        super.onDestroy();

        send_disconnect(port_index);
        executorService.shutdown();
        stopSendingResults();
        DisconnectServer();
        handler.removeCallbacksAndMessages(null);
    }

    private void send_connect(int port_index){
        new Thread(new Runnable() {
            @Override
            public void run() {

                String stringValue = bboxdata; // BBOX데이터 전송

                try {
                    Socket clientSocket = new Socket(master_IP, PORT[port_index]);
                    BufferedOutputStream outToServer = new BufferedOutputStream(clientSocket.getOutputStream());

                    byte[] byteArray = stringValue.getBytes();

                    outToServer.write(byteArray);
                    outToServer.flush(); // 버퍼 비우기
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("SendData", "Error sending data: " + e.getMessage());
                }
            }
        }).start();
    }

    // 수신부
    private void DisconnectServer(){
        receiveDataTaskTask.stopReceiving();
    }
    
    private void ConnectServer(){
        receiveDataTaskTask = new ReceiveDataTask(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS);
        receiveDataTaskTask.setDataReceivedCallback(callback);
        receiveDataTaskTask.startReceiving();
    }

    ReceiveDataTask.DataReceivedCallback callback = new ReceiveDataTask.DataReceivedCallback() {
        @Override
        public void onDataReceived(byte[] data) {
            if (data != null) {
                // 스레드 동기화 -> 이 작업이 끝나기 전까지 데이터 접근x
                synchronized (bitmapLock) {
                    receiveBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            receiveimg.setImageBitmap(receiveBitmap); // Update the UI on the main thread
                            bboxdata = nanodetncnn.predict(detectView, receiveBitmap);
                        }
                    });

                }
            } else {
                Log.e(TAG, "Received data is null.");
            }
        }
    };

    // 송신부
    private void startSendingResults() {
        sendRunning = true;
        handler.post(sendRunnable);
    }

    private void stopSendingResults() {
        sendRunning = false;
        handler.removeCallbacks(sendRunnable);
    }

    private void send_disconnect(int port_index){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String stringValue = "off";
                try {
                    Socket clientSocket = new Socket(master_IP, PORT[port_index]);
                    BufferedOutputStream outToServer = new BufferedOutputStream(clientSocket.getOutputStream());

                    byte[] byteArray = stringValue.getBytes();

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
