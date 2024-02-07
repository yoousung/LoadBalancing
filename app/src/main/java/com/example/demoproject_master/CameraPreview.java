package com.example.demoproject_master;

import android.annotation.SuppressLint;
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
    private Button toggleSegButton, toggleDetButton, toggleDet2Button;

    private boolean toggleSeg = false;
    private boolean toggleDet = false;
    private boolean toggleDet2 = false;
    private Thread serverThread, serverThread2, serverThread3;
    private final Ncnn model = new Ncnn();
    private int current_cpugpu = 1; // GPU사용
    private ImageView bdbox;

    private TextView device1_state, device2_state, device3_state;
    private CustomSurfaceListener customSurfaceListener = null;
    private ArrayList<String> ip_data;

    // 송수신 데이터 자바내 데이터 송수신
    private final Handler uiHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case ServerThreadDET.MESSAGE_DET_DATA:
                    String bboxdata = (String) msg.obj;
                    DataHolderDET.getInstance().setBboxdata(bboxdata);
                    break;
                case ServerThreadSEG.MESSAGE_SEG_DATA:
                    Bitmap receivedBitmap = (Bitmap) msg.obj;
                    DataHolderSEG.getInstance().setSegdata(receivedBitmap);
                    break;
            }
        }
    };

    private void setupCustomSurfaceListener() {
        if (customSurfaceListener == null) {
            customSurfaceListener = new CustomSurfaceListener(
                    cameraHandler,
                    textureView,
                    model,
                    toggleSeg, toggleDet, toggleDet2,
                    bdbox,
                    device1_state, device2_state, device3_state,
                    ip_data);
            textureView.setSurfaceTextureListener(customSurfaceListener);
        } else {
            customSurfaceListener.updateVariables(
                    toggleSeg, toggleDet, toggleDet2,
                    device1_state, device2_state, device3_state,
                    ip_data);
        }
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
        serverThread = new Thread(new ServerThreadDET(3001, uiHandler, device1_state));
        serverThread.start();

        // Device2 (SEG)
        serverThread2 = new Thread(new ServerThreadSEG(3002, uiHandler, device2_state));
        serverThread2.start();

        // Device3 (DET)
        serverThread3 = new Thread(new ServerThreadDET(3003, uiHandler, device3_state));
        serverThread3.start();

        // set the camera preview state
        this.cameraHandler = new CameraHandler(this, getApplicationContext(), textureView);
        setupCustomSurfaceListener();


        updateButtonText(toggleSegButton, "Seg", toggleSeg);
        updateButtonText(toggleDetButton, "Det", toggleDet);
        updateButtonText(toggleDet2Button, "Det", toggleDet2);
        StateSingleton.runScanning = true;
        updateButtonText(startBtn, "Send", StateSingleton.runScanning);

        // stop/start the client to server bytes transfer
        this.startBtn.setOnClickListener(view -> {
            StateSingleton.runScanning = !StateSingleton.runScanning;
            updateButtonText(startBtn, "Send", StateSingleton.runScanning);
        });

        // Seg Button
        this.toggleSegButton.setOnClickListener(v -> {
            toggleSeg = !toggleSeg;
            toggleSegButton.setEnabled(true);
            updateButtonText(toggleSegButton, "Seg", toggleSeg);

            setupCustomSurfaceListener();
        });

        // Det Button
        this.toggleDetButton.setOnClickListener(v -> {
            toggleDet = !toggleDet;
            toggleDetButton.setEnabled(true);
            updateButtonText(toggleDetButton, "Det", toggleDet);

            setupCustomSurfaceListener();
        });

        // Det2 Button
        this.toggleDet2Button.setOnClickListener(v -> {
            toggleDet2 = !toggleDet2;
            toggleDet2Button.setEnabled(true);
            updateButtonText(toggleDet2Button, "Det", toggleDet2);

            setupCustomSurfaceListener();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        serverThread.interrupt();
        serverThread2.interrupt();
        serverThread3.interrupt();

        serverThread = null;
        serverThread2 = null;
        serverThread3 = null;

        DataHolderDET.getInstance().setBboxdata(null);
        DataHolderSEG.getInstance().setSegdata(null);
    }

    private void updateButtonText(Button button, String label, boolean isToggleOn) {
        String buttonText = label + ": " + (isToggleOn ? "ON" : "OFF");
        button.setText(buttonText);
    }

    private void reload() {
        if (!model.loadModel(getAssets(), current_cpugpu))
            Log.e(TAG, "model load failed");
        Log.e(TAG, "model load success");
    }
}