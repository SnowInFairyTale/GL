package com.example.gl;

// TerrainRenderer.java
import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class TerrainRenderer implements GLSurfaceView.Renderer {
    private Context context;
    private int program;
    private int positionHandle;
    private int colorHandle;
    private int normalHandle;
    private int mvpMatrixHandle;
    private int modelMatrixHandle;
    private int lightPositionHandle;

    private TerrainData.MeshData meshData;

    private float[] modelMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    private float[] mvpMatrix = new float[16];

    private float angle = 0;
    private float[] lightPosition = {50.0f, 50.0f, 50.0f};

    public TerrainRenderer(Context context) {
        this.context = context;
        meshData = TerrainData.generateTerrainMesh();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glClearColor(0.5f, 0.7f, 1.0f, 1.0f); // 天空蓝背景
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        // 暂时禁用背面剔除，便于调试
        // GLES30.glEnable(GLES30.GL_CULL_FACE);
        // GLES30.glCullFace(GLES30.GL_BACK);

        // 加载着色器
        String vertexShader = ShaderUtils.loadShader(context, R.raw.vertex_shader);
        String fragmentShader = ShaderUtils.loadShader(context, R.raw.fragment_shader);
        program = ShaderUtils.createProgram(vertexShader, fragmentShader);

        if (program == 0) {
            throw new RuntimeException("Failed to create shader program");
        }

        // 获取属性位置
        positionHandle = GLES30.glGetAttribLocation(program, "aPosition");
        colorHandle = GLES30.glGetAttribLocation(program, "aColor");
        normalHandle = GLES30.glGetAttribLocation(program, "aNormal");
        mvpMatrixHandle = GLES30.glGetUniformLocation(program, "uMVPMatrix");
        modelMatrixHandle = GLES30.glGetUniformLocation(program, "uModelMatrix");
        lightPositionHandle = GLES30.glGetUniformLocation(program, "uLightPosition");

        // 检查属性位置
        if (positionHandle == -1 || colorHandle == -1 || normalHandle == -1 ||
                mvpMatrixHandle == -1 || modelMatrixHandle == -1) {
            throw new RuntimeException("Failed to get shader attribute locations");
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES30.glViewport(0, 0, width, height);

        float ratio = (float) width / height;
        // 调整投影矩阵，让地形在视野内
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 5, 200);

        // 设置相机，从更高角度观看
        Matrix.setLookAtM(viewMatrix, 0,
                0, 40, 80,    // 相机位置 - 更高更远
                0, 0, 0,      // 观察点
                0, 1, 0       // 上向量
        );
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        // 更新模型矩阵（缓慢旋转）
        angle += 0.2f;
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, angle, 0, 1, 0);

        // 计算MVP矩阵
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);

        // 使用着色器程序
        GLES30.glUseProgram(program);

        // 传递矩阵和光照
        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES30.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0);
        GLES30.glUniform3f(lightPositionHandle, lightPosition[0], lightPosition[1], lightPosition[2]);

        // 传递顶点数据
        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 12, meshData.vertices);

        // 传递颜色数据
        GLES30.glEnableVertexAttribArray(colorHandle);
        GLES30.glVertexAttribPointer(colorHandle, 3, GLES30.GL_FLOAT, false, 12, meshData.colors);

        // 传递法线数据
        GLES30.glEnableVertexAttribArray(normalHandle);
        GLES30.glVertexAttribPointer(normalHandle, 3, GLES30.GL_FLOAT, false, 12, meshData.normals);

        // 绘制三角形
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, meshData.vertexCount);

        // 禁用顶点数组
        GLES30.glDisableVertexAttribArray(positionHandle);
        GLES30.glDisableVertexAttribArray(colorHandle);
        GLES30.glDisableVertexAttribArray(normalHandle);

        // 检查GL错误
        ShaderUtils.checkGLError("onDrawFrame");
    }
}