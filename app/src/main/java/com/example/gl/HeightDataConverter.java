package com.example.gl;

import android.util.Log;

public class HeightDataConverter {
    private static final String TAG = "HeightDataConverter";

    /**
     * 将高度数据转换为正方形，保持原始比例，留白位置用NaN填充
     * @param heightData 原始高度数据 [width][height]
     * @param targetSize 目标正方形尺寸
     * @return 转换后的正方形高度数据
     */
    public static float[][] convertToSquare(float[][] heightData, int targetSize) {
        if (heightData == null || heightData.length == 0) {
            Log.e(TAG, "Invalid height data");
            return null;
        }

        int srcWidth = heightData.length;
        int srcHeight = heightData[0].length;

        Log.i(TAG, String.format("Converting height data: %dx%d -> %dx%d",
                srcWidth, srcHeight, targetSize, targetSize));

        // 创建目标正方形数组，初始化为NaN
        float[][] squareData = new float[targetSize][targetSize];
        initializeWithNaN(squareData);

        // 计算源数据的宽高比
        float srcAspect = (float) srcWidth / srcHeight;
        float targetAspect = 1.0f; // 正方形宽高比为1

        // 计算在目标正方形中的有效区域
        int effectiveWidth, effectiveHeight;
        int offsetX = 0, offsetY = 0;

        if (srcAspect > targetAspect) {
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

        // 使用双线性插值将源数据映射到目标正方形的有效区域
        for (int i = 0; i < effectiveWidth; i++) {
            for (int j = 0; j < effectiveHeight; j++) {
                // 计算源数据中的对应坐标（浮点数用于插值）
                float srcX = mapToSourceCoord(i, effectiveWidth, srcWidth);
                float srcY = mapToSourceCoord(j, effectiveHeight, srcHeight);

                // 使用双线性插值获取高度值
                float height = bilinearInterpolate(heightData, srcX, srcY);

                // 将结果放入目标数组的对应位置
                int targetX = offsetX + i;
                int targetY = offsetY + j;
                squareData[targetX][targetY] = height;
            }
        }

        Log.i(TAG, "Height data conversion completed");
        return squareData;
    }

    /**
     * 将坐标映射到源数据坐标（返回浮点数用于插值）
     */
    private static float mapToSourceCoord(int targetCoord, int targetSize, int srcSize) {
        return ((float) targetCoord / (targetSize - 1)) * (srcSize - 1);
    }

    /**
     * 双线性插值获取高度值
     */
    private static float bilinearInterpolate(float[][] heightMap, float srcX, float srcY) {
        int x1 = (int) Math.floor(srcX);
        int y1 = (int) Math.floor(srcY);
        int x2 = Math.min(x1 + 1, heightMap.length - 1);
        int y2 = Math.min(y1 + 1, heightMap[0].length - 1);

        float dx = srcX - x1;
        float dy = srcY - y1;

        // 四个相邻点的高度
        float h11 = heightMap[x1][y1];
        float h12 = heightMap[x1][y2];
        float h21 = heightMap[x2][y1];
        float h22 = heightMap[x2][y2];

        // 双线性插值
        float h1 = h11 * (1 - dx) + h21 * dx;
        float h2 = h12 * (1 - dx) + h22 * dx;
        return h1 * (1 - dy) + h2 * dy;
    }

    /**
     * 使用NaN初始化数组
     */
    private static void initializeWithNaN(float[][] array) {
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                array[i][j] = Float.NaN;
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

    /**
     * 专门用于357×203转357×357的便捷方法
     */
    public static float[][] convert357x203To357x357(float[][] heightData) {
        return convertToSquare(heightData, 357);
    }
}
