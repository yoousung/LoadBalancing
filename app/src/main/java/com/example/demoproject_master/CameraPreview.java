package com.example.demoproject_master;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraPreview extends AppCompatActivity {

    private final String TAG = "CameraPreviewTask";
    private TextureView cameraview;
    private CameraSetting cameraSetting; // class
    private final SendDataTask sendDataTask = new SendDataTask(CameraPreview.this);
    private final ReceiveDataTask receiveDataTask = new ReceiveDataTask(CameraPreview.this);
    private long lastCaptureTime;
    private ExecutorService executorService;
    private int current_model = 0;
    private int current_cpugpu = 1; // GPU사용
    private String ip_data;
    private int case_index;
    private ImageView bdbox;
    private TextView device1_state;
    private TextView device2_state;
    private boolean toggleSeg = false;
    private boolean toggleDet = false;
    private boolean toggleThird = false;

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        // Seg Button 클릭 이벤트
        Button toggleSegButton = findViewById(R.id.toggleSegButton);
        updateButtonText(toggleSegButton, "Seg", toggleSeg);
        toggleSegButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSeg = !toggleSeg; // 토글
                toggleSegButton.setEnabled(true);

                // 버튼 텍스트 변경
                updateButtonText(toggleSegButton, "Seg", toggleSeg);

                Log.d(TAG, "toggleSegButton clicked, isEnabled: " + toggleSegButton.isEnabled());
            }
        });

        // Det button 클릭 이벤트
        Button toggleDetButton = findViewById(R.id.toggleDetButton);
        // 버튼 색상 및 글자색 변경
        updateButtonText(toggleDetButton, "Det", toggleDet);
        toggleDetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDet = !toggleDet; // 토글
                toggleDetButton.setEnabled(true);

                // 버튼 텍스트 변경
                updateButtonText(toggleDetButton, "Det", toggleDet);

                Log.d(TAG, "toggleDetButton clicked, isEnabled: " + toggleDetButton.isEnabled());
            }
        });

        Button exitButton = findViewById(R.id.exit_button);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 앱 종료
                finishAffinity();

                // 앱 재시작
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });


        Context context = getApplicationContext();
        initsetting();
        sendDataTask.set_socket(ip_data);

        reload();
        startSendingResults();
    }

    @Override
    public void onBackPressed() {
        stopSendingResults();

        bdbox.setImageBitmap(null);
        cameraSetting.closeCamera();

        Intent intent = new Intent(CameraPreview.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        super.onBackPressed();
        finish();
    }

    private void updateButtonText(Button button, String label, boolean isToggleOn) {
        String buttonText = label + ": " + (isToggleOn ? "ON" : "OFF");
        button.setText(buttonText);
    }

    private boolean sendRunning = false;
    private Runnable receiveRunnable = new Runnable() {
        @Override
        public void run() {
            for (int port_index = 0; port_index < 2; port_index++) {
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
    public static final int[] PORT = {13579, 2468}; // 결과값 송신을 위한 포트

    private final String[] state_connecting = {"off", "off"};
    private String Bbox_data = "";

    // TODO : 모델 선언부
    // 1) single
    // nanodet
    // private NanoDetNcnn model = new NanoDetNcnn();
    // yolov8
    // private Yolov8Ncnn model = new Yolov8Ncnn();

    // 2) multi
    private Ncnn model = new Ncnn();

    private void initsetting() {
        // Main에서 셋팅값 가져오기
        case_index = getIntent().getIntExtra("case_index", -1);
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
        cameraSetting = new CameraSetting(CameraPreview.this, cameraview);
    }

    private void reload() {
        // TODO : 모델 로드부
        // single
//        model.loadModel(getAssets(), current_model, current_cpugpu);

        // multi
        if (!model.loadModel(getAssets(), current_model, current_cpugpu))
            Log.e(TAG, "model load failed");
        Log.e(TAG, "model load success");
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
    protected void onDestroy() {
        bdbox.setImageBitmap(null);
        cameraSetting.closeCamera();

        stopSendingResults();
        executorService.shutdown();

        handler.removeCallbacksAndMessages(null);

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

            // TODO: 모델 추론부
            boolean[] opt = new boolean[3];
            opt[0] = toggleSeg;
            opt[1] = toggleDet;
            opt[2] = false;

            Bbox_data = receiveDataTask.getBboxdata();
            Log.e(TAG, "BBOX : " + Bbox_data);

            // 1) device1 & device2 OFF
            if (device1_state.getText().equals("off") && device2_state.getText().equals("off")) {
                model.homoGen(bdbox, bitmap, opt);
            }
            // 2) device1 ON
            else {
                if (Bbox_data != null) {
                    model.homoGen(bdbox, bitmap, opt);

                    Drawable drawable = bdbox.getDrawable();
                    Bitmap newbitmap = ((BitmapDrawable) drawable).getBitmap();

                    bdbox.setImageBitmap(null);

                    model.heteroGen(bdbox, newbitmap, Bbox_data);
                } else {
                    model.homoGen(bdbox, bitmap, opt);
                }
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCaptureTime >= 100) {
                lastCaptureTime = currentTime;
                for (int deviceIndex = 0; deviceIndex < 2; deviceIndex++) {
                    final int currentDeviceIndex = deviceIndex;
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            sendDataTask.sendBitmapOverNetwork(bitmap, currentDeviceIndex);
                            receiveDataTask.set_state(device1_state, device2_state);
                        }
                    });
                }
            }
        }
    };

    public void stopSendingResults() {
        sendRunning = false;
        handler.removeCallbacks(receiveRunnable);
    }

    public void startSendingResults() {
        sendRunning = true;
        handler.post(receiveRunnable);
    }
}