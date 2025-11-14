package com.example.gl;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

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
        findViewById(R.id.combine_v2_bt).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GLV2Activity.class);
            startActivity(intent);
        });
        findViewById(R.id.combine_v2_bt).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GLV2Activity.class);
            startActivity(intent);
        });
        findViewById(R.id.height_map_bt).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HeightMapRenderActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.combine_v4_bt).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GLV4Activity.class);
            startActivity(intent);
        });

        ImageView imageView = findViewById(R.id.iv_tiff_image_view);
        new Thread() {
            @Override
            public void run() {
                Bitmap bitmap = TiffBitmapFactoryConverter.convert32BitTIFF(getApplication(), R.raw.adapt_20251114_102916);
                runOnUiThread(() -> imageView.setImageBitmap(bitmap));

                Bitmap bitmap11 = ManualTIFFParser.parseTIFFToBitmap(getApplication(), R.raw.adapt_20251114_102916);
                runOnUiThread(() -> imageView.setImageBitmap(bitmap11));
            }
        }.start();
    }
}
