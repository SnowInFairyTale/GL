package com.example.gl;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TerrainGLSurfaceView glSurfaceView;
    private Button toggleButton;
    private TextView modeTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 创建主布局
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));

        // 创建顶部控制栏
        LinearLayout controlLayout = new LinearLayout(this);
        controlLayout.setOrientation(LinearLayout.HORIZONTAL);
        controlLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        controlLayout.setGravity(Gravity.CENTER);
        controlLayout.setPadding(20, 20, 20, 20);
        controlLayout.setBackgroundColor(0xFF333333);

        // 创建切换按钮
        toggleButton = new Button(this);
        toggleButton.setText("切换至骨架图");
        toggleButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // 创建模式显示文本
        modeTextView = new TextView(this);
        modeTextView.setText("当前模式: 3D实体模式");
        modeTextView.setTextColor(Color.WHITE);
        modeTextView.setTextSize(16);
        modeTextView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        modeTextView.setPadding(40, 0, 0, 0);

        // 创建GLSurfaceView
        glSurfaceView = new TerrainGLSurfaceView(this);
        glSurfaceView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));

        // 添加视图到布局
        controlLayout.addView(toggleButton);
        controlLayout.addView(modeTextView);
        mainLayout.addView(controlLayout);
        mainLayout.addView(glSurfaceView);

        setContentView(mainLayout);

        // 设置按钮点击事件
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TerrainRenderer renderer = glSurfaceView.getRenderer();
                if (renderer != null) {
                    renderer.toggleRenderMode();
                    modeTextView.setText("当前模式: " + renderer.getCurrentModeName());
                    toggleButton.setText(renderer.getCurrentMode() == TerrainRenderer.RenderMode.SOLID ?
                            "切换至骨架图" : "切换至3D图");

                    // 请求重绘
                    glSurfaceView.requestRender();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
    }
}
