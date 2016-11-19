package lesnik.com.arapp_1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

@SuppressWarnings("FieldCanBeLocal")
public final class ARAppTextureLoader {
    public static float[] vertices;
    public static short[] indices;
    public static float[] uvs;
    public FloatBuffer vertexBuffer;
    public ShortBuffer drawListBuffer;
    public FloatBuffer uvBuffer;

    private int mProgram;
    private int vertexShader, fragmentShader;
    private int mPositionHandle, mTexCoordLoc, mMtrxHandle, mSamplerLoc;

    private static ARAppTextureLoader mARAppTextureLoader;

    public ARAppTextureLoader() {
        setup();
    }

    public static void createInstance() {
        mARAppTextureLoader = new ARAppTextureLoader();
    }

    public static ARAppTextureLoader getInstance() {
        return mARAppTextureLoader;
    }

    public void setup() {
        texture = createTexture();

        // We have to create the vertices of our triangle. This is actually size.
        vertices = new float[] {
                0.0f, 480f, 0.0f,
                0.0f, 0.0f, 0.0f,
                640f, 0.0f, 0.0f,
                640f, 480f, 0.0f};

        // Create our UV coordinates.
        uvs = new float[] {
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f
        };

        indices = new short[] {0, 1, 2, 0, 2, 3}; // The order of vertex rendering.

        // The vertex buffer.
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(indices);
        drawListBuffer.position(0);

        // The texture buffer
        ByteBuffer bb1 = ByteBuffer.allocateDirect(uvs.length * 4);
        bb1.order(ByteOrder.nativeOrder());
        uvBuffer = bb1.asFloatBuffer();
        uvBuffer.put(uvs);
        uvBuffer.position(0);

        // Enable transparency
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        vertexShader = ARAppStereoRenderer.loadShader(GLES20.GL_VERTEX_SHADER,
                R.raw.texture_vertex);
        fragmentShader = ARAppStereoRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER,
                R.raw.texture_fragment);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // creates OpenGL ES program executables
    }

    private int texture;

    public int createTexture() {
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

    public boolean loadTexture(int id) {
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

    public static void resetAlpha() {
        alphaValue = 0.0f;
    }

    private static float alphaValue = 0.0f;

    public void draw(float[] m) {
        GLES20.glUseProgram(mProgram);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        mTexCoordLoc = GLES20.glGetAttribLocation(mProgram, "a_texCoord");
        GLES20.glEnableVertexAttribArray(mTexCoordLoc);
        GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);

        mMtrxHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mMtrxHandle, 1, false, m, 0);

        mSamplerLoc = GLES20.glGetUniformLocation(mProgram, "s_texture");
        GLES20.glUniform1i(mSamplerLoc, texture);

        if (alphaValue < 1.0f) {
            alphaValue += 0.01f;
        }
        int mAlphaHandle = GLES20.glGetUniformLocation(mProgram, "alpha");
        GLES20.glUniform1f(mAlphaHandle, alphaValue);

        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordLoc);
    }
}
