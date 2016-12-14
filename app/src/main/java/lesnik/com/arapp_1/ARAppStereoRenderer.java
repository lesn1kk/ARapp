package lesnik.com.arapp_1;

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
final class ARAppStereoRenderer implements GvrView.StereoRenderer {

    /**
     * These matrices are used to properly draw a texture/
     * Projection matrix.
     */
    private final float[] textureProjectionMatrix = new float[16];

    /**
     * View matrix.
     */
    private final float[] textureViewMatrix = new float[16];

    /**
     * View and projection matrix.
     */
    private final float[] textureMatrix = new float[16];

    /**
     * These matrices are used to properly draw a camera's input image.
     * Projection matrix.
     */
    //private float[] cameraViewMatrix;

    /**
     * Variable used to apply transformation to camera matrix.
     */
    //private float[] cameraMatrix;

    /**
     * Instance of ARAppCamera class.
     */
    private ARAppCamera mARAppCamera;

    /**
     * Surface used to draw camera's images.
     * See {@link #setSurface(SurfaceTexture)}
     */
    private SurfaceTexture mSurface;

    /**
     * Instance of QR Code scanner.
     */
    private ARAppQRCodeScanner mScanningLine;

    /**
     * This class singleton instance.
     */
    private static ARAppStereoRenderer mRenderer;

    /**
     * Instance of texture loader class.
     */
    private static ARAppTextureManager mARAppTextureManager;

    /**
     * This clss tag, used in debug logging.
     */
    private static final String mTag = "ARAppStereoRenderer";

    /**
     * If true, draw scanning line.
     */
    private boolean isScanning = false;

    /**
     * Because loading of texture takes time and causes lag, to avoid this make sure texture is
     * only loaded once.
     */
    private boolean isTextureLoaded = false;

    /**
     * Texture id to draw.
     */
    private int mTexture = 0;

    /**
     * If true, draw error texture.
     */
    private boolean onErrorListening = false;

    /**
     * If true, process camera's input and save.
     */
    private boolean isTakingScreenshot = false;

    /**
     * Error number from speech recognition listener. Based on this, the cause of problem can
     * be identified.
     */
    private int onErrorListeningNumber = 0;

    /**
     * A private constructor to create only one instance.
     */
    private ARAppStereoRenderer() {
        //cameraMatrix = new float[16];
        //cameraViewMatrix = new float[16];
    }

    /**
     * A public method to create a singleton instance of this class.
     */
    static void createInstance() {
        mRenderer = new ARAppStereoRenderer();
    }

    /**
     * A method used to get instance of this class.
     * @return Returns this instance.
     */
    static ARAppStereoRenderer getInstance() {
        return mRenderer;
    }

    /**
     * A method used to set surface which we will use to draw camera's input stream.
     * @param surface Surface used by ARAppCamera class.
     */
    void setSurface(SurfaceTexture surface) {
        mSurface = surface;
    }

    /**
     * This method is called from outside this class everytime we need to change the actual texture
     * we are drawing. This is used by ARAppSpeech, which set textures id when such command is
     * recognized, or by ARAppQRCodeScanner, when some qr code is decoded and new texture
     * needs to be drawn.
     *
     * @param id An id of that should be drawn.
     */
    void setTexture(int id) {
        ARAppTextureManager.resetAlpha();
        mTexture = id;
        isTextureLoaded = false;
    }

    /**
     * Method used to clear screen by setting actual texture to 0.
     */
    void clearTexture() {
        mTexture = 0;
        isTextureLoaded = false;
    }

    /**
     * Called when QR Code Scanner is turned on. Used to draw scanning line.
     * @param value turn on or off drawing line
     */
    void setIsScanning(boolean value) {
        isScanning = value;
    }

    /**
     * Called when {@link ARAppSpeech#onError(int)} is called.
     * @param error error number
     * @param value turn on drawing error message
     */
    void setListeningError(int error, boolean value) {
        onErrorListeningNumber = error;
        onErrorListening = value;
        clearTexture();
    }

    /**
     * Called when {@link ARAppSpeech#onReadyForSpeech(Bundle)} is called.
     * @param value draw or not draw error message
     */
    void setListeningError(boolean value) {
        onErrorListening = value;
    }

    /**
     * Called when user wants to take screenshot.
     * @param value true or false
     */
    void setIsTakingScreenshot(boolean value) {
        isTakingScreenshot = value;
    }

    /**
     * OpenGL ES 2.0 GvrView.StereoRenderer
     *
     * This method is called every time new frame appears.
     * @param headTransform The head transformation in the new frame.
     *                      Describes the head transform independently of any eye parameters.
     */
    @Override
    public void onNewFrame(HeadTransform headTransform) {
    }

    /**
     * OpenGL ES 2.0 GvrView.StereoRenderer
     *
     * This method is called every time an eye is drawn. In other words, when stereo mode is enabled
     * this is called two times for every frame. If stereo mode is disabled, it is called only once.
     *
     * First, clear OpenGL context.
     * Then update surface content with actual camera's image.
     * And then draw everything.
     * @param eye Requests to draw the contents from the point of view of an eye.
     */
    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glClearColor(0.0f, 200.0f, 0.0f, 1);

        mSurface.updateTexImage();

        mARAppCamera.draw();

        if (isScanning) {
            mScanningLine.draw();
        }

        if (!isTextureLoaded) {
            checkError();
            isTextureLoaded = mARAppTextureManager.loadTexture(mTexture);
        } else {
            mARAppTextureManager.draw(textureMatrix);
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
        if (isTakingScreenshot) { // TODO ?Make sure we load proper texture takingscreenshot before
            int width = viewport.width;
            int height = viewport.height;
            int screenshotSize = width * height;

            ByteBuffer bb = ByteBuffer.allocateDirect(screenshotSize * 4);
            bb.order(ByteOrder.nativeOrder());
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bb);

            int[] pixelsBuffer = new int[screenshotSize];
            bb.asIntBuffer().get(pixelsBuffer);

            for (int i = 0; i < screenshotSize; ++i) {
                // The alpha and green channels' positions are preserved while the
                // red and blue are swapped
                pixelsBuffer[i] = ((pixelsBuffer[i] & 0xff00ff00))
                        | ((pixelsBuffer[i] & 0x000000ff) << 16)
                        | ((pixelsBuffer[i] & 0x00ff0000) >> 16);
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixelsBuffer, screenshotSize - width, -width, 0, 0, width, height);

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

            setIsTakingScreenshot(false);
            clearTexture();
        }
    }

    /**
     * OpenGL ES 2.0 GvrView.StereoRenderer
     *
     * This is called for example then device orientation was changed. In this app case, this
     * is called only once, because orientation is always horizontal. Because it contains
     * information about screen width and screen height, it is used to setup matrices.
     * Setup our screen width and height for normal sprite translation.
     *
     * @param width Screen width
     * @param height Screen height
     */
    @Override
    public void onSurfaceChanged(int width, int height) {
        float aspectRatio = (float) width / (float) height;
        Matrix.orthoM(textureMatrix, 0, -aspectRatio, aspectRatio, -1, 1, -1, 1);
    }

    /**
     * OpenGL ES 2.0 GvrView.StereoRenderer
     *
     * This is called when OpenGL context is created, only once. Good place to setup all of used
     * classes. Set local instances of ARAppCamera and ARAppTextureManager to have access to their
     * draw methods.
     *
     * @param eglConfig Unused
     */
    @Override
    public void onSurfaceCreated(javax.microedition.khronos.egl.EGLConfig eglConfig) {
        ARAppCamera.createInstance();
        mARAppCamera = ARAppCamera.getInstance();
        mARAppCamera.startCamera();

        mScanningLine = new ARAppQRCodeScanner();

        ARAppTextureManager.createInstance();
        mARAppTextureManager = ARAppTextureManager.getInstance();
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
        clearTexture();
    }

    /**
     * Method that checks if speech recognition throws error. Error 4 means no internet connection.
     */
    private void checkError() {
        if (onErrorListening) {
            if (onErrorListeningNumber != 4) {
                mTexture = R.drawable.errorlistening;
            } else {
                mTexture = R.drawable.errorlisteningfour;
            }
        }
    }
    /**
     * Converts a raw text file into a string.
     *
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return The context of the text file, or null in case of error.
     */
    private static String readRawTextFile(int resId) {
        InputStream inputStream = ARAppActivity.getARAppContext().getResources()
                .openRawResource(resId);
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

    static int loadShader(int type, int resId) {
        String code = readRawTextFile(resId);
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(mTag, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shader;
    }
}
