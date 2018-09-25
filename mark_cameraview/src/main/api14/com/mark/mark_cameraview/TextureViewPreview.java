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
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

@TargetApi(14)
class TextureViewPreview extends PreviewImpl {

    private final TextureView mTextureView;

    private int mDisplayOrientation;
    private Surface mSurface;

    TextureViewPreview(Context context, ViewGroup parent) {
        final View view = View.inflate(context, R.layout.texture_view, parent);
        mTextureView = view.findViewById(R.id.texture_view);
        mTextureView.bringToFront();
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.e("mark", "onSurfaceTextureAvailable: " +width+height);
                setSize(width, height);
                configureTransform();
                dispatchSurfaceChanged();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                Log.e("mark", "onSurfaceTextureSizeChanged: " +width+height);
                setSize(width, height);
                configureTransform();
                dispatchSurfaceChanged();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.e("mark", "onSurfaceTextureDestroyed: " );
                setSize(0, 0);
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
        if (mSurface==null){
            mSurface = new Surface(mTextureView.getSurfaceTexture());
        }
        return mSurface;
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
    void updateVideoPreviewSizeCenter(int width, int height) {
        float sx = (float) getWidth() / (float) width;
        float sy = (float) getHeight() / (float) height;

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
        if (degrees==0){
            return;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees, getWidth() / 2, getHeight() / 2);
        if (degrees == 90 || degrees == 270) {
            float s = (float) getWidth() / (float) getHeight();
            matrix.postScale(s,s,getWidth() / 2,  getHeight() / 2);
        }
        mTextureView.setTransform(matrix);
        mTextureView.postInvalidate();
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
