package com.example.gl;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRenderer implements GLSurfaceView.Renderer {
    private Context context;
    private int program;
    private int wireframeProgram;
    private int positionHandle;
    private int colorHandle;
    private int normalHandle;
    private int mvpMatrixHandle;
    private int modelMatrixHandle;
    private int lightPositionHandle;
    private int cameraPositionHandle;
    private int typeHandle;
    private int minHeightHandle;
    private int maxHeightHandle;

    private TerrainData.MeshData meshData;

    private float[] modelMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    private float[] mvpMatrix = new float[16];

    private float angle = 0;
    private float[] lightPosition = {50.0f, 80.0f, 50.0f};
    private float[] cameraPosition = {0.0f, 40.0f, 80.0f};

    // 动画相关
    private long startTime;
    private float waterAnimation = 0.0f;

    // 新增：第一人称漫游控制
    private float[] fpvPosition = {0.0f, 5.0f, 0.0f}; // 第一人称位置
    private float fpvYaw = 0.0f;   // 偏航角（左右旋转）
    private float fpvPitch = 0.0f; // 俯仰角（上下看）
    private float moveSpeed = 5.0f; // 移动速度
    private float mouseSensitivity = 0.5f; // 触摸灵敏度

    // 控制状态
    private boolean isFirstPersonView = true;
    private boolean isAutoRotating = false; // 默认自动旋转

    // 触摸控制
    private float previousX, previousY;
    private boolean isRotating = false;

    // 移动控制
    private boolean moveForward = false;
    private boolean moveBackward = false;
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private boolean moveUp = false;
    private boolean moveDown = false;

    public enum RenderMode {
        SOLID,
        WIREFRAME
    }

    private RenderMode currentMode = RenderMode.SOLID;

    public GLRenderer(Context context) {
        this.context = context;
        meshData = TerrainData.generateTerrainMesh();
        startTime = System.currentTimeMillis();

        // 初始化位置在地形中心上方
        fpvPosition[0] = 0.0f;
        fpvPosition[1] = getTerrainHeight(0, 0) + 2.0f; // 站在地面上方2个单位
        fpvPosition[2] = 0.0f;
    }

    // 新增：获取地形高度（用于碰撞检测）
    private float getTerrainHeight(float worldX, float worldZ) {
        // 简化版本：返回基础高度
        // 在实际项目中，你应该查询高度图
        return (float) (Math.sin(worldX * 0.1) * Math.cos(worldZ * 0.1) * 3.0f +
                Math.sin(worldX * 0.05) * Math.cos(worldZ * 0.03) * 2.0f);
    }

    // 新增：切换视角模式
    public void toggleViewMode() {
        isFirstPersonView = !isFirstPersonView;
        isAutoRotating = !isFirstPersonView; // 第一人称时停止自动旋转
    }

    public String getCurrentViewMode() {
        return isFirstPersonView ? "第一人称漫游" : "上帝视角";
    }

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

    // 新增：触摸控制方法
    public void onTouchEvent(MotionEvent event) {
        if (!isFirstPersonView) {
            // 上帝视角的旋转控制
            handleGodViewTouch(event);
        } else {
            // 第一人称的视角控制
            handleFirstPersonTouch(event);
        }
    }

    private void handleGodViewTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                previousX = event.getX();
                previousY = event.getY();
                isRotating = true;
                isAutoRotating = false; // 手动控制时停止自动旋转
                break;

            case MotionEvent.ACTION_MOVE:
                if (isRotating) {
                    float deltaX = event.getX() - previousX;
                    float deltaY = event.getY() - previousY;

                    angle += deltaX * 0.5f;
                    previousX = event.getX();
                    previousY = event.getY();
                }
                break;

            case MotionEvent.ACTION_UP:
                isRotating = false;
                break;
        }
    }

    private void handleFirstPersonTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                previousX = event.getX();
                previousY = event.getY();
                isRotating = true;
                break;

            case MotionEvent.ACTION_MOVE:
                if (isRotating) {
                    float deltaX = event.getX() - previousX;
                    float deltaY = event.getY() - previousY;

                    // 更新视角
                    fpvYaw -= deltaX * mouseSensitivity;
                    fpvPitch -= deltaY * mouseSensitivity;

                    // 限制俯仰角范围
                    fpvPitch = Math.max(-89.0f, Math.min(89.0f, fpvPitch));

                    previousX = event.getX();
                    previousY = event.getY();
                }
                break;

            case MotionEvent.ACTION_UP:
                isRotating = false;
                break;
        }
    }

    // 新增：移动控制方法
    public void setMovement(boolean forward, boolean backward, boolean left, boolean right, boolean up, boolean down) {
        this.moveForward = forward;
        this.moveBackward = backward;
        this.moveLeft = left;
        this.moveRight = right;
        this.moveUp = up;
        this.moveDown = down;
    }

    // 新增：更新第一人称位置
    private void updateFirstPersonPosition(float deltaTime) {
        if (!isFirstPersonView) return;

        // 计算移动方向
        float moveX = 0, moveZ = 0, moveY = 0;

        if (moveForward) {
            moveX += (float) Math.sin(Math.toRadians(fpvYaw));
            moveZ += (float) Math.cos(Math.toRadians(fpvYaw));
        }
        if (moveBackward) {
            moveX -= (float) Math.sin(Math.toRadians(fpvYaw));
            moveZ -= (float) Math.cos(Math.toRadians(fpvYaw));
        }
        if (moveLeft) {
            moveX += (float) Math.sin(Math.toRadians(fpvYaw - 90));
            moveZ += (float) Math.cos(Math.toRadians(fpvYaw - 90));
        }
        if (moveRight) {
            moveX += (float) Math.sin(Math.toRadians(fpvYaw + 90));
            moveZ += (float) Math.cos(Math.toRadians(fpvYaw + 90));
        }
        if (moveUp) moveY += 1;
        if (moveDown) moveY -= 1;

        // 标准化移动向量
        float length = (float) Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (length > 0) {
            moveX /= length;
            moveZ /= length;
        }

        // 应用移动
        float speed = moveSpeed * deltaTime;
        fpvPosition[0] += moveX * speed;
        fpvPosition[2] += moveZ * speed;
        fpvPosition[1] += moveY * speed;

        // 简单的边界检查
        fpvPosition[0] = Math.max(-45f, Math.min(45f, fpvPosition[0]));
        fpvPosition[2] = Math.max(-45f, Math.min(45f, fpvPosition[2]));
        fpvPosition[1] = Math.max(1f, Math.min(50f, fpvPosition[1])); // 限制高度范围
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glClearColor(0.6f, 0.8f, 1.0f, 1.0f);
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

        String wireframeVertexShader = ShaderUtils.loadShader(context, R.raw.wireframe_vertex_shader);
        String wireframeFragmentShader = ShaderUtils.loadShader(context, R.raw.wireframe_fragment_shader);
        wireframeProgram = ShaderUtils.createProgram(wireframeVertexShader, wireframeFragmentShader);

        if (program == 0 || wireframeProgram == 0) {
            throw new RuntimeException("Failed to create shader program");
        }

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
        typeHandle = GLES30.glGetAttribLocation(program, "aType");
        minHeightHandle = GLES30.glGetUniformLocation(program, "minHeight");
        maxHeightHandle = GLES30.glGetUniformLocation(program, "maxHeight");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES30.glViewport(0, 0, width, height);

        float ratio = (float) width / height;
        // 使用透视投影，更真实的3D效果
//        Matrix.perspectiveM(projectionMatrix, 0, 45.0f, ratio, 0.1f, 300.0f); // 修改近平面为0.1
        Matrix.perspectiveM(projectionMatrix, 0, 45.0f, ratio, 1.0f, 300.0f);
    }

    private void updateCamera() {
        if (isFirstPersonView) {
            // 第一人称相机
            updateFirstPersonCamera();
        } else {
            // 上帝视角相机
            updateGodViewCamera();
        }
    }

    private void updateFirstPersonCamera() {
        // 计算相机方向
        float yawRad = (float) Math.toRadians(fpvYaw);
        float pitchRad = (float) Math.toRadians(fpvPitch);

        float lookX = (float) (Math.sin(yawRad) * Math.cos(pitchRad));
        float lookY = (float) Math.sin(pitchRad);
        float lookZ = (float) (Math.cos(yawRad) * Math.cos(pitchRad));

        float lookAtX = fpvPosition[0] + lookX;
        float lookAtY = fpvPosition[1] + lookY;
        float lookAtZ = fpvPosition[2] + lookZ;

        Matrix.setLookAtM(viewMatrix, 0,
                fpvPosition[0], fpvPosition[1], fpvPosition[2], // 相机位置
                lookAtX, lookAtY, lookAtZ,                      // 看向的点
                0, 1, 0                                         // 上向量
        );

        // 更新相机位置用于着色器
        cameraPosition[0] = fpvPosition[0];
        cameraPosition[1] = fpvPosition[1];
        cameraPosition[2] = fpvPosition[2];
    }

    private void updateGodViewCamera() {
        if (isAutoRotating) {
            angle += 0.3f; // 自动旋转速度
        }

        float radius = 80.0f;
        float camX = (float) (Math.sin(angle * 0.01f) * radius);
        float camZ = (float) (Math.cos(angle * 0.01f) * radius);
        cameraPosition[0] = camX;
        cameraPosition[1] = 40.0f;
        cameraPosition[2] = camZ;

        Matrix.setLookAtM(viewMatrix, 0,
                cameraPosition[0], cameraPosition[1], cameraPosition[2],
                0, 5, 0,
                0, 1, 0
        );
    }

    private long lastFrameTime = System.currentTimeMillis();

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        // 计算帧时间
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastFrameTime) * 0.001f;
        lastFrameTime = currentTime;

        // 更新时间动画
        waterAnimation += deltaTime;

        // 更新模型矩阵
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, angle, 0, 1, 0);

        // 添加轻微的上下浮动动画
        // Matrix.translateM(modelMatrix, 0, 0, (float)Math.sin(elapsedTime) * 0.5f, 0);

        // 更新第一人称位置
        updateFirstPersonPosition(deltaTime);

        // 更新相机
        updateCamera();

        // 计算MVP矩阵
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);

        // 渲染
        if (currentMode == RenderMode.SOLID) {
            renderSolid();
        } else {
            renderWireframe();
        }

        ShaderUtils.checkGLError("onDrawFrame");
    }

    private void renderSolid() {
        GLES30.glUseProgram(program);

        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES30.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0);
        GLES30.glUniform3f(lightPositionHandle, lightPosition[0], lightPosition[1], lightPosition[2]);
        GLES30.glUniform3f(cameraPositionHandle, cameraPosition[0], cameraPosition[1], cameraPosition[2]);
        GLES30.glUniform1f(minHeightHandle, meshData.minHeight);
        GLES30.glUniform1f(maxHeightHandle, meshData.maxHeight);

        // 传递时间动画uniform（如果需要）
        // int timeHandle = GLES30.glGetUniformLocation(program, "uTime");
        // GLES30.glUniform1f(timeHandle, waterAnimation);

        // 传递顶点数据
        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 12, meshData.vertices);

        GLES30.glEnableVertexAttribArray(colorHandle);
        GLES30.glVertexAttribPointer(colorHandle, 3, GLES30.GL_FLOAT, false, 12, meshData.colors);

        GLES30.glEnableVertexAttribArray(normalHandle);
        GLES30.glVertexAttribPointer(normalHandle, 3, GLES30.GL_FLOAT, false, 12, meshData.normals);

        GLES30.glEnableVertexAttribArray(typeHandle);
        GLES30.glVertexAttribIPointer(typeHandle, 1, GLES30.GL_INT, 4, meshData.types);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, meshData.vertexCount);

        GLES30.glDisableVertexAttribArray(positionHandle);
        GLES30.glDisableVertexAttribArray(colorHandle);
        GLES30.glDisableVertexAttribArray(normalHandle);
        GLES30.glDisableVertexAttribArray(typeHandle);
    }

    private void renderWireframe() {
        GLES30.glUseProgram(wireframeProgram);

        int wireframeMvpMatrixHandle = GLES30.glGetUniformLocation(wireframeProgram, "uMVPMatrix");
        int wireframeColorHandle = GLES30.glGetUniformLocation(wireframeProgram, "uColor");
        int wireframePositionHandle = GLES30.glGetAttribLocation(wireframeProgram, "aPosition");

        GLES30.glUniformMatrix4fv(wireframeMvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES30.glUniform3f(wireframeColorHandle, 0.0f, 1.0f, 0.0f);

        GLES30.glEnableVertexAttribArray(wireframePositionHandle);
        GLES30.glVertexAttribPointer(wireframePositionHandle, 3, GLES30.GL_FLOAT, false, 12, meshData.vertices);

        GLES30.glUniform3f(wireframeColorHandle, 1.0f, 0.0f, 0.0f);
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, meshData.vertexCount);

        GLES30.glUniform3f(wireframeColorHandle, 0.0f, 1.0f, 0.0f);
        GLES30.glLineWidth(2.0f);

        for (int i = 0; i < meshData.vertexCount; i += 3) {
            GLES30.glDrawArrays(GLES30.GL_LINE_LOOP, i, 3);
        }

        GLES30.glDisableVertexAttribArray(wireframePositionHandle);
    }
}