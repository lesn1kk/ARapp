package lesnik.com.arapp_1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Environment;
import android.util.Log;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ARAppStereoRenderer implements GvrView.StereoRenderer {
    // Our matrices
    private final float[] mtrxProjection = new float[16];
    private final float[] mtrxView = new float[16];
    private final float[] mtrxProjectionAndView = new float[16];

    private final float[] mLineView = new float[16];
    private final float[] mLineProjectionAndView = new float[16];

    // Our screenresolution
    float	mScreenWidth = 640;
    float	mScreenHeight = 480;

    ARAppCamera mARAppCamera;
    private SurfaceTexture surface;
    static ARAppActivity mContext;
    private Triangle mTriangle;
    private ARAppQRCodeScanner mLine;

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
    private static String TAG = "MyGL20Renderer";

    public static boolean drawLine = false;

    public static int texture = 0;

    /**
     * Converts a raw text file into a string.
     *
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return The context of the text file, or null in case of error.
     */
    private static String readRawTextFile(int resId) {
        InputStream inputStream = mContext.getResources().openRawResource(resId);
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

    /**
     * Converts a raw text file, saved as a resource, into an OpenGL ES shader.
     *
     * @param type  The type of shader we will be creating.
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

    private static ARAppStereoRenderer mRenderer;

    public static void createInstance(Context _mContext) {
        mRenderer = new ARAppStereoRenderer(_mContext);
    }

    public static ARAppStereoRenderer getInstance() {
        return mRenderer;
    }

    private ARAppStereoRenderer(Context _mContext) {
        mContext = (ARAppActivity)_mContext;

        triangleModel = new float[16];
        camera = new float[16];
        view = new float[16];
        triangleViewProjection = new float[16];
        triangleView = new float[16];
        trianglePosition = new float[]{0.0f, 0.0f, -MAX_MODEL_DISTANCE / 2.0f};
    }

    public void setSurface(SurfaceTexture _surface) {
        surface = _surface;
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        // Build the camera matrix and apply it to the ModelView.
        Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
    }

    public static boolean isLoaded = false;

    @Override
    public void onDrawEye(Eye eye) {
        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);

        float[] mtx = new float[16];
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glClearColor(0.0f, 200.0f, 0.0f, 1);
        surface.updateTexImage();
        surface.getTransformMatrix(mtx);

        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
        Matrix.multiplyMM(triangleView, 0, view, 0, triangleModel, 0);
        Matrix.multiplyMM(triangleViewProjection, 0, perspective, 0, triangleView, 0);

        mARAppCamera.draw();
        mTriangle.draw(triangleViewProjection);

        if(drawLine) {
            mLine.draw(mLineProjectionAndView);
        }

        // For now, draw only one texture at once, should be enough
        if (texture != 0) {
            if(!isLoaded) {
                if(onErrorListening) {
                    mARAppTextureLoader.loadTexture(R.drawable.errorlistening);
                } else {
                    mARAppTextureLoader.loadTexture(texture);
                }
                isLoaded = true;
            }
            mARAppTextureLoader.draw(mtrxProjectionAndView);
        }
    }

    public static boolean onErrorListening = false;
    public static boolean takeScreenshot = false;

    public static void setTexture(int id) {
        texture = id;
        isLoaded = false;
    }

    @Override
    public void onFinishFrame(Viewport viewport) {
        if (takeScreenshot) { // TODO ?Make sure we load proper texture takingscreenshot before
            int width = viewport.width;
            int height = viewport.height;
            int screenshotSize = width * height;

            ByteBuffer bb = ByteBuffer.allocateDirect(screenshotSize * 4);
            bb.order(ByteOrder.nativeOrder());
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bb);
            int pixelsBuffer[] = new int[screenshotSize];
            bb.asIntBuffer().get(pixelsBuffer);
            bb = null;

            for (int i = 0; i < screenshotSize; ++i) {
                // The alpha and green channels' positions are preserved while the      red and blue are swapped
                pixelsBuffer[i] = ((pixelsBuffer[i] & 0xff00ff00)) |    ((pixelsBuffer[i] & 0x000000ff) << 16) | ((pixelsBuffer[i] & 0x00ff0000) >> 16);
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixelsBuffer, screenshotSize-width, -width, 0, 0, width, height);

            try {
                FileOutputStream fos = new FileOutputStream(new File(Environment
                        .getExternalStorageDirectory().toString(), "SCREEN"
                        + System.currentTimeMillis() + ".png"));
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            takeScreenshot = false;
            ARAppStereoRenderer.setTexture(0);
        }
    }

    @Override
    public void onSurfaceChanged(int i, int i1) {
        mScreenWidth = i;
        mScreenHeight = i1;

        // Setup our screen width and height for normal sprite translation.
        Matrix.orthoM(mtrxProjection, 0, 0f, mScreenWidth, 0.0f, mScreenHeight, 0, 100);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mtrxView, 0, 0f, 0f, 0.01f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        Matrix.setLookAtM(mLineView, 0, 0f, 0f, 0.01f, 0.0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxProjection, 0, mtrxView, 0);
        Matrix.multiplyMM(mLineProjectionAndView, 0, mtrxProjection, 0, mLineView, 0);
    }

    public static ARAppTextureLoader mARAppTextureLoader;

    @Override
    public void onSurfaceCreated(javax.microedition.khronos.egl.EGLConfig eglConfig) {
        //TODO Implement singleton instances
        Matrix.setIdentityM(triangleModel, 0);
        Matrix.translateM(triangleModel, 0, trianglePosition[0], trianglePosition[1], trianglePosition[2]);

        mARAppCamera = new ARAppCamera(mContext);

        int texture = mARAppCamera.getTexture();
        GLES20.glClearColor(0.0f, 200.0f, 0.0f, 1.0f);

        mLine = new ARAppQRCodeScanner();

        surface = new SurfaceTexture(texture);
        mARAppCamera.startCamera(texture);

        mTriangle = new Triangle();

        //TODO Move this code out of here
        mARAppTextureLoader = new ARAppTextureLoader(mContext);
        mARAppTextureLoader.setup();

    }

    @Override
    public void onRendererShutdown() {

    }
}

