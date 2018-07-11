package com.mvcoder.opengldemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private FrameLayout container;
    private GLSurfaceView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        surfaceView = new MyGLSurfaceView(this);
        setContentView(surfaceView);
        initView();
    }

    private void initView() {
        //container = findViewById(R.id.glSurfaceViewContainer);
        // container.addView(surfaceView);
    }


    class MyGLSurfaceView extends GLSurfaceView {

        private MyRender mRenderer;
        private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
        private float mPreviousX;
        private float mPreviousY;


        public MyGLSurfaceView(Context context) {
            super(context);

            setEGLContextFactory(new ContextFactory());

            mRenderer = new MyRender();
            setRenderer(mRenderer);

            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            // MotionEvent reports input details from the touch screen
            // and other input controls. In this case, you are only
            // interested in events where the touch position changed.

            float x = e.getX();
            float y = e.getY();

            switch (e.getAction()) {
                case MotionEvent.ACTION_MOVE:

                    float dx = x - mPreviousX;
                    float dy = y - mPreviousY;

                    // reverse direction of rotation above the mid-line
                    if (y > getHeight() / 2) {
                        dx = dx * -1 ;
                    }

                    // reverse direction of rotation to left of the mid-line
                    if (x < getWidth() / 2) {
                        dy = dy * -1 ;
                    }

                    mRenderer.setAngle(
                            mRenderer.getAngle() +
                                    ((dx + dy) * TOUCH_SCALE_FACTOR));
                    requestRender();
            }

            mPreviousX = x;
            mPreviousY = y;
            return true;

        }
    }


    class ContextFactory implements GLSurfaceView.EGLContextFactory {

        private int EGL_CONTEXT_CLIENT_VERSION = 0x3098;


        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
            egl.eglDestroyContext(display, context);
        }

        @Override
        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
            int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
            EGLContext context = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
            return context;
        }
    }

    class MyRender implements GLSurfaceView.Renderer {

        // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
        private final float[] mMVPMatrix = new float[16];
        private final float[] mProjectionMatrix = new float[16];
        private final float[] mViewMatrix = new float[16];
        private float[] mRotationMatrix = new float[16];


        private Triangle mTriangle;

        public volatile float mAngle;

        public float getAngle() {
            return mAngle;
        }

        public void setAngle(float angle) {
            mAngle = angle;
        }


        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            log("onSurfaceCreated");
            mTriangle = new Triangle();
            String version = GLES10.glGetString(GLES10.GL_VERSION);
            log("glversion : " + version);
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            log("onSurfaceChanged");
            //设置投影到的屏幕的大小
            GLES20.glViewport(0, 0, width, height);

            float ratio = (float) width / height;

            System.out.println("width : " + width + " , height : " + height);

            Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            //log("onDrawFrame");
            // Set GL_MODELVIEW transformation mode

            float[] scratch = new float[16];
            // Create a rotation transformation for the triangle
          //  long time = SystemClock.uptimeMillis() % 4000L;
          //  float angle = 0.090f * ((int) time);
            Matrix.setRotateM(mRotationMatrix, 0, mAngle, 0, 0, -1.0f);


            Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0);

            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

            Matrix.multiplyMM(scratch, 0 , mMVPMatrix, 0,mRotationMatrix, 0);

            // Draw shape
            mTriangle.draw(scratch);

        }
    }


    class Triangle {

        private FloatBuffer vertexBuffer;
        private final int mProgram;

        static final int COORDS_PER_VERTEX = 3;


        float triangleCoords[] = new float[]{
                0.0f, 0.622008459f, 0.0f, // top
                -0.5f, -0.311004243f, 0.0f, // bottom left
                0.5f, -0.311004243f, 0.0f  // bottom right
        };

        private final int vertexCount = triangleCoords.length / COORDS_PER_VERTEX;

        private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex


        float color[] = {0.63671875f, 0.76953125f, 0.22265625f, 1.0f};

        float texCoords[] = {
                0.0f, 0.0f, // 左下角
                1.0f, 0.0f, // 右下角
                0.5f, 1.0f // 上中
        };

        private final String vertexShaderCode =
                "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                        "void main() {" +
                        "  gl_Position = uMVPMatrix * vPosition;" +
                        "}";

        private final String fragmentShaderCode =
                "precision mediump float;" +
                        "uniform vec4 vColor;" +
                        "void main() {" +
                        "  gl_FragColor = vColor;" +
                        "}";

        private final String fragmentTextureShaderCode =
                "varying vec2 v_texCoord; \n" +
                        "uniform sampler2D u_samplerTexture; \n" +
                        "void main() \n" +
                        "{ \n" +
                        "gl_FragColor = texture2D(u_samplerTexture, v_texCoord); \n" +
                        "}";

        // Use to access and set the view transformation
        private int mMVPMatrixHandle;

        private int mPositionHandle;

        private int mColorHandle;

        public Triangle() {
            // initialize vertex byte buffer for shape coordinates
            ByteBuffer bb = ByteBuffer.allocateDirect(
                    // (number of coordinate values * 4 bytes per float)
                    triangleCoords.length * 4);
            // use the device hardware's native byte order
            bb.order(ByteOrder.nativeOrder());

            // create a floating point buffer from the ByteBuffer
            vertexBuffer = bb.asFloatBuffer();
            // add the coordinates to the FloatBuffer
            vertexBuffer.put(triangleCoords);
            // set the buffer to read the first coordinate
            vertexBuffer.position(0);

            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);

            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            int fragmentTexShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentTextureShaderCode);

            mProgram = GLES20.glCreateProgram();

            GLES20.glAttachShader(mProgram, vertexShader);

            //GLES20.glAttachShader(mProgram, fragmentShader);

            //纹理
            GLES20.glAttachShader(mProgram, fragmentTexShader);

            GLES20.glLinkProgram(mProgram);

        }


        private int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
        }

        int[] loadTexture() {
            int[] textureId = new int[1];

            // Generate a texture object
            GLES20.glGenTextures(1, textureId, 0);
            int[] result = null;
            if (textureId[0] != 0) {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher);
                result = new int[3];
                result[0] = textureId[0]; // TEXTURE_ID
                result[1] = bitmap.getWidth(); // TEXTURE_WIDTH
                result[2] = bitmap.getHeight(); // TEXTURE_HEIGHT
                // Bind to the texture in OpenGL
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);
                // Set filtering
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                        GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                        GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                        GLES20.GL_REPEAT);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                        GLES20.GL_REPEAT);
                // Load the bitmap into the bound texture.
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
                // Recycle the bitmap, since its data has been loaded into OpenGL.
                bitmap.recycle();
            } else {
                throw new RuntimeException("Error loading texture.");
            }
            return result;
        }

        public void draw(float[] mvpMatrix) { // pass in the calculated transformation matrix

            GLES20.glUseProgram(mProgram);

            // get handle to vertex shader's vPosition member
            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(mPositionHandle);

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    vertexStride, vertexBuffer);



            // get handle to fragment shader's vColor member
            //mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

            // Set color for drawing the triangle
            //GLES20.glUniform4fv(mColorHandle, 1, color, 0);

            //GLES20.glGenTextures();


            //应用projection投影和camera View
            mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

            // Draw the triangle
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(mPositionHandle);
        }
    }

    public class Square {

        private FloatBuffer vertexBuffer;
        private ShortBuffer drawListBuffer;

        // number of coordinates per vertex in this array
        static final int COORDS_PER_VERTEX = 3;
        float squareCoords[] = {
                -0.5f, 0.5f, 0.0f,   // top left
                -0.5f, -0.5f, 0.0f,   // bottom left
                0.5f, -0.5f, 0.0f,   // bottom right
                0.5f, 0.5f, 0.0f}; // top right

        private short drawOrder[] = {0, 1, 2, 0, 2, 3}; // order to draw vertices

        public Square() {
            // initialize vertex byte buffer for shape coordinates
            ByteBuffer bb = ByteBuffer.allocateDirect(
                    // (# of coordinate values * 4 bytes per float)
                    squareCoords.length * 4);
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(squareCoords);
            vertexBuffer.position(0);

            // initialize byte buffer for the draw list
            ByteBuffer dlb = ByteBuffer.allocateDirect(
                    // (# of coordinate values * 2 bytes per short)
                    drawOrder.length * 2);
            dlb.order(ByteOrder.nativeOrder());
            drawListBuffer = dlb.asShortBuffer();
            drawListBuffer.put(drawOrder);
            drawListBuffer.position(0);
        }
    }

    private void log(String str) {
        Log.d(TAG, str);
    }
}
