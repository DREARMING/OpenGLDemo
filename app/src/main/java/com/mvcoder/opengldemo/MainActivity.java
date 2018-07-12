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

        private Square mSquare;

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
            //必须在surface建立后才能创建图形，因为图形所需要的GLES环境这时候才可用
            mTriangle = new Triangle();
            mSquare = new Square();
            String version = GLES10.glGetString(GLES10.GL_VERSION);
            log("glversion : " + version);

            //设置清除每一帧的默认颜色，即背景色
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            log("onSurfaceChanged");
            //设置投影到的屏幕的大小
            GLES20.glViewport(0, 0, width, height);

            float ratio = (float) width / height;

            System.out.println("width : " + width + " , height : " + height);

            //设置投影矩阵
            Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
        }

        //该方法会频繁调用，每一帧都会被回掉，但是当GlSurfaceView 设置了setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        // 就只能在 requestRender() 的时候才会回掉
        @Override
        public void onDrawFrame(GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            //log("onDrawFrame");
            // Set GL_MODELVIEW transformation mode

            float[] scratch = new float[16];
            // Create a rotation transformation for the triangle
          //  long time = SystemClock.uptimeMillis() % 4000L;
          //  float angle = 0.090f * ((int) time);
            //设置旋转矩阵
            Matrix.setRotateM(mRotationMatrix, 0, mAngle, 0, 0, -1.0f);

            //设置摄像机矩阵
            Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0);

            //矩阵相乘，得到投影矩阵和摄像机矩阵的融合矩阵，该组合可以让顶点着色正确的将坐标转换成屏幕的具体坐标
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

            //再相乘旋转矩阵，让手势控制图形进行旋转
            Matrix.multiplyMM(scratch, 0 , mMVPMatrix, 0,mRotationMatrix, 0);

            // Draw shape
            //mTriangle.draw(scratch);
            mSquare.draw(scratch);

        }
    }


    class Triangle {

        private FloatBuffer vertexBuffer;
        private FloatBuffer texBuffer;
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
                0.5f, 1.0f, // 上中
                0.0f, 0.0f, // 左下角
                1.0f, 0.0f // 右下角
        };

        private final String vertexShaderCode =
                "uniform mat4 uMVPMatrix;" +
                        "varying vec2 v_texCoord;" +
                        "layout (location = 1) attribute vec2 aTexCoord;" +
                "attribute vec4 vPosition;" +
                        "void main() {" +
                        "  gl_Position = uMVPMatrix * vPosition;" +
                        "  v_texCoord = aTexCoord;" +
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

        private int mTextureId = -1;

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


            ByteBuffer tb = ByteBuffer.allocateDirect(texCoords.length * 4);
            tb.order(ByteOrder.nativeOrder());
            texBuffer = tb.asFloatBuffer();
            texBuffer.put(texCoords);
            texBuffer.position(0);

            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);

            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            int fragmentTexShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentTextureShaderCode);

            loadTexture();

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
                Bitmap bitmap = BitmapFactory.decodeStream(getResources().openRawResource(R.raw.ic_launcher));
                result = new int[3];
                result[0] = textureId[0]; // TEXTURE_ID
                result[1] = bitmap.getWidth(); // TEXTURE_WIDTH
                result[2] = bitmap.getHeight(); // TEXTURE_HEIGHT
                // Bind to the texture in OpenGL
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);

                mTextureId = textureId[0];
                // Set filtering
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                        GLES20.GL_NEAREST);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                        GLES20.GL_NEAREST);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                        GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                        GLES20.GL_CLAMP_TO_EDGE);
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

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);

            GLES20.glEnableVertexAttribArray(1);

            GLES20.glVertexAttribPointer(1, 2,GLES20.GL_FLOAT,false, 2 * 4, texBuffer);

            // get handle to fragment shader's vColor member
            //mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

            // Set color for drawing the triangle
            //GLES20.glUniform4fv(mColorHandle, 1, color, 0);


            //应用projection投影和camera View
            mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

            // Draw the triangle
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(mPositionHandle);
            GLES20.glDisableVertexAttribArray(1);
        }
    }

    public class Square {

        private int program;

        private FloatBuffer vertexBuffer;
        private ShortBuffer drawListBuffer;
        private FloatBuffer mTexBuffer;

        // number of coordinates per vertex in this array
        static final int COORDS_PER_VERTEX = 3;

        float color[] = {0.63671875f, 0.76953125f, 0.22265625f, 1.0f};

        float squareCoords[] = {
                -0.5f, 0.5f, 0.0f,   // top left
                -0.5f, -0.5f, 0.0f,   // bottom left
                0.5f, -0.5f, 0.0f,   // bottom right
                0.5f, 0.5f, 0.0f    // top right
        };

        private short drawOrder[] = {0, 1, 2, 0, 2, 3}; // order to draw vertices

        float texCoords[] = {
                0.0f, 1.0f, // 上中
                0.0f, 0.0f, // 左下角
                1.0f, 0.0f, // 右下角
                1.0f, 1.0f
        };

        /**
         * GLSL 语法
         *
         * 1. varying 关键字一般用于输出顶点着色器的属性到片段着色器
         * 2. layout(location = 1) 用于标记一个属性所在的位置，可以通过 glGetAttribLocation 通过变量名找到属性，然后glVertexAttribPointer 方法注入属性值
         * 3. uniform变量的注入 可以通过glGetUniformLocation(program, "uMVPMatrix") 获取 uniform 变量的位置pos，再通过 glUniformMatrix4fv(pos, 1, false, mvpMatrix,0)
         */
        private final String vertexShaderCode =
                "uniform mat4 uMVPMatrix;" +
                        "varying vec2 v_texCoord;" +
                        "attribute vec4 vPosition;" +
                        "layout (location = 1) attribute vec2 aTexCoord;" +
                        "void main() {" +
                        "  gl_Position = uMVPMatrix * vPosition;" +
                        "  v_texCoord = aTexCoord;" +
                        "}";

        private final String fragmentTexShaderCode =
                        "varying vec2 v_texCoord;" +
                        "uniform sampler2D u_samplerTexture;" +
                        "void main() {" +
                        "  gl_FragColor = texture2D(u_samplerTexture, v_texCoord);" +
                        "}";

        private final String fragmentShaderCode =
                "precision mediump float;" +
                        "uniform vec4 vColor;" +
                        "void main() {" +
                        "  gl_FragColor = vColor;" +
                        "}";

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

            ByteBuffer tb = ByteBuffer.allocateDirect(texCoords.length * 4);
            tb.order(ByteOrder.nativeOrder());
            mTexBuffer = tb.asFloatBuffer();
            mTexBuffer.put(texCoords);
            mTexBuffer.position(0);


            //创建顶点着色器
            int vertextShaderId = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
            //设置顶点着色器的代码字符串
            GLES20.glShaderSource(vertextShaderId, vertexShaderCode);
            //编译顶点着色器
            GLES20.glCompileShader(vertextShaderId);

            //创建片段着色器
            int fragmentTexShaderId = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
            //设置片段着色器的代码字符串
            GLES20.glShaderSource(fragmentTexShaderId, fragmentTexShaderCode);
            //编译片段着色器
            GLES20.glCompileShader(fragmentTexShaderId);

            int fragmentShaderId = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
            GLES20.glShaderSource(fragmentShaderId, fragmentShaderCode);
            GLES20.glCompileShader(fragmentShaderId);

            //创建该图形的渲染程序
            program = GLES20.glCreateProgram();

            bindTexture();

            //让程序绑定顶点着色器
            GLES20.glAttachShader(program,vertextShaderId);
            //让程序绑定片段着色器
            GLES20.glAttachShader(program,fragmentTexShaderId);
            GLES20.glLinkProgram(program);


            //绑定完之后就可以删除掉着色器
            GLES20.glDeleteShader(vertextShaderId);
            //GLES20.glDeleteShader(fragmentShaderId);
            GLES20.glDeleteShader(fragmentTexShaderId);
        }

        private int textureId = -1;

        private void bindTexture(){
            Bitmap bitmap = BitmapFactory.decodeStream(getResources().openRawResource(R.raw.ic_launcher));
            if(bitmap == null) return;
            int[] texture = new int[1];

            //产生一个纹理对象。
            GLES20.glGenTextures(texture.length, texture, 0);
            textureId = texture[0];
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            //绑定纹理对象，绑定以后，对于GL_TEXTURE_2D的所有操作，都会操作到该纹理对象中
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

            //设置过滤器，在空间坐标选择纹理点的时候用线性还是附近的方式，GL_NEAREST是设置选取纹理点到具体坐标的最附近值，GL_linear则选取附近集中颜色的叠加
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

            //设置包裹模式，当顶点坐标在纹理坐标之外，会以哪种模式显示
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

            //设置当前纹理对象的纹理为bitmap
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }


        public void draw(float[] mvpMatrix){
            //设置渲染程序
            GLES20.glUseProgram(program);

            //这里采用索引坐标方法，这里是填充一个索引坐标数组到GL_ELEMENT_ARRAY_BUFFER中，该数组代表顶点数组中哪几个顶点组成一个图形。
            int[] drawBuffer = new int[1];
            GLES20.glGenBuffers(1, drawBuffer, 0);
            int bufferId = drawBuffer[0];
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, bufferId);
            //第二个参数是整个索引坐标数组的占用字节数，第三个参数是索引坐标数组的direct native order buffer
            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, 12, drawListBuffer,GLES20.GL_STATIC_DRAW);

            //填充顶点数组
            //第一个参数是填充 vertex shader具体属性的location，第二个参数指需要多个元素代表一个属性，这里是三维坐标，所以是3
            //第五个参数是跨度，下一个属性需要跳跃多少字节。这里显而易见是 3 * sizeof(float); 第六个就是三位坐标的buffer。
            GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT,false, 3 * 4, vertexBuffer);
            //开启location = 0的属性设置，默认为false
            GLES20.glEnableVertexAttribArray(0);


            //绑纹理题对象
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

            //设置纹理坐标属性，设置跟顶点属性一致
            GLES20.glVertexAttribPointer(1, 2,GLES20.GL_FLOAT,false, 2 * 4, mTexBuffer);
            GLES20.glEnableVertexAttribArray(1);


            //获取uniform 属性，并且填充它
            int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix,0);


            /*
            //设置颜色
            int mColorHandle = GLES20.glGetUniformLocation(program, "vColor");
            //Set color for drawing the triangle
            GLES20.glUniform4fv(mColorHandle, 1, color, 0);*/

            //绘制三角形，并且以索引坐标数组指定的属性为准。第二个参数是画多少个点，第三个参数是索引坐标的类型
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, 0);

            //同样是绘制三角形，但是是通过顶点坐标数组实现
            //GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);

            //用完可以清理掉
            GLES20.glDisableVertexAttribArray(0);
            GLES20.glDisableVertexAttribArray(1);

        }
    }

    private void log(String str) {
        Log.d(TAG, str);
    }
}
