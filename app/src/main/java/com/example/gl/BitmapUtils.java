package com.example.gl;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

public class BitmapUtils {
    private static final String TAG = "BitmapUtils";

    /**
     * 使用Matrix旋转Bitmap
     */
    public static Bitmap rotateBitmap(Bitmap source, float degrees) {
        if (source == null) {
            Log.e(TAG, "Source bitmap is null");
            return null;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);

        Bitmap rotatedBitmap = Bitmap.createBitmap(
                source,
                0, 0,
                source.getWidth(),
                source.getHeight(),
                matrix,
                true
        );

        Log.i(TAG, String.format("Bitmap rotated %.0f degrees: %dx%d -> %dx%d",
                degrees,
                source.getWidth(), source.getHeight(),
                rotatedBitmap.getWidth(), rotatedBitmap.getHeight()));

        return rotatedBitmap;
    }

    /**
     * 顺时针旋转90度
     */
    public static Bitmap rotate90Clockwise(Bitmap source) {
        return rotateBitmap(source, 90);
    }

    /**
     * 逆时针旋转90度
     */
    public static Bitmap rotate90CounterClockwise(Bitmap source) {
        return rotateBitmap(source, -90);
    }

    /**
     * 旋转180度
     */
    public static Bitmap rotate180(Bitmap source) {
        return rotateBitmap(source, 180);
    }

    // ==================== 镜像功能 ====================

    /**
     * 水平镜像（左右翻转）
     */
    public static Bitmap flipHorizontal(Bitmap source) {
        if (source == null) {
            Log.e(TAG, "Source bitmap is null");
            return null;
        }

        Matrix matrix = new Matrix();
        matrix.preScale(-1, 1); // 水平镜像
        matrix.postTranslate(source.getWidth(), 0); // 平移回正确位置

        Bitmap flippedBitmap = Bitmap.createBitmap(
                source,
                0, 0,
                source.getWidth(),
                source.getHeight(),
                matrix,
                true
        );

        Log.i(TAG, String.format("Bitmap flipped horizontally: %dx%d",
                flippedBitmap.getWidth(), flippedBitmap.getHeight()));

        return flippedBitmap;
    }

    /**
     * 垂直镜像（上下翻转）
     */
    public static Bitmap flipVertical(Bitmap source) {
        if (source == null) {
            Log.e(TAG, "Source bitmap is null");
            return null;
        }

        Matrix matrix = new Matrix();
        matrix.preScale(1, -1); // 垂直镜像
        matrix.postTranslate(0, source.getHeight()); // 平移回正确位置

        Bitmap flippedBitmap = Bitmap.createBitmap(
                source,
                0, 0,
                source.getWidth(),
                source.getHeight(),
                matrix,
                true
        );

        Log.i(TAG, String.format("Bitmap flipped vertically: %dx%d",
                flippedBitmap.getWidth(), flippedBitmap.getHeight()));

        return flippedBitmap;
    }

    /**
     * 同时水平和垂直镜像（相当于旋转180度）
     */
    public static Bitmap flipBoth(Bitmap source) {
        if (source == null) {
            Log.e(TAG, "Source bitmap is null");
            return null;
        }

        Matrix matrix = new Matrix();
        matrix.preScale(-1, -1); // 水平和垂直镜像
        matrix.postTranslate(source.getWidth(), source.getHeight()); // 平移回正确位置

        Bitmap flippedBitmap = Bitmap.createBitmap(
                source,
                0, 0,
                source.getWidth(),
                source.getHeight(),
                matrix,
                true
        );

        Log.i(TAG, String.format("Bitmap flipped both ways: %dx%d",
                flippedBitmap.getWidth(), flippedBitmap.getHeight()));

        return flippedBitmap;
    }

    // ==================== 组合操作 ====================

    /**
     * 旋转并镜像
     */
    public static Bitmap rotateAndFlip(Bitmap source, float degrees, boolean flipHorizontal, boolean flipVertical) {
        if (source == null) {
            Log.e(TAG, "Source bitmap is null");
            return null;
        }

        Matrix matrix = new Matrix();

        // 应用旋转
        if (degrees != 0) {
            matrix.postRotate(degrees);
        }

        // 应用镜像
        float scaleX = flipHorizontal ? -1 : 1;
        float scaleY = flipVertical ? -1 : 1;

        if (scaleX != 1 || scaleY != 1) {
            matrix.preScale(scaleX, scaleY);

            // 计算平移量
            float translateX = flipHorizontal ? source.getWidth() : 0;
            float translateY = flipVertical ? source.getHeight() : 0;
            matrix.postTranslate(translateX, translateY);
        }

        Bitmap resultBitmap = Bitmap.createBitmap(
                source,
                0, 0,
                source.getWidth(),
                source.getHeight(),
                matrix,
                true
        );

        Log.i(TAG, String.format("Bitmap rotated %.0f° and flipped (H:%s, V:%s): %dx%d",
                degrees, flipHorizontal, flipVertical,
                resultBitmap.getWidth(), resultBitmap.getHeight()));

        return resultBitmap;
    }

    /**
     * 常用组合：顺时针90度+水平镜像
     */
    public static Bitmap rotate90ClockwiseAndFlipHorizontal(Bitmap source) {
        return rotateAndFlip(source, 90, true, false);
    }

    /**
     * 常用组合：逆时针90度+垂直镜像
     */
    public static Bitmap rotate90CounterClockwiseAndFlipVertical(Bitmap source) {
        return rotateAndFlip(source, -90, false, true);
    }

    // ==================== 手动像素操作的镜像功能 ====================

    /**
     * 手动像素操作水平镜像
     */
    public static Bitmap flipHorizontalManual(Bitmap source) {
        if (source == null) {
            Log.e(TAG, "Source bitmap is null");
            return null;
        }

        int width = source.getWidth();
        int height = source.getHeight();

        Bitmap flippedBitmap = Bitmap.createBitmap(width, height, source.getConfig());

        int[] pixels = new int[width * height];
        source.getPixels(pixels, 0, width, 0, 0, width, height);

        int[] flippedPixels = new int[width * height];

        // 手动水平镜像
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sourceIndex = y * width + x;
                int flippedIndex = y * width + (width - 1 - x);
                flippedPixels[flippedIndex] = pixels[sourceIndex];
            }
        }

        flippedBitmap.setPixels(flippedPixels, 0, width, 0, 0, width, height);

        Log.i(TAG, String.format("Bitmap flipped horizontally (manual): %dx%d",
                width, height));

        return flippedBitmap;
    }

    /**
     * 手动像素操作垂直镜像
     */
    public static Bitmap flipVerticalManual(Bitmap source) {
        if (source == null) {
            Log.e(TAG, "Source bitmap is null");
            return null;
        }

        int width = source.getWidth();
        int height = source.getHeight();

        Bitmap flippedBitmap = Bitmap.createBitmap(width, height, source.getConfig());

        int[] pixels = new int[width * height];
        source.getPixels(pixels, 0, width, 0, 0, width, height);

        int[] flippedPixels = new int[width * height];

        // 手动垂直镜像
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sourceIndex = y * width + x;
                int flippedIndex = (height - 1 - y) * width + x;
                flippedPixels[flippedIndex] = pixels[sourceIndex];
            }
        }

        flippedBitmap.setPixels(flippedPixels, 0, width, 0, 0, width, height);

        Log.i(TAG, String.format("Bitmap flipped vertically (manual): %dx%d",
                width, height));

        return flippedBitmap;
    }

    // ==================== 内存优化版本 ====================

    /**
     * 镜像并自动回收原Bitmap
     */
    public static Bitmap flipAndRecycle(Bitmap source, boolean horizontal, boolean vertical) {
        if (source == null) {
            Log.e(TAG, "Source bitmap is null");
            return null;
        }

        Matrix matrix = new Matrix();
        float scaleX = horizontal ? -1 : 1;
        float scaleY = vertical ? -1 : 1;

        matrix.preScale(scaleX, scaleY);
        matrix.postTranslate(
                horizontal ? source.getWidth() : 0,
                vertical ? source.getHeight() : 0
        );

        Bitmap flippedBitmap = null;
        try {
            flippedBitmap = Bitmap.createBitmap(
                    source,
                    0, 0,
                    source.getWidth(),
                    source.getHeight(),
                    matrix,
                    true
            );

            // 回收原Bitmap
            if (flippedBitmap != source && !source.isRecycled()) {
                source.recycle();
                Log.i(TAG, "Original bitmap recycled after flipping");
            }

            Log.i(TAG, String.format("Bitmap flipped and recycled (H:%s, V:%s): %dx%d",
                    horizontal, vertical,
                    flippedBitmap.getWidth(), flippedBitmap.getHeight()));

        } catch (Exception e) {
            Log.e(TAG, "Error flipping bitmap: " + e.getMessage());
        }

        return flippedBitmap;
    }
}
