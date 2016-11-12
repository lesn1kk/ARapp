package lesnik.com.arapp_1;

import android.content.Context;
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

@SuppressWarnings("FieldCanBeLocal")
public class ARAppCamera {

    private static String TAG = "ARAppCamera";

    // Below are variables used in OpenGL program
    private FloatBuffer screenVertBuffer, textVertBuffer;
    private final int mProgram;
    private ARAppActivity mContext;
    private static Camera mCamera;
    private ARAppStereoRenderer mRenderer;

    private int mPositionHandle, mTextureCoordHandle;

    private float[] screenVert = {
            -1.0F, 1.0F,
            1.0F, 1.0F,
            1.0F, -1.0F,

            1.0F, -1.0F,
            -1.0F, -1.0F,
            -1.0F, 1.0F,};

    private float[] textVert = {
            0.0F, 0.0F,
            1.0F, 0.0F,
            1.0F, 1.0F,

            1.0F, 1.0F,
            0.0F, 1.0F,
            0.0F, 0.0F};

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 2;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    private int texture;

    public int getTexture() {
        return texture;
    }

    private int createTexture() {
        int[] texture = new int[1];

        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
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

    public ARAppCamera(Context _mContext) {
        mContext = (ARAppActivity)_mContext;
        texture = createTexture();

        ByteBuffer bb = ByteBuffer.allocateDirect(screenVert.length * 4);
        bb.order(ByteOrder.nativeOrder());
        screenVertBuffer = bb.asFloatBuffer();
        screenVertBuffer.put(screenVert);
        screenVertBuffer.position(0);

        ByteBuffer bb2 = ByteBuffer.allocateDirect(textVert.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        textVertBuffer = bb2.asFloatBuffer();
        textVertBuffer.put(textVert);
        textVertBuffer.position(0);

        int vertexShader = ARAppStereoRenderer.loadShader(GLES20.GL_VERTEX_SHADER, R.raw.cam_vertex);
        int fragmentShader = ARAppStereoRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, R.raw.cam_fragment);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);
    }

    public void draw() {
        GLES20.glUseProgram(mProgram);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, true, vertexStride, screenVertBuffer);

        mTextureCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle);
        GLES20.glVertexAttribPointer(mTextureCoordHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, textVertBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordHandle);
    }

    public void startCamera(int texture) {
        SurfaceTexture surface = new SurfaceTexture(texture);
        mRenderer = mContext.getARAppView().getRenderer();
        mRenderer.setSurface(surface);

        mCamera = Camera.open();

        try {
            mCamera.setPreviewTexture(surface);
            mCamera.setPreviewCallback(mContext);
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
            Log.e(TAG, "CAM LAUNCH FAILED");
        }
    }

    public static Camera getCamera() {
        return mCamera;
    }

    public static void focusCamera() {
        if(mCamera != null) {
            try {
                mCamera.cancelAutoFocus();
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {

                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error during focusCamera!");
                e.printStackTrace();
            }
        }
    }
}

