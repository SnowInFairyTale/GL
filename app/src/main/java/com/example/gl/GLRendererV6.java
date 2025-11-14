package com.example.gl;

import android.content.Context;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRendererV6 implements GLSurfaceView.Renderer {
    private static final String TAG = "GLRendererV6";
    private final Context context;

    // 着色器程序
    private int tessellationProgram;

    // 曲面细分着色器属性
    private int tessMvpMatrixHandle;
    private int tessModelMatrixHandle;
    private int tessTessLevelHandle;
    private int tessHeightMapHandle;
    private int tessTerrainSizeHandle;
    private int tessCameraPositionHandle;
    private int tessLightPositionHandle;

    // 纹理
    private int heightMapTextureId;

    private final TerrainDataV2.MeshData meshData;

    // 矩阵
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // 相机和光照
    private float angle = 0;
    private final float[] lightPosition = {50.0f, 80.0f, 50.0f};
    private final float[] cameraPosition = {0.0f, 40.0f, 80.0f};

    // 第一人称控制（保持不变）
    private float[] fpvPosition = {0.0f, 5.0f, 0.0f};
    private float fpvYaw = 0.0f;
    private float fpvPitch = 0.0f;
    private float moveSpeed = 5.0f;
    private float mouseSensitivity = 0.5f;

    // 控制状态（保持不变）
    private boolean isFirstPersonView = false;
    private boolean isAutoRotating = true;
    private boolean isRotating = false;
    private float previousX, previousY;

    // 移动控制（保持不变）
    private boolean moveForward = false;
    private boolean moveBackward = false;
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private boolean moveUp = false;
    private boolean moveDown = false;

    private long lastFrameTime = System.currentTimeMillis();

    // 性能监控
    private int frameCount = 0;
    private long lastFpsTime = 0;
    private float fps = 0;

    public GLRendererV6(Context context) {
        this.context = context;
        // 配置高级地形特性
        configureAdvancedFeatures();

        // 生成地形网格
        meshData = TerrainDataV2.generateTerrainMesh();

        // 初始化位置
        fpvPosition[0] = 0.0f;
        fpvPosition[1] = getTerrainHeight(0, 0) + 2.0f;
        fpvPosition[2] = 0.0f;

        lastFpsTime = System.currentTimeMillis();
    }

    private void configureAdvancedFeatures() {
        // 根据设备能力配置特性
        if (GLSupportChecker.supportsTessellation()) {
            TerrainDataV2.setEnableTessellation(true);
            TerrainDataV2.setTessellationLevel(6); // 中等细分级别
            Log.i(TAG, "Tessellation enabled with level: " + TerrainDataV2.getTessellationLevel());
        } else {
            TerrainDataV2.setEnableTessellation(false);
            Log.i(TAG, "Tessellation not supported, using standard rendering");
        }

        TerrainDataV2.setUseInterpolation(true);
        TerrainDataV2.setEnableNormalMapping(true);
    }

    private float getTerrainHeight(float worldX, float worldZ) {
        return (float) (Math.sin(worldX * 0.1) * Math.cos(worldZ * 0.1) * 3.0f +
                Math.sin(worldX * 0.05) * Math.cos(worldZ * 0.03) * 2.0f);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(TAG, "Surface created with OpenGL ES 3.2 support");

        GLES32.glClearColor(0.1f, 0.1f, 0.1f, 1.0f); // 深色背景便于观察线框
        GLES32.glEnable(GLES32.GL_DEPTH_TEST);

        // 加载着色器
        loadTessellationShaders();

        // 生成高度图纹理
        heightMapTextureId = GLTools.loadTexture(context, R.drawable.sz);
        Log.i(TAG, "Height map texture loaded: " + heightMapTextureId);
    }

    private void loadTessellationShaders() {
        try {
            String tessVertexShader = ShaderUtils.loadShader(context, R.raw.tess_vertex_shader);
            String tessControlShader = ShaderUtils.loadShader(context, R.raw.tess_control_shader);
            String tessEvalShader = ShaderUtils.loadShader(context, R.raw.tess_evaluation_shader);
            String tessFragmentShader = ShaderUtils.loadShader(context, R.raw.tess_fragment_shader);

            tessellationProgram = GLES32.glCreateProgram();

            attachShader(tessellationProgram, GLES32.GL_VERTEX_SHADER, tessVertexShader);
            attachShader(tessellationProgram, GLES32.GL_TESS_CONTROL_SHADER, tessControlShader);
            attachShader(tessellationProgram, GLES32.GL_TESS_EVALUATION_SHADER, tessEvalShader);
            attachShader(tessellationProgram, GLES32.GL_FRAGMENT_SHADER, tessFragmentShader);

            GLES32.glLinkProgram(tessellationProgram);

            int[] linkStatus = new int[1];
            GLES32.glGetProgramiv(tessellationProgram, GLES32.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES32.GL_TRUE) {
                String error = GLES32.glGetProgramInfoLog(tessellationProgram);
                Log.e(TAG, "Tessellation shader link error: " + error);
                tessellationProgram = 0;
            } else {
                tessMvpMatrixHandle = GLES32.glGetUniformLocation(tessellationProgram, "uMVPMatrix");
                tessModelMatrixHandle = GLES32.glGetUniformLocation(tessellationProgram, "uModelMatrix");
                tessTessLevelHandle = GLES32.glGetUniformLocation(tessellationProgram, "uTessLevel");
                tessHeightMapHandle = GLES32.glGetUniformLocation(tessellationProgram, "uHeightMap");
                tessTerrainSizeHandle = GLES32.glGetUniformLocation(tessellationProgram, "uTerrainSize");
                tessCameraPositionHandle = GLES32.glGetUniformLocation(tessellationProgram, "uCameraPosition");
                tessLightPositionHandle = GLES32.glGetUniformLocation(tessellationProgram, "uLightPosition");
                Log.i(TAG, "Tessellation shaders loaded successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading tessellation shaders", e);
            tessellationProgram = 0;
        }
    }

    private void attachShader(int program, int type, String source) {
        int shader = GLES32.glCreateShader(type);
        GLES32.glShaderSource(shader, source);
        GLES32.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES32.glGetShaderiv(shader, GLES32.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String error = GLES32.glGetShaderInfoLog(shader);
            Log.e(TAG, "Shader compilation error: " + error);
            GLES32.glDeleteShader(shader);
            return;
        }

        GLES32.glAttachShader(program, shader);
        GLES32.glDeleteShader(shader);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES32.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.perspectiveM(projectionMatrix, 0, 45.0f, ratio, 0.1f, 300.0f);
    }

    // 相机控制方法（保持不变）
    private void updateCamera() {
        if (isFirstPersonView) {
            updateFirstPersonCamera();
        } else {
            updateGodViewCamera();
        }
    }

    private void updateFirstPersonCamera() {
        float yawRad = (float) Math.toRadians(fpvYaw);
        float pitchRad = (float) Math.toRadians(fpvPitch);

        float lookX = (float) (Math.sin(yawRad) * Math.cos(pitchRad));
        float lookY = (float) Math.sin(pitchRad);
        float lookZ = (float) (Math.cos(yawRad) * Math.cos(pitchRad));

        float lookAtX = fpvPosition[0] + lookX;
        float lookAtY = fpvPosition[1] + lookY;
        float lookAtZ = fpvPosition[2] + lookZ;

        Matrix.setLookAtM(viewMatrix, 0,
                fpvPosition[0], fpvPosition[1], fpvPosition[2],
                lookAtX, lookAtY, lookAtZ,
                0, 1, 0
        );

        cameraPosition[0] = fpvPosition[0];
        cameraPosition[1] = fpvPosition[1];
        cameraPosition[2] = fpvPosition[2];
    }

    private void updateGodViewCamera() {
        if (isAutoRotating) {
            angle += 0.3f;
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

    @Override
    public void onDrawFrame(GL10 gl) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastFrameTime) * 0.001f;
        lastFrameTime = currentTime;

        updateFPS();
        updateFirstPersonPosition(deltaTime);
        updateCamera();

        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT | GLES32.GL_DEPTH_BUFFER_BIT);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, angle, 0, 1, 0);

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);

        renderWithTessellation();

        ShaderUtils.checkGLError("onDrawFrame");
    }

    private void updateFPS() {
        frameCount++;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFpsTime >= 1000) {
            fps = frameCount * 1000.0f / (currentTime - lastFpsTime);
            frameCount = 0;
            lastFpsTime = currentTime;
        }
    }

    // 第一人称移动（保持不变）
    private void updateFirstPersonPosition(float deltaTime) {
        if (!isFirstPersonView) return;

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

        float length = (float) Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (length > 0) {
            moveX /= length;
            moveZ /= length;
        }

        float speed = moveSpeed * deltaTime;
        fpvPosition[0] += moveX * speed;
        fpvPosition[2] += moveZ * speed;
        fpvPosition[1] += moveY * speed;

        fpvPosition[0] = Math.max(-45f, Math.min(45f, fpvPosition[0]));
        fpvPosition[2] = Math.max(-45f, Math.min(45f, fpvPosition[2]));
        fpvPosition[1] = Math.max(1f, Math.min(50f, fpvPosition[1]));
    }

    // 渲染曲面细分 - 使用高度图纹理
    private void renderWithTessellation() {
        if (tessellationProgram == 0) {
            Log.e(TAG, "Tessellation program not loaded");
            return;
        }

        if (heightMapTextureId == -1) {
            Log.e(TAG, "Height map texture not loaded");
            return;
        }

        GLES32.glUseProgram(tessellationProgram);

        // 设置uniforms
        GLES32.glUniformMatrix4fv(tessMvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES32.glUniformMatrix4fv(tessModelMatrixHandle, 1, false, modelMatrix, 0);
        GLES32.glUniform1f(tessTessLevelHandle, TerrainDataV2.getTessellationLevel()); // 增加细分级别
        GLES32.glUniform1f(tessTerrainSizeHandle, TerrainDataV2.TERRAIN_SIZE);
        GLES32.glUniform3f(tessCameraPositionHandle, cameraPosition[0], cameraPosition[1], cameraPosition[2]);
        GLES32.glUniform3f(tessLightPositionHandle, lightPosition[0], lightPosition[1], lightPosition[2]);

        // 绑定高度图纹理
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, heightMapTextureId);
        GLES32.glUniform1i(tessHeightMapHandle, 0);

        // 设置顶点数据
        int tessPositionHandle = GLES32.glGetAttribLocation(tessellationProgram, "aPosition");
        GLES32.glEnableVertexAttribArray(tessPositionHandle);
        meshData.vertices.position(0); // 重置buffer位置
        GLES32.glVertexAttribPointer(tessPositionHandle, 3, GLES32.GL_FLOAT, false, 12, meshData.vertices);

        // 使用曲面细分绘制
        GLES32.glPatchParameteri(GLES32.GL_PATCH_VERTICES, 3);
        GLES32.glDrawArrays(GLES32.GL_PATCHES, 0, meshData.vertexCount);

        GLES32.glDisableVertexAttribArray(tessPositionHandle);
    }

    public String getCurrentViewMode() {
        return isFirstPersonView ? "第一人称漫游" : "上帝视角";
    }

    protected boolean isFirstPersonView() {
        return isFirstPersonView;
    }

    // 触摸控制（保持不变）
    public void onTouchEvent(MotionEvent event) {
        if (!isFirstPersonView) {
            handleGodViewTouch(event);
        } else {
            handleFirstPersonTouch(event);
        }
    }

    private void handleGodViewTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                previousX = event.getX();
                previousY = event.getY();
                isRotating = true;
                isAutoRotating = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (isRotating) {
                    float deltaX = event.getX() - previousX;
                    angle += deltaX * 0.5f;
                    previousX = event.getX();
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
                    fpvYaw -= deltaX * mouseSensitivity;
                    fpvPitch -= deltaY * mouseSensitivity;
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

    public void setMovement(boolean forward, boolean backward, boolean left, boolean right, boolean up, boolean down) {
        this.moveForward = forward;
        this.moveBackward = backward;
        this.moveLeft = left;
        this.moveRight = right;
        this.moveUp = up;
        this.moveDown = down;
    }

    public void cleanup() {
        if (tessellationProgram != 0) {
            GLES32.glDeleteProgram(tessellationProgram);
        }
        if (heightMapTextureId != -1) {
            int[] textures = {heightMapTextureId};
            GLES32.glDeleteTextures(1, textures, 0);
        }
        Log.i(TAG, "GLRendererV2 resources cleaned up");
    }

    public void toggleViewMode() {
    }

    public void toggleRenderMode() {
    }

    public String getCurrentModeName() {
    }
}
