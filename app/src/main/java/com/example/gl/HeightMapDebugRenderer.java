package com.example.gl;

import android.content.Context;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class HeightMapDebugRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "HeightMapDebug";
    private Context context;
    private int debugProgram;
    private int heightMapTextureId;
    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;

    // 简单的全屏四边形顶点
    private static final float[] VERTICES = {
            -1.0f, -1.0f, 0.0f,  // 左下
            1.0f, -1.0f, 0.0f,  // 右下
            -1.0f, 1.0f, 0.0f,  // 左上
            1.0f, 1.0f, 0.0f   // 右上
    };

    private static final float[] TEX_COORDS = {
            0.0f, 0.0f,  // 左下
            1.0f, 0.0f,  // 右下
            0.0f, 1.0f,  // 左上
            1.0f, 1.0f   // 右上
    };

    public HeightMapDebugRenderer(Context context, int heightMapTextureId) {
        this.context = context;

        // 直接使用传入的纹理ID，或者使用测试纹理
        if (heightMapTextureId == -1) {
            Log.w(TAG, "Using test texture instead of provided texture");
            this.heightMapTextureId = loadTestTexture();
        } else {
            this.heightMapTextureId = heightMapTextureId;
            Log.i(TAG, "Using provided texture ID: " + heightMapTextureId);
        }

        // 初始化缓冲区
        vertexBuffer = ByteBuffer.allocateDirect(VERTICES.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(VERTICES).position(0);

        texCoordBuffer = ByteBuffer.allocateDirect(TEX_COORDS.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        texCoordBuffer.put(TEX_COORDS).position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // 对于2D纹理渲染，禁用深度测试
        GLES32.glDisable(GLES32.GL_DEPTH_TEST);
        GLES32.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        loadDebugShaders();

        Log.i(TAG, "Height map texture ID: " + heightMapTextureId);

        // 检查纹理是否存在
        if (heightMapTextureId == -1) {
            Log.e(TAG, "Height map texture is invalid!");
        }

        // 添加详细的纹理检查
        checkTextureStatus();
    }

    private void checkTextureStatus() {
        if (heightMapTextureId != -1) {
            GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, heightMapTextureId);

            int[] width = new int[1];
            int[] height = new int[1];
            GLES32.glGetTexLevelParameteriv(GLES32.GL_TEXTURE_2D, 0, GLES32.GL_TEXTURE_WIDTH, width, 0);
            GLES32.glGetTexLevelParameteriv(GLES32.GL_TEXTURE_2D, 0, GLES32.GL_TEXTURE_HEIGHT, height, 0);

            Log.i(TAG, "Texture dimensions: " + width[0] + "x" + height[0]);

            int error = GLES32.glGetError();
            if (error != GLES32.GL_NO_ERROR) {
                Log.e(TAG, "OpenGL error after texture bind: " + error);
            }
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES32.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // 只清除颜色缓冲，不清除深度缓冲
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT);

        if (debugProgram == 0 || heightMapTextureId == -1) {
            Log.e(TAG, "Cannot render: program=" + debugProgram + ", texture=" + heightMapTextureId);
            return;
        }

        GLES32.glUseProgram(debugProgram);

        // 设置顶点属性
        int positionHandle = GLES32.glGetAttribLocation(debugProgram, "aPosition");
        if (positionHandle == -1) {
            Log.e(TAG, "aPosition attribute not found!");
            return;
        }
        GLES32.glEnableVertexAttribArray(positionHandle);
        GLES32.glVertexAttribPointer(positionHandle, 3, GLES32.GL_FLOAT, false, 12, vertexBuffer);

        int texCoordHandle = GLES32.glGetAttribLocation(debugProgram, "aTexCoord");
        if (texCoordHandle == -1) {
            Log.e(TAG, "aTexCoord attribute not found!");
            return;
        }
        GLES32.glEnableVertexAttribArray(texCoordHandle);
        GLES32.glVertexAttribPointer(texCoordHandle, 2, GLES32.GL_FLOAT, false, 8, texCoordBuffer);

        // 绑定高度图纹理
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, heightMapTextureId);

        int textureHandle = GLES32.glGetUniformLocation(debugProgram, "uHeightMap");
        if (textureHandle == -1) {
            Log.e(TAG, "uHeightMap uniform not found!");
            return;
        }
        GLES32.glUniform1i(textureHandle, 0);

        // 绘制全屏四边形
        GLES32.glDrawArrays(GLES32.GL_TRIANGLE_STRIP, 0, 4);

        // 检查绘制错误
        int error = GLES32.glGetError();
        if (error != GLES32.GL_NO_ERROR) {
            Log.e(TAG, "OpenGL draw error: " + error);
        }

        // 清理
        GLES32.glDisableVertexAttribArray(positionHandle);
        GLES32.glDisableVertexAttribArray(texCoordHandle);
    }

    // 手动加载一个测试纹理
    private int loadTestTexture() {
        // 创建一个简单的测试纹理（2x2 红绿蓝白纹理）
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * 4 * 4); // 2x2 RGBA
        buffer.order(ByteOrder.nativeOrder());

        // 填充四个不同颜色的像素
        // 左下: 红色
        buffer.put((byte)255); buffer.put((byte)0); buffer.put((byte)0); buffer.put((byte)255);
        // 右下: 绿色
        buffer.put((byte)0); buffer.put((byte)255); buffer.put((byte)0); buffer.put((byte)255);
        // 左上: 蓝色
        buffer.put((byte)0); buffer.put((byte)0); buffer.put((byte)255); buffer.put((byte)255);
        // 右上: 白色
        buffer.put((byte)255); buffer.put((byte)255); buffer.put((byte)255); buffer.put((byte)255);

        buffer.position(0);

        int[] textureId = new int[1];
        GLES32.glGenTextures(1, textureId, 0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, textureId[0]);

        GLES32.glTexImage2D(
                GLES32.GL_TEXTURE_2D, 0, GLES32.GL_RGBA,
                2, 2, 0, GLES32.GL_RGBA,
                GLES32.GL_UNSIGNED_BYTE, buffer
        );

        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_NEAREST);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_NEAREST);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_CLAMP_TO_EDGE);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_CLAMP_TO_EDGE);

        Log.i(TAG, "Test texture created: " + textureId[0]);
        return textureId[0];
    }

    private void loadDebugShaders() {
        String vertexShaderSource = ShaderUtils.loadShader(context, R.raw.height_map_debug_vertex_shader);
        String fragmentShaderSource = ShaderUtils.loadShader(context, R.raw.height_map_debug_fragment_shader);

        debugProgram = GLES32.glCreateProgram();
        int vertexShader = compileShader(GLES32.GL_VERTEX_SHADER, vertexShaderSource);
        int fragmentShader = compileShader(GLES32.GL_FRAGMENT_SHADER, fragmentShaderSource);

        GLES32.glAttachShader(debugProgram, vertexShader);
        GLES32.glAttachShader(debugProgram, fragmentShader);
        GLES32.glLinkProgram(debugProgram);

        // 检查链接状态
        int[] linkStatus = new int[1];
        GLES32.glGetProgramiv(debugProgram, GLES32.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES32.GL_TRUE) {
            String error = GLES32.glGetProgramInfoLog(debugProgram);
            Log.e(TAG, "Shader link error: " + error);
            debugProgram = 0;
        } else {
            Log.i(TAG, "Debug shaders loaded successfully");
        }

        GLES32.glDeleteShader(vertexShader);
        GLES32.glDeleteShader(fragmentShader);
    }

    private int compileShader(int type, String source) {
        int shader = GLES32.glCreateShader(type);
        GLES32.glShaderSource(shader, source);
        GLES32.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES32.glGetShaderiv(shader, GLES32.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String error = GLES32.glGetShaderInfoLog(shader);
            Log.e(TAG, "Shader compilation error: " + error);
            GLES32.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }
}
