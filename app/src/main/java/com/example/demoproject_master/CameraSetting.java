package com.example.demoproject_master;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.CAMERA_SERVICE;

public class CameraSetting {
    private static final String TAG = "CameraSetting";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private Context context;
    private TextureView cameraView;
    private CameraDevice cameraDevice;
    private String cameraId;
    private CameraCaptureSession previewSession;
    private CaptureRequest.Builder previewBuilder;
    private List<Surface> outputSurfaces = new ArrayList<>();
    private Range<Integer> desiredFpsRange = null;

    public CameraSetting(Context context, TextureView cameraView) {
        this.context = context;
        this.cameraView = cameraView;
    }


    // 카메라 열기
    public void openCamera() {
        openCameraInternal();
    }

    private void openCameraInternal(){
        System.out.println("openCamera를 실행합니다.");
        CameraManager manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);

        if (cameraView.getSurfaceTexture() != null ) {
            outputSurfaces.add( new Surface(cameraView.getSurfaceTexture() ) ); // 카메라 뷰 서페이스에 추가 (현재 1개)
        }

        try {
            // 카메라 API2 매니저 생성
            cameraId = manager.getCameraIdList()[0];
            Log.e(TAG, "카메라 ID : " + manager.getCameraIdList());

            // 카메라 특징 및 하드웨어 레벨 얻어오기
            CameraCharacteristics characteristic = manager.getCameraCharacteristics(cameraId);
            int level = characteristic.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);

            // Stram캡처 모드로 설정
            StreamConfigurationMap map = characteristic.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            // 카메라에 나타나는 해상도 Size알기
            Size[] outputSizes = map.getOutputSizes(SurfaceTexture.class);
            for (Size size : outputSizes) {
                Log.e(TAG, "카메라 해상도 : "+size);
            }

            // 카메라 노츨 및 프레임 속도 설정
            Range<Integer> fps[] = characteristic.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            for (int i = 0; i < fps.length; i++) {
                Range<Integer> FPS = fps[i];
                Log.e(TAG, (i + 1) + "th fps : " + FPS);
            }
            Range<Integer> fpsForVideo[] = map.getHighSpeedVideoFpsRanges();
            for (int i = 0; i < fpsForVideo.length; i++) {
                Range<Integer> FPSFORVIDEO = fpsForVideo[i];
                Log.d(TAG, (i + 1) + "th fpsForVideo : " + FPSFORVIDEO);
            }

            //*********************************************************************
            // 프리뷰 해상도 선택하기
            Size mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];
            // 프리뷰 fps 설정
            desiredFpsRange = fps[fps.length-1];
            //*********************************************************************

            Log.e(TAG, "프리뷰 크기 : height : " + mPreviewSize.getHeight() + " , width : " + mPreviewSize.getWidth());
            Log.e(TAG, "카메라 하드웨어 레벨 : " + level);
            Log.e(TAG, "현재 프리뷰 FPS : "+ desiredFpsRange);
            Log.d(TAG, cameraId + "번 카메라를 엽니다.");

            // 카메라 권한이 확인 되었을 때
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                // 카메라 디바이스 콜백 메서드 -> 프리뷰 설정 및 시작
                manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        cameraDevice = camera;

                        Log.e(TAG, "카메라 연결을 시작합니다.");

                        try {
                            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                        previewBuilder.addTarget(outputSurfaces.get(0));

                        // 이 부분 수정
                        try {
                            //============================================================================================================================
                            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                                @Override
                                public void onConfigured(@NonNull CameraCaptureSession session) {
                                    if (cameraDevice == null) {
                                        Log.e(TAG, "오류가 발생했습니다.");
                                        return;
                                    }

                                    previewSession = session;
                                    // FPS설정
                                    //previewBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                                    previewBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, desiredFpsRange);

                                    HandlerThread thread = new HandlerThread( "CameraPreview" );
                                    thread.start();
                                    Handler backgroundHandler = new Handler( thread.getLooper() );

                                    try {
                                        previewSession.setRepeatingRequest(previewBuilder.build(), null, backgroundHandler);
                                        Log.e(TAG, "프리뷰를 시작합니다.");

                                    } catch (CameraAccessException e) {
                                        e.printStackTrace();
                                    }
                                    Log.e(TAG, "카메라를 열었습니다.");
                                }

                                @Override
                                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                    // configure이 실패했을 때
                                    Log.e(TAG, "configure에 실패했습니다.");
                                    camera.close();
                                }
                            }, null);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {
                        Log.e(TAG, "카메라 연결이 끊겼습니다.");
                        camera.close();
                        cameraDevice = null;
                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                        camera.close();
                        cameraDevice = null;
                        Log.e(TAG, "카메라를 열 수 없습니다."+error);
                    }
                }, null);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "카메라를 여는 데 문제가 발생했습니다: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (previewSession != null) {
            previewSession.close();
            previewSession = null;
        }
        outputSurfaces.clear();
    }
}
