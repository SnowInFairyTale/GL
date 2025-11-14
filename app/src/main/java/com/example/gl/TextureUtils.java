package com.example.gl;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.opengl.GLES32;
import android.opengl.GLUtils;
import android.util.Log;

public class TextureUtils {
    private static final String TAG = "TextureUtils";

    /**
     * 将Bitmap转换为灰度图
     */
    public static Bitmap convertToGrayscale(Bitmap original) {
        if (original == null) return null;

        int width = original.getWidth();
        int height = original.getHeight();

        Bitmap grayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(grayscale);
        Paint paint = new Paint();

        // 使用ColorMatrix转换为灰度
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0); // 0表示完全去色
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        paint.setColorFilter(filter);

        canvas.drawBitmap(original, 0, 0, paint);

        Log.i(TAG, "Converted to grayscale: " + width + "x" + height);
        return grayscale;
    }

    /**
     * 手动像素级灰度转换（更精确控制）
     */
    public static Bitmap convertToGrayscaleManual(Bitmap original) {
        if (original == null) return null;

        int width = original.getWidth();
        int height = original.getHeight();

        Bitmap grayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];

        original.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;

            // 使用亮度公式计算灰度值
            int gray = (int) (r * 0.299 + g * 0.587 + b * 0.114);
            gray = Math.max(0, Math.min(255, gray));

            pixels[i] = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
        }

        grayscale.setPixels(pixels, 0, width, 0, 0, width, height);
        return grayscale;
    }

    /**
     * 从32位浮点数据创建灰度高度图
     */
    public static Bitmap createHeightMapFromFloatData(float[] heightData, int width, int height) {
        if (heightData == null || heightData.length != width * height) {
            Log.e(TAG, "Invalid height data");
            return null;
        }

        Bitmap heightMap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];

        // 找到数据的最大最小值用于归一化
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (float value : heightData) {
            if (value < min) min = value;
            if (value > max) max = value;
        }
        float range = max - min;

        Log.i(TAG, String.format("Height range: %.2f to %.2f", min, max));

        for (int i = 0; i < heightData.length; i++) {
            // 归一化到0-1范围
            float normalized = (heightData[i] - min) / range;
            int gray = (int) (normalized * 255);
            gray = Math.max(0, Math.min(255, gray));

            pixels[i] = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
        }

        heightMap.setPixels(pixels, 0, width, 0, 0, width, height);
        return heightMap;
    }

    /**
     * 创建OpenGL纹理
     */
    public static int createTexture(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "Bitmap is null");
            return -1;
        }

        int[] textureHandle = new int[1];
        GLES32.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {
            GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, textureHandle[0]);

            // 设置纹理参数
            GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR);
            GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);
            GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_CLAMP_TO_EDGE);
            GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_CLAMP_TO_EDGE);

            // 上传纹理数据
            GLUtils.texImage2D(GLES32.GL_TEXTURE_2D, 0, bitmap, 0);

            Log.i(TAG, "Texture created: " + textureHandle[0]);
        } else {
            Log.e(TAG, "Failed to generate texture");
        }

        return textureHandle[0];
    }

    /**
     * 创建高度图纹理（带Mipmap）
     */
    public static int createHeightMapTexture(Bitmap bitmap) {
        if (bitmap == null) return -1;

        int[] textureHandle = new int[1];
        GLES32.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {
            GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, textureHandle[0]);

            // 对于高度图，使用更好的过滤
            GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR_MIPMAP_LINEAR);
            GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);
            GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_CLAMP_TO_EDGE);
            GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_CLAMP_TO_EDGE);

            // 上传纹理并生成Mipmap
            GLUtils.texImage2D(GLES32.GL_TEXTURE_2D, 0, bitmap, 0);
            GLES32.glGenerateMipmap(GLES32.GL_TEXTURE_2D);

            Log.i(TAG, "Height map texture created with mipmaps: " + textureHandle[0]);
        }

        return textureHandle[0];
    }
}
