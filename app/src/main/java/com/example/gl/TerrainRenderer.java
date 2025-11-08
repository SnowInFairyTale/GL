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
    private int wireframeProgram; // 新增：线框模式着色器程序
    private int positionHandle;
    private int colorHandle;
    private int normalHandle;
    private int mvpMatrixHandle;
    private int modelMatrixHandle;
    private int lightPositionHandle;
    private int cameraPositionHandle; // 新增相机位置uniform

    private TerrainData.MeshData meshData;

    private float[] modelMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    private float[] mvpMatrix = new float[16];

    private float angle = 0;
    private float[] lightPosition = {50.0f, 80.0f, 50.0f}; // 提高光源位置
    private float[] cameraPosition = {0.0f, 40.0f, 80.0f}; // 相机位置

    // 动画相关
    private long startTime;
    private float waterAnimation = 0.0f;

    // 新增：渲染模式控制
    public enum RenderMode {
        SOLID,      // 实体模式
        WIREFRAME   // 线框模式
    }

    private RenderMode currentMode = RenderMode.SOLID;

    public TerrainRenderer(Context context) {
        this.context = context;
        meshData = TerrainData.generateTerrainMesh();
        startTime = System.currentTimeMillis();
    }

    // 新增：切换渲染模式的方法
    public void toggleRenderMode() {
        if (currentMode == RenderMode.SOLID) {
            currentMode = RenderMode.WIREFRAME;
        } else {
            currentMode = RenderMode.SOLID;
        }
    }

    public String getCurrentModeName() {
        return currentMode == RenderMode.SOLID ? "3D实体模式" : "骨架线框模式";
    }

    public RenderMode getCurrentMode() {
        return currentMode;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glClearColor(0.6f, 0.8f, 1.0f, 1.0f); // 更亮的天空蓝
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);

        // 启用混合用于透明效果（如果需要）
        // GLES30.glEnable(GLES30.GL_BLEND);
        // GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // 启用背面剔除提高性能
//        GLES30.glEnable(GLES30.GL_CULL_FACE);
//        GLES30.glCullFace(GLES30.GL_BACK);

//        GLES30.glEnable(GLES30.GL_CULL_FACE);
//        GLES30.glCullFace(GLES30.GL_FRONT); // 剔除正面而不是背面

        // 加载着色器
        String vertexShader = ShaderUtils.loadShader(context, R.raw.vertex_shader);
        String fragmentShader = ShaderUtils.loadShader(context, R.raw.fragment_shader);
        program = ShaderUtils.createProgram(vertexShader, fragmentShader);

        // 加载线框模式着色器
        String wireframeVertexShader = ShaderUtils.loadShader(context, R.raw.wireframe_vertex_shader);
        String wireframeFragmentShader = ShaderUtils.loadShader(context, R.raw.wireframe_fragment_shader);
        wireframeProgram = ShaderUtils.createProgram(wireframeVertexShader, wireframeFragmentShader);

        if (program == 0 || wireframeProgram == 0) {
            throw new RuntimeException("Failed to create shader program");
        }

        // 获取实体模式着色器的属性位置
        setupSolidShaderAttributes();
    }

    private void setupSolidShaderAttributes() {
        positionHandle = GLES30.glGetAttribLocation(program, "aPosition");
        colorHandle = GLES30.glGetAttribLocation(program, "aColor");
        normalHandle = GLES30.glGetAttribLocation(program, "aNormal");
        mvpMatrixHandle = GLES30.glGetUniformLocation(program, "uMVPMatrix");
        modelMatrixHandle = GLES30.glGetUniformLocation(program, "uModelMatrix");
        lightPositionHandle = GLES30.glGetUniformLocation(program, "uLightPosition");
        cameraPositionHandle = GLES30.glGetUniformLocation(program, "uCameraPosition");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES30.glViewport(0, 0, width, height);

        float ratio = (float) width / height;
        // 使用透视投影，更真实的3D效果
        Matrix.perspectiveM(projectionMatrix, 0, 45.0f, ratio, 1.0f, 300.0f);

        // 更新相机位置
        updateCameraPosition();
    }

    private void updateCameraPosition() {
        // 围绕场景旋转的相机
        float radius = 80.0f;
        float camX = (float) (Math.sin(angle * 0.01f) * radius);
        float camZ = (float) (Math.cos(angle * 0.01f) * radius);
        cameraPosition[0] = camX;
        cameraPosition[1] = 40.0f; // 固定高度
        cameraPosition[2] = camZ;

        Matrix.setLookAtM(viewMatrix, 0,
                cameraPosition[0], cameraPosition[1], cameraPosition[2], // 相机位置
                0, 5, 0,      // 看向场景中心稍高的位置
                0, 1, 0       // 上向量
        );
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        // 更新时间动画
        long currentTime = System.currentTimeMillis();
        float elapsedTime = (currentTime - startTime) * 0.001f;
        waterAnimation = elapsedTime;

        // 更新模型矩阵（缓慢旋转）
        angle += 0.5f; // 更慢的旋转
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, angle, 0, 1, 0);

        // 添加轻微的上下浮动动画
        // Matrix.translateM(modelMatrix, 0, 0, (float)Math.sin(elapsedTime) * 0.5f, 0);

        // 更新相机位置
        updateCameraPosition();

        // 计算MVP矩阵
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);

        // 根据当前模式选择渲染方式
        if (currentMode == RenderMode.SOLID) {
            renderSolid();
        } else {
            renderWireframe();
        }

        ShaderUtils.checkGLError("onDrawFrame");
    }

    private void renderSolid() {
        // 使用实体模式着色器
        GLES30.glUseProgram(program);

        // 传递矩阵和uniform
        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES30.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0);
        GLES30.glUniform3f(lightPositionHandle, lightPosition[0], lightPosition[1], lightPosition[2]);
        GLES30.glUniform3f(cameraPositionHandle, cameraPosition[0], cameraPosition[1], cameraPosition[2]);

        // 传递时间动画uniform（如果需要）
        // int timeHandle = GLES30.glGetUniformLocation(program, "uTime");
        // GLES30.glUniform1f(timeHandle, waterAnimation);

        // 传递顶点数据
        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 12, meshData.vertices);

        // 传递颜色数据
        GLES30.glEnableVertexAttribArray(colorHandle);
        GLES30.glVertexAttribPointer(colorHandle, 3, GLES30.GL_FLOAT, false, 12, meshData.colors);

        // 传递法线数据
        GLES30.glEnableVertexAttribArray(normalHandle);
        GLES30.glVertexAttribPointer(normalHandle, 3, GLES30.GL_FLOAT, false, 12, meshData.normals);

        // 绘制地形
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, meshData.vertexCount);

        // 禁用顶点数组
        GLES30.glDisableVertexAttribArray(positionHandle);
        GLES30.glDisableVertexAttribArray(colorHandle);
        GLES30.glDisableVertexAttribArray(normalHandle);
    }

    private void renderWireframe() {
        // 使用线框模式着色器
        GLES30.glUseProgram(wireframeProgram);

        // 获取线框着色器的uniform位置
        int wireframeMvpMatrixHandle = GLES30.glGetUniformLocation(wireframeProgram, "uMVPMatrix");
        int wireframeColorHandle = GLES30.glGetUniformLocation(wireframeProgram, "uColor");

        // 获取线框着色器的attribute位置
        int wireframePositionHandle = GLES30.glGetAttribLocation(wireframeProgram, "aPosition");

        // 传递MVP矩阵
        GLES30.glUniformMatrix4fv(wireframeMvpMatrixHandle, 1, false, mvpMatrix, 0);

        // 设置线框颜色（亮绿色，便于观察）
        GLES30.glUniform3f(wireframeColorHandle, 0.0f, 1.0f, 0.0f);

        // 传递顶点数据
        GLES30.glEnableVertexAttribArray(wireframePositionHandle);
        GLES30.glVertexAttribPointer(wireframePositionHandle, 3, GLES30.GL_FLOAT, false, 12, meshData.vertices);

        // 首先绘制点（红色）
        GLES30.glUniform3f(wireframeColorHandle, 1.0f, 0.0f, 0.0f);
        // 移除：GLES30.glPointSize(6.0f); // 这行不再需要
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, meshData.vertexCount);

        // 然后绘制线框（绿色）
        GLES30.glUniform3f(wireframeColorHandle, 0.0f, 1.0f, 0.0f);
        GLES30.glLineWidth(2.0f);

        // 注意：GL_LINES 需要特殊的顶点数据，这里我们用三角形来模拟线框
        // 为了正确显示线框，我们需要修改绘制方式
        for (int i = 0; i < meshData.vertexCount; i += 3) {
            // 绘制三角形的三条边
            GLES30.glDrawArrays(GLES30.GL_LINE_LOOP, i, 3);
        }

        // 禁用顶点数组
        GLES30.glDisableVertexAttribArray(wireframePositionHandle);
    }
}