package com.example.gl;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class GLActivity extends AppCompatActivity {
    private GLSurfaceView glSurfaceView;
    private GLRenderer glRenderer;

    // 控制按钮
    private Button toggleRenderBtn;
    private Button forwardBtn, backwardBtn, leftBtn, rightBtn, upBtn, downBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gl);

        setupUI();
        setupGLSurfaceView();
    }

    private void setupUI() {
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
        toggleRenderBtn.setOnClickListener(v -> glRenderer.toggleRenderMode());

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
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    glRenderer.setMovement(forward, backward, left, right, up, down);
                    break;
                case MotionEvent.ACTION_UP:
                    glRenderer.setMovement(false, false, false, false, false, false);
                    break;
            }
            return true;
        });
    }

    private void setupGLSurfaceView() {
        glSurfaceView = findViewById(R.id.glSurfaceView);
        glSurfaceView.setEGLContextClientVersion(3);
        glRenderer = new GLRenderer(this);
        glSurfaceView.setRenderer(glRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // 设置触摸监听
        glSurfaceView.setOnTouchListener((v, event) -> {
            glRenderer.onTouchEvent(event);
            glSurfaceView.requestRender(); // 请求重绘
            return true;
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
