package com.mark.mark_cameraview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.mark.aoplibrary.annotation.TimeLog;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.mark.mark_cameraview.CameraView.FACING_BACK;

/**
 * <pre>
 *     author : Mark
 *     e-mail : makun.cai@aorise.org
 *     time   : 2018/09/19
 *     desc   : TODO
 *     version: 1.0
 * </pre>
 */
public class RecordVideoView extends FrameLayout {

    private Context mContext;
    private CameraView mCameraView;
    private CaptureButton mCaptureButton;
    private ImageView ivSwapCamera;
    private RecordVideoListener mRecordVideoListener;
    private MediaPlayer mediaPlayer;
    private String mRecordFileDir = Constants.TEMP_PATH;
    private String mRecordFilePath;
    private Handler mBackgroundHandler;
    private boolean isBrowse;
    private boolean isSetWHFinish;
    private byte[] mPictureByte;
    private Bitmap mPictureBitmap;


    public RecordVideoView(@NonNull Context context) {
        this(context, null);
    }

    public RecordVideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecordVideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        // 初始化各项组件
        FrameLayout.inflate(context, R.layout.layout_record_video_view, this);
        mCameraView = findViewById(R.id.cameraView);
        mCaptureButton = findViewById(R.id.captureButton);
        ivSwapCamera = findViewById(R.id.swap_camera);
        mCameraView.addCallback(mCallback);
        mCaptureButton.setCaptureListener(mCaptureListener);
        ivSwapCamera.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                swapCamera();
            }
        });
        // Attributes
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RecordVideoView, defStyleAttr,
                R.style.Widget_RecordVideoView);
        mCameraView.setFacing(a.getInt(R.styleable.RecordVideoView_facing, FACING_BACK));
        String aspectRatio = a.getString(R.styleable.RecordVideoView_aspectRatio);
        if (aspectRatio != null) {
            mCameraView.setAspectRatio(AspectRatio.parse(aspectRatio));
        } else {
            mCameraView.setAspectRatio(Constants.DEFAULT_ASPECT_RATIO);
        }
        mCameraView.setAutoFocus(a.getBoolean(R.styleable.RecordVideoView_autoFocus, true));
        mCameraView.setFlash(a.getInt(R.styleable.RecordVideoView_flash, Constants.FLASH_AUTO));
        a.recycle();

    }

    public void swapCamera() {
        if (mCameraView.mImpl.getFacing() == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mCameraView.mImpl.setFacing(Camera.CameraInfo.CAMERA_FACING_BACK);
        } else {
            mCameraView.mImpl.setFacing(Camera.CameraInfo.CAMERA_FACING_FRONT);
        }
    }

    public void setRecordVideoListener(RecordVideoListener recordVideoListener) {
        mRecordVideoListener = recordVideoListener;
    }

    public CameraView getCameraView() {
        return mCameraView;
    }

    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background", 1000);
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }


    private CameraView.Callback mCallback
            = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {

        }

        @Override
        public void onCameraClosed(CameraView cameraView) {

        }

        @Override
        public void onPictureTaken(CameraView cameraView, Object data) {
            if (data instanceof Bitmap) {
                mPictureBitmap = (Bitmap) data;
            } else {
                mPictureByte = (byte[]) data;
            }
        }

    };

    private void browseRecord(File file) {
        if (!file.exists()) {
            Toast.makeText(getContext(), "视频文件路径错误", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            }
            mediaPlayer.reset();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            // 设置播放的视频源
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.prepareAsync();
            // 设置显示视频的SurfaceHolder
            mediaPlayer.setSurface(mCameraView.getPreview().getSurface());
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    if (mediaPlayer != null) {
                        mediaPlayer.start();
                        // 按照初始位置播放
                        mediaPlayer.seekTo(0);
                    }
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (mediaPlayer != null) {
                        // 在播放完毕被回调
                        mediaPlayer.seekTo(0);
                        mediaPlayer.start();
                    }
                }
            });
            mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(MediaPlayer mp, final int width, final int height) {
                    if (isSetWHFinish) {
                        return;
                    }
                    isSetWHFinish = true;
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mCameraView.getPreview().updateVideoPreviewSizeCenter(width, height);
                        }
                    },200);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CaptureButton.CaptureListener mCaptureListener = new CaptureButton.CaptureListener() {
        @Override
        public void capture() {
            isBrowse = true;
            ivSwapCamera.setVisibility(GONE);
            getBackgroundHandler().postAtFrontOfQueue(new Runnable() {
                @Override
                public void run() {
                    Log.e("mark", "run:start ");
                    mCameraView.takePicture();
                    deleteFile();
                    getBackgroundHandler().removeCallbacksAndMessages(null);
                    Log.e("mark", "run:end ");
                }
            });
        }

        @Override
        public void cancel() {
            isBrowse = false;
            ivSwapCamera.setVisibility(VISIBLE);
            mCameraView.start();
            mPictureByte = null;
            if (mPictureBitmap != null) {
                mPictureBitmap.recycle();
                mPictureBitmap = null;
            }
//            getBackgroundHandler().post(new Runnable() {
//                @Override
//                public void run() {
//                }
//            });
        }

        @Override
        public void determine() {
            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {
                    savePicture();
                }
            });
        }

        @Override
        public void quit() {
            if (mRecordVideoListener != null) {
                mRecordVideoListener.quit();
            }
        }

        @Override
        public void record() {
            getBackgroundHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCameraView.startRecord(createRecordDir());
                }
            }, 200);
        }

        @Override
        public void rencodEnd() {
            mCameraView.stopRecord();
            mCameraView.stop();
            isBrowse = true;
            ivSwapCamera.setVisibility(GONE);
            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {
                    browseRecord(new File(mRecordFilePath));
                }
            });
        }

        @Override
        public void getRecordResult() {
            if (mRecordVideoListener != null) {
                mRecordVideoListener.onRecordTaken(new File(mRecordFilePath));
            }
        }

        @Override
        public void deleteRecordResult() {
            if (mediaPlayer != null) {
                isSetWHFinish = false;
                mediaPlayer.stop();
                mediaPlayer.reset();
                cleanDraw();
            }
            mCameraView.start();
            isBrowse = false;
            ivSwapCamera.setVisibility(VISIBLE);
            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {
                    deleteFile();
                }
            });
        }

        @Override
        public void scale(float scaleValue) {

        }
    };

    private void savePicture() {
        File pictureFile = createPictureDir();
        if (mPictureBitmap != null) {
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(pictureFile);
                mPictureBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                mPictureBitmap.recycle();
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(pictureFile));
                bos.write(mPictureByte, 0, mPictureByte.length);
                bos.flush();
                bos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mRecordVideoListener != null) {
            mRecordVideoListener.onPictureTaken(pictureFile);
        }
        Toast.makeText(mContext, "保存成功", Toast.LENGTH_LONG).show();
    }

    private void deleteFile() {
        if (TextUtils.isEmpty(mRecordFilePath)) {
            return;
        }
        File file = new File(mRecordFilePath);
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    public void start() {
        if (isBrowse) {
            if (mediaPlayer != null) {
                mediaPlayer.start();
            }
            return;
        }
        isSetWHFinish = false;
        mCameraView.start();
    }

    public void stop() {
        if (isBrowse) {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
            return;
        }
        mCameraView.stop();
    }

    public void onDestroy() {
        mPictureByte = null;
        if (mPictureBitmap != null) {
            mPictureBitmap.recycle();
            mPictureBitmap = null;
        }
        releaseMediaPlayer();
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.setDisplay(null);
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void cleanDraw() {
//        Canvas canvas = ((TextureView)mCameraView.getPreview().getView()).lockCanvas();
//        canvas.drawColor(Color.BLACK);
//        ((TextureView)mCameraView.getPreview().getView()).unlockCanvasAndPost(canvas);
    }

    private File createRecordDir() {
        File sampleDir = new File(mRecordFileDir);
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        // 创建文件
        File file;
        try {
            file = File.createTempFile("record", ".mp4", sampleDir);
        } catch (IOException e) {
            e.printStackTrace();
            file = new File(mRecordFileDir, "record" + System.currentTimeMillis() + ".mp4");
            if (!file.exists()) {
                file.mkdirs();
            }
        }
        mRecordFilePath = file.getAbsolutePath();
        return file;
    }

    private File createPictureDir() {
        File sampleDir = new File(mRecordFileDir);
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        // 创建文件
        File file;
        try {
            file = File.createTempFile("picture", ".png", sampleDir);
        } catch (IOException e) {
            e.printStackTrace();
            file = new File(mRecordFileDir, "picture" + System.currentTimeMillis() + ".png");
            if (!file.exists()) {
                file.mkdirs();
            }
        }
        mRecordFilePath = file.getAbsolutePath();
        return file;
    }

    public interface RecordVideoListener {
        void onRecordTaken(File recordFile);

        void onPictureTaken(File pictureFile);

        void quit();
    }

}
