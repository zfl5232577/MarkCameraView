package com.mark.markcameravew;

import android.Manifest;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.cjt2325.cameralibrary.JCameraView;
import com.mark.mark_cameraview.RecordVideoView;

public class MainActivity extends AppCompatActivity {

    private RecordVideoView mRecordVideoView;
    private JCameraView mJCameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                100);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        if (Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(option);
        }
        mRecordVideoView = findViewById(R.id.recordVideoView);
//        mJCameraView = findViewById(R.id.cameraview);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRecordVideoView.start();

    }
    @Override
    protected void onPause() {
        super.onPause();
        mRecordVideoView.stop();
    }
}
