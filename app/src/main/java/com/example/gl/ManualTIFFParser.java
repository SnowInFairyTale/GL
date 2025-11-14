package com.example.gl;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ManualTIFFParser {
    private static final String TAG = "ManualTIFFParser";

    // TIFF标签常量
    private static final int TAG_IMAGE_WIDTH = 256;
    private static final int TAG_IMAGE_HEIGHT = 257;
    private static final int TAG_BITS_PER_SAMPLE = 258;
    private static final int TAG_COMPRESSION = 259;
    private static final int TAG_PHOTOMETRIC_INTERPRETATION = 262;
    private static final int TAG_STRIP_OFFSETS = 273;
    private static final int TAG_SAMPLES_PER_PIXEL = 277;
    private static final int TAG_ROWS_PER_STRIP = 278;
    private static final int TAG_STRIP_BYTE_COUNTS = 279;

    // 解析结果容器
    public static class TIFFParseResult {
        public Bitmap bitmap;
        public float[][] heightData; // 原始32位浮点数据
        public int width;
        public int height;
        public float minValue = Float.MAX_VALUE;
        public float maxValue = Float.MIN_VALUE;

        @Override
        public String toString() {
            return "TIFFParseResult{" +
                    "bitmap=" + bitmap +
                    ", heightData=" + heightData.length +
                    ", width=" + width +
                    ", height=" + height +
                    ", minValue=" + minValue +
                    ", maxValue=" + maxValue +
                    '}';
        }
    }

    public static TIFFParseResult parseTIFFToBitmapAndData(Context context, int resourceId) {
        try {
            InputStream inputStream = context.getResources().openRawResource(resourceId);
            byte[] tiffData = readFully(inputStream);
            return parseTIFFData(tiffData);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing TIFF", e);
            return null;
        }
    }

    // 保持原有方法兼容性
    public static TIFFParseResult parseTIFFToBitmap(Context context, int resourceId) {
        TIFFParseResult result = parseTIFFToBitmapAndData(context, resourceId);
        return result;
    }

    private static byte[] readFully(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    private static TIFFParseResult parseTIFFData(byte[] data) {
        if (data.length < 8) {
            Log.e(TAG, "TIFF file too small");
            return null;
        }

        // 读取字节顺序
        boolean isLittleEndian = (data[0] == 0x49 && data[1] == 0x49);

        // 读取TIFF标识
        int tiffIdentifier = readShort(data, 2, isLittleEndian);
        if (tiffIdentifier != 42) {
            Log.e(TAG, "Invalid TIFF file - identifier: " + tiffIdentifier);
            return null;
        }

        // 读取第一个IFD偏移量
        long firstIFDOffset = readLong(data, 4, isLittleEndian);

        // 解析IFD
        TIFFInfo tiffInfo = parseIFD(data, (int) firstIFDOffset, isLittleEndian);

        if (tiffInfo == null) {
            Log.e(TAG, "Failed to parse IFD");
            return null;
        }

        Log.i(TAG, String.format("TIFF Info: %dx%d, %d bits, %d samples",
                tiffInfo.width, tiffInfo.height, tiffInfo.bitsPerSample, tiffInfo.samplesPerPixel));

        // 检查是否为32位数据
        if (tiffInfo.bitsPerSample != 32) {
            Log.w(TAG, "Not 32-bit TIFF, bits per sample: " + tiffInfo.bitsPerSample);
        }

        // 读取图像数据和原始浮点数据
        return readImageDataAndHeight(data, tiffInfo, isLittleEndian);
    }

    private static TIFFInfo parseIFD(byte[] data, int ifdOffset, boolean isLittleEndian) {
        if (ifdOffset + 2 > data.length) {
            Log.e(TAG, "IFD offset out of bounds");
            return null;
        }

        // 读取IFD条目数量
        int entryCount = readShort(data, ifdOffset, isLittleEndian);
        TIFFInfo info = new TIFFInfo();

        int entryPos = ifdOffset + 2;

        for (int i = 0; i < entryCount; i++) {
            if (entryPos + 12 > data.length) {
                Log.e(TAG, "IFD entry out of bounds");
                break;
            }

            int tag = readShort(data, entryPos, isLittleEndian);
            int type = readShort(data, entryPos + 2, isLittleEndian);
            long count = readLong(data, entryPos + 4, isLittleEndian);
            long valueOffset = readLong(data, entryPos + 8, isLittleEndian);

            switch (tag) {
                case TAG_IMAGE_WIDTH:
                    info.width = (int) getValue(data, type, valueOffset, count, isLittleEndian);
                    break;
                case TAG_IMAGE_HEIGHT:
                    info.height = (int) getValue(data, type, valueOffset, count, isLittleEndian);
                    break;
                case TAG_BITS_PER_SAMPLE:
                    info.bitsPerSample = (int) getValue(data, type, valueOffset, count, isLittleEndian);
                    break;
                case TAG_SAMPLES_PER_PIXEL:
                    info.samplesPerPixel = (int) getValue(data, type, valueOffset, count, isLittleEndian);
                    break;
                case TAG_STRIP_OFFSETS:
                    if (count == 1) {
                        info.stripOffsets = new long[] { valueOffset };
                    } else {
                        info.stripOffsets = readLongArray(data, (int) valueOffset, (int) count, isLittleEndian);
                    }
                    break;
                case TAG_STRIP_BYTE_COUNTS:
                    if (count == 1) {
                        info.stripByteCounts = new long[] { valueOffset };
                    } else {
                        info.stripByteCounts = readLongArray(data, (int) valueOffset, (int) count, isLittleEndian);
                    }
                    break;
                case TAG_PHOTOMETRIC_INTERPRETATION:
                    info.photometricInterpretation = (int) valueOffset;
                    break;
            }

            entryPos += 12;
        }

        return info;
    }

    private static TIFFParseResult readImageDataAndHeight(byte[] data, TIFFInfo info, boolean isLittleEndian) {
        if (info.stripOffsets == null || info.stripOffsets.length == 0) {
            Log.e(TAG, "No strip offsets found");
            return null;
        }

        int dataOffset = (int) info.stripOffsets[0];

        // 创建结果对象
        TIFFParseResult result = new TIFFParseResult();
        result.width = info.width;
        result.height = info.height;

        // 初始化高度数据数组
        result.heightData = new float[info.height][info.width];
        Bitmap bitmap = Bitmap.createBitmap(info.width, info.height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[info.width * info.height];

        int pixelIndex = 0;

        for (int y = 0; y < info.height; y++) {
            for (int x = 0; x < info.width; x++) {
                if (dataOffset + 3 >= data.length) {
                    Log.w(TAG, "Reached end of data at position: " + dataOffset);
                    break;
                }

                if (info.bitsPerSample == 32) {
                    // 读取32位浮点数
                    int intValue = readInt(data, dataOffset, isLittleEndian);
                    float floatValue = Float.intBitsToFloat(intValue);

                    // 存储原始浮点数据
                    result.heightData[y][x] = floatValue;

                    // 更新最小最大值
                    if (floatValue < result.minValue) result.minValue = floatValue;
                    if (floatValue > result.maxValue) result.maxValue = floatValue;

                    // 转换为ARGB用于Bitmap显示
                    pixels[pixelIndex] = convertFloatToARGB(floatValue);
                    dataOffset += 4;

                } else if (info.bitsPerSample == 16) {
                    // 16位数据
                    int value = readShort(data, dataOffset, isLittleEndian) & 0xFFFF;
                    float floatValue = value / 65535.0f; // 归一化到0-1
                    result.heightData[y][x] = floatValue;
                    pixels[pixelIndex] = convert16BitToARGB(value);
                    dataOffset += 2;

                } else if (info.bitsPerSample == 8) {
                    // 8位数据
                    int value = data[dataOffset] & 0xFF;
                    float floatValue = value / 255.0f; // 归一化到0-1
                    result.heightData[y][x] = floatValue;
                    pixels[pixelIndex] = convert8BitToARGB(value);
                    dataOffset += 1;
                } else {
                    // 默认处理
                    result.heightData[y][x] = 0f;
                    pixels[pixelIndex] = 0xFF000000;
                    dataOffset += info.bitsPerSample / 8;
                }

                pixelIndex++;
            }
        }

        bitmap.setPixels(pixels, 0, info.width, 0, 0, info.width, info.height);
        result.bitmap = bitmap;

        // 记录数据统计信息
        Log.i(TAG, String.format("Height data range: %.6f to %.6f",
                result.minValue, result.maxValue));
        Log.i(TAG, String.format("Height data dimensions: %dx%d",
                result.heightData[0].length, result.heightData.length));

        return result;
    }

    // 读取工具方法
    private static int readShort(byte[] data, int offset, boolean isLittleEndian) {
        if (offset + 1 >= data.length) return 0;

        if (isLittleEndian) {
            return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
        } else {
            return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
        }
    }

    private static long readLong(byte[] data, int offset, boolean isLittleEndian) {
        if (offset + 3 >= data.length) return 0;

        if (isLittleEndian) {
            return (data[offset] & 0xFF) |
                    ((data[offset + 1] & 0xFF) << 8) |
                    ((data[offset + 2] & 0xFF) << 16) |
                    ((long) (data[offset + 3] & 0xFF) << 24);
        } else {
            return ((long) (data[offset] & 0xFF) << 24) |
                    ((data[offset + 1] & 0xFF) << 16) |
                    ((data[offset + 2] & 0xFF) << 8) |
                    (data[offset + 3] & 0xFF);
        }
    }

    private static int readInt(byte[] data, int offset, boolean isLittleEndian) {
        return (int) readLong(data, offset, isLittleEndian);
    }

    private static long[] readLongArray(byte[] data, int offset, int count, boolean isLittleEndian) {
        long[] array = new long[count];
        for (int i = 0; i < count; i++) {
            array[i] = readLong(data, offset + i * 4, isLittleEndian);
        }
        return array;
    }

    private static long getValue(byte[] data, int type, long valueOffset, long count, boolean isLittleEndian) {
        switch (type) {
            case 1: // BYTE
                return valueOffset & 0xFF;
            case 3: // SHORT
                return valueOffset & 0xFFFF;
            case 4: // LONG
                return valueOffset;
            case 11: // FLOAT
                return (long) Float.intBitsToFloat((int) valueOffset);
            default:
                return valueOffset;
        }
    }

    // 像素转换方法
    private static int convertFloatToARGB(float value) {
        // 将浮点数转换为灰度
        // 使用自动范围调整，避免硬编码的0-1范围
        float normalized = (value + 1.0f) / 2.0f; // 假设数据在-1到1之间
        normalized = clamp(normalized, 0.0f, 1.0f);
        int intensity = (int) (normalized * 255);
        return 0xFF000000 | (intensity << 16) | (intensity << 8) | intensity;
    }

    private static int convert16BitToARGB(int value) {
        int intensity = (value >> 8) & 0xFF; // 取高8位
        return 0xFF000000 | (intensity << 16) | (intensity << 8) | intensity;
    }

    private static int convert8BitToARGB(int value) {
        return 0xFF000000 | (value << 16) | (value << 8) | value;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    // TIFF信息容器类
    private static class TIFFInfo {
        int width;
        int height;
        int bitsPerSample = 8;
        int samplesPerPixel = 1;
        int photometricInterpretation;
        long[] stripOffsets;
        long[] stripByteCounts;
    }
}
