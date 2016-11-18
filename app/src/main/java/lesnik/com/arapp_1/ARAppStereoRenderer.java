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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Class implementing GvrView.StereoRenderer, wrapped OpenGL ES 2.0 from google virtual kit library.
 * It is responsible for setting OpenGL context, setting up the program and drawing everything.
 */
public class ARAppStereoRenderer implements GvrView.StereoRenderer {
    // These matrices are used to properly draw a texture
    private final float[] mtrxProjection = new float[16];
    private final float[] mtrxView = new float[16];
    private final float[] mtrxProjectionAndView = new float[16];

    // These matrices are used to properly draw a camera's input image
    private float[] view;
    private float[] camera;

    // TODO Docs these variables
    private ARAppCamera mARAppCamera;
    private SurfaceTexture surface;
    private ARAppQRCodeScanner mScanningLine;
    private static ARAppActivity mContext;
    private static ARAppStereoRenderer mRenderer;
    private static ARAppTextureLoader mARAppTextureLoader;

    private static String TAG = "MyGL20Renderer";
    private static final float CAMERA_Z = 0.01f;

    public static boolean drawScanningLine = false;
    public static boolean isLoaded = false;
    public static int texture = 0;

    public static boolean onErrorListening = false;
    public static boolean takeScreenshot = false;
    public static int onErrorListeningNumber = 0;

    /**
     * A private constructor to create only one instance.
     * @param _mContext Context of the main activity, used for loading resources and also
     *                  creating instance of ARAppCamera class.
     */
    private ARAppStereoRenderer(Context _mContext) {
        mContext = (ARAppActivity)_mContext;

        camera = new float[16];
        view = new float[16];
    }

    /**
     * A public method to create a singleton instance of this class.
     * @param _mContext Context of the main activity, used for loading resources and also
     *                  creating instance of ARAppCamera class.
     */
    public static void createInstance(Context _mContext) {
        mRenderer = new ARAppStereoRenderer(_mContext);
    }

    /**
     * A method used to get instance of this class.
     * @return Returns this instance.
     */
    public static ARAppStereoRenderer getInstance() {
        return mRenderer;
    }

    /**
     * A method used to set surface which we will use to draw camera's input stream.
     * @param _surface Surface used by ARAppCamera class.
     */
    public void setSurface(SurfaceTexture _surface) {
        surface = _surface;
    }

    /**
     * This method is called from outside this class everytime we need to change the actual texture
     * we are drawing. This is used by ARAppSpeech, which set textures id when such command is
     * recognized, or by ARAppQRCodeScanner, when some qr code is decoded and new texture with
     * needs to be drawn.
     *
     * @param id An id of that should be drawn.
     */
    public static void setTexture(int id) {
        ARAppTextureLoader.resetAlpha();
        texture = id;
        isLoaded = false;
    }

    /**
     * Method used to clear screen by setting actual texture to 0;
     */
    public static void clearTexture() {
        texture = 0;
    }
    /**
     * OpenGL ES 2.0 GvrView.StereoRenderer
     *
     * This method is called every time new frame appears.
     *
     * @param headTransform The head transformation in the new frame.
     *                      Describes the head transform independently of any eye parameters.
     */
    @Override
    public void onNewFrame(HeadTransform headTransform) {
        // Build the camera matrix and apply it to the ModelView.
        Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
    }

    /**
     * OpenGL ES 2.0 GvrView.StereoRenderer
     *
     * This method is called every time an eye is drawn. In other words, when stereo mode is enabled
     * this is called two times for every frame. If stereo mode is disabled, it is called only once.
     *
     * @param eye Requests to draw the contents from the point of view of an eye.
     */
    @Override
    public void onDrawEye(Eye eye) {
        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);

        // First, clear OpenGL context
        float[] mtx = new float[16];
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glClearColor(0.0f, 200.0f, 0.0f, 1);

        // Update surface content with actual camera's image
        surface.updateTexImage();
        surface.getTransformMatrix(mtx);

        // Draw camera's input
        mARAppCamera.draw();

        if(drawScanningLine) {
            mScanningLine.draw();
        }

        // For now, draw only one texture at once, should be enough
        if (texture != 0) {
            if(!isLoaded) {
                if(onErrorListening) {
                    if(onErrorListeningNumber != 4) { // 4 means there is no internet connection
                        mARAppTextureLoader.loadTexture(R.drawable.errorlistening);
                    } else {
                        mARAppTextureLoader.loadTexture(R.drawable.errorlisteningfour);
                    }
                } else {
                    mARAppTextureLoader.loadTexture(texture);
                }
                isLoaded = true;
            }
            mARAppTextureLoader.draw(mtrxProjectionAndView);
        }
    }

    /**
     * OpenGL ES 2.0 GvrView.StereoRenderer
     *
     * This method is called before finishing the frame. By the time of this call, the frame
     * contents have been already drawn and, if enabled, distortion correction has been applied.
     * Logic in this method is responsible for catching actual image that is displayed
     * on user's device.
     *
     * @param viewport Viewport of the full GL surface. Automatically set before this call.
     */

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
            clearTexture();
        }
    }

    /**
     * OpenGL ES 2.0 GvrView.StereoRenderer
     *
     * This is called for example then device orientation was changed. In this app case, this
     * is called only once, because orientation is always horizontal. Because it contains
     * information about screen width and screen height, it is used to setup matrices.
     *
     * @param width Screen width
     * @param height Screen height
     */
    @Override
    public void onSurfaceChanged(int width, int height) {
        // Setup our screen width and height for normal sprite translation.
        Matrix.orthoM(mtrxProjection, 0, 0f, width, 0.0f, height, 0, 100);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mtrxView, 0, 0f, 0f, 0.01f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxProjection, 0, mtrxView, 0);
    }

    /**
     * OpenGL ES 2.0 GvrView.StereoRenderer
     *
     * This is called when OpenGL context is created, only once. Good place to setup all of used
     * classes.
     *
     * @param eglConfig Unused
     */
    @Override
    public void onSurfaceCreated(javax.microedition.khronos.egl.EGLConfig eglConfig) {
        //TODO Implement singleton instances
        mARAppCamera = new ARAppCamera(mContext);

        int texture = mARAppCamera.getTexture();

        mScanningLine = new ARAppQRCodeScanner();

        surface = new SurfaceTexture(texture);
        mARAppCamera.startCamera(texture);

        //TODO Move this code out of here
        mARAppTextureLoader = new ARAppTextureLoader(mContext);
        mARAppTextureLoader.setup();
    }

    /**
     * OpenGL ES 2.0 GvrView.StereoRenderer
     *
     * Called on the renderer thread when the thread is shutting down.
     * Allows releasing GL resources and performing shutdown operations in the renderer thread.
     * Called only if there was a previous call to onSurfaceCreated.
     */
    @Override
    public void onRendererShutdown() {

    }

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


}

