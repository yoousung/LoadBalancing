package com.example.demoproject_master;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.TextureView;
import android.widget.ImageView;

public class CustomSurfaceListener implements TextureView.SurfaceTextureListener {

    protected CameraHandler cameraHandler;
    protected TextureView textureView;
    protected boolean wait = false;
    //protected int interval = 1000;
    protected int interval = 50; // 이미지 데이터 전송 딜레이 설정
    private Ncnn model;
    private boolean toggleSeg;
    private boolean toggleDet;
    private ImageView bdbox;

    public CustomSurfaceListener(CameraHandler cameraHandler, TextureView textureView, Ncnn model, boolean toggleSeg, boolean toggleDet, ImageView bdbox) {
        this.cameraHandler = cameraHandler;
        this.textureView = textureView;
        this.model = model;
        this.toggleSeg = toggleSeg;
        this.toggleDet = toggleDet;
        this.bdbox = bdbox;
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
        opt[2] = false;

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

        // 2) 캡쳐한 이미지 객체 인식
        model.predict(bdbox, currentbmp, opt);

        Drawable drawable = bdbox.getDrawable();
        Bitmap newbitmap = ((BitmapDrawable) drawable).getBitmap();

        // 2-1) BBOX그리기
//        String bboxdata = BboxDataHolder.getInstance().getBboxdata();
//        //Log.e("CustomSurfaceListener", "BBOX : " + bboxdata);
//        if(bboxdata!=null && bboxdata!=" "){
//            model.draw_Bbox(bdbox, newbitmap, bboxdata);
//        }

        // 2-2) seg그리기
        Bitmap segbitmap = SegDataHolder.getInstance().getSegdata();
        if(segbitmap!=null){
            model.draw_Seg(bdbox, newbitmap, segbitmap);
        }



    }
}
