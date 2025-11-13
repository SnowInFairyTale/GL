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
    private FloatBuffer vertexBuffer;

    // 简单的全屏四边形顶点（使用vec4，包含w分量）
    private static final float[] VERTICES = {
            -1.0f, -1.0f, 0.0f, 1.0f,  // 左下
            1.0f, -1.0f, 0.0f, 1.0f,   // 右下
            -1.0f, 1.0f, 0.0f, 1.0f,   // 左上
            1.0f, 1.0f, 0.0f, 1.0f     // 右上
    };

    public HeightMapDebugRenderer(Context context) {
        this.context = context;

        // 初始化缓冲区
        vertexBuffer = ByteBuffer.allocateDirect(VERTICES.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(VERTICES).position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(TAG, "=== onSurfaceCreated ===");

        // 打印GL版本信息
        String version = GLES32.glGetString(GLES32.GL_VERSION);
        String renderer = GLES32.glGetString(GLES32.GL_RENDERER);
        Log.i(TAG, "OpenGL Version: " + version);
        Log.i(TAG, "Renderer: " + renderer);

        // 禁用深度测试
        GLES32.glDisable(GLES32.GL_DEPTH_TEST);
        GLES32.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // 加载最简单的着色器
        loadDebugShaders();

        // 检查OpenGL错误
        int error = GLES32.glGetError();
        if (error != GLES32.GL_NO_ERROR) {
            Log.e(TAG, "OpenGL error in onSurfaceCreated: " + error);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i(TAG, "onSurfaceChanged: " + width + "x" + height);
        GLES32.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // 清除颜色缓冲
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT);

        Log.d(TAG, "onDrawFrame - program: " + debugProgram);

        if (debugProgram == 0) {
            Log.e(TAG, "Program is invalid!");
            return;
        }

        GLES32.glUseProgram(debugProgram);

        // 只设置位置属性
        int positionHandle = GLES32.glGetAttribLocation(debugProgram, "aPosition");
        Log.d(TAG, "Position handle: " + positionHandle);

        if (positionHandle == -1) {
            Log.e(TAG, "aPosition attribute not found!");

            // 打印所有active attributes来调试
            printAllAttributes();
            return;
        }

        GLES32.glEnableVertexAttribArray(positionHandle);
        GLES32.glVertexAttribPointer(positionHandle, 4, GLES32.GL_FLOAT, false, 16, vertexBuffer);

        // 绘制全屏四边形
        GLES32.glDrawArrays(GLES32.GL_TRIANGLE_STRIP, 0, 4);

        // 检查绘制错误
        int error = GLES32.glGetError();
        if (error != GLES32.GL_NO_ERROR) {
            Log.e(TAG, "OpenGL draw error: " + error);
        }

        // 清理
        GLES32.glDisableVertexAttribArray(positionHandle);
    }

    private void loadDebugShaders() {
        try {
            // 绝对最简单的测试着色器
            String vertexShaderSource = ShaderUtils.loadShader(context,R.raw.height_map_debug_vertex_shader);

            String fragmentShaderSource = ShaderUtils.loadShader(context,R.raw.height_map_debug_fragment_shader);

            Log.d(TAG, "Compiling vertex shader...");
            int vertexShader = compileShader(GLES32.GL_VERTEX_SHADER, vertexShaderSource);
            if (vertexShader == 0) {
                Log.e(TAG, "Vertex shader compilation failed");
                debugProgram = 0;
                return;
            }

            Log.d(TAG, "Compiling fragment shader...");
            int fragmentShader = compileShader(GLES32.GL_FRAGMENT_SHADER, fragmentShaderSource);
            if (fragmentShader == 0) {
                Log.e(TAG, "Fragment shader compilation failed");
                GLES32.glDeleteShader(vertexShader);
                debugProgram = 0;
                return;
            }

            debugProgram = GLES32.glCreateProgram();
            GLES32.glAttachShader(debugProgram, vertexShader);
            GLES32.glAttachShader(debugProgram, fragmentShader);

            Log.d(TAG, "Linking program...");
            GLES32.glLinkProgram(debugProgram);

            // 检查链接状态
            int[] linkStatus = new int[1];
            GLES32.glGetProgramiv(debugProgram, GLES32.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES32.GL_TRUE) {
                String error = GLES32.glGetProgramInfoLog(debugProgram);
                Log.e(TAG, "Shader link error: " + error);
                debugProgram = 0;
            } else {
                Log.i(TAG, "Simple test shaders loaded successfully");

                // 打印程序信息
                printProgramInfo();
            }

            GLES32.glDeleteShader(vertexShader);
            GLES32.glDeleteShader(fragmentShader);
        } catch (Exception e) {
            Log.e(TAG, "Error loading shaders", e);
            debugProgram = 0;
        }
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

    private void printProgramInfo() {
        if (debugProgram == 0) return;

        int[] attribCount = new int[1];
        GLES32.glGetProgramiv(debugProgram, GLES32.GL_ACTIVE_ATTRIBUTES, attribCount, 0);
        Log.i(TAG, "Active attributes count: " + attribCount[0]);

        for (int i = 0; i < attribCount[0]; i++) {
            int[] length = new int[1];
            int[] size = new int[1];
            int[] type = new int[1];
            byte[] nameBytes = new byte[256];

            GLES32.glGetActiveAttrib(debugProgram, i, 256, length, 0, size, 0, type, 0, nameBytes, 0);
            String name = new String(nameBytes, 0, length[0]);
            int location = GLES32.glGetAttribLocation(debugProgram, name);

            Log.i(TAG, "Attribute [" + i + "]: " + name + " (location: " + location + ")");
        }
    }

    private void printAllAttributes() {
        if (debugProgram == 0) return;

        int[] attribCount = new int[1];
        GLES32.glGetProgramiv(debugProgram, GLES32.GL_ACTIVE_ATTRIBUTES, attribCount, 0);
        Log.i(TAG, "=== All Active Attributes ===");
        Log.i(TAG, "Total attributes: " + attribCount[0]);

        for (int i = 0; i < attribCount[0]; i++) {
            int[] length = new int[1];
            int[] size = new int[1];
            int[] type = new int[1];
            byte[] nameBytes = new byte[256];

            GLES32.glGetActiveAttrib(debugProgram, i, 256, length, 0, size, 0, type, 0, nameBytes, 0);
            String name = new String(nameBytes, 0, length[0]);
            int location = GLES32.glGetAttribLocation(debugProgram, name);

            Log.i(TAG, "  " + name + " -> location: " + location);
        }
    }
}
