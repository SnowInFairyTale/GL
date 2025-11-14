package com.example.gl;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.beyka.tiffbitmapfactory.TiffBitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class TiffBitmapFactoryConverter {
    private static final String TAG = "TiffBitmapFactoryConverter";

    public static Bitmap convert32BitTIFF(Context context, int resourceId) {
        try {
            // 获取资源路径
            String resourcePath = getResourcePath(context, resourceId);

            File file = new File(resourcePath);
            Log.e(TAG, "file length=" + file.length());

            // 使用TiffBitmapFactory解码32位TIFF
//            TiffBitmapFactory.Options options = new TiffBitmapFactory.Options();
//            options.inPreferredConfig = TiffBitmapFactory.ImageConfig.ALPHA_8;
//            options.inSampleSize
            Log.e(TAG, "path:" + resourcePath);
//            TiffBitmapFactory.

            Bitmap bitmap = TiffBitmapFactory.decodeFile(file);

            if (bitmap != null) {
                Log.i(TAG, "32-bit TIFF converted successfully: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                return bitmap;
            } else {
                Log.i(TAG, "bitmap=null");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error with TiffBitmapFactory", e);
        }
        return null;
    }

    private static String getResourcePath(Context context, int resourceId) {
        // 将资源复制到临时文件
        try {
            File tempFile = File.createTempFile("tiff_temp", ".tiff", context.getCacheDir());
            InputStream inputStream = context.getResources().openRawResource(resourceId);
            FileOutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            return tempFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "Error creating temp file", e);
            return null;
        }
    }
}
