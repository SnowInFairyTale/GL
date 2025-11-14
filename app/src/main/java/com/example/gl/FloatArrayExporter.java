package com.example.gl;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FloatArrayExporter {
    private static final String TAG = "FloatArrayExporter";

    /**
     * Android专用版本，写入应用缓存目录
     */
    public static String exportToCacheFile(Context context, float[][] data, String fileName) {
        if (data == null || data.length == 0) {
            Log.e(TAG, "Invalid data");
            return null;
        }

        FileOutputStream fos = null;
        try {
            // 创建缓存文件
            File cacheDir = context.getExternalCacheDir();
            File outputFile = new File(cacheDir, fileName);

            fos = new FileOutputStream(outputFile);

            for (int i = 0; i < data.length; i++) {
                if (data[i] != null) {
                    StringBuilder line = new StringBuilder();
                    for (int j = 0; j < data[i].length; j++) {
                        line.append(data[i][j]);
                        if (j < data[i].length - 1) {
                            line.append(" ");
                        }
                    }
                    line.append("\n");
                    fos.write(line.toString().getBytes());
                }
            }

            String filePath = outputFile.getAbsolutePath();
            Log.i(TAG, "Data exported to cache: " + filePath);
            Log.i(TAG, String.format("Data dimensions: %dx%d", data.length, data[0].length));

            return filePath;

        } catch (IOException e) {
            Log.e(TAG, "Error writing cache file: " + e.getMessage());
            return null;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing stream: " + e.getMessage());
                }
            }
        }
    }
}
