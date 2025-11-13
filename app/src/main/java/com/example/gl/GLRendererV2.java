package com.example.gl;

import android.content.Context;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRendererV2 implements GLSurfaceView.Renderer {
    private static final String TAG = "GLRendererV2";
    private Context context;

    // 着色器程序
    private int standardProgram;
    private int tessellationProgram;
    private int wireframeProgram;
    private int currentProgram;

    // 标准着色器属性
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
    private int texCoordHandle;
    private int useTextureHandle;

    // 曲面细分着色器属性
    private int tessMvpMatrixHandle;
    private int tessModelMatrixHandle;
    private int tessTessLevelHandle;
    private int tessHeightMapHandle;
    private int tessTerrainSizeHandle;
    private int tessCameraPositionHandle;
    private int tessLightPositionHandle;

    // 纹理
    private int wallTextureId;
    private int roofTextureId;
    private int heightMapTextureId;

    // 网格数据
    private TerrainDataV2.MeshData meshData;

    // 矩阵
    private float[] modelMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    private float[] mvpMatrix = new float[16];

    // 相机和光照
    private float angle = 0;
    private float[] lightPosition = {50.0f, 80.0f, 50.0f};
    private float[] cameraPosition = {0.0f, 40.0f, 80.0f};

    // 第一人称控制
    private float[] fpvPosition = {0.0f, 5.0f, 0.0f};
    private float fpvYaw = 0.0f;
    private float fpvPitch = 0.0f;
    private float moveSpeed = 5.0f;
    private float mouseSensitivity = 0.5f;

    // 控制状态
    private boolean isFirstPersonView = false;
    private boolean isAutoRotating = true;
    private boolean isRotating = false;
    private float previousX, previousY;

    // 移动控制
    private boolean moveForward = false;
    private boolean moveBackward = false;
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private boolean moveUp = false;
    private boolean moveDown = false;

    // 渲染模式
    public enum RenderMode {
        SOLID,
        WIREFRAME,
        TESSELLATION
    }

    private RenderMode currentMode = RenderMode.TESSELLATION;
    private long lastFrameTime = System.currentTimeMillis();

    // 性能监控
    private int frameCount = 0;
    private long lastFpsTime = 0;
    private float fps = 0;

    public GLRendererV2(Context context) {
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

    public void toggleViewMode() {
        isFirstPersonView = !isFirstPersonView;
        isAutoRotating = !isFirstPersonView;
    }

    public String getCurrentViewMode() {
        return isFirstPersonView ? "第一人称漫游" : "上帝视角";
    }

    protected boolean isFirstPersonView() {
        return isFirstPersonView;
    }

    public void toggleRenderMode() {
        switch (currentMode) {
            case SOLID:
                if (GLSupportChecker.supportsTessellation() && TerrainDataV2.isTessellationEnabled()) {
                    currentMode = RenderMode.TESSELLATION;
                } else {
                    currentMode = RenderMode.WIREFRAME;
                }
                break;
            case WIREFRAME:
                currentMode = RenderMode.SOLID;
                break;
            case TESSELLATION:
                currentMode = RenderMode.SOLID;
                break;
        }
    }

    public String getCurrentModeName() {
        switch (currentMode) {
            case SOLID:
                return "3D实体模式";
            case WIREFRAME:
                return "骨架线框模式";
            case TESSELLATION:
                return "曲面细分模式";
            default:
                return "未知模式";
        }
    }

    public RenderMode getCurrentMode() {
        return currentMode;
    }

    // 触摸控制
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

        // 边界检查
        fpvPosition[0] = Math.max(-45f, Math.min(45f, fpvPosition[0]));
        fpvPosition[2] = Math.max(-45f, Math.min(45f, fpvPosition[2]));
        fpvPosition[1] = Math.max(1f, Math.min(50f, fpvPosition[1]));
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(TAG, "Surface created with OpenGL ES 3.2 support");

        GLES32.glClearColor(0.6f, 0.8f, 1.0f, 1.0f);
        GLES32.glEnable(GLES32.GL_DEPTH_TEST);

        // 启用面剔除提高性能
        GLES32.glEnable(GLES32.GL_CULL_FACE);
        GLES32.glCullFace(GLES32.GL_BACK);

        // 加载所有着色器
        loadStandardShaders();
        loadWireframeShaders();

        if (GLSupportChecker.supportsTessellation()) {
            loadTessellationShaders();
        }

        // 加载纹理
        loadTextures();

        // 生成高度图纹理
        if (TerrainDataV2.isTessellationEnabled()) {
            heightMapTextureId = TerrainDataV2.generateHeightMapTexture();
            Log.i(TAG, "Height map texture generated: " + heightMapTextureId);
        }
    }

    private void loadStandardShaders() {
        String vertexShader = ShaderUtils.loadShader(context, R.raw.vertex_shader);
        String fragmentShader = ShaderUtils.loadShader(context, R.raw.fragment_shader);
        standardProgram = ShaderUtils.createProgram(vertexShader, fragmentShader);

        if (standardProgram == 0) {
            throw new RuntimeException("Failed to create standard shader program");
        }

        // 获取属性位置
        positionHandle = GLES32.glGetAttribLocation(standardProgram, "aPosition");
        colorHandle = GLES32.glGetAttribLocation(standardProgram, "aColor");
        normalHandle = GLES32.glGetAttribLocation(standardProgram, "aNormal");
        mvpMatrixHandle = GLES32.glGetUniformLocation(standardProgram, "uMVPMatrix");
        modelMatrixHandle = GLES32.glGetUniformLocation(standardProgram, "uModelMatrix");
        lightPositionHandle = GLES32.glGetUniformLocation(standardProgram, "uLightPosition");
        cameraPositionHandle = GLES32.glGetUniformLocation(standardProgram, "uCameraPosition");
        typeHandle = GLES32.glGetAttribLocation(standardProgram, "aType");
        minHeightHandle = GLES32.glGetUniformLocation(standardProgram, "minHeight");
        maxHeightHandle = GLES32.glGetUniformLocation(standardProgram, "maxHeight");
        texCoordHandle = GLES32.glGetAttribLocation(standardProgram, "aTexCoord");
        useTextureHandle = GLES32.glGetUniformLocation(standardProgram, "uUseTexture");

        Log.i(TAG, "Standard shaders loaded successfully");
    }

    private void loadWireframeShaders() {
        String wireframeVertexShader = ShaderUtils.loadShader(context, R.raw.wireframe_vertex_shader);
        String wireframeFragmentShader = ShaderUtils.loadShader(context, R.raw.wireframe_fragment_shader);
        wireframeProgram = ShaderUtils.createProgram(wireframeVertexShader, wireframeFragmentShader);

        if (wireframeProgram == 0) {
            Log.w(TAG, "Failed to create wireframe shader program");
        }
    }

    private void loadTessellationShaders() {
        try {
            String tessVertexShader = ShaderUtils.loadShader(context, R.raw.tess_vertex_shader);
            String tessControlShader = ShaderUtils.loadShader(context, R.raw.tess_control_shader);
            String tessEvalShader = ShaderUtils.loadShader(context, R.raw.tess_evaluation_shader);
            String tessFragmentShader = ShaderUtils.loadShader(context, R.raw.tess_fragment_shader);

            tessellationProgram = GLES32.glCreateProgram();

            // 编译并附加着色器 - 使用正确的常量名
            attachShader(tessellationProgram, GLES32.GL_VERTEX_SHADER, tessVertexShader);
            attachShader(tessellationProgram, GLES32.GL_TESS_CONTROL_SHADER, tessControlShader);
            attachShader(tessellationProgram, GLES32.GL_TESS_EVALUATION_SHADER, tessEvalShader);
            attachShader(tessellationProgram, GLES32.GL_FRAGMENT_SHADER, tessFragmentShader);

            GLES32.glLinkProgram(tessellationProgram);

            // 检查链接状态
            int[] linkStatus = new int[1];
            GLES32.glGetProgramiv(tessellationProgram, GLES32.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES32.GL_TRUE) {
                String error = GLES32.glGetProgramInfoLog(tessellationProgram);
                Log.e(TAG, "Tessellation shader link error: " + error);
                tessellationProgram = 0;
            } else {
                // 获取uniform位置
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

    private void loadTextures() {
        wallTextureId = GLTools.loadTexture(context, R.drawable.wall_texture);
        roofTextureId = GLTools.loadTexture(context, R.drawable.roof_texture);

        if (wallTextureId == 0 || roofTextureId == 0) {
            Log.w(TAG, "Some textures failed to load, using default colors");
        } else {
            Log.i(TAG, "All textures loaded successfully");
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES32.glViewport(0, 0, width, height);

        float ratio = (float) width / height;
        Matrix.perspectiveM(projectionMatrix, 0, 45.0f, ratio, 0.1f, 300.0f);

        Log.i(TAG, "Surface changed: " + width + "x" + height);
    }

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

        // 更新FPS
        updateFPS();

        // 更新第一人称位置
        updateFirstPersonPosition(deltaTime);

        // 更新相机
        updateCamera();

        // 清除屏幕
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT | GLES32.GL_DEPTH_BUFFER_BIT);

        // 更新模型矩阵
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, angle, 0, 1, 0);

        // 计算MVP矩阵
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);

        // 根据模式渲染
        switch (currentMode) {
            case SOLID:
                renderStandard();
                break;
            case WIREFRAME:
                renderWireframe();
                break;
            case TESSELLATION:
                if (tessellationProgram != 0) {
                    renderWithTessellation();
                } else {
                    renderStandard(); // 回退到标准渲染
                }
                break;
        }

        // 检查错误
        ShaderUtils.checkGLError("onDrawFrame");
    }

    private void updateFPS() {
        frameCount++;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFpsTime >= 1000) {
            fps = frameCount * 1000.0f / (currentTime - lastFpsTime);
            frameCount = 0;
            lastFpsTime = currentTime;

            // 每5秒记录一次FPS（避免日志过多）
            if (currentTime % 5000 < 16) { // 约每5秒
                Log.i(TAG, String.format("FPS: %.1f, Mode: %s", fps, getCurrentModeName()));
            }
        }
    }

    private void renderStandard() {
        GLES32.glUseProgram(standardProgram);

        // 设置uniforms
        GLES32.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES32.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0);
        GLES32.glUniform3f(lightPositionHandle, lightPosition[0], lightPosition[1], lightPosition[2]);
        GLES32.glUniform3f(cameraPositionHandle, cameraPosition[0], cameraPosition[1], cameraPosition[2]);
        GLES32.glUniform1f(minHeightHandle, meshData.minHeight);
        GLES32.glUniform1f(maxHeightHandle, meshData.maxHeight);

        // 启用纹理
        if (useTextureHandle != -1) {
            GLES32.glUniform1i(useTextureHandle, 1);
        }

        // 绑定纹理
        if (wallTextureId != 0) {
            GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
            GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, wallTextureId);
            int wallTextureHandle = GLES32.glGetUniformLocation(standardProgram, "uWallTexture");
            if (wallTextureHandle != -1) {
                GLES32.glUniform1i(wallTextureHandle, 0);
            }
        }

        if (roofTextureId != 0) {
            GLES32.glActiveTexture(GLES32.GL_TEXTURE1);
            GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, roofTextureId);
            int roofTextureHandle = GLES32.glGetUniformLocation(standardProgram, "uRoofTexture");
            if (roofTextureHandle != -1) {
                GLES32.glUniform1i(roofTextureHandle, 1);
            }
        }

        // 传递顶点数据
        GLES32.glEnableVertexAttribArray(positionHandle);
        GLES32.glVertexAttribPointer(positionHandle, 3, GLES32.GL_FLOAT, false, 12, meshData.vertices);

        GLES32.glEnableVertexAttribArray(colorHandle);
        GLES32.glVertexAttribPointer(colorHandle, 3, GLES32.GL_FLOAT, false, 12, meshData.colors);

        GLES32.glEnableVertexAttribArray(normalHandle);
        GLES32.glVertexAttribPointer(normalHandle, 3, GLES32.GL_FLOAT, false, 12, meshData.normals);

        if (texCoordHandle != -1 && meshData.texCoords != null) {
            GLES32.glEnableVertexAttribArray(texCoordHandle);
            GLES32.glVertexAttribPointer(texCoordHandle, 2, GLES32.GL_FLOAT, false, 8, meshData.texCoords);
        }

        GLES32.glEnableVertexAttribArray(typeHandle);
        GLES32.glVertexAttribIPointer(typeHandle, 1, GLES32.GL_INT, 4, meshData.types);

        // 绘制
        GLES32.glDrawArrays(GLES32.GL_TRIANGLES, 0, meshData.vertexCount);

        // 禁用顶点数组
        disableVertexArrays();
    }

    private void renderWireframe() {
        if (wireframeProgram == 0) {
            renderStandard(); // 回退到标准渲染
            return;
        }

        GLES32.glUseProgram(wireframeProgram);

        int wireframeMvpMatrixHandle = GLES32.glGetUniformLocation(wireframeProgram, "uMVPMatrix");
        int wireframeColorHandle = GLES32.glGetUniformLocation(wireframeProgram, "uColor");
        int wireframePositionHandle = GLES32.glGetAttribLocation(wireframeProgram, "aPosition");

        GLES32.glUniformMatrix4fv(wireframeMvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES32.glUniform3f(wireframeColorHandle, 0.0f, 1.0f, 0.0f);

        GLES32.glEnableVertexAttribArray(wireframePositionHandle);
        GLES32.glVertexAttribPointer(wireframePositionHandle, 3, GLES32.GL_FLOAT, false, 12, meshData.vertices);

        // 绘制点
        GLES32.glUniform3f(wireframeColorHandle, 1.0f, 0.0f, 0.0f);
        GLES32.glDrawArrays(GLES32.GL_POINTS, 0, meshData.vertexCount);

        // 绘制线框
        GLES32.glUniform3f(wireframeColorHandle, 0.0f, 1.0f, 0.0f);
        GLES32.glLineWidth(2.0f);

        for (int i = 0; i < meshData.vertexCount; i += 3) {
            GLES32.glDrawArrays(GLES32.GL_LINE_LOOP, i, 3);
        }

        GLES32.glDisableVertexAttribArray(wireframePositionHandle);
    }

    private void renderWithTessellation() {
        if (tessellationProgram == 0 || heightMapTextureId == -1) {
            renderStandard(); // 回退到标准渲染
            return;
        }

        GLES32.glUseProgram(tessellationProgram);

        // 设置uniforms
        GLES32.glUniformMatrix4fv(tessMvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES32.glUniformMatrix4fv(tessModelMatrixHandle, 1, false, modelMatrix, 0);
        GLES32.glUniform1f(tessTessLevelHandle, TerrainDataV2.getTessellationLevel());
        GLES32.glUniform1f(tessTerrainSizeHandle, TerrainDataV2.TERRAIN_SIZE);
        GLES32.glUniform3f(tessCameraPositionHandle, cameraPosition[0], cameraPosition[1], cameraPosition[2]);
        GLES32.glUniform3f(tessLightPositionHandle, lightPosition[0], lightPosition[1], lightPosition[2]);

        // 绑定高度图纹理
        GLES32.glActiveTexture(GLES32.GL_TEXTURE2);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, heightMapTextureId);
        GLES32.glUniform1i(tessHeightMapHandle, 2);

        // 设置顶点数据（只需要位置）
        int tessPositionHandle = GLES32.glGetAttribLocation(tessellationProgram, "aPosition");
        GLES32.glEnableVertexAttribArray(tessPositionHandle);
        GLES32.glVertexAttribPointer(tessPositionHandle, 3, GLES32.GL_FLOAT, false, 12, meshData.vertices);

        // 使用曲面细分绘制
        GLES32.glPatchParameteri(GLES32.GL_PATCH_VERTICES, 3);
        GLES32.glDrawArrays(GLES32.GL_PATCHES, 0, meshData.vertexCount);

        GLES32.glDisableVertexAttribArray(tessPositionHandle);
        Log.e("csdcdscdscdscds","akssajqj");
    }

    private void disableVertexArrays() {
        GLES32.glDisableVertexAttribArray(positionHandle);
        GLES32.glDisableVertexAttribArray(colorHandle);
        GLES32.glDisableVertexAttribArray(normalHandle);
        GLES32.glDisableVertexAttribArray(typeHandle);
        if (texCoordHandle != -1) {
            GLES32.glDisableVertexAttribArray(texCoordHandle);
        }
    }

    // 公共方法获取状态信息
    public String getPerformanceInfo() {
        return String.format("FPS: %.1f\n顶点数: %d\n模式: %s\n视角: %s\n细分: %s",
                fps, meshData.vertexCount, getCurrentModeName(), getCurrentViewMode(),
                TerrainDataV2.isTessellationEnabled() ? "启用" : "禁用");
    }

    public String getDetailedInfo() {
        String capabilities = GLSupportChecker.getCapabilityReport();
        return capabilities + "\n\n" + getPerformanceInfo();
    }

    public void setTessellationLevel(int level) {
        if (GLSupportChecker.supportsTessellation()) {
            TerrainDataV2.setTessellationLevel(level);
            Log.i(TAG, "Tessellation level set to: " + level);
        }
    }

    public void cycleQualityLevel() {
        int currentLevel = TerrainDataV2.getTessellationLevel();
        int newLevel = (currentLevel % 8) + 2; // 在2-8之间循环
        setTessellationLevel(newLevel);
    }

    // 资源清理
    public void cleanup() {
        if (standardProgram != 0) {
            GLES32.glDeleteProgram(standardProgram);
        }
        if (wireframeProgram != 0) {
            GLES32.glDeleteProgram(wireframeProgram);
        }
        if (tessellationProgram != 0) {
            GLES32.glDeleteProgram(tessellationProgram);
        }
        if (heightMapTextureId != -1) {
            int[] textures = {heightMapTextureId};
            GLES32.glDeleteTextures(1, textures, 0);
        }

        Log.i(TAG, "GLRendererV2 resources cleaned up");
    }
}
