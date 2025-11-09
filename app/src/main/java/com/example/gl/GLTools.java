package com.example.gl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

public class GLTools {

    public static int loadTexture(Context context, int resourceId) {
        int[] textureHandle = new int[1];
        GLES30.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] == 0) {
            Log.e("GLRenderer", "Failed to generate texture");
            return 0;
        }

        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);
            if (bitmap == null) {
                Log.e("GLRenderer", "Failed to decode texture resource: " + resourceId);
                GLES30.glDeleteTextures(1, textureHandle, 0);
                return 0;
            }

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureHandle[0]);

            // 设置纹理参数
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);

            // 使用 GLUtils 加载纹理
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);

            Log.i("GLRenderer", "Texture loaded: " + resourceId + " (" +
                    bitmap.getWidth() + "x" + bitmap.getHeight() + ")");

        } catch (Exception e) {
            Log.e("GLRenderer", "Error loading texture: " + e.getMessage());
            if (textureHandle[0] != 0) {
                GLES30.glDeleteTextures(1, textureHandle, 0);
            }
            return 0;
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }

        return textureHandle[0];
    }
}
