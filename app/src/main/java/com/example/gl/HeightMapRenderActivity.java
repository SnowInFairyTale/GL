package com.example.gl;

import android.opengl.GLSurfaceView;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

// 在Activity中
public class HeightMapRenderActivity extends AppCompatActivity {
    private GLSurfaceView glSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        glSurfaceView = new GLSurfaceView(this);

        // 重要：设置EGL上下文版本
        glSurfaceView.setEGLContextClientVersion(3);

        // 可选：设置EGL配置
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);

        HeightMapDebugRenderer renderer = new HeightMapDebugRenderer(this);
        glSurfaceView.setRenderer(renderer);

        setContentView(glSurfaceView);

        // 设置渲染模式为连续渲染
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
    }
}
