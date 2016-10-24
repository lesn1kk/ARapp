package com.diligimus.glcam;

import android.content.Context;
import android.opengl.GLSurfaceView;

import com.google.vr.sdk.base.GvrView;

/**
 * Created by chau on 04.03.15.
 */
class MyGLSurfaceView extends GvrView
{
    MyGL20Renderer renderer;
    public MyGLSurfaceView(Context context)
    {
        super(context);

        setEGLContextClientVersion(2);

        renderer = new MyGL20Renderer((CameraActivity)context);
        setRenderer(renderer);
        //setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

    }
    public MyGL20Renderer getRenderer()
    {
        return renderer;
    }
}
