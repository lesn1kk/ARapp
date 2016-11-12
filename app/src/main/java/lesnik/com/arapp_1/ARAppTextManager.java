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
import java.util.Vector;

@SuppressWarnings("FieldCanBeLocal")
public class ARAppTextManager {
	
	private static final float RI_TEXT_UV_BOX_WIDTH = 0.125f;
	private static final float RI_TEXT_WIDTH = 32.0f;
	private static final float RI_TEXT_SPACESIZE = 20f;
	
	private FloatBuffer vertexBuffer;
	private FloatBuffer textureBuffer;
	private FloatBuffer colorBuffer;
	private ShortBuffer drawListBuffer;
	
	private float[] vecs;
	private float[] uvs;
	private short[] indices;
	private float[] colors;
	
	private int index_vecs;
	private int index_indices;
	private int index_uvs;
	private int index_colors;

	private int mTextureNr;
	
	private float mUniformScale;

	private Context mContext;
	private int mProgram;
	private int mPositionHandle, mTexCoordLoc, mColorHandle, mMtrxHandle, mSamplerLoc;

	public static int[] l_size = {36,29,30,34,25,25,34,33,
								   11,20,31,24,48,35,39,29,
								   42,31,27,31,34,35,46,35,
								   31,27,30,26,28,26,31,28,
								   28,28,29,29,14,24,30,18,
								   26,14,14,14,25,28,31,0,
								   0,38,39,12,36,34,0,0,
								   0,38,0,0,0,0,0,0};
	
	public Vector<ARAppTextObject> txtCollection;

	public ARAppTextManager(Context mContext) {
		// Set context
		this.mContext = mContext;
		// Create our container
		txtCollection = new Vector<>();

		// Create the arrays
		vecs = new float[3 * 10];
		colors = new float[4 * 10];
		uvs = new float[2 * 10];
		indices = new short[10];

		// init as 0 as default
		mTextureNr = 2;
	}

	public void addText(ARAppTextObject obj) {
		// Add text object to our collection
		txtCollection.add(obj);
	}

	public void clearText() {
		txtCollection.clear();
	}

	public void setTextureID(int val) {
		mTextureNr = val;
	}

	public void setup() {
		// The vertex buffer.
		ByteBuffer bb = ByteBuffer.allocateDirect(vecs.length * 4);
		bb.order(ByteOrder.nativeOrder());
		vertexBuffer = bb.asFloatBuffer();
		vertexBuffer.put(vecs);
		vertexBuffer.position(0);

		// The vertex buffer.
		ByteBuffer bb3 = ByteBuffer.allocateDirect(colors.length * 4);
		bb3.order(ByteOrder.nativeOrder());
		colorBuffer = bb3.asFloatBuffer();
		colorBuffer.put(colors);
		colorBuffer.position(0);

		// The texture buffer
		ByteBuffer bb2 = ByteBuffer.allocateDirect(uvs.length * 4);
		bb2.order(ByteOrder.nativeOrder());
		textureBuffer = bb2.asFloatBuffer();
		textureBuffer.put(uvs);
		textureBuffer.position(0);

		// initialize byte buffer for the draw list
		ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length * 2);
		dlb.order(ByteOrder.nativeOrder());
		drawListBuffer = dlb.asShortBuffer();
		drawListBuffer.put(indices);
		drawListBuffer.position(0);

		// Again for the text texture
		int id = mContext.getResources().getIdentifier("drawable/font", null, mContext.getPackageName());
		Bitmap bmp = BitmapFactory.decodeResource(mContext.getResources(), id);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);

		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

		// Text shader
		int vertexShader = ARAppStereoRenderer.loadShader(GLES20.GL_VERTEX_SHADER, R.raw.tm_vertex);
		int fragmentShader = ARAppStereoRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, R.raw.tm_fragment);

		mProgram = GLES20.glCreateProgram();
		GLES20.glAttachShader(mProgram, vertexShader);
		GLES20.glAttachShader(mProgram, fragmentShader); 		// add the fragment shader to program
		GLES20.glLinkProgram(mProgram);                  		// creates OpenGL ES program executabl
	}

	public void draw(float[] m) {
		//setup();
		// Set the correct shader for our grid object.
		GLES20.glUseProgram(mProgram);

		// get handle to vertex shader's vPosition member
		mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
		// Enable a handle to the triangle vertices
		GLES20.glEnableVertexAttribArray(mPositionHandle);
		// Prepare the background coordinate data
		GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

		mTexCoordLoc = GLES20.glGetAttribLocation(mProgram, "a_texCoord" );
		// Prepare the texturecoordinates
		GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
		GLES20.glEnableVertexAttribArray(mPositionHandle);
		GLES20.glEnableVertexAttribArray(mTexCoordLoc);

		mColorHandle = GLES20.glGetAttribLocation(mProgram, "a_Color");
		// Enable a handle to the triangle vertices
		GLES20.glEnableVertexAttribArray(mColorHandle);
		// Prepare the background coordinate data
		GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);

		// get handle to shape's transformation matrix
		mMtrxHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
		// Apply the projection and view transformation
		GLES20.glUniformMatrix4fv(mMtrxHandle, 1, false, m, 0);
		mSamplerLoc = GLES20.glGetUniformLocation (mProgram, "s_texture" );

		// Set the sampler texture unit to our selected id
		//GLES20.glUniform1i (mSamplerLoc, mTextureNr);

		// draw the triangle
		//GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

		// Disable vertex array
		GLES20.glDisableVertexAttribArray(mPositionHandle);
		GLES20.glDisableVertexAttribArray(mTexCoordLoc);
		GLES20.glDisableVertexAttribArray(mColorHandle);

	}

	public void prepareDraw() {
		// Setup all the arrays
		prepareDrawInfo();

		// Using the iterator protects for problems with concurrency
		for (ARAppTextObject txt : txtCollection) {
			if (txt != null) {
				if (!(txt.text == null)) {
					convertTextToTriangleInfo(txt);
				}
			}
		}
	}

	public void prepareDrawInfo() {
		// Reset the indices.
		index_vecs = 0;
		index_indices = 0;
		index_uvs = 0;
		index_colors = 0;

		// Get the total amount of characters
		int charCount = 0;

		for (ARAppTextObject txt : txtCollection) {
			if(txt!=null)
			{
				if(!(txt.text==null))
				{
					charCount += txt.text.length();
				}
			}
		}

		// Create the arrays we need with the correct size.
		vecs = null;
		colors = null;
		uvs = null;
		indices = null;

		vecs = new float[charCount * 12];
		colors = new float[charCount * 16];
		uvs = new float[charCount * 8];
		indices = new short[charCount * 6];
	}

	private void convertTextToTriangleInfo(ARAppTextObject val) {
		// Get attributes from text object
		float x = val.x;
		float y = val.y;
		String text = val.text;

		// Create
		for(int j=0; j<text.length(); j++)
		{
			// get ascii value
			char c = text.charAt(j);
			int c_val = (int)c;

			int index = convertCharToIndex(c_val);

			if(index==-1) {
				// unknown character, we will add a space for it to be save.
				x += ((RI_TEXT_SPACESIZE) * mUniformScale);
				continue;
			}

			// Calculate the uv parts
			int row = index / 8;
			int col = index % 8;

			float v = row * RI_TEXT_UV_BOX_WIDTH;
			float v2 = v + RI_TEXT_UV_BOX_WIDTH;
			float u = col * RI_TEXT_UV_BOX_WIDTH;
			float u2 = u + RI_TEXT_UV_BOX_WIDTH;

			// Creating the triangle information
			float[] vec = new float[12];
			float[] uv = new float[8];
			float[] colors;

			vec[0] = x;
			vec[1] = y + (RI_TEXT_WIDTH * mUniformScale);
			vec[2] = 0.99f;
			vec[3] = x;
			vec[4] = y;
			vec[5] = 0.99f;
			vec[6] = x + (RI_TEXT_WIDTH * mUniformScale);
			vec[7] = y;
			vec[8] = 0.99f;
			vec[9] = x + (RI_TEXT_WIDTH * mUniformScale);
			vec[10] = y + (RI_TEXT_WIDTH * mUniformScale);
			vec[11] = 0.99f;

			colors = new float[] {
					val.color[0], val.color[1], val.color[2], val.color[3],
					val.color[0], val.color[1], val.color[2], val.color[3],
					val.color[0], val.color[1], val.color[2], val.color[3],
					val.color[0], val.color[1], val.color[2], val.color[3]
			};
			// 0.001f = texture bleeding hack/fix
			uv[0] = u+0.001f;
			uv[1] = v+0.001f;
			uv[2] = u+0.001f;
			uv[3] = v2-0.001f;
			uv[4] = u2-0.001f;
			uv[5] = v2-0.001f;
			uv[6] = u2-0.001f;
			uv[7] = v+0.001f;

			short[] inds = {0, 1, 2, 0, 2, 3};

			// Add our triangle information to our collection for 1 render call.
			addCharRenderInformation(vec, colors, uv, inds);

			// Calculate the new position
			x += ((l_size[index]/2)  * mUniformScale);
		}
	}

	private int convertCharToIndex(int c_val) {
		int index = -1;

		// Retrieve the index
		if(c_val>64&&c_val<91) // A-Z
			index = c_val - 65;
		else if(c_val>96&&c_val<123) // a-z
			index = c_val - 97;
		else if(c_val>47&&c_val<58) // 0-9
			index = c_val - 48 + 26;
		else if(c_val==43) // +
			index = 38;
		else if(c_val==45) // -
			index = 39;
		else if(c_val==33) // !
			index = 36;
		else if(c_val==63) // ?
			index = 37;
		else if(c_val==61) // =
			index = 40;
		else if(c_val==58) // :
			index = 41;
		else if(c_val==46) // .
			index = 42;
		else if(c_val==44) // ,
			index = 43;
		else if(c_val==42) // *
			index = 44;
		else if(c_val==36) // $
			index = 45;

		return index;
	}

	public void addCharRenderInformation(float[] vec, float[] cs, float[] uv, short[] indi)	{
		// We need a base value because the object has indices related to 
		// that object and not to this collection so basicly we need to 
		// translate the indices to align with the vertexlocation in ou
		// vecs array of vectors.
		short base = (short) (index_vecs / 3);
			
		// We should add the vec, translating the indices to our saved vector
		for (float aVec : vec) {
			vecs[index_vecs] = aVec;
			index_vecs++;
		}
		
		// We should add the colors, so we can use the same texture for multiple effects.
		for (float c : cs) {
			colors[index_colors] = c;
			index_colors++;
		}
		
		// We should add the uvs
		for (float anUv : uv) {
			uvs[index_uvs] = anUv;
			index_uvs++;
		}

		// We handle the indices
		for (short anIndi : indi) {
			indices[index_indices] = (short) (base + anIndi);
			index_indices++;
		}

		// Call setup here where everything is set correctly
		setup();
	}

	public float getUniformScale() {
		return mUniformScale;
	}

	public void setUniformScale(float mUniformScale) {
		this.mUniformScale = mUniformScale;
	}
}
