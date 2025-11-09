package com.example.gl;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class GLActivity extends AppCompatActivity {
    private GLSurfaceView glSurfaceView;
    private TerrainRenderer terrainRenderer;
    private TextView infoText;

    // 控制按钮
    private Button toggleViewBtn, toggleRenderBtn;
    private Button forwardBtn, backwardBtn, leftBtn, rightBtn, upBtn, downBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupUI();
        setupGLSurfaceView();
    }

    private void setupUI() {
        infoText = findViewById(R.id.infoText);
        toggleViewBtn = findViewById(R.id.toggleViewBtn);
        toggleRenderBtn = findViewById(R.id.toggleRenderBtn);

        forwardBtn = findViewById(R.id.forwardBtn);
        backwardBtn = findViewById(R.id.backwardBtn);
        leftBtn = findViewById(R.id.leftBtn);
        rightBtn = findViewById(R.id.rightBtn);
        upBtn = findViewById(R.id.upBtn);
        downBtn = findViewById(R.id.downBtn);

        setupButtonListeners();
    }

    private void setupButtonListeners() {
        toggleViewBtn.setOnClickListener(v -> {
            terrainRenderer.toggleViewMode();
            updateUI();
        });

        toggleRenderBtn.setOnClickListener(v -> {
            terrainRenderer.toggleRenderMode();
            updateUI();
        });

        // 移动控制（按下和松开）
        setMovementButton(forwardBtn, true, false, false, false, false, false);
        setMovementButton(backwardBtn, false, true, false, false, false, false);
        setMovementButton(leftBtn, false, false, true, false, false, false);
        setMovementButton(rightBtn, false, false, false, true, false, false);
        setMovementButton(upBtn, false, false, false, false, true, false);
        setMovementButton(downBtn, false, false, false, false, false, true);
    }

    private void setMovementButton(Button button, boolean forward, boolean backward,
                                   boolean left, boolean right, boolean up, boolean down) {
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        terrainRenderer.setMovement(forward, backward, left, right, up, down);
                        break;
                    case MotionEvent.ACTION_UP:
                        terrainRenderer.setMovement(false, false, false, false, false, false);
                        break;
                }
                return true;
            }
        });
    }

    private void setupGLSurfaceView() {
        glSurfaceView = findViewById(R.id.glSurfaceView);
        glSurfaceView.setEGLContextClientVersion(3);
        terrainRenderer = new TerrainRenderer(this);
        glSurfaceView.setRenderer(terrainRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // 设置触摸监听
        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                terrainRenderer.onTouchEvent(event);
                glSurfaceView.requestRender(); // 请求重绘
                return true;
            }
        });
    }

    private void updateUI() {
        runOnUiThread(() -> {
            String info = "模式: " + terrainRenderer.getCurrentViewMode() + " | " +
                    terrainRenderer.getCurrentModeName();
            infoText.setText(info);
        });
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
