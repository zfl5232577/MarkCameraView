/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mark.mark_cameraview;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;

@TargetApi(14)
class TextureViewPreview extends PreviewImpl {

    private static final String TAG = TextureViewPreview.class.getSimpleName();
    private final TextureView mTextureView;

    private int mDisplayOrientation;
    private MediaPlayer mediaPlayer;
    private boolean isSetWHFinish;
    private int mCurrentPosition;
    private File mRecordFile;

    TextureViewPreview(Context context, ViewGroup parent) {
        final View view = View.inflate(context, R.layout.texture_view, parent);
        mTextureView = view.findViewById(R.id.texture_view);
        mTextureView.bringToFront();
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.e(TAG, "onSurfaceTextureAvailable: "+width+height );
                setSize(width, height);
                configureTransform();
                dispatchSurfaceChanged();
                if (mRecordFile!=null){
                    playVideo(mRecordFile);
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                Log.e(TAG, "onSurfaceTextureSizeChanged: "+width+height );
                setSize(width, height);
                configureTransform();
                dispatchSurfaceChanged();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.e(TAG, "onSurfaceTextureDestroyed: ");
                setSize(0, 0);
                isSetWHFinish = false;
                if (mediaPlayer != null) {
                    mCurrentPosition = mediaPlayer.getCurrentPosition();
                    isSetWHFinish = false;
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    // This method is called only from Camera2.
    @TargetApi(15)
    @Override
    void setBufferSize(int width, int height) {
        mTextureView.getSurfaceTexture().setDefaultBufferSize(width, height);
    }

    @Override
    Surface getSurface() {
        return new Surface(mTextureView.getSurfaceTexture());
    }

    @Override
    SurfaceTexture getSurfaceTexture() {
        return mTextureView.getSurfaceTexture();
    }

    @Override
    View getView() {
        return mTextureView;
    }

    @Override
    Class getOutputClass() {
        return SurfaceTexture.class;
    }

    @Override
    void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
        configureTransform();
    }

    @Override
    boolean isReady() {
        return mTextureView.getSurfaceTexture() != null;
    }

    @Override
    void playVideo(File recordFile) {
        if (!recordFile.exists()) {
            return;
        }
        mRecordFile = recordFile;
        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            }
            mediaPlayer.reset();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            // 设置播放的视频源
            mediaPlayer.setDataSource(recordFile.getAbsolutePath());
            mediaPlayer.prepareAsync();
            // 设置显示视频的SurfaceHolder
            mediaPlayer.setSurface(getSurface());
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    if (mediaPlayer != null) {
                        mediaPlayer.start();
                        // 按照初始位置播放
                        mediaPlayer.seekTo(mCurrentPosition);
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
                    Log.e(TAG, "onVideoSizeChanged: "+ width+height);
                    if (width==getWidth()&& height==getHeight() || isSetWHFinish) {
                        return;
                    }
                    mTextureView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            updateVideoPreviewSizeCenter(width, height);
                        }
                    }, 200);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    void stopVideo() {
        if (mediaPlayer != null) {
            mRecordFile = null;
            mCurrentPosition = 0;
            mediaPlayer.reset();
            mediaPlayer.stop();
        }
    }

    @Override
    boolean pauseVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            return true;
        }
        //TextureView.onSurfaceTextureDestroyed息屏后被调用。mediaPlayer被回收需要返回false
        return false;
    }

    @Override
    boolean resumeVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            return true;
        }
        //TextureView.onSurfaceTextureDestroyed息屏后被调用。mediaPlayer被回收需要返回false
        return false;
    }

    @Override
    void updateVideoPreviewSizeCenter(int width, int height) {
        Log.e(TAG, "updateVideoPreviewSizeCenter: width"+width+height );
        Log.e(TAG, "updateVideoPreviewSizeCenter: getWidth()"+getWidth()+getHeight() );
        float sx = (float) getWidth() / (float) width;
        float sy = (float) getHeight() / (float) height;
        if (sx==1 && sy==1){
            isSetWHFinish =false;
        }else {
            isSetWHFinish =true;
        }
        Matrix matrix = new Matrix();

        //第1步:把视频区移动到View区,使两者中心点重合.
        matrix.preTranslate((getWidth() - width) / 2, (getHeight() - height) / 2);

        //第2步:因为默认视频是fitXY的形式显示的,所以首先要缩放还原回来.
        matrix.preScale(width / (float) getWidth(), height / (float) getHeight());

        //第3步,等比例放大或缩小,直到视频区的一边和View一边相等.如果另一边和view的一边不相等，则留下空隙
        if (sx >= sy) {
            matrix.postScale(sy, sy, getWidth() / 2, getHeight() / 2);
        } else {
            matrix.postScale(sx, sx, getWidth() / 2, getHeight() / 2);
        }

        mTextureView.setTransform(matrix);
        mTextureView.postInvalidate();
    }

    @Override
    void updatePicturePreviewSizeCenter(int degrees) {
        if (degrees == 0) {
            return;
        }
        isSetWHFinish = true;
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees, getWidth() / 2, getHeight() / 2);
        if (degrees == 90 || degrees == 270) {
            float s = (float) getWidth() / (float) getHeight();
            matrix.postScale(s, s, getWidth() / 2, getHeight() / 2);
        }
        mTextureView.setTransform(matrix);
        mTextureView.postInvalidate();
    }

    public boolean isSetWHFinish() {
        return isSetWHFinish;
    }

    /**
     * Configures the transform matrix for TextureView based on {@link #mDisplayOrientation} and
     * the surface size.
     */
    void configureTransform() {
        Matrix matrix = new Matrix();
        if (mDisplayOrientation % 180 == 90) {
            final int width = getWidth();
            final int height = getHeight();
            // Rotate the camera preview when the screen is landscape.
            matrix.setPolyToPoly(
                    new float[]{
                            0.f, 0.f, // top left
                            width, 0.f, // top right
                            0.f, height, // bottom left
                            width, height, // bottom right
                    }, 0,
                    mDisplayOrientation == 90 ?
                            // Clockwise
                            new float[]{
                                    0.f, height, // top left
                                    0.f, 0.f, // top right
                                    width, height, // bottom left
                                    width, 0.f, // bottom right
                            } : // mDisplayOrientation == 270
                            // Counter-clockwise
                            new float[]{
                                    width, 0.f, // top left
                                    width, height, // top right
                                    0.f, 0.f, // bottom left
                                    0.f, height, // bottom right
                            }, 0,
                    4);
        } else if (mDisplayOrientation == 180) {
            matrix.postRotate(180, getWidth() / 2, getHeight() / 2);
        }
        mTextureView.setTransform(matrix);
    }

}
