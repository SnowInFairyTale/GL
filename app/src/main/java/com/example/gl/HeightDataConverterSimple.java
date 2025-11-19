package com.example.gl;

import android.util.Log;

public class HeightDataConverterSimple {
    private static final String TAG = "HeightDataConverter";

    /**
     * 将高度数据转换为正方形，保持原始比例，留白位置用NaN填充
     * 自动选择正方形尺寸为宽度和高度的较大值
     * @param heightData 原始高度数据 [width][height]
     * @return 转换后的正方形高度数据
     */
    public static float[][] convertToSquare(float[][] heightData) {
        if (heightData == null || heightData.length == 0) {
            Log.e(TAG, "Invalid height data");
            return null;
        }

        int srcWidth = heightData.length;
        int srcHeight = heightData[0].length;

        // 自动选择正方形尺寸为宽度和高度的较大值
        int targetSize = Math.max(srcWidth, srcHeight);

        Log.i(TAG, String.format("Converting height data: %dx%d -> %dx%d",
                srcWidth, srcHeight, targetSize, targetSize));

        // 创建目标正方形数组，初始化为NaN
        float[][] squareData = new float[targetSize][targetSize];
        initializeWithNaN(squareData);

        // 计算源数据的宽高比
        float srcAspect = (float) srcWidth / srcHeight;

        // 计算在目标正方形中的有效区域
        int effectiveWidth, effectiveHeight;
        int offsetX = 0, offsetY = 0;

        if (srcAspect > 1.0f) {
            // 源数据更宽，在宽度方向填满，高度方向留白
            effectiveWidth = targetSize;
            effectiveHeight = (int) (targetSize / srcAspect);
            offsetY = (targetSize - effectiveHeight) / 2;
        } else {
            // 源数据更高，在高度方向填满，宽度方向留白
            effectiveHeight = targetSize;
            effectiveWidth = (int) (targetSize * srcAspect);
            offsetX = (targetSize - effectiveWidth) / 2;
        }

        Log.i(TAG, String.format("Effective area: %dx%d, offset: (%d, %d)",
                effectiveWidth, effectiveHeight, offsetX, offsetY));

        // 直接复制原始数据到有效区域，不需要插值！
        for (int i = 0; i < Math.min(effectiveWidth, srcWidth); i++) {
            for (int j = 0; j < Math.min(effectiveHeight, srcHeight); j++) {
                int targetX = offsetX + i;
                int targetY = offsetY + j;
                squareData[targetX][targetY] = heightData[i][j];
            }
        }

        Log.i(TAG, "Height data conversion completed");
        return squareData;
    }

    /**
     * 使用NaN初始化数组
     */
    private static void initializeWithNaN(float[][] array) {
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                array[i][j] = 0.0f;
            }
        }
    }

    /**
     * 统计有效数据点数量（非NaN的点）
     */
    public static int countValidDataPoints(float[][] data) {
        if (data == null) return 0;

        int count = 0;
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                if (!Float.isNaN(data[i][j])) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 计算有效数据区域的高度范围
     */
    public static float[] calculateValidHeightRange(float[][] data) {
        if (data == null) return new float[]{0, 0};

        float minHeight = Float.MAX_VALUE;
        float maxHeight = Float.MIN_VALUE;
        boolean hasValidData = false;

        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                float height = data[i][j];
                if (!Float.isNaN(height)) {
                    minHeight = Math.min(minHeight, height);
                    maxHeight = Math.max(maxHeight, height);
                    hasValidData = true;
                }
            }
        }

        if (!hasValidData) {
            return new float[]{0, 0};
        }

        return new float[]{minHeight, maxHeight};
    }

    /**
     * 打印数据统计信息
     */
    public static void printDataInfo(float[][] data, String name) {
        if (data == null) {
            Log.i(TAG, name + ": null");
            return;
        }

        int validCount = countValidDataPoints(data);
        int totalCount = data.length * data[0].length;
        float validRatio = (float) validCount / totalCount * 100;

        float[] range = calculateValidHeightRange(data);

        Log.i(TAG, String.format("%s: %dx%d, valid: %d/%d (%.1f%%), height range: [%.3f, %.3f]",
                name, data.length, data[0].length, validCount, totalCount, validRatio,
                range[0], range[1]));
    }

    /**
     * 将NaN值替换为指定值
     */
    public static float[][] replaceNaNWithValue(float[][] data, float replacement) {
        if (data == null) return null;

        float[][] result = new float[data.length][data[0].length];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                if (Float.isNaN(data[i][j])) {
                    result[i][j] = replacement;
                } else {
                    result[i][j] = data[i][j];
                }
            }
        }
        return result;
    }
}
