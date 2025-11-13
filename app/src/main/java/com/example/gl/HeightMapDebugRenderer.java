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
            -1.0f,  1.0f, 0.0f,  // 左上
            1.0f,  1.0f, 0.0f   // 右上
    };

    private static final float[] TEX_COORDS = {
            0.0f, 0.0f,  // 左下
            1.0f, 0.0f,  // 右下
            0.0f, 1.0f,  // 左上
            1.0f, 1.0f   // 右上
    };

    public HeightMapDebugRenderer(Context context, int heightMapTextureId) {
        this.context = context;
        int roofImageTextureId = GLTools.loadTexture(context, R.drawable.roof_texture);
        this.heightMapTextureId = roofImageTextureId;

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
        GLES32.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        loadDebugShaders();

        Log.i(TAG, "Height map texture ID: " + heightMapTextureId);

        // 检查纹理是否存在
        if (heightMapTextureId == -1) {
            Log.e(TAG, "Height map texture is invalid!");
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES32.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT | GLES32.GL_DEPTH_BUFFER_BIT);

        if (debugProgram == 0 || heightMapTextureId == -1) {
            Log.e(TAG, "Cannot render: program=" + debugProgram + ", texture=" + heightMapTextureId);
            return;
        }

        GLES32.glUseProgram(debugProgram);

        // 设置顶点属性
        int positionHandle = GLES32.glGetAttribLocation(debugProgram, "aPosition");
        GLES32.glEnableVertexAttribArray(positionHandle);
        GLES32.glVertexAttribPointer(positionHandle, 3, GLES32.GL_FLOAT, false, 12, vertexBuffer);

        int texCoordHandle = GLES32.glGetAttribLocation(debugProgram, "aTexCoord");
        GLES32.glEnableVertexAttribArray(texCoordHandle);
        GLES32.glVertexAttribPointer(texCoordHandle, 2, GLES32.GL_FLOAT, false, 8, texCoordBuffer);

        // 绑定高度图纹理
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, heightMapTextureId);

        int textureHandle = GLES32.glGetUniformLocation(debugProgram, "uHeightMap");
        GLES32.glUniform1i(textureHandle, 0);

        // 绘制全屏四边形
        GLES32.glDrawArrays(GLES32.GL_TRIANGLE_STRIP, 0, 4);

        // 清理
        GLES32.glDisableVertexAttribArray(positionHandle);
        GLES32.glDisableVertexAttribArray(texCoordHandle);
    }

    private void loadDebugShaders() {
        String vertexShaderSource =
                "#version 300 es\n" +
                        "layout(location = 0) in vec3 aPosition;\n" +
                        "layout(location = 1) in vec2 aTexCoord;\n" +
                        "out vec2 vTexCoord;\n" +
                        "void main() {\n" +
                        "    gl_Position = vec4(aPosition, 1.0);\n" +
                        "    vTexCoord = aTexCoord;\n" +
                        "}";

        String fragmentShaderSource =
                "#version 300 es\n" +
                        "precision mediump float;\n" +
                        "in vec2 vTexCoord;\n" +
                        "uniform sampler2D uHeightMap;\n" +
                        "out vec4 fragColor;\n" +
                        "void main() {\n" +
                        "    float height = texture(uHeightMap, vTexCoord).r;\n" +
                        "    \n" +
                        "    // 调试输出：显示原始高度值\n" +
                        "    fragColor = vec4(height, height, height, 1.0);\n" +
                        "    \n" +
                        "    // 或者使用彩色显示：\n" +
                        "    // if (height < 0.3) {\n" +
                        "    //     fragColor = vec4(0.0, 0.0, 1.0, 1.0); // 蓝色\n" +
                        "    // } else if (height < 0.6) {\n" +
                        "    //     fragColor = vec4(0.0, 1.0, 0.0, 1.0); // 绿色\n" +
                        "    // } else {\n" +
                        "    //     fragColor = vec4(1.0, 1.0, 1.0, 1.0); // 白色\n" +
                        "    // }\n" +
                        "}";

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
