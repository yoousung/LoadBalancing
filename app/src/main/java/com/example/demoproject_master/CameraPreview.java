package com.example.demoproject_master;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class CameraPreview extends AppCompatActivity {

    private CameraHandler cameraHandler;
    private TextureView textureView;
    private final String TAG = "CameraPreviewTask";

    private Button startBtn;
    private Button toggleSegButton;
    private Button toggleDetButton;
    private Button toggleDet2Button;

    private boolean toggleSeg = false;
    private boolean toggleDet = false;
    private boolean toggleDet2 = false;

    private Ncnn model = new Ncnn();
    private int current_model = 0;
    private int current_cpugpu = 1; // GPU사용
    private ImageView bdbox;

    private TextView device1_state;
    private TextView device2_state;
    private TextView device3_state;

    private ArrayList<String> ip_data;
    private int case_index;
    private Handler handler = new Handler();

    // 송수신 데이터 자바내 데이터 송수신
    private Handler uiHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case ServerThread.MESSAGE_BBOX_DATA:
                    String bboxdata = (String) msg.obj;
                    updateBboxdata(bboxdata);
                    break;
                case ServerThread2.MESSAGE_SEG_DATA:
                    Bitmap receivedBitmap = (Bitmap) msg.obj;
                    // 받아온 Bitmap을 사용하여 UI 업데이트 등을 수행
                    updateBboxImage(receivedBitmap);
                    break;
            }
        }
    };

    private void setupCustomSurfaceListener() {
        textureView.setSurfaceTextureListener(new CustomSurfaceListener(
                cameraHandler,
                textureView,
                model,
                toggleSeg, toggleDet, toggleDet2,
                bdbox,
                device1_state, device2_state, device3_state,
                ip_data));
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        textureView = findViewById(R.id.camera_view);
        assert textureView != null;
        this.startBtn = findViewById(R.id.btn_start);
        this.toggleSegButton = findViewById(R.id.toggleSegButton);
        this.toggleDetButton = findViewById(R.id.toggleDetButton);
        this.toggleDet2Button = findViewById(R.id.toggleDet2Button);
        this.bdbox = findViewById(R.id.bdbox_imageview);
        this.device1_state = findViewById(R.id.device1_state);
        this.device2_state = findViewById(R.id.device2_state);
        this.device3_state = findViewById(R.id.device3_state);
        this.ip_data = getIntent().getStringArrayListExtra("ip_data");

        //Log.e(TAG, "IP : "+ip_data);

        reload();

        // Device1 (DET)
        Thread serverThread = new Thread(new ServerThread(uiHandler,device1_state));
        serverThread.start();

        // Device2 (SEG)
        Thread serverThread2 = new Thread(new ServerThread2(uiHandler,device2_state, bdbox));
        serverThread2.start();

        // Device3 (DET)
        Thread serverThread3 = new Thread(new ServerThread2(uiHandler,device3_state, bdbox));
        serverThread3.start();

        // set the camera preview state
        this.cameraHandler = new CameraHandler(this, getApplicationContext(), textureView);
        textureView.setSurfaceTextureListener(new CustomSurfaceListener(
                cameraHandler,
                textureView,
                model,
                toggleSeg, toggleDet, toggleDet2,
                bdbox,
                device1_state, device2_state, device3_state,
                ip_data));

        // stop/start the client to server bytes transfer
        this.startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StateSingleton.getInstance().runScanning = !StateSingleton.getInstance().runScanning;
                startBtn.setText(startBtn.getText().equals("Start") ? "Stop" : "Start");
            }
        });

        // Seg Button
        updateButtonText(toggleSegButton, "Seg", toggleSeg);
        this.toggleSegButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSeg = !toggleSeg; // 토글
                toggleSegButton.setEnabled(true);

                // 버튼 텍스트 변경
                updateButtonText(toggleSegButton, "Seg", toggleSeg);

                setupCustomSurfaceListener();
            }
        });

        // Det Button
        updateButtonText(toggleDetButton, "Det", toggleDet);
        this.toggleDetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDet = !toggleDet; // 토글
                toggleDetButton.setEnabled(true);

                // 버튼 텍스트 변경
                updateButtonText(toggleDetButton, "Det", toggleDet);

                setupCustomSurfaceListener();
            }
        });

        // Det2 Button
        updateButtonText(toggleDet2Button, "Det", toggleDet2);
        this.toggleDet2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDet2 = !toggleDet2; // 토글
                toggleDet2Button.setEnabled(true);

                // 버튼 텍스트 변경
                updateButtonText(toggleDet2Button, "Det", toggleDet2);

                setupCustomSurfaceListener();
            }
        });
    }

    private void updateButtonText(Button button, String label, boolean isToggleOn) {
        String buttonText = label + ": " + (isToggleOn ? "ON" : "OFF");
        button.setText(buttonText);
    }

    private void reload() {
        // TODO : 모델 로드부
        // single
        // model.loadModel(getAssets(), current_model, current_cpugpu);

        // multi
        if (!model.loadModel(getAssets(), current_model, current_cpugpu))
            Log.e(TAG, "model load failed");
        Log.e(TAG, "model load success");
    }

    private void updateBboxdata(String data) {
        BboxDataHolder.getInstance().setBboxdata(data);
    }

    private void updateBboxImage(Bitmap receivedBitmap) {
        SegDataHolder.getInstance().setSegdata(receivedBitmap);
    }
}