package com.angel.testjarapplication;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.Toast;

import com.angel.makejar.theta_v.DeviceOp_GetInfo;
import com.angel.makejar.theta_v.ImageCaptureControl;
import com.angel.makejar.theta_v.LivePreviewControl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 创建日期：2018/9/17 on 15:24
 * 描述:
 * 作者:波波 yjb
 */
public class DemoJavaActivity extends AppCompatActivity {
    private DeviceOp_GetInfo deviceOp_getInfo;
    private LivePreviewControl livePreviewControl;
    private ImageCaptureControl imageCaptureControl;
    private GLSurfaceView glSurfaceView;

    public float mAngleX = 0;// 摄像机所在的x坐标
    public float mAngleY = 0;// 摄像机所在的y坐标
    public float mAngleZ = 3;// 摄像机所在的z坐标
    public static String VL = "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "attribute vec2 a_texCoord;" +
            "varying vec2 v_texCoord;" +
            "void main() {" +
            " gl_Position = uMVPMatrix * vPosition;" +
            " v_texCoord = a_texCoord;" +
            "}";
    public static String FL = "precision mediump float;" +
            "varying vec2 v_texCoord;" +
            "uniform sampler2D s_texture;" +
            "void main() {" +
            " gl_FragColor = texture2D( s_texture, v_texCoord );" +
            "}";

    float startRawX;
    float startRawY;

    double xFlingAngle;
    double xFlingAngleTemp;

    double yFlingAngle;
    double yFlingAngleTemp;
    private Bitmap bitmap;
    boolean canDrag = false;
    private boolean rendererSet = false;
    //** 惯性自滚标志
    private volatile boolean gestureInertia_isStop = true;
    private VelocityTracker mVelocityTracker = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initInfo();
        initEvent();
    }

    private void initEvent() {
        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent me) {
                //处理手指滑动事件，我这里的处理是判断手指在横向和竖向滑动的距离
                //这个距离隐射到球体上经度和纬度的距离，根据这个距离计算三维空间的两个
                //夹角，根据这个夹角调整摄像机所在位置
                if (me.getAction() == MotionEvent.ACTION_DOWN||me.getAction()==MotionEvent.ACTION_POINTER_DOWN) {
                    // ▼ 判断是否是第一个手指 && 是否包含在图片区域内
                    if (me.getPointerId(me.getActionIndex()) == 0) {
                        canDrag = true;
                        gestureInertia_isStop=true;
                        startRawX = me.getRawX();
                        startRawY = me.getRawY();

                        // 增加速度
                        if (mVelocityTracker == null) {
                            mVelocityTracker = VelocityTracker.obtain();
                        } else {
                            mVelocityTracker.clear();
                        }
                        mVelocityTracker.addMovement(me);
                    }
                } else if (me.getAction() == MotionEvent.ACTION_MOVE) {
                    // 如果存在第一个手指，且这个手指的落点在图片区域内
                    if (canDrag) {
                        mVelocityTracker.addMovement(me);
                        mVelocityTracker.computeCurrentVelocity(1000);
                        // 在获取速度之前总要进行以上两步
                        float distanceX = startRawX - me.getRawX();
                        float distanceY = startRawY - me.getRawY();
                        //这里的0.1f是为了不上摄像机移动的过快
                        distanceY = 0.1f * (distanceY) / getWindowManager().getDefaultDisplay().getHeight();
                        yFlingAngleTemp = distanceY * 180 / (Math.PI * 3);
                        if (yFlingAngleTemp + yFlingAngle > Math.PI / 2) {
                            yFlingAngleTemp = Math.PI / 2 - yFlingAngle;
                        }
                        if (yFlingAngleTemp + yFlingAngle < -Math.PI / 2) {
                            yFlingAngleTemp = -Math.PI / 2 - yFlingAngle;
                        }
                        //这里的0.1f是为了不上摄像机移动的过快
                        distanceX = 0.1f * (-distanceX) / getWindowManager().getDefaultDisplay().getWidth();
                        xFlingAngleTemp = distanceX * 180 / (Math.PI * 3);
                        mAngleX = (float) (3 * Math.cos(yFlingAngle + yFlingAngleTemp) * Math.sin(xFlingAngle + xFlingAngleTemp));
                        mAngleY = (float) (3 * Math.sin(yFlingAngle + yFlingAngleTemp));
                        mAngleZ = (float) (3 * Math.cos(yFlingAngle + yFlingAngleTemp) * Math.cos(xFlingAngle + xFlingAngleTemp));
                        glSurfaceView.requestRender();
                    }
                } else if (me.getAction() == MotionEvent.ACTION_UP||me.getAction()==MotionEvent.ACTION_POINTER_UP) {
                    // ▼ 判断是否是第一个手指
                    if (me.getPointerId(me.getActionIndex()) == 0) {
                        canDrag = false;
                        xFlingAngle += xFlingAngleTemp;
                        yFlingAngle += yFlingAngleTemp;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                gestureInertia_isStop = false;
                                float mXVelocity = mVelocityTracker.getXVelocity();
                                float mYVelocity = mVelocityTracker.getYVelocity();
                                while (!gestureInertia_isStop) {
                                    float offsetY = mYVelocity / 1000;
                                    //这里的0.3f是为了不上摄像机移动的过快
                                    offsetY = 0.3f*(-offsetY) / getWindowManager().getDefaultDisplay().getHeight();
                                    yFlingAngleTemp = offsetY * 180 / (Math.PI * 3);
                                    if (yFlingAngleTemp + yFlingAngle > Math.PI / 2) {
                                        yFlingAngleTemp = Math.PI / 2 - yFlingAngle;
                                    }
                                    if (yFlingAngleTemp + yFlingAngle < -Math.PI / 2) {
                                        yFlingAngleTemp = -Math.PI / 2 - yFlingAngle;
                                    }
                                    float offsetX = mXVelocity / 1000;
                                    offsetX = 0.3f*(offsetX) / getWindowManager().getDefaultDisplay().getWidth();
                                    xFlingAngleTemp = offsetX * 180 / (Math.PI * 3);
                                    mAngleX = (float) (3 * Math.cos(yFlingAngle + yFlingAngleTemp) * Math.sin(xFlingAngle + xFlingAngleTemp));
                                    mAngleY = (float) (3 * Math.sin(yFlingAngle + yFlingAngleTemp));
                                    mAngleZ = (float) (3 * Math.cos(yFlingAngle + yFlingAngleTemp) * Math.cos(xFlingAngle + xFlingAngleTemp));
                                    glSurfaceView.requestRender();

                                    if (Math.abs(mYVelocity - 0.97f * mYVelocity) < 0.00001f
                                            || Math.abs(mXVelocity - 0.97f * mXVelocity) < 0.00001f) {
                                        gestureInertia_isStop = true;
                                    }
                                    mYVelocity = 0.975f * mYVelocity;
                                    mXVelocity = 0.975f * mXVelocity;
                                    xFlingAngle += xFlingAngleTemp;
                                    yFlingAngle += yFlingAngleTemp;
                                    try {
                                        Thread.sleep(5);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }).start();
                    }
                }
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if( rendererSet ) {
            glSurfaceView.onResume();
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if( rendererSet ) {
            glSurfaceView.onPause();
        }
        if(null != mVelocityTracker) {
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void initInfo() {
        deviceOp_getInfo=new DeviceOp_GetInfo();
        deviceOp_getInfo.setIResult(new DeviceOp_GetInfo.IResult() {
            @Override
            public void onResult(final boolean s) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(s){
                            Toast.makeText(DemoJavaActivity.this,"连接成功",Toast.LENGTH_SHORT).show();
                        }else {
                            Toast.makeText(DemoJavaActivity.this,"没有连接全景相机",Toast.LENGTH_SHORT).show();
                        }

                    }
                });
            }
        });

        livePreviewControl=new LivePreviewControl(){
            @Override
            protected void onLiveFrameReceived(Bitmap image) {
                super.onLiveFrameReceived(image);
                bitmap=image;
            }
        };

        imageCaptureControl=new ImageCaptureControl();
        imageCaptureControl.setIResult(new ImageCaptureControl.IResult() {
            @Override
            public void onSuccess(final String s) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(DemoJavaActivity.this,"拍照完成"+s,Toast.LENGTH_SHORT).show();
                        livePreviewControl.begin();
                    }
                });
            }
        });
    }

    private void initView() {
       setTitle("JAVA案例");
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        glSurfaceView=findViewById(R.id.gl_surface);

        int glVersion = getGLVersion();
        if(glVersion > 0x20000
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                && (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")))
                ){
            glSurfaceView.setEGLContextClientVersion(2);
            glSurfaceView.setRenderer(new RenderListener());
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
            rendererSet = true;
        } else {
            Toast.makeText(this, "该设备不支持OpenGL ES 2.0",
                    Toast.LENGTH_SHORT).show();
            return;
        }
    }

    private int getGLVersion(){
        ActivityManager activityManager =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo deviceConfigurationInfo =
                activityManager.getDeviceConfigurationInfo();
        return deviceConfigurationInfo.reqGlEsVersion;
    }

    class RenderListener implements GLSurfaceView.Renderer {
        FloatBuffer verticalsBuffer;
        int CAP = 9;//绘制球体时，每次增加的角度
        float[] verticals = new float[(180/CAP) * (360/CAP) * 6 * 3];
        private final FloatBuffer mUvTexVertexBuffer;
        private final float[] UV_TEX_VERTEX = new float[(180/CAP) * (360/CAP) * 6 * 2];
        private int mProgram;
        private int mPositionHandle;
        private int mTexCoordHandle;
        private int mMatrixHandle;
        private int mTexSamplerHandle;
        int[] mTexNames;
        private final float[] mProjectionMatrix = new float[16];
        private final float[] mCameraMatrix = new float[16];
        private final float[] mMVPMatrix = new float[16];
        private int mWidth;
        private int mHeight;

        public RenderListener() {
            float x = 0;
            float y = 0;
            float z = 0;

            float r = 3;//球体半径
            int index = 0;
            int index1 = 0;
            double d = CAP * Math.PI / 180;//每次递增的弧度
            for (int i = 0; i < 180; i += CAP) {
                double d1 = i * Math.PI / 180;
                for (int j = 0; j < 360; j += CAP) {
                    //获得球体上切分的超小片矩形的顶点坐标（两个三角形组成，所以有六点顶点）
                    double d2 = j * Math.PI / 180;
                    verticals[index++] = (float) (x + r * Math.sin(d1 + d) * Math.cos(d2 + d));
                    verticals[index++] = (float) (y + r * Math.cos(d1 + d));
                    verticals[index++] = (float) (z + r * Math.sin(d1 + d) * Math.sin(d2 + d));
                    //获得球体上切分的超小片三角形的纹理坐标
                    UV_TEX_VERTEX[index1++] = (j + CAP) * 1f / 360;
                    UV_TEX_VERTEX[index1++] = (i + CAP) * 1f / 180;
                    verticals[index++] = (float) (x + r * Math.sin(d1) * Math.cos(d2));
                    verticals[index++] = (float) (y + r * Math.cos(d1));
                    verticals[index++] = (float) (z + r * Math.sin(d1) * Math.sin(d2));

                    UV_TEX_VERTEX[index1++] = j * 1f / 360;
                    UV_TEX_VERTEX[index1++] = i * 1f / 180;

                    verticals[index++] = (float) (x + r * Math.sin(d1) * Math.cos(d2 + d));
                    verticals[index++] = (float) (y + r * Math.cos(d1));
                    verticals[index++] = (float) (z + r * Math.sin(d1) * Math.sin(d2 + d));

                    UV_TEX_VERTEX[index1++] = (j + CAP) * 1f / 360;
                    UV_TEX_VERTEX[index1++] = i * 1f / 180;

                    verticals[index++] = (float) (x + r * Math.sin(d1 + d) * Math.cos(d2 + d));
                    verticals[index++] = (float) (y + r * Math.cos(d1 + d));
                    verticals[index++] = (float) (z + r * Math.sin(d1 + d) * Math.sin(d2 + d));

                    UV_TEX_VERTEX[index1++] = (j + CAP) * 1f / 360;
                    UV_TEX_VERTEX[index1++] = (i + CAP) * 1f / 180;

                    verticals[index++] = (float) (x + r * Math.sin(d1 + d) * Math.cos(d2));
                    verticals[index++] = (float) (y + r * Math.cos(d1 + d));
                    verticals[index++] = (float) (z + r * Math.sin(d1 + d) * Math.sin(d2));

                    UV_TEX_VERTEX[index1++] = j * 1f / 360;
                    UV_TEX_VERTEX[index1++] = (i + CAP) * 1f / 180;

                    verticals[index++] = (float) (x + r * Math.sin(d1) * Math.cos(d2));
                    verticals[index++] = (float) (y + r * Math.cos(d1));
                    verticals[index++] = (float) (z + r * Math.sin(d1) * Math.sin(d2));

                    UV_TEX_VERTEX[index1++] = j * 1f / 360;
                    UV_TEX_VERTEX[index1++] = i * 1f / 180;

                }
            }
            verticalsBuffer = ByteBuffer.allocateDirect(verticals.length * 4) .order(ByteOrder.nativeOrder()) .asFloatBuffer() .put(verticals);
            verticalsBuffer.position(0);

            mUvTexVertexBuffer = ByteBuffer.allocateDirect(UV_TEX_VERTEX.length * 4) .order(ByteOrder.nativeOrder()) .asFloatBuffer() .put(UV_TEX_VERTEX);
            mUvTexVertexBuffer.position(0);
        }
        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            mWidth = width;
            mHeight = height;
            mProgram = GLES20.glCreateProgram();

            int vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
            GLES20.glShaderSource(vertexShader, VL);
            GLES20.glCompileShader(vertexShader);

            int fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
            GLES20.glShaderSource(fragmentShader, FL);
            GLES20.glCompileShader(fragmentShader);

            GLES20.glAttachShader(mProgram, vertexShader);
            GLES20.glAttachShader(mProgram, fragmentShader);

            GLES20.glLinkProgram(mProgram);

            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
            mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "a_texCoord");
            mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            mTexSamplerHandle = GLES20.glGetUniformLocation(mProgram, "s_texture");

            mTexNames = new int[1];
            GLES20.glGenTextures(1, mTexNames, 0);
            //这里的全景图需要长宽的比例使2：1，不然上下顶点会出现形变
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexNames[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

            float ratio = (float) height / width;
            Matrix.frustumM(mProjectionMatrix, 0, -1, 1, -ratio, ratio, 3, 7);

        }

        @Override
        public void onDrawFrame(GL10 gl) {
            if(bitmap!=null) {
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
                bitmap=null;
            }
            //调整摄像机焦点位置，使画面滚动
            Matrix.setLookAtM(mCameraMatrix, 0, mAngleX, mAngleY, mAngleZ, 0, 0, 0, 0, 1, 0);
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mCameraMatrix, 0);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            GLES20.glUseProgram(mProgram);
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false,
                    12, verticalsBuffer);
            GLES20.glEnableVertexAttribArray(mTexCoordHandle);
            GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false,
                    0, mUvTexVertexBuffer);
            GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMVPMatrix, 0);
            GLES20.glUniform1i(mTexSamplerHandle, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, (180/CAP) * (360/CAP) * 6);
            GLES20.glDisableVertexAttribArray(mPositionHandle);
        }
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.bt_con_info:
                new Thread(deviceOp_getInfo).start();
                break;
            case R.id.bt_pre:
                //开始预览
                livePreviewControl.begin();
                break;
            case R.id.bt_cap:
                livePreviewControl.end();
                imageCaptureControl.begin();
                break;
            case R.id.bt_jump:
                Intent intent=new Intent(this,JavaImageListActivity.class);
                startActivity(intent);
                break;
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }



}
