package com.example.gl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRendererV6 implements GLSurfaceView.Renderer {
    private static final String TAG = "GLRendererV4";
    private Context context;

    // 着色器程序
    private int tessellationProgram;
    private int wireframeProgram;
    private int currentProgram;

    // 曲面细分着色器属性
    private int tessMvpMatrixHandle;
    private int tessModelMatrixHandle;
    private int tessTessLevelHandle;
    private int tessHeightMapHandle;
    private int tessTerrainSizeHandle;
    private int tessCameraPositionHandle;
    private int tessLightPositionHandle;

    // 线框着色器属性
    private int wireframeMvpMatrixHandle;
    private int wireframeColorHandle;

    // 纹理
    private int heightMapTextureId;

    private TerrainDataV2.MeshData meshData;

    // 线框数据
    private FloatBuffer wireframeVerticesBuffer;
    private int wireframeVertexCount;
    private int wireframeVAO;
    private int wireframeVBO;

    // 矩阵
    private float[] modelMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    private float[] mvpMatrix = new float[16];

    // 相机和光照
    private float angle = 0;
    private float[] lightPosition = {50.0f, 80.0f, 50.0f};
    private float[] cameraPosition = {0.0f, 40.0f, 80.0f};

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

    // 渲染模式
    public enum RenderMode {
        WIREFRAME,
        TESSELLATION
    }

    private RenderMode currentMode = RenderMode.TESSELLATION;
    private long lastFrameTime = System.currentTimeMillis();

    // 性能监控
    private int frameCount = 0;
    private long lastFpsTime = 0;
    private float fps = 0;

    // 高度图数据
    private Bitmap heightMapBitmap;
    private int heightMapWidth;
    private int heightMapHeight;
    private float maxHeight = 20.0f; // 最大高度

    public GLRendererV6(Context context) {
        this.context = context;
        // 配置高级地形特性
        configureAdvancedFeatures();

        // 生成地形网格
        meshData = TerrainDataV2.generateTerrainMesh();

        // 加载高度图Bitmap
//        loadHeightMapBitmap();

        // 从高度图生成线框数据
//        generateWireframeFromHeightMap();

        // 初始化位置
        fpvPosition[0] = 0.0f;
        fpvPosition[1] = getTerrainHeight(0, 0) + 2.0f;
//        fpvPosition[1] = getHeightFromBitmap(0, 0) + 2.0f;
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

    private void loadHeightMapBitmap() {
        // 从资源加载高度图
        heightMapBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.sz);
        heightMapWidth = heightMapBitmap.getWidth();
        heightMapHeight = heightMapBitmap.getHeight();

        Log.i(TAG, "Height map loaded: " + heightMapWidth + "x" + heightMapHeight);
    }

    // 从Bitmap获取高度值
    private float getHeightFromBitmap(float worldX, float worldZ) {
        // 将世界坐标转换为Bitmap像素坐标
        int pixelX = (int) ((worldX / TerrainDataV2.TERRAIN_SIZE + 0.5f) * heightMapWidth);
        int pixelY = (int) ((worldZ / TerrainDataV2.TERRAIN_SIZE + 0.5f) * heightMapHeight);

        // 边界检查
        pixelX = Math.max(0, Math.min(heightMapWidth - 1, pixelX));
        pixelY = Math.max(0, Math.min(heightMapHeight - 1, pixelY));

        // 获取像素灰度值（使用红色通道）
        int pixel = heightMapBitmap.getPixel(pixelX, pixelY);
        int r = (pixel >> 16) & 0xFF;

        // 将0-255映射到0-maxHeight
        return (r / 255.0f) * maxHeight;
    }

    // 直接从高度图Bitmap生成线框网格
    private void generateWireframeFromHeightMap() {
        int gridSpacing = 4; // 网格间距，数值越小网格越密
        int pointsX = heightMapWidth / gridSpacing;
        int pointsZ = heightMapHeight / gridSpacing;

        // 计算顶点数量：水平线 + 垂直线
        wireframeVertexCount = (pointsX - 1) * pointsZ * 2 + // 水平线
                pointsX * (pointsZ - 1) * 2;   // 垂直线

        float[] vertices = new float[wireframeVertexCount * 3];
        int vertexIndex = 0;

        // 生成网格线
        for (int z = 0; z < pointsZ; z++) {
            for (int x = 0; x < pointsX; x++) {
                float worldX = (x - pointsX / 2.0f) * gridSpacing;
                float worldZ = (z - pointsZ / 2.0f) * gridSpacing;

                // 水平线 (x方向)
                if (x < pointsX - 1) {
                    float height1 = getHeightFromBitmap(worldX, worldZ);
                    float height2 = getHeightFromBitmap(worldX + gridSpacing, worldZ);

                    vertices[vertexIndex++] = worldX;
                    vertices[vertexIndex++] = height1;
                    vertices[vertexIndex++] = worldZ;

                    vertices[vertexIndex++] = worldX + gridSpacing;
                    vertices[vertexIndex++] = height2;
                    vertices[vertexIndex++] = worldZ;
                }

                // 垂直线 (z方向)
                if (z < pointsZ - 1) {
                    float height1 = getHeightFromBitmap(worldX, worldZ);
                    float height2 = getHeightFromBitmap(worldX, worldZ + gridSpacing);

                    vertices[vertexIndex++] = worldX;
                    vertices[vertexIndex++] = height1;
                    vertices[vertexIndex++] = worldZ;

                    vertices[vertexIndex++] = worldX;
                    vertices[vertexIndex++] = height2;
                    vertices[vertexIndex++] = worldZ + gridSpacing;
                }
            }
        }

        wireframeVertexCount = vertexIndex / 3;

        // 创建顶点缓冲区
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        wireframeVerticesBuffer = bb.asFloatBuffer();
        wireframeVerticesBuffer.put(vertices);
        wireframeVerticesBuffer.position(0);

        Log.i(TAG, "Wireframe generated: " + wireframeVertexCount + " vertices from height map");
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(TAG, "Surface created with OpenGL ES 3.2 support");

        GLES32.glClearColor(0.1f, 0.1f, 0.1f, 1.0f); // 深色背景便于观察线框
        GLES32.glEnable(GLES32.GL_DEPTH_TEST);

        // 加载着色器
//        loadWireframeShaders();
        if (true) {
            loadTessellationShaders();
        }

        // 生成高度图纹理
        heightMapTextureId = GLTools.loadTexture(context, R.drawable.sz);
        Log.i(TAG, "Height map texture loaded: " + heightMapTextureId);

        // 创建线框缓冲区
//        createWireframeBuffers();
    }

    private void loadWireframeShaders() {
        String wireframeVertexShader = ShaderUtils.loadShader(context, R.raw.wireframe_vertex_shader);
        String wireframeFragmentShader = ShaderUtils.loadShader(context, R.raw.wireframe_fragment_shader);
        wireframeProgram = ShaderUtils.createProgram(wireframeVertexShader, wireframeFragmentShader);

        if (wireframeProgram != 0) {
            wireframeMvpMatrixHandle = GLES32.glGetUniformLocation(wireframeProgram, "uMVPMatrix");
            wireframeColorHandle = GLES32.glGetUniformLocation(wireframeProgram, "uColor");
            Log.i(TAG, "Wireframe shaders loaded successfully");
        } else {
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

    private void createWireframeBuffers() {
        int[] vaos = new int[1];
        int[] vbos = new int[1];

        GLES32.glGenVertexArrays(1, vaos, 0);
        GLES32.glGenBuffers(1, vbos, 0);

        wireframeVAO = vaos[0];
        wireframeVBO = vbos[0];

        GLES32.glBindVertexArray(wireframeVAO);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, wireframeVBO);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER,
                wireframeVerticesBuffer.capacity() * 4,
                wireframeVerticesBuffer, GLES32.GL_STATIC_DRAW);

        GLES32.glVertexAttribPointer(0, 3, GLES32.GL_FLOAT, false, 12, 0);
        GLES32.glEnableVertexAttribArray(0);

        GLES32.glBindVertexArray(0);

        Log.i(TAG, "Wireframe buffers created");
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

        switch (currentMode) {
            case WIREFRAME:
                renderWireframe();
                break;
            case TESSELLATION:
                if (tessellationProgram != 0) {
                    renderWithTessellation();
                }
                break;
        }

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

    // 渲染线框 - 直接从高度图数据渲染
    private void renderWireframe() {
        if (wireframeProgram == 0) return;

        GLES32.glUseProgram(wireframeProgram);

        GLES32.glUniformMatrix4fv(wireframeMvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES32.glUniform3f(wireframeColorHandle, 0.0f, 1.0f, 0.0f); // 绿色线框

        GLES32.glLineWidth(1.5f);
        GLES32.glEnable(GLES32.GL_DEPTH_TEST);
        GLES32.glDisable(GLES32.GL_CULL_FACE);

        GLES32.glBindVertexArray(wireframeVAO);
        GLES32.glDrawArrays(GLES32.GL_LINES, 0, wireframeVertexCount);
        GLES32.glBindVertexArray(0);

        GLES32.glEnable(GLES32.GL_CULL_FACE);
    }

    // 渲染曲面细分 - 使用高度图纹理
    private void renderWithTessellation() {
        if (tessellationProgram == 0 || heightMapTextureId == -1) return;

        GLES32.glUseProgram(tessellationProgram);

        GLES32.glUniformMatrix4fv(tessMvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES32.glUniformMatrix4fv(tessModelMatrixHandle, 1, false, modelMatrix, 0);
        GLES32.glUniform1f(tessTessLevelHandle, 8.0f); // 固定细分级别
        GLES32.glUniform1f(tessTerrainSizeHandle, 100.0f);
        GLES32.glUniform3f(tessCameraPositionHandle, cameraPosition[0], cameraPosition[1], cameraPosition[2]);
        GLES32.glUniform3f(tessLightPositionHandle, lightPosition[0], lightPosition[1], lightPosition[2]);

        GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, heightMapTextureId);
        GLES32.glUniform1i(tessHeightMapHandle, 0);

        // 使用线框数据作为细分的基础网格
        int tessPositionHandle = GLES32.glGetAttribLocation(tessellationProgram, "aPosition");
        GLES32.glEnableVertexAttribArray(tessPositionHandle);
        GLES32.glVertexAttribPointer(tessPositionHandle, 3, GLES32.GL_FLOAT, false, 12, meshData.vertices);

//        GLES32.glBindVertexArray(wireframeVAO);
        GLES32.glPatchParameteri(GLES32.GL_PATCH_VERTICES, 3);
        GLES32.glDrawArrays(GLES32.GL_PATCHES, 0, meshData.vertexCount);
//        GLES32.glBindVertexArray(0);

        GLES32.glDisableVertexAttribArray(tessPositionHandle);
    }

    // 视图控制方法（保持不变）
    public void toggleViewMode() {
        isFirstPersonView = !isFirstPersonView;
        isAutoRotating = !isFirstPersonView;
    }

    public String getCurrentViewMode() {
        return isFirstPersonView ? "第一人称漫游" : "上帝视角";
    }

    public void toggleRenderMode() {
        currentMode = (currentMode == RenderMode.WIREFRAME) ? RenderMode.TESSELLATION : RenderMode.WIREFRAME;
    }

    public String getCurrentModeName() {
        return (currentMode == RenderMode.WIREFRAME) ? "骨架线框模式" : "曲面细分模式";
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

    public String getPerformanceInfo() {
        return String.format("FPS: %.1f\n顶点数: %d\n模式: %s\n视角: %s",
                fps, wireframeVertexCount, getCurrentModeName(), getCurrentViewMode());
    }

    public void cleanup() {
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
        if (wireframeVAO != 0) {
            int[] vaos = {wireframeVAO};
            GLES32.glDeleteVertexArrays(1, vaos, 0);
        }
        if (wireframeVBO != 0) {
            int[] vbos = {wireframeVBO};
            GLES32.glDeleteBuffers(1, vbos, 0);
        }
        if (heightMapBitmap != null) {
            heightMapBitmap.recycle();
        }
        Log.i(TAG, "GLRendererV2 resources cleaned up");
    }
}
