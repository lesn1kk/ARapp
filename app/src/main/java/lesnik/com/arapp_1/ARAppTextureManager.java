package lesnik.com.arapp_1;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Setup and prepares OpenGL program for drawing texture.
 */
@SuppressWarnings("FieldCanBeLocal")
final class ARAppTextureManager {

    /**
     * We have to create the vertices of our screen. This is actually size. X, Y, Z.
     */
    private float[] screenVertices = new float[] {
            -1.0f, 1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            1.0f, 1.0f, 0.0f
    };

    /**
     * OpenGL ES 2.0
     *
     * Create our UV coordinates.
     * Array that contains a positions of texture vertices.
     * |0, 1       1, 1|
     * |               |
     * |               |
     * |0, 0       1, 0|
     */
    private float[] textureVertices = new float[] {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
    };

    /**
     * The order of vertex rendering.
     */
    private short[] drawOrder = new short[] {0, 1, 2, 0, 2, 3};

    /**
     * Screen vertices buffer.
     */
    private FloatBuffer screenVerticesBuffer;

    /**
     * Texture vertices buffer.
     */
    private FloatBuffer textureVerticesBuffer;

    /**
     * Draw order buffer.
     */
    private ShortBuffer drawOrderBuffer;

    /**
     * OpenGL program.
     */
    private int mProgram;

    /**
     * Singleton instance of this class.
     */
    private static ARAppTextureManager mARAppTextureManager;

    /**
     * Used for making fade effect.
     */
    private static float mAlpha = 0.0f;

    /**
     * Texture used to draw on.
     */
    private int mTexture;

    /**
     * Constructor, setup and prepares OpenGL.
     */
    private ARAppTextureManager() {
        mTexture = createTexture();

        // The vertex buffer.
        ByteBuffer bb = ByteBuffer.allocateDirect(screenVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        screenVerticesBuffer = bb.asFloatBuffer();
        screenVerticesBuffer.put(screenVertices);
        screenVerticesBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawOrderBuffer = dlb.asShortBuffer();
        drawOrderBuffer.put(drawOrder);
        drawOrderBuffer.position(0);

        // The texture buffer
        ByteBuffer bb1 = ByteBuffer.allocateDirect(textureVertices.length * 4);
        bb1.order(ByteOrder.nativeOrder());
        textureVerticesBuffer = bb1.asFloatBuffer();
        textureVerticesBuffer.put(textureVertices);
        textureVerticesBuffer.position(0);

        // Enable transparency
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int vertexShader = ARAppStereoRenderer.loadShader(GLES20.GL_VERTEX_SHADER,
                R.raw.texture_vertex);
        int fragmentShader = ARAppStereoRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER,
                R.raw.texture_fragment);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // creates OpenGL ES program executables
    }

    /**
     * Creates singleton instance of this class.
     */
    static void createInstance() {
        mARAppTextureManager = new ARAppTextureManager();
    }

    /**
     * Returns instance of this class.
     * @return This class
     */
    static ARAppTextureManager getInstance() {
        return mARAppTextureManager;
    }

    /**
     * Creates new OpenGL texture and sets parameters.
     * @return Texture
     */
    private int createTexture() {
        int[] textureArray = new int[1];
        GLES20.glGenTextures(1, textureArray, 0);

        // Set filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);

        return textureArray[0];
    }

    /**
     * Load texture from file id to OpenGL texture 1.
     * @param id ID of texture
     * @return true if success
     */
    boolean loadTexture(int id) {
        // Temporary create a bitmap
        //BitmapFactory.Options opts = new BitmapFactory.Options();
        //opts.inPreferredConfig = Bitmap.Config.ARGB_8888;

        if (id == 0) {
            return false;
        }

        Bitmap bmp =
                BitmapFactory.decodeResource(ARAppActivity.getARAppContext().getResources(), id);
        //bmp.setHasAlpha(true);
        // Bind texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        // Load the bitmap into the bound texture.
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);

        // We are done using the bitmap so we should recycle it.
        bmp.recycle();
        return true;
    }

    /**
     * Resets alpha. Called when new texture appears.
     */
    static void resetAlpha() {
        mAlpha = 0.0f;
    }

    /**
     * Called every time when {@link ARAppStereoRenderer#onDrawEye(Eye)} is called and texture is
     * loaded.
     * @param textureViewAndProjectionMatrix View and projection matrix.
     */
    void draw(float[] textureViewAndProjectionMatrix) {
        GLES20.glUseProgram(mProgram);

        int mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0,
                screenVerticesBuffer);

        int mTexCoordinatesLoc = GLES20.glGetAttribLocation(mProgram, "a_texCoord");
        GLES20.glEnableVertexAttribArray(mTexCoordinatesLoc);
        GLES20.glVertexAttribPointer(mTexCoordinatesLoc, 2, GLES20.GL_FLOAT, false, 0,
                textureVerticesBuffer);

        int mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, textureViewAndProjectionMatrix, 0);

        int mSamplerLoc = GLES20.glGetUniformLocation(mProgram, "s_texture");
        GLES20.glUniform1i(mSamplerLoc, mTexture);

        if (mAlpha < 1.0f) {
            mAlpha += 0.01f;
        }

        int mAlphaHandle = GLES20.glGetUniformLocation(mProgram, "alpha");
        GLES20.glUniform1f(mAlphaHandle, mAlpha);

        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawOrderBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordinatesLoc);
    }
}

