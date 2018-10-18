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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import java.io.File;

class SurfaceViewPreview extends PreviewImpl {

    final SurfaceView mSurfaceView;
    private MediaPlayer mediaPlayer;
    private File mRecordFile;
    private int mCurrentPosition;
    private boolean isSetWHFinish;

    SurfaceViewPreview(Context context, ViewGroup parent) {
        final View view = View.inflate(context, R.layout.surface_view, parent);
        mSurfaceView = (SurfaceView) view.findViewById(R.id.surface_view);
        final SurfaceHolder holder = mSurfaceView.getHolder();
        //noinspection deprecation
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder h) {
            }

            @Override
            public void surfaceChanged(SurfaceHolder h, int format, int width, int height) {
                setSize(width, height);
                if (!ViewCompat.isInLayout(mSurfaceView)) {
                    dispatchSurfaceChanged();
                    if (mRecordFile != null) {
                        playVideo(mRecordFile);
                    }
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder h) {
                setSize(0, 0);
                if (mediaPlayer != null) {
                    mCurrentPosition = mediaPlayer.getCurrentPosition();
                    isSetWHFinish = false;
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
            }
        });
    }

    @Override
    Surface getSurface() {
        return getSurfaceHolder().getSurface();
    }

    @Override
    SurfaceHolder getSurfaceHolder() {
        return mSurfaceView.getHolder();
    }

    @Override
    View getView() {
        return mSurfaceView;
    }

    @Override
    Class getOutputClass() {
        return SurfaceHolder.class;
    }

    @Override
    void setDisplayOrientation(int displayOrientation) {
    }

    @Override
    boolean isReady() {
        return getWidth() != 0 && getHeight() != 0;
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
                    if (isSetWHFinish) {
                        return;
                    }
                    isSetWHFinish = true;
                    mSurfaceView.postDelayed(new Runnable() {
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
            isSetWHFinish = false;
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
    }

    @Override
    boolean pauseVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            return true;
        }
        return false;
    }

    @Override
    boolean resumeVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            return true;
        }
        return false;
    }

    @Override
    void updateVideoPreviewSizeCenter(int width, int height) {
//        float max = Math.max((float) width / (float) getWidth(), (float) height / (float) getHeight());
//
//        //视频宽高分别/最大倍数值 计算出放大后的视频尺寸
//        width = (int) Math.ceil((float) width / max);
//        height = (int) Math.ceil((float) height / max);
//
//        //无法直接设置视频尺寸，将计算出的视频尺寸设置到surfaceView 让视频自动填充。
//        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, height);
//        layoutParams.setMargins(0, (getHeight() - height) / 2, 0, 0);
        float sx = (float) getWidth() / (float) width;
        float sy = (float) getHeight() / (float) height;

//        Matrix matrix = new Matrix();
//
//        //第1步:把视频区移动到View区,使两者中心点重合.
//        matrix.preTranslate((getWidth() - width) / 2, (getHeight() - height) / 2);
//
//        //第2步:因为默认视频是fitXY的形式显示的,所以首先要缩放还原回来.
//        matrix.preScale(width / (float) getWidth(), height / (float) getHeight());
//
//        //第3步,等比例放大或缩小,直到视频区的一边和View一边相等.如果另一边和view的一边不相等，则留下空隙
//        if (sx >= sy) {
//            matrix.postScale(sy, sy, getWidth() / 2, getHeight() / 2);
//        } else {
//            matrix.postScale(sx, sx, getWidth() / 2, getHeight() / 2);
//        }
//
//        mSurfaceView.getMatrix().set(matrix);
        mSurfaceView.postInvalidate();
    }

    @Override
    void updatePicturePreviewSizeCenter(int degrees) {
        if (degrees == 0) {
            return;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees, getWidth() / 2, getHeight() / 2);
        if (degrees == 90 || degrees == 270) {
            float s = (float) getWidth() / (float) getHeight();
            matrix.postScale(s, s, getWidth() / 2, getHeight() / 2);
        }
        mSurfaceView.getMatrix().set(matrix);
        mSurfaceView.postInvalidate();
    }

}
