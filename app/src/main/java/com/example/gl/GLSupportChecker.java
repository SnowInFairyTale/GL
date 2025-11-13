package com.example.gl;

import android.opengl.GLES30;
import android.util.Log;

public class GLSupportChecker {
    private static final String TAG = "GLSupportChecker";

    // 检查是否支持曲面细分着色器
    public static boolean supportsTessellation() {
        try {
            String extensions = GLES30.glGetString(GLES30.GL_EXTENSIONS);
            if (extensions != null) {
                boolean supported = extensions.contains("GL_EXT_tessellation_shader") ||
                        extensions.contains("GL_OES_tessellation_shader");
                Log.i(TAG, "Tessellation support: " + supported);
                return supported;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking tessellation support", e);
        }
        return false;
    }

    // 检查是否支持几何着色器
    public static boolean supportsGeometryShader() {
        try {
            String extensions = GLES30.glGetString(GLES30.GL_EXTENSIONS);
            if (extensions != null) {
                boolean supported = extensions.contains("GL_EXT_geometry_shader") ||
                        extensions.contains("GL_OES_geometry_shader");
                Log.i(TAG, "Geometry shader support: " + supported);
                return supported;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking geometry shader support", e);
        }
        return false;
    }

    // 检查是否支持计算着色器
    public static boolean supportsComputeShader() {
        try {
            String extensions = GLES30.glGetString(GLES30.GL_EXTENSIONS);
            if (extensions != null) {
                boolean supported = extensions.contains("GL_EXT_compute_shader");
                Log.i(TAG, "Compute shader support: " + supported);
                return supported;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking compute shader support", e);
        }
        return false;
    }

    // 获取支持的OpenGL ES版本
    public static String getGLVersion() {
        try {
            String version = GLES30.glGetString(GLES30.GL_VERSION);
            Log.i(TAG, "OpenGL ES version: " + version);
            return version != null ? version : "Unknown";
        } catch (Exception e) {
            Log.e(TAG, "Error getting GL version", e);
            return "Error";
        }
    }

    // 获取渲染器信息
    public static String getGLRenderer() {
        try {
            String renderer = GLES30.glGetString(GLES30.GL_RENDERER);
            return renderer != null ? renderer : "Unknown";
        } catch (Exception e) {
            Log.e(TAG, "Error getting GL renderer", e);
            return "Error";
        }
    }

    // 获取供应商信息
    public static String getGLVendor() {
        try {
            String vendor = GLES30.glGetString(GLES30.GL_VENDOR);
            return vendor != null ? vendor : "Unknown";
        } catch (Exception e) {
            Log.e(TAG, "Error getting GL vendor", e);
            return "Error";
        }
    }

    // 检查是否支持ASTC纹理压缩
    public static boolean supportsASTCTexture() {
        try {
            String extensions = GLES30.glGetString(GLES30.GL_EXTENSIONS);
            if (extensions != null) {
                return extensions.contains("GL_KHR_texture_compression_astc_ldr");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking ASTC support", e);
        }
        return false;
    }

    // 获取最大纹理大小
    public static int getMaxTextureSize() {
        try {
            int[] maxSize = new int[1];
            GLES30.glGetIntegerv(GLES30.GL_MAX_TEXTURE_SIZE, maxSize, 0);
            Log.i(TAG, "Max texture size: " + maxSize[0]);
            return maxSize[0];
        } catch (Exception e) {
            Log.e(TAG, "Error getting max texture size", e);
            return 2048; // 安全默认值
        }
    }

    // 获取最大顶点属性数量
    public static int getMaxVertexAttribs() {
        try {
            int[] maxAttribs = new int[1];
            GLES30.glGetIntegerv(GLES30.GL_MAX_VERTEX_ATTRIBS, maxAttribs, 0);
            Log.i(TAG, "Max vertex attributes: " + maxAttribs[0]);
            return maxAttribs[0];
        } catch (Exception e) {
            Log.e(TAG, "Error getting max vertex attributes", e);
            return 8; // 安全默认值
        }
    }

    // 综合能力报告
    public static String getCapabilityReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== OpenGL ES Capability Report ===\n");
        report.append("Version: ").append(getGLVersion()).append("\n");
        report.append("Renderer: ").append(getGLRenderer()).append("\n");
        report.append("Vendor: ").append(getGLVendor()).append("\n");
        report.append("Tessellation: ").append(supportsTessellation() ? "YES" : "NO").append("\n");
        report.append("Geometry Shader: ").append(supportsGeometryShader() ? "YES" : "NO").append("\n");
        report.append("Compute Shader: ").append(supportsComputeShader() ? "YES" : "NO").append("\n");
        report.append("ASTC Texture: ").append(supportsASTCTexture() ? "YES" : "NO").append("\n");
        report.append("Max Texture Size: ").append(getMaxTextureSize()).append("\n");
        report.append("Max Vertex Attributes: ").append(getMaxVertexAttribs()).append("\n");

        return report.toString();
    }

    // 检查是否支持高级地形特性
    public static boolean supportsAdvancedTerrain() {
        return supportsTessellation() || supportsGeometryShader();
    }

    // 获取推荐的地形质量级别
    public static TerrainQuality getRecommendedTerrainQuality() {
        if (supportsTessellation()) {
            return TerrainQuality.HIGH;
        } else if (getMaxTextureSize() >= 4096 && getMaxVertexAttribs() >= 16) {
            return TerrainQuality.MEDIUM;
        } else {
            return TerrainQuality.LOW;
        }
    }

    public enum TerrainQuality {
        LOW,    // 基础网格 + 简单光照
        MEDIUM, // 密集网格 + 法线贴图
        HIGH    // 动态细分 + 高级特效
    }
}
