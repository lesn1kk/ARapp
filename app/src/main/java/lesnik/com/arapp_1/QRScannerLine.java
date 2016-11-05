package lesnik.com.arapp_1;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class QRScannerLine {
    private final int mProgram;
    private FloatBuffer vertexBuffer;

    static final int COORDS_PER_VERTEX = 2;

    static float lineCoords[] = {   // in counterclockwise order:
            -1.0f, 0.0f,
            1.0f, 0.0f,
    };

    float color[] = {1.0f, 0.0f, 0.0f, 1.0f}; //set line color to red

    public QRScannerLine() {
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
                R.raw.vertex_line);
        int fragmentShader = ARAppStereoRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER,
                R.raw.fragment_line);

        // create empty OpenGL ES Program
        mProgram = GLES20.glCreateProgram();

        // add the vertex shader to program
        GLES20.glAttachShader(mProgram, vertexShader);

        // add the fragment shader to program
        GLES20.glAttachShader(mProgram, fragmentShader);

        // creates OpenGL ES program executables
        GLES20.glLinkProgram(mProgram);
    }

    private int mPositionHandle;
    private int mColorHandle;
    private float diff = 0.05f;

    private final int vertexCount = lineCoords.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex
    private int lineProjectionViewParam;
    private float[] lineMatrix = {0.0f, 0.0f, 0.0f, 0.0f};
    private int counter;

    public void draw() {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        // get handle to fragment shader's vColor member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        lineProjectionViewParam = GLES20.glGetUniformLocation(mProgram, "u_MVP");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);
        //GLES20.glUniformMatrix4fv(lineProjectionViewParam, 1, false, lineMatrix, 0);
        GLES20.glUniform4fv(lineProjectionViewParam, 1, lineMatrix, 0);

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertexCount);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        counter++;

        //TODO This is temporary solution to see if it works, I need to figure out better way, because this is highly related with phones cpu and gpu power
        if(counter >= 5) {
            if (lineMatrix[1] < -1.0f || lineMatrix[1] > 1.0f) {
                diff = -diff;
            }

            lineMatrix[1] += diff;
            //lineMatrix[3] += diff;

            counter = 0;
        }
    }

}
