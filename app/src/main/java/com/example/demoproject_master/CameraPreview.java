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
    private SendDataTask sendDataTask = new SendDataTask(CameraPreview.this);
    private ReceiveDataTask receiveDataTask = new ReceiveDataTask(CameraPreview.this);
    private long lastCaptureTime;
    private ExecutorService executorService;
    private int current_model = 0;
    private int current_cpugpu = 1; // GPU사용
    private String ip_data;
    private int case_index;
    private ImageView bdbox;
    private TextView device1_state;
    private TextView device2_state;

    private Handler handler = new Handler();
    private boolean sendRunning = false;
    private Runnable sendRunnable = new Runnable() {
        @Override
        public void run() {
            for(int port_index = 0; port_index <2; port_index++){
                final int currentPortIndex = port_index;
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        if (sendRunning) {
                            receiveDataTask.receive_state(currentPortIndex);
                        }
                    }
                });
            }
            handler.postDelayed(this, 100); // 8fps로 데이터 수신
        }
    };

    // TODO : 모델 선언부
    private Yolov8Ncnn model = new Yolov8Ncnn();
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        context = getApplicationContext();
        initsetting();
        sendDataTask.set_socket(ip_data);

        reload();
        startSendingResults();
    }

    private void initsetting(){
        // Main에서 셋팅값 가져오기
        case_index = getIntent().getIntExtra("case_index",-1);
        ip_data = getIntent().getStringExtra("ip_data");
        // 스레드 풀 초기화
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        int nThreads = Runtime.getRuntime().availableProcessors();
        executorService = Executors.newFixedThreadPool(nThreads);
        device1_state = findViewById(R.id.device1_state);
        device2_state = findViewById(R.id.device2_state);
        cameraview = findViewById(R.id.camera_view);
        cameraview.setSurfaceTextureListener(textureListener);
        bdbox = findViewById(R.id.bdbox_imageview);
        cameraSetting = new CameraSetting(CameraPreview.this,cameraview);
    }

    private void reload() {
        boolean ret_init = model.loadModel(getAssets(), current_model, current_cpugpu);
        if (!ret_init)
        {
            Log.e(TAG, "nanodetncnn loadModel failed");
        }
        Log.e(TAG, "nanodetncnn loadModel success");
    }

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

    @Override
    protected void onDestroy(){
        super.onDestroy();
        executorService.shutdown();
        stopSendingResults();
    }

    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
            cameraSetting.openCamera();
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

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCaptureTime >= 200){
                lastCaptureTime = currentTime;
                for(int deviceIndex = 0; deviceIndex <2; deviceIndex++){
                    final int currentDeviceIndex = deviceIndex;
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            SendDataTask.sendBitmapOverNetwork(bitmap, currentDeviceIndex);
                            receiveDataTask.set_state(device1_state, device2_state);
                        }
                    });
                }
            }
            // TODO : 모델 동작 코드 추가
            // detection
            //model.predict(bdbox, bitmap);
        }
    };

    public void stopSendingResults() {
        sendRunning = false;
        handler.removeCallbacks(sendRunnable);
    }

    public void startSendingResults() {
        sendRunning = true;
        handler.post(sendRunnable);
    }
}