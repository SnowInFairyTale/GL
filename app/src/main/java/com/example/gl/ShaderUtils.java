package com.example.gl;

// ShaderUtils.java
import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.IntBuffer;

public class ShaderUtils {
    private static final String TAG = "ShaderUtils";

    public static String loadShader(Context context, int resourceId) {
        StringBuilder shaderSource = new StringBuilder();
        try {
            InputStream inputStream = context.getResources().openRawResource(resourceId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                shaderSource.append(line).append("\n");
            }
            reader.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not read shader: " + e.getMessage());
        }
        return shaderSource.toString();
    }

    public static int createProgram(String vertexShaderCode, String fragmentShaderCode) {
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);

        if (vertexShader == 0 || fragmentShader == 0) {
            return 0;
        }

        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);

        // 修正：使用 IntBuffer 检查链接状态
        IntBuffer linkStatus = IntBuffer.allocate(1);
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus);
        if (linkStatus.get(0) != GLES30.GL_TRUE) {
            Log.e(TAG, "Could not link program: " + GLES30.glGetProgramInfoLog(program));
            GLES30.glDeleteProgram(program);
            return 0;
        }

        // 清理着色器对象
        GLES30.glDeleteShader(vertexShader);
        GLES30.glDeleteShader(fragmentShader);

        return program;
    }

    private static int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);

        // 修正：使用 IntBuffer 检查编译状态
        IntBuffer compileStatus = IntBuffer.allocate(1);
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus);
        if (compileStatus.get(0) != GLES30.GL_TRUE) {
            Log.e(TAG, "Could not compile shader: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    // 添加检查GL错误的方法
    public static void checkGLError(String operation) {
        int error;
        while ((error = GLES30.glGetError()) != GLES30.GL_NO_ERROR) {
            Log.e(TAG, operation + ": glError " + error);
        }
    }
}
