package lesnik.com.arapp_1;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;

import com.google.vr.sdk.base.GvrActivity;

import java.io.IOException;

public class ARAppActivity extends GvrActivity {

    private Camera mCamera;
    private ARAppGvrView glSurfaceView;
    private SurfaceTexture surface;
    ARAppStereoRenderer renderer;

    private float[] headView;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        glSurfaceView = new ARAppGvrView(this);
        renderer = glSurfaceView.getRenderer();
        setContentView(glSurfaceView);
    }

    public void startCamera(int texture)
    {
        surface = new SurfaceTexture(texture);
        renderer.setSurface(surface);

        mCamera = Camera.open();

        try
        {
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();

            //set camera to continually auto-focus
            Camera.Parameters params = mCamera.getParameters();
            //*EDIT*//params.setFocusMode("continuous-picture");
            //It is better to use defined constraints as opposed to String, thanks to AbdelHady
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mCamera.setParameters(params);
        }
        catch (IOException ioe)
        {
            Log.w("ARAppActivity","CAM LAUNCH FAILED");
            System.out.println("Camera error");
        }
    }

    @Override
    public void onPause()
    {
        mCamera.stopPreview();
        mCamera.release();
        System.exit(0);
    }
}

