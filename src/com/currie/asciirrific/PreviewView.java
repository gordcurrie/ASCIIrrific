package com.currie.asciirrific;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class PreviewView extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private Size bestSize;
    private AsciiView asciiView;
    private int bestArea;

    public PreviewView(Context context) {
        super(context);
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
    }
    
    public PreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
    }

    public void setCamera(Camera camera, int defaultCameraId) {
        this.camera = camera;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        bestSize = getBestPreviewSize(asciiView.getWidth(), asciiView.getHeight());
        asciiView.setCameraSize(bestSize);
        Camera.Parameters params = camera.getParameters();
        params.setPreviewSize(bestSize.width, bestSize.height);
        camera.setParameters(params);
        camera.startPreview();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (camera != null) {
                camera.setPreviewDisplay(holder);
                camera.setPreviewCallback(new PreviewCallback() {

                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        asciiView.setImageData(data);
                        asciiView.invalidate();
                    }
                });
            }
        } catch (IOException exception) {
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (camera != null) {
            camera.stopPreview();
        }
    }

    /**
     * selects the preview size with the largest area
     */
    private Camera.Size getBestPreviewSize(int width, int height) {
        Camera.Size bestSize = null;
        List<Size> sizes = camera.getParameters().getSupportedPreviewSizes();
        for (Camera.Size size : sizes) {
            if (bestSize == null) {
                bestSize = size;
            } else {
                bestArea = bestSize.width * bestSize.height;
                int area = size.width * size.height;
                if (bestArea < area) {
                    bestSize = size;
                }
            }
        }
        return bestSize;
    }

    public void setAsciiView(AsciiView asciiView) {
        this.asciiView = asciiView;
    }
    
    /**
     * not used saving in case add portrait capabilities
     */
    public static void setCameraDisplayOrientation(Activity activity,
            int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }
}
