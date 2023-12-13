package com.example.demoproject_master;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraPreview extends AppCompatActivity {

    private String TAG = "CameraPreviewTask";
    private TextureView cameraview;
    private Context context;
    private CameraSetting cameraSetting; // class

    // 소켓 통신을 위한 변수 설정
    public static final int PORT_NUMBER = 1357; // 송신을 위한 포트 넘버
    public static final int PORT[] = {13579, 2468}; // 결과값 리턴을 위한 포트 넘버
    private String[] SERVER_IP_LIST = new String[2]; // Device ip : Device1, Device2

    // 프리뷰 해상도, 현재 캡쳐되는 이미지 크기 902x320
    private Range<Integer> desiredFpsRange = null;
    private Matrix matrix;
    private long lastCaptureTime;

    // 결과값 수신
    private int case_index;
    private String state_connecting[] = {"off","off"};
    private Handler handler = new Handler();
    private boolean sendRunning = false;

    private Runnable sendRunnable = new Runnable() {
        @Override
        public void run() {
            // 스레드 풀 이용한 데이터 수신
            for(int port_index = 0; port_index <PORT.length; port_index++){
                final int currentPortIndex = port_index;
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        if (sendRunning) {
                            //receive_state(currentPortIndex);
                        }
                    }
                });
            }
            handler.postDelayed(this, 100); // 8fps로 데이터 수신
        }
    };

    //스레드 풀
    private ExecutorService executorService;

    // Model
    // nanodet
//    private NanoDetNcnn model = new NanoDetNcnn();
    // yolov5
    //private YoloDetNcnn model = new YoloDetNcnn();


    //private Yolov8Ncnn yolov8 = new Yolov8Ncnn();
    //private NanoDetNcnn nanodet = new NanoDetNcnn();
    private Ncnn ncnn = new Ncnn();

    private int current_model = 0;
    private int current_cpugpu = 1; // GPU사용

    // BBox수신받아 그리기
    private ImageView bdbox;
    private String Bbox_data;
    private String pre_Bbox_data = "";
    private String ip_data;
    private String part[];

    private TextView device1_state;
    private TextView device2_state;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        context = getApplicationContext();

        // Main에서 셋팅값 가져오기
        case_index = getIntent().getIntExtra("case_index",-1);
        ip_data = getIntent().getStringExtra("ip_data");

       // set_socket(ip_data);

        // 스레드 풀 초기화
        int nThreads = Runtime.getRuntime().availableProcessors();
        executorService = Executors.newFixedThreadPool(nThreads);

        device1_state = findViewById(R.id.device1_state);
        device2_state = findViewById(R.id.device2_state);

        // matrix 초기화
        matrix = new Matrix();
        matrix.postScale(0.6f, 0.6f); // 50%로 리사이즈

        cameraview = findViewById(R.id.camera_view);
        cameraview.setSurfaceTextureListener(textureListener);
        bdbox = findViewById(R.id.bdbox_imageview);

        //화면 계속 켜진 상태로 유지
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        current_model = 0;

        reload();
        //startSendingResults();
    }

    private void reload()
    {
        /*
        boolean ret_init = yolov8.loadModel(getAssets(), current_model, current_cpugpu);
        if (!ret_init)
        {
            Log.e(TAG, "nanodetncnn loadModel failed");
        }
        Log.e(TAG, "nanodetncnn loadModel success");*/

        /*
        boolean ret_init = nanodet.loadModel(getAssets(), current_model, current_cpugpu);
        if (!ret_init)
        {
            Log.e(TAG, "nanodetncnn loadModel failed");
        }
        Log.e(TAG, "nanodetncnn loadModel success");
        */

        boolean ret_init = ncnn.loadModel_nanodet(getAssets(), current_model, current_cpugpu);
        if (!ret_init)
        {
            Log.e(TAG, "nanodetncnn loadModel failed");
        }
        Log.e(TAG, "nanodetncnn loadModel success");

        boolean ret_init2 = ncnn.loadModel_yolov8(getAssets(), current_model, current_cpugpu);
        if (!ret_init2)
        {
            Log.e(TAG, "yolov8 loadModel failed");
        }
        Log.e(TAG, "yolov8 loadModel success");

    }

    // 파괴되었을 떄
    @Override
    protected void onDestroy(){
        super.onDestroy();
        executorService.shutdown();
        //stopSendingResults(); // 화면이 일시 중단될 때 전송 중지
    }

    // 화면 리스너
    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
            //openCamera();
            cameraSetting = new CameraSetting(CameraPreview.this,cameraview);
            cameraSetting.openCamera();

            Log.e(TAG,"cameraview를 스캔합니다.");
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            return false;
        }


        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

            Bitmap bitmap = cameraview.getBitmap();
            //yolov8.predict(bdbox, bitmap);
            //nanodet.predict(bdbox, bitmap);

            //ncnn.predict_nanodet(bdbox, bitmap);
            ncnn.predict_yolov8(bdbox, bitmap);



            /*
            // 스레드 풀 이용한 데이터 전송
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCaptureTime >= 200){
                lastCaptureTime = currentTime;
                for(int deviceIndex = 0; deviceIndex <SERVER_IP_LIST.length; deviceIndex++){
                    final int currentDeviceIndex = deviceIndex;
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            sendBitmapOverNetwork(bitmap, currentDeviceIndex);
                        }
                    });
                }
            }

            device1_state.setText(state_connecting[0]); // Device1 연결되었을 때
            device2_state.setText(state_connecting[1]); // Device2 연결되었을 때

            // off시 master에서 예측
            if(state_connecting[0].equals("off") && state_connecting[1].equals("off")){
                model.predict(bdbox, bitmap);
            }
            // 핸드폰 연결이 있을시 핸드폰에서 예측 -> master로 데이터 받아오기
            else{
                // BBox안바꼈을 때
                if(pre_Bbox_data.equals(Bbox_data)){
                    bdbox.setImageBitmap(bitmap);
                }
                // BBox바꼈을 때
                else {
                    //model.draw_Bbox(bdbox, bitmap, Bbox_data, Bbox_data); // 예측데이터 그리기
                    pre_Bbox_data = Bbox_data;
                }
            }*/
        }
    };

    // socket 통신 설정하기
    private void set_socket(String ip_data){
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

    // 이미지를 설정하는 메서드
    public void setImageBitmap(Bitmap bitmap) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bdbox.setImageBitmap(bitmap);
                    }
                });
            }
        }).start();
    }

    /*
    // CameraPreview Device에 전송
    private void sendBitmapOverNetwork(Bitmap resizedBitmap, int deviceIndex) {
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

    // 예측 결과 master로 전송 중지
    private void stopSendingResults() {
        sendRunning = false;
        handler.removeCallbacks(sendRunnable);
    }

    // 예측 결과 master로 전송 시작
    private void startSendingResults() {
        sendRunning = true;
        handler.post(sendRunnable);
    }

    // master로 연결상태 return -> on or off 전송
    private void receive_state(int port_index){
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

                        // 핸드폰 연결 확인
                        if(receivedText.equals("off")) {
                            state_connecting[port_index] = "off";
                            Log.e(TAG, "State : "+state_connecting[port_index]);
                        } else {state_connecting[port_index] = "on";}

                    } else {
                        Log.e(TAG, "Received data is null.");
                    }

                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }
        }).start();
    }*/


}