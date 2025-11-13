package com.example.gl;

import android.content.Context;
import android.opengl.GLES30;
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
    private FloatBuffer texCoordBuffer;


    private int positionHandle;
    private int texCoordHandle;
    private int roofTextureHandle;

    private int roofTextureId;

    // 简单的全屏四边形顶点（使用vec4，包含w分量）
    private static final float[] VERTICES = {
            -1.0f, -1.0f, 0.0f,  // 左下
            1.0f, -1.0f, 0.0f,   // 右下
            -1.0f, 1.0f, 0.0f,   // 左上
            1.0f, 1.0f, 0.0f     // 右上
    };

    private static final float[] TEX_COORDS = {
            0.0f, 0.0f,  // 左下
            1.0f, 0.0f,  // 右下
            0.0f, 1.0f,  // 左上
            1.0f, 1.0f   // 右上
    };

    public HeightMapDebugRenderer(Context context) {
        this.context = context;

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
        Log.i(TAG, "=== onSurfaceCreated ===");

        // 禁用深度测试
        GLES30.glClearColor(0.6f, 0.8f, 1.0f, 1.0f);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);

        // 加载着色器
        String vertexShader = ShaderUtils.loadShader(context, R.raw.height_map_debug_vertex_shader);
        String fragmentShader = ShaderUtils.loadShader(context, R.raw.height_map_debug_fragment_shader);
        debugProgram = ShaderUtils.createProgram(vertexShader, fragmentShader);

        // 检查OpenGL错误
        int error = GLES30.glGetError();
        if (error != GLES30.GL_NO_ERROR) {
            Log.e(TAG, "OpenGL error in onSurfaceCreated: " + error);
        }

        loadTextures();
        setupShaderAttributes();
    }

    private void setupShaderAttributes() {
        positionHandle = GLES30.glGetAttribLocation(debugProgram, "aPosition");
        // 纹理相关属性
        texCoordHandle = GLES30.glGetAttribLocation(debugProgram, "aTexCoord");
        roofTextureHandle = GLES30.glGetUniformLocation(debugProgram, "uRoofTexture");

        // 检查是否获取成功
        if (texCoordHandle == -1) Log.e(TAG, "aTexCoord attribute not found");
        if (roofTextureHandle == -1) Log.e(TAG, "uRoofTexture uniform not found");
    }

    private void loadTextures() {
        // 加载屋顶纹理
        roofTextureId = GLTools.loadTexture(context, R.drawable.wall_texture);
//        roofTextureId = TerrainDataV2.generateHeightMapTexture();

        // 如果纹理加载失败，使用默认颜色
        if (roofTextureId == 0) {
            Log.e(TAG, "Failed to load textures, using default colors");
        } else {
            Log.i(TAG, "Textures loaded successfully");
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i(TAG, "onSurfaceChanged: " + width + "x" + height);
        GLES30.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
//        Log.i(TAG, "onDrawFrame");
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        GLES30.glUseProgram(debugProgram);

        if (roofTextureHandle != -1 && roofTextureId != 0) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, roofTextureId);
            GLES30.glUniform1i(roofTextureHandle, 1);
        }

        if (positionHandle != -1) {
            GLES30.glEnableVertexAttribArray(positionHandle);
//            GLES30.glVertexAttribPointer(positionHandle, 4, GLES30.GL_FLOAT, false, 16, vertexBuffer);
            GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 12, vertexBuffer);
        }

        if (texCoordHandle != -1) {
            GLES30.glEnableVertexAttribArray(texCoordHandle);
            GLES30.glVertexAttribPointer(texCoordHandle, 2, GLES30.GL_FLOAT, false, 8, texCoordBuffer);
        }

        // 绘制全屏四边形
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        // 检查绘制错误
        int error = GLES30.glGetError();
        if (error != GLES30.GL_NO_ERROR) {
            Log.e(TAG, "OpenGL draw error: " + error);
        }

        // 清理
        GLES30.glDisableVertexAttribArray(positionHandle);
    }

    private void printAllAttributes() {
        if (debugProgram == 0) return;

        int[] attribCount = new int[1];
        GLES30.glGetProgramiv(debugProgram, GLES30.GL_ACTIVE_ATTRIBUTES, attribCount, 0);
        Log.i(TAG, "=== All Active Attributes ===");
        Log.i(TAG, "Total attributes: " + attribCount[0]);

        for (int i = 0; i < attribCount[0]; i++) {
            int[] length = new int[1];
            int[] size = new int[1];
            int[] type = new int[1];
            byte[] nameBytes = new byte[256];

            GLES30.glGetActiveAttrib(debugProgram, i, 256, length, 0, size, 0, type, 0, nameBytes, 0);
            String name = new String(nameBytes, 0, length[0]);
            int location = GLES30.glGetAttribLocation(debugProgram, name);

            Log.i(TAG, "  " + name + " -> location: " + location);
        }
    }
}
