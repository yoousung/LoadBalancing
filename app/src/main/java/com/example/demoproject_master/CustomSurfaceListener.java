package com.example.demoproject_master;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.view.TextureView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Objects;

public class CustomSurfaceListener implements TextureView.SurfaceTextureListener {

    protected CameraHandler cameraHandler;
    protected TextureView textureView;
    protected boolean wait = false;
    //protected int interval = 1000;
    protected int interval = 50; // 이미지 데이터 전송 딜레이 설정
    private Ncnn model;
    private boolean toggleSeg, toggleDet, toggleDet2;
    private ImageView bdbox;
    private TextView device1_state, device2_state, device3_state;

    public CustomSurfaceListener(CameraHandler cameraHandler,
                                 TextureView textureView,
                                 Ncnn model,
                                 boolean toggleSeg, boolean toggleDet, boolean toggleDet2,
                                 ImageView bdbox,
                                 TextView device1_state, TextView device2_state, TextView device3_state) {
        this.cameraHandler = cameraHandler;
        this.textureView = textureView;
        this.model = model;
        this.toggleSeg = toggleSeg;
        this.toggleDet = toggleDet;
        this.toggleDet2 = toggleDet2;
        this.bdbox = bdbox;
        this.device1_state = device1_state;
        this.device2_state = device2_state;
        this.device3_state = device3_state;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        this.cameraHandler.openCamera();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) { }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    // 추론부
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        // 1) 이미지 캡쳐
        Bitmap currentbmp = this.textureView.getBitmap();

        boolean[] opt = new boolean[3];
        opt[0] = toggleSeg;
        opt[1] = toggleDet;
        opt[2] = toggleDet2;

        // 이미지 전송 버튼 start일때 데이터 전송
        if(!StateSingleton.getInstance().waitInterval && StateSingleton.getInstance().runScanning) {
            StateSingleton.getInstance().waitInterval = true;
            // 이미지 전송
            Thread socketThread = new Thread(new SocketThread(currentbmp));
            socketThread.start();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    StateSingleton.getInstance().waitInterval = false;
                }
            }, interval);
        }
        Bitmap newbitmap;
        if (opt[0] | opt[1] | opt[2]) {
            model.homogeneousComputing(bdbox, currentbmp, opt);
            Drawable drawable = bdbox.getDrawable();
            newbitmap = ((BitmapDrawable) drawable).getBitmap();
        }
        else {
            newbitmap = currentbmp;
        }

        // TODO 동작설정
        String bboxdata = null;
        Bitmap segbitmap = null;
        // 2-1) det
        if(device1_state.getText().equals("on"))
            bboxdata = BboxDataHolder.getInstance().getBboxdata();
        if(Objects.equals(bboxdata, " "))
            bboxdata = null;
        // 2-2) seg
        if(device2_state.getText().equals("on"))
            segbitmap = SegDataHolder.getInstance().getSegdata();

        model.heterogeneousComputing(bdbox, newbitmap, bboxdata, segbitmap);
    }
}
