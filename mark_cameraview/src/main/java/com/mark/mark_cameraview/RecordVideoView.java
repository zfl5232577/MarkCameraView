package com.mark.mark_cameraview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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
    private ImageView ivShowPicture;
    private CaptureButton mCaptureButton;
    private ImageView ivSwapCamera;
    private RecordVideoListener mRecordVideoListener;
    private String mRecordFileDir = Constants.TEMP_PATH;
    private String mRecordFilePath;
    private static Handler mBackgroundHandler;
    private boolean isBrowse;
    private byte[] mPictureByte;
    private Bitmap mPictureBitmap;

    private int mMode;
    public static final int TAKE_PHOTO = 1;
    public static final int TAKE_RECORD = 2;
    public static final int TAKE_PHOTO_RECORD = 3;


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
        ivShowPicture = findViewById(R.id.iv_ShowPicture);
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

    public static Handler getBackgroundHandler() {
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
        mCameraView.getPreview().playVideo(file);
    }

    private CaptureButton.CaptureListener mCaptureListener = new CaptureButton.CaptureListener() {
        @Override
        public void capture() {
            isBrowse = true;
            ivSwapCamera.setVisibility(GONE);
            getBackgroundHandler().postAtFrontOfQueue(new Runnable() {
                @Override
                public void run() {
                    mCameraView.takePicture();
                    deleteFile();
                    getBackgroundHandler().removeCallbacksAndMessages(null);
                }
            });
        }

        @Override
        public void cancel() {
            isBrowse = false;
            if (ivShowPicture.isShown()) {
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ivShowPicture.setVisibility(GONE);
                    }
                }, 400);
            }
            ivSwapCamera.setVisibility(VISIBLE);
            mPictureByte = null;
            if (mPictureBitmap != null) {
                mPictureBitmap.recycle();
                mPictureBitmap = null;
            }
            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {
                    mCameraView.start();
                }
            });
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
            ivSwapCamera.setVisibility(GONE);
            if (Build.VERSION.SDK_INT < 21) {
                getBackgroundHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mCameraView.startRecord(createRecordDir());
                    }
                }, 200);
            } else {
                getBackgroundHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        mCameraView.startRecord(createRecordDir());
                    }
                });
            }
        }

        @Override
        public void rencordEnd() {
            ivSwapCamera.setVisibility(GONE);
            isBrowse = true;
            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {
                    mCameraView.stopRecord();
                    mCameraView.stop();
                    browseRecord(new File(mRecordFilePath));
                }
            });
        }

        @Override
        public void rencordFail() {
            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {
                    mCameraView.stopRecord();
                    getBackgroundHandler().removeCallbacksAndMessages(null);
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
            mCameraView.getPreview().stopVideo();
            isBrowse = false;
            ivSwapCamera.setVisibility(VISIBLE);
            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {
                    mCameraView.start();
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
            if (mPictureBitmap != null && !mCameraView.getPreview().isReady()) {
                ivShowPicture.setImageBitmap(mPictureBitmap);
                ivShowPicture.setVisibility(VISIBLE);
            } else {
                mCameraView.getPreview().resumeVideo();
            }
            return;
        }
        mCameraView.start();
    }

    public void stop() {
        if (isBrowse) {
            mCameraView.getPreview().pauseVideo();
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
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
    }

    @Mode
    public int getMode() {
        return mMode;
    }

    public void setMode(@Mode int mode) {
        mMode = mode;
        mCaptureButton.setMode(mode);
    }

    @IntDef({TAKE_PHOTO, TAKE_RECORD, TAKE_PHOTO_RECORD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Mode {
    }

    public void setFacing(@CameraView.Facing int facing) {
        mCameraView.setFacing(facing);
    }

    /**
     * Gets the direction that the current camera faces.
     *
     * @return The camera facing.
     */
    @CameraView.Facing
    public int getFacing() {
        //noinspection WrongConstant
        return mCameraView.getFacing();
    }


    /**
     * Sets the aspect ratio of camera.
     *
     * @param ratio The {@link AspectRatio} to be set.
     */
    public void setAspectRatio(@NonNull AspectRatio ratio) {
        mCameraView.setAspectRatio(ratio);
    }

    /**
     * Gets the current aspect ratio of camera.
     *
     * @return The current {@link AspectRatio}. Can be {@code null} if no camera is opened yet.
     */
    @Nullable
    public AspectRatio getAspectRatio() {
        return mCameraView.getAspectRatio();
    }

    /**
     * Enables or disables the continuous auto-focus mode. When the current camera doesn't support
     * auto-focus, calling this method will be ignored.
     *
     * @param autoFocus {@code true} to enable continuous auto-focus mode. {@code false} to
     *                  disable it.
     */
    public void setAutoFocus(boolean autoFocus) {
        mCameraView.setAutoFocus(autoFocus);
    }

    /**
     * Returns whether the continuous auto-focus mode is enabled.
     *
     * @return {@code true} if the continuous auto-focus mode is enabled. {@code false} if it is
     * disabled, or if it is not supported by the current camera.
     */
    public boolean getAutoFocus() {
        return mCameraView.getAutoFocus();
    }

    /**
     * Sets the flash mode.
     *
     * @param flash The desired flash mode.
     */
    public void setFlash(@CameraView.Flash int flash) {
        mCameraView.setFlash(flash);
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
            file = File.createTempFile("picture", ".jpg", sampleDir);
        } catch (IOException e) {
            e.printStackTrace();
            file = new File(mRecordFileDir, "picture" + System.currentTimeMillis() + ".jpg");
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
