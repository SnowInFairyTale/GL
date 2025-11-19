package com.example.gl;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
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
        findViewById(R.id.combine_v5_bt).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GLV5Activity.class);
            startActivity(intent);
        });
        findViewById(R.id.combine_v7_bt).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GLV7Activity.class);
            startActivity(intent);
        });

        ImageView imageView = findViewById(R.id.iv_tiff_image_view);
        new Thread() {
            @Override
            public void run() {
                Bitmap bitmap = TiffBitmapFactoryConverter.convert32BitTIFF(getApplication(), R.raw.adapt_20251114_102916);
                runOnUiThread(() -> imageView.setImageBitmap(bitmap));

                ManualTIFFParser.TIFFParseResult result = ManualTIFFParser.parseTIFFToBitmap(getApplication(), R.raw.adapt_20251114_102916);
                String adptPath = FloatArrayExporter.exportToCacheFile(getApplication(), result.heightData, "adpt.txt");
                float[][] heightData = result.heightData;

                int width = heightData.length;
                int height = heightData[0].length;

                float minHeight = Float.MAX_VALUE;
                float maxHeight = Float.MIN_VALUE;

                float[][] dHeightData = new float[width][height];
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        float v = heightData[i][j];
                        float h;
                        if (Float.isNaN(v)) {
                            h = 0;
                        } else {
                            h = (v + 2.41f);
                        }
                        dHeightData[i][j] = h;

                        minHeight = Math.min(minHeight, h);
                        maxHeight = Math.max(maxHeight, h);
                    }
                }

                ManualTIFFParser.TIFFParseResult dResult = new ManualTIFFParser.TIFFParseResult();
                dResult.minValue = minHeight;
                dResult.maxValue = maxHeight;
                dResult.heightData = dHeightData;
                dResult.width = width;
                dResult.heightData = heightData;

                TIFFParseResultData.result = dResult;
                TIFFParseResultData.meshData = RealTerrainData.generateTerrainMeshFromHeightData(dHeightData);

                TIFFParseResultData.meshData2 = RealTerrainData2.generateTerrainMeshFromHeightData(dHeightData);

                String adpt2Path = FloatArrayExporter.exportToCacheFile(getApplication(), dHeightData, "adpt2.txt");

                runOnUiThread(() -> imageView.setImageBitmap(result.bitmap));
                Log.e("TIFFParseResult", "result " + result + ",adptPath " + adptPath);
                Log.e("TIFFParseResult", "dResult " + dResult + ",adpt2Path " + adpt2Path);
            }
        }.start();
    }
}
