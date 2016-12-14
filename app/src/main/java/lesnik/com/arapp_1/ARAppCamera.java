package lesnik.com.arapp_1;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Class responsible for setting up camera's hardware and drawing its input on screen.
 */
@SuppressWarnings("FieldCanBeLocal, deprecation")
final class ARAppCamera {

    /**
     * Class TAG, used to log data.
     */
    private static String mTag = "ARAppCamera";

    /**
     * Instance of camera object. It is used to connect to the hardware camera, set its parameters,
     * open etc.
     */
    private static Camera mCamera;

    /**
     * Instance of this class. Singleton.
     */
    private static ARAppCamera mARAppCamera;

    /**
     * OpenGL ES 2.0
     * Buffers used by OpenGL program.
     */
    private FloatBuffer screenVerticesBuffer, textVerticesBuffer;

    /**
     * OpenGL ES 2.0
     * OpenGL program.
     */
    private final int mProgram;

    /**
     * OpenGL ES 2.0
     * Handlers to variables from OpenGL shaders.
     */
    private int mPositionHandle, mTextureCoordsHandle;

    /**
     * OpenGL ES 2.0
     * Array that contains a positions of screen vertices.
     * |-1, 1       1, 1|
     * |                |
     * |                |
     * |-1,-1       1,-1|
     * These are two triangles that will be used to draw.
     *
     * First triangle
     * |*  *  *  *  *|
     * |   *        *|
     * |      *     *|
     * |         *  *|
     * |            *|
     *
     * Second triangle
     * |*            |
     * |*  *         |
     * |*     *      |
     * |*        *   |
     * |*  *  *  *  *|
     */
    private float[] screenVertices = {
            -1.0F, 1.0F,
            1.0F, 1.0F,
            1.0F, -1.0F,

            1.0F, -1.0F,
            -1.0F, -1.0F,
            -1.0F, 1.0F
    };

    /**
     * OpenGL ES 2.0
     * Array that contains a positions of texture vertices.
     * |0, 1       1, 1|
     * |               |
     * |               |
     * |0, 0       1, 0|
     * These are two triangles that will be used to draw.
     * These triangles are the triangles from screenVertices rotated 90 degrees, because we
     * work in landscape orientation.
     *
     * * First triangle
     * |            *|
     * |         *  *|
     * |      *     *|
     * |   *        *|
     * |*  *  *  *  *|
     *
     * Second triangle
     * |*  *  *  *  *|
     * |*        *   |
     * |*     *      |
     * |*  *         |
     * |*            |
     */
    private float[] textureVertices = {
            0.0F, 0.0F,
            1.0F, 0.0F,
            1.0F, 1.0F,

            1.0F, 1.0F,
            0.0F, 1.0F,
            0.0F, 0.0F
    };

    /**
     * OpenGL ES 2.0
     * Number of coordinates per vertex in above arrays.
     */
    private static final int COORDINATES_PER_VERTEX = 2;

    /**
     * OpenGL ES 2.0
     * Number of bytes to allocate per vertex.
     */
    private final int vertexStride = COORDINATES_PER_VERTEX * 4;

    /**
     * Texture we will use to draw in OpenGL. It is created when instance of this class is created,
     * then in ARAppStereoRenderer, it is used to create a surface, which will be used for
     * camera input stream
     */
    private int mTexture;

    /**
     * Method used to create a proper texture for OpenGL.
     * glGenTextures - OpenGL generate its own texture id.
     * glBindTexture - bind this texture to some OpenGL texture, in this case to
     * GL_TEXTURE_EXTERNAL_OS
     * glTexParameterf - set texture parameters. f means float, i means integer.
     * https://www.khronos.org/opengles/sdk/docs/man/xhtml/glTexParameter.xml
     *
     * @return id of generated texture
     * TODO Check if setting these parameters is necessary
     */
    private int createTexture() {
        int[] texture = new int[1];

        GLES20.glGenTextures(1, texture, 0);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        return texture[0];
    }

    /**
     * Constructor. Prepares OpenGL program by creating buffers with screen and texture vertices.
     * Creates empty OpenGL ES program.
     * Loads shaders to OpenGL and adds them to this class program. glLinkProgram is called to
     * make possible communication between vertex and fragment shaders.
     */
    private ARAppCamera() {
        mTexture = createTexture();

        ByteBuffer bb = ByteBuffer.allocateDirect(screenVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        screenVerticesBuffer = bb.asFloatBuffer();
        screenVerticesBuffer.put(screenVertices);
        screenVerticesBuffer.position(0);

        ByteBuffer bb2 = ByteBuffer.allocateDirect(textureVertices.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        textVerticesBuffer = bb2.asFloatBuffer();
        textVerticesBuffer.put(textureVertices);
        textVerticesBuffer.position(0);

        int vertexShader = ARAppStereoRenderer.loadShader(GLES20.GL_VERTEX_SHADER,
                R.raw.cam_vertex);
        int fragmentShader = ARAppStereoRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER,
                R.raw.cam_fragment);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);
    }

    /**
     * Creates instance of this class. Singleton.
     */
    static void createInstance() {
        mARAppCamera = new ARAppCamera();
    }
    /**
     * Returns instance of this class. Singleton.
     *
     * @return Instance of this class.
     */
    static ARAppCamera getInstance() {
        return mARAppCamera;
    }

    /**
     * Draws camera's input on the screen. Called every time in onEyeDraw OpenGL method.
     * glUseProgram - tell OpenGL what program it must use.
     * glActiveTexture - tell OpenGl to what texture we will bind. Must be unique, in case
     * when we would want to draw a second texture, we must use other, for example GL_TEXTURE1
     * glBindTexture - bind our texture to OpenGL texture.
     * glGetAttribLocation - pointer to variable in OpenGL shader.
     * glEnableVertexAtribArray - tell OpenGl that we want this attribute to be used during
     * rendering.
     * glVertexAttribPointer - set its value
     * glDrawArrays - draw everything
     * glDisableVertexAttribArray - disable it. Good practice to disable any modified property
     * of OpenGL after operation is complete.
     */
    void draw() {
        GLES20.glUseProgram(mProgram);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTexture);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDINATES_PER_VERTEX,
                GLES20.GL_FLOAT, true, vertexStride, screenVerticesBuffer);

        mTextureCoordsHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        GLES20.glEnableVertexAttribArray(mTextureCoordsHandle);
        GLES20.glVertexAttribPointer(mTextureCoordsHandle, COORDINATES_PER_VERTEX,
                GLES20.GL_FLOAT, false, vertexStride, textVerticesBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordsHandle);
    }

    /**
     * Method called from onSurfaceCreated. Sets proper surface to camera, sets ARAppActivity
     * class as a preview callback, sets camera's autofocus to continuous mode and sets size
     * of images.
     * TODO Consider not turning continuous mode but focus on voice command, because in
     * better devices, this could be not comfortable for user. Also, consider turning on
     * constant fps.
     */
    void startCamera() {
        SurfaceTexture surface = new SurfaceTexture(mTexture);
        ARAppStereoRenderer.getInstance().setSurface(surface);

        mCamera = Camera.open();

        try {
            mCamera.setPreviewTexture(surface);
            mCamera.setPreviewCallback(ARAppActivity.getARAppContext());
            //set camera to continually auto-focus
            Camera.Parameters params = mCamera.getParameters();
            //*EDIT*//params.setFocusMode("continuous-picture");
            //It is better to use defined constraints as opposed to String, thanks to AbdelHady
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

//            List<int[]> list = params.getSupportedPreviewFpsRange();
//            params.setPreviewFrameRate(30);
//            params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT);
//            params.setPreviewFpsRange(1000,30000);
//            params.getPictureSize();

            //params.setPictureSize(1920,1080);
            params.setPreviewSize(1280, 720);

            mCamera.setParameters(params);
            mCamera.startPreview();

        } catch (IOException ex) {
            Log.e(mTag, "CAM LAUNCH FAILED");
        }
    }

    /**
     * Returns camera object. Used in onPause to stop and release.
     *
     * @return Camera's instance.
     */
    static Camera getCamera() {
        return mCamera;
    }

    /**
     * Focus camera's captured images.
     */
    static void focusCamera() {
        if (mCamera != null) {
            try {
                mCamera.cancelAutoFocus();
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {

                    }
                });
            } catch (Exception e) {
                Log.e(mTag, "Error during focusCamera!");
                e.printStackTrace();
            }
        }
    }
}

