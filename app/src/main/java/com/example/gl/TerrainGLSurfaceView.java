package com.example.gl;

// TerrainGLSurfaceView.java
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class TerrainGLSurfaceView extends GLSurfaceView {
    private TerrainRenderer renderer;

    public TerrainGLSurfaceView(Context context) {
        super(context);
        init();
    }

    public TerrainGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setEGLContextClientVersion(3);
        renderer = new TerrainRenderer(getContext());
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    public TerrainRenderer getRenderer() {
        return renderer;
    }
}
