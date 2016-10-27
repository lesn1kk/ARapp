package lesnik.com.arapp_1;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Vibrator;
import android.util.Log;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ARAppStereoRenderer implements GvrView.StereoRenderer {// {

    ARAppCamera mARAppCamera;
    private SurfaceTexture surface;
    static ARAppActivity mARAppContext;
    private Triangle mTriangle;

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;
    private static final float CAMERA_Z = 0.01f;
    private static final float MIN_MODEL_DISTANCE = 3.0f;
    private static final float MAX_MODEL_DISTANCE = 5.0f;

    private float[] view;
    private float[] camera;
    private float[] triangleViewProjection;
    private float[] triangleView;

    protected float[] triangleModel;
    protected float[] trianglePosition;

    private Vibrator vibrator;

    private static String TAG = "MyGL20Renderer";

    /**
     * Converts a raw text file into a string.
     *
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return The context of the text file, or null in case of error.
     */
    private static String readRawTextFile(int resId) {
        InputStream inputStream = mARAppContext.getResources().openRawResource(resId);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    /**
     * Converts a raw text file, saved as a resource, into an OpenGL ES shader.
     *
     * @param type The type of shader we will be creating.
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return The shader object handler.
     */
    public static int loadShader(int type, int resId) {
        String code = readRawTextFile(resId);
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shader;
    }

        public ARAppStereoRenderer(ARAppActivity _cameraContext)
        {
            mARAppContext = _cameraContext;

            triangleModel = new float[16];

            camera = new float[16];
            view = new float[16];
            triangleViewProjection = new float[16];
            triangleView = new float[16];
            trianglePosition = new float[] {0.0f, 0.0f, -MAX_MODEL_DISTANCE / 2.0f};

            vibrator = (Vibrator) mARAppContext.getSystemService(Context.VIBRATOR_SERVICE);
        }

    public void setSurface(SurfaceTexture _surface)
    {
        surface = _surface;
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        // Build the camera matrix and apply it to the ModelView.
        Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
    }

    @Override
    public void onDrawEye(Eye eye) {
        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);

        float[] mtx = new float[16];
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        surface.updateTexImage();
        surface.getTransformMatrix(mtx);

        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
        Matrix.multiplyMM(triangleView, 0, view, 0, triangleModel, 0);
        Matrix.multiplyMM(triangleViewProjection, 0, perspective, 0, triangleView, 0);

        mARAppCamera.draw();
        mTriangle.draw(triangleViewProjection);
    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onSurfaceCreated(javax.microedition.khronos.egl.EGLConfig eglConfig) {
        Matrix.setIdentityM(triangleModel, 0);
        Matrix.translateM(triangleModel, 0, trianglePosition[0], trianglePosition[1], trianglePosition[2]);

        mARAppCamera = new ARAppCamera(mARAppContext);
        int texture = mARAppCamera.getTexture();
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        surface = new SurfaceTexture(texture);
        mARAppContext.startCamera(texture);

        mTriangle = new Triangle();


    }

    @Override
    public void onRendererShutdown() {

    }
}

