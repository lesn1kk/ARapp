package com.diligimus.glcam;

import android.graphics.SurfaceTexture;
import android.opengl.EGLConfig;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import javax.microedition.khronos.opengles.GL10;

public class MyGL20Renderer implements GvrView.StereoRenderer {// {

    DirectVideo mDirectVideo;
    int texture;
    private SurfaceTexture surface;
    CameraActivity delegate;

    public MyGL20Renderer(CameraActivity _delegate)
    {
        delegate = _delegate;
    }

    static public int loadShader(int type, String shaderCode)
    {
        int shader = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    static private int createTexture()
    {
        int[] texture = new int[1];

        GLES20.glGenTextures(1,texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        return texture[0];
    }

    public void setSurface(SurfaceTexture _surface)
    {
        surface = _surface;
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {

    }

    @Override
    public void onDrawEye(Eye eye) {
        float[] mtx = new float[16];
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        surface.updateTexImage();
        surface.getTransformMatrix(mtx);

        mDirectVideo.draw();
    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onSurfaceCreated(javax.microedition.khronos.egl.EGLConfig eglConfig) {
        texture = createTexture();
        mDirectVideo = new DirectVideo(texture);

        surface = new SurfaceTexture(texture);
        delegate.startCamera(texture);
    }

    @Override
    public void onRendererShutdown() {

    }
}
