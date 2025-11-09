package com.example.gl;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.sd_bt).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TerrainGLActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.zy_bt).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GLActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.combine_bt).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CombineActivity.class);
            startActivity(intent);
        });
    }
}
