package lesnik.com.arapp_1;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Class responsible for drawing 'scanning line' and preparing OpenGL for it.
 */
class ARAppQRCodeScanner {

    /**
     * Main OpenGL program.
     */
    private final int mProgram;

    /**
     * Vertex buffer, used for drawing buffer in appropriate order.
     */
    private FloatBuffer vertexBuffer;

    /**
     * Number of coordinates per vertex, we only need x and y so 2 is enough.
     */
    static final int COORDS_PER_VERTEX = 2;

    /**
     * Coordinates of two line vertexes, it does not use all screen wide.
     */
    private static float[] lineCoords = {
            -1.5f, 0.0f,
            1.5f, 0.0f
    };

    /**
     * Color of line, red with alpha set to 0.3.
     */
    private float[] color = {1.0f, 0.0f, 0.0f, 0.3f};

    /**
     * A value which is added to lineMatrix coordinates every time it is drawn to make it move.
     */
    private float diff = 0.01f;

    /**
     * Number of vertexes.
     */
    private final int vertexCount = lineCoords.length / COORDS_PER_VERTEX;

    /**
     * Reserve 4 bytes per vertex.
     */
    private final int vertexStride = COORDS_PER_VERTEX * 4;

    /**
     * Matrix which is added to actual line position every time it is drawn, to make it move.
     */
    // TODO Figure out how it works, the last one is Z I believe
    // TODO Use Matrix to modify model position like in onSurfaceChaned in StereoRenderer
    private float[] lineMatrix = {
            0.0f, 0.0f,
            0.0f, 1.0f};

    /**
     * Variable used to add diff to lineMatrix every second call to draw, because otherwise, line
     * position on one eye would be always different than the other.
     */
    private byte counter = 0;

    /**
     * Variable to store actual system time, used to control speed of moving line.
     */
    private long systemTime;

    /**
     * Constructor, prepares OpenGL program and sets systemTime variable.
     */
     ARAppQRCodeScanner() {
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                lineCoords.length * 4);
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());

        // create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        vertexBuffer.put(lineCoords);
        // set the buffer to read the first coordinate
        vertexBuffer.position(0);

        int vertexShader = ARAppStereoRenderer.loadShader(GLES20.GL_VERTEX_SHADER,
                R.raw.line_vertex);
        int fragmentShader = ARAppStereoRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER,
                R.raw.line_fragment);

        // create empty OpenGL ES Program
        mProgram = GLES20.glCreateProgram();

        // add the vertex shader to program
        GLES20.glAttachShader(mProgram, vertexShader);

        // add the fragment shader to program
        GLES20.glAttachShader(mProgram, fragmentShader);

        // creates OpenGL ES program executables
        GLES20.glLinkProgram(mProgram);

        systemTime = System.currentTimeMillis();
    }

    /**
     * Draw method, called every time onDrawEye is called. Simple draws line on the screen.
     */
    void draw() {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram);

        int mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        int mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        int lineProjectionViewParam = GLES20.glGetUniformLocation(mProgram, "u_MVP");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);
        //GLES20.glUniformMatrix4fv(lineProjectionViewParam, 1, false, lineMatrix, 0);
        GLES20.glUniform4fv(lineProjectionViewParam, 1, lineMatrix, 0);

        // Set color for drawing the line
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        GLES20.glLineWidth(10.0f);
        // draw the triangle
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertexCount);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);

        //TODO This is temporary solution to see if it works, I need to figure out better way,
        // because this is highly related with phones cpu and gpu power
        long systemTempTime = System.currentTimeMillis();

        counter++;
        if (systemTempTime - systemTime >= 30) {
            // We check if counter is greater than 1 because we need to update the y coord
            // every second time, because otherwise, the lines wont be synchronized
            if (counter > 1) {
                if (lineMatrix[1] < -1.0f || lineMatrix[1] > 1.0f) {
                    diff = -diff;
                }

                // TODO Figure out why I need only to increment one of the y values ?
                lineMatrix[1] += diff;
                //lineMatrix[3] += diff;
                counter = 0;
            }

            systemTime = systemTempTime;
        }
    }
}

