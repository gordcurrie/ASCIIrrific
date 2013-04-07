package com.currie.asciirrific;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity implements OnGestureListener {

    private static final String USE_FRONT_CAMERA = "useFrontCamera";
    private static final String PREFERENCES_NAME = "my_preferences";
    private PreviewView previewView;
    private Camera camera;
    private CameraInfo cameraInfo;
    private int numberOfCameras;
    private int cameraId;
    private AsciiView asciiView;
    private boolean useFrontCamera;
    private ImageButton shutterButton;
    private boolean hidePreview = false;
    private SharedPreferences preferences;
    private GestureDetector detector;
    private MediaPlayer shutterClick;
    private static TextView seekbarText;
    private static boolean isLandscape;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences(PREFERENCES_NAME, 0);
        useFrontCamera = preferences.getBoolean(USE_FRONT_CAMERA, false);
        numberOfCameras = Camera.getNumberOfCameras();
        selectCamera();
        asciiView = (AsciiView) findViewById(R.id.asciiView);
        previewView = (PreviewView) findViewById(R.id.preview);
        previewView.setAsciiView(asciiView);
        shutterButton = (ImageButton) findViewById(R.id.shutterButton);
        //Setting up gesture detector for fling to gallery
        detector = new GestureDetector(this, this);
        //Adding vibration and sound on shutter button click
        shutterButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    shutterButton.setImageResource(R.drawable.btn_camera_shutter_pressed_holo);
                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    playShutterClick();
                    vibrator.vibrate(25);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    shutterButton.setImageResource(R.drawable.btn_camera_shutter_holo);
                } 
                return false;
            }
        });
    }

    /**
     * getting id of selected camera
     */
    private void selectCamera() {
        cameraInfo = new CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == (useFrontCamera ? CameraInfo.CAMERA_FACING_FRONT : CameraInfo.CAMERA_FACING_BACK)) {
                cameraId = i;
            }
        }
    }

    /**
     * toggles between cameras and updates shared pref
     */
    public void switchCameras(View view) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(USE_FRONT_CAMERA, !useFrontCamera);
        editor.commit();
        finish();
        startActivity(getIntent());
    }

    /**
     * toggles size of preview windown
     */
    public void hidePreview(View view) {
        LayoutParams params = previewView.getLayoutParams();
        double factor = hidePreview ? 0.2 : 0.1;
        params.height = (int) (asciiView.getHeight() * factor);
        params.width = (int) (asciiView.getWidth() * factor);
        hidePreview = !hidePreview;
        previewView.setLayoutParams(params);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (camera != null) {
            previewView.setCamera(null, -1);
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera = Camera.open(cameraId);
        previewView.setCamera(camera, cameraId);
        checkOrientation();
    }

    /**
     * checks orientation of camera asks to rotate if in portrait.
     */
    private void checkOrientation() {
        getResources().getConfiguration();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            isLandscape = false;
            setContentView(R.layout.view_please_rotate);
        } else {
            isLandscape = true;
        }
    }
    
    /**
     * when called launched dialog for changing font size
     */
    public void changeFontSize(View view) {
        View fontView = View.inflate(this, R.layout.font_size_view, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //using seekbar for user input of selected size
        final SeekBar seekbar = (SeekBar) fontView.findViewById(R.id.sb_font_size);
        seekbarText = (TextView) fontView.findViewById(R.id.tv_font_size);
        seekbar.setMax(25);
        seekbar.setProgress((int) AsciiView.getFontSize());
        seekbarText.setText(seekbar.getProgress() + "pt");
        builder.setView(fontView)
            .setTitle("Set font size:")
            .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AsciiView.setFontSize(seekbar.getProgress());
                    
                }
            })
            .setNegativeButton("Cancle", new DialogInterface.OnClickListener() {
                
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    
                }
            })
            .create()
            .show();
        //listener to track value of seekbar and update text view
        seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress > 5) {
                    seekbarText.setText(progress + "pt");
                } else {
                    progress = 5;
                    seekbarText.setText(progress + "pt");
                    seekbar.setProgress(progress);
                }
                
            }
        });
    }

    /**
     * calls the savePicture method of asciiView
     */
    public void takePicture(View view) {
        asciiView.savePicture();
    }

    @Override
    public boolean onDown(MotionEvent arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * onFling opens up ViewImageActivity
     */
    @Override
    public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
        if (Math.abs(arg0.getRawY() - arg1.getRawY()) > 5) {
            if (AsciiView.mediaStorageDir.listFiles().length > 0) {
                startActivity(new Intent(this, ViewImageActivity.class));
            }
        }
        return false;
    }
    

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return detector.onTouchEvent(event);
    }

    @Override
    public void onLongPress(MotionEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onShowPress(MotionEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onSingleTapUp(MotionEvent arg0) {
        // TODO Auto-generated method stub
        return false;
    }
    
    /**
     * plays shutter click sound file
     */
    private void playShutterClick()
    {
        AudioManager meng = (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);
        int volume = meng.getStreamVolume( AudioManager.STREAM_NOTIFICATION);

        if (volume != 0)
        {
            if (shutterClick == null) {
                shutterClick = MediaPlayer.create(this, Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
            }
            shutterClick.start();
        }
    }

    public static boolean isLandscape() {
        return isLandscape;
    }

    public static void setLandscape(boolean isLandscape) {
        MainActivity.isLandscape = isLandscape;
    }
    
    /**
     * launches intent to share url of app on play store.
     */
    public void shareApp(View view) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=com.currie.asciirrific");
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Check out this app!");
        startActivity(Intent.createChooser(intent, "Share"));
    }
}
