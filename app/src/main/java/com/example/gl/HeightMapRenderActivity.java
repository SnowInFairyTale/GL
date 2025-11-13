package com.example.gl;

import android.opengl.GLSurfaceView;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

// 在Activity中
public class HeightMapRenderActivity extends AppCompatActivity {
    private GLSurfaceView debugSurfaceView;
    private HeightMapDebugRenderer debugRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 获取高度图纹理ID
        int heightMapTextureId = TerrainDataV2.generateHeightMapTexture();

        debugSurfaceView = new GLSurfaceView(this);
        debugSurfaceView.setEGLContextClientVersion(3);
        debugRenderer = new HeightMapDebugRenderer(this, heightMapTextureId);
        debugSurfaceView.setRenderer(debugRenderer);

        setContentView(debugSurfaceView);
    }
}
