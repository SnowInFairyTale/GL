package com.example.gl;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RealTerrainData3 {
    private static final String TAG = "RealTerrainData";

    // 地形配置
    public static final float TERRAIN_SIZE = 100.0f;

    // 顶点类
    public static class Vertex {
        public float x, y, z;
        public float r, g, b;
        public float nx, ny, nz; // 法线
        public float u, v; // 纹理坐标

        public Vertex(float x, float y, float z, float r, float g, float b) {
            this(x, y, z, r, g, b, 0, 0);
        }

        public Vertex(float x, float y, float z, float r, float g, float b, float u, float v) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.r = r;
            this.g = g;
            this.b = b;
            this.u = u;
            this.v = v;
            this.nx = 0.0f;
            this.ny = 1.0f; // 默认朝上的法线
            this.nz = 0.0f;
        }
    }

    public static class MeshData {
        public FloatBuffer vertices;
        public FloatBuffer colors;
        public FloatBuffer normals;
        public FloatBuffer texCoords;
        public IntBuffer indices;        // 三角形索引
        public IntBuffer lineIndices;    // 线框专用索引
        public int vertexCount;
        public int indexCount;
        public int lineIndexCount;
        public float minHeight;
        public float maxHeight;

        // 生成线框索引的方法
        public void generateLineIndices() {
            if (indices == null || indexCount == 0) {
                Log.e(TAG, "无法生成线框索引：没有三角形索引数据");
                return;
            }

            // 使用HashMap避免重复边
            HashMap<Long, Boolean> edgeMap = new HashMap<>();
            List<Integer> lineIndexList = new ArrayList<>();

            for (int i = 0; i < indexCount; i += 3) {
                int i0 = indices.get(i);
                int i1 = indices.get(i + 1);
                int i2 = indices.get(i + 2);

                // 添加三条边，避免重复
                addUniqueEdge(edgeMap, lineIndexList, i0, i1);
                addUniqueEdge(edgeMap, lineIndexList, i1, i2);
                addUniqueEdge(edgeMap, lineIndexList, i2, i0);
            }

            // 创建线框索引缓冲
            lineIndexCount = lineIndexList.size();
            lineIndices = ByteBuffer.allocateDirect(lineIndexCount * 4)
                    .order(ByteOrder.nativeOrder())
                    .asIntBuffer();

            for (int index : lineIndexList) {
                lineIndices.put(index);
            }
            lineIndices.position(0);

            Log.i(TAG, "线框索引生成完成：" + lineIndexCount + "个索引");
        }

        private void addUniqueEdge(HashMap<Long, Boolean> edgeMap, List<Integer> lineIndices,
                                   int a, int b) {
            // 创建有序的边键
            long key;
            int min, max;
            if (a < b) {
                min = a;
                max = b;
                key = ((long)a << 32) | b;
            } else {
                min = b;
                max = a;
                key = ((long)b << 32) | a;
            }

            if (!edgeMap.containsKey(key)) {
                edgeMap.put(key, true);
                lineIndices.add(min);
                lineIndices.add(max);
            }
        }
    }

    /**
     * 双线性插值获取高度值，并应用高度缩放
     */
    private static float bilinearInterpolate(float[][] heightMap, float srcX, float srcY, float heightScale) {
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
        float interpolatedHeight = h1 * (1 - dy) + h2 * dy;

        // 应用高度缩放
        return interpolatedHeight * heightScale;
    }

    /**
     * 从真实的32位浮点数据生成地形网格
     * 修改：使用共享顶点网格
     */
    public static MeshData generateTerrainMeshFromHeightData(float[][] heightData) {
        if (heightData == null || heightData.length == 0) {
            Log.e(TAG, "Invalid height data");
            return null;
        }

        int srcWidth = heightData.length;
        int srcHeight = heightData[0].length;
        int meshWidth = 300;
        int meshHeight = 300;

        Log.i(TAG, String.format("Generating terrain from real data: %dx%d -> %dx%d with interpolation",
                srcWidth, srcHeight, meshWidth, meshHeight));

        // 计算真实的高度范围
        float minHeight = Float.MAX_VALUE;
        float maxHeight = Float.MIN_VALUE;
        for (int i = 0; i < srcWidth; i++) {
            for (int j = 0; j < srcHeight; j++) {
                float h = heightData[i][j];
                minHeight = Math.min(minHeight, h);
                maxHeight = Math.max(maxHeight, h);
            }
        }

        Log.i(TAG, String.format("Real height range: %.3f to %.3f", minHeight, maxHeight));

        // 计算源数据的宽高比和目标网格的宽高比
        float srcAspect = (float) srcWidth / srcHeight;
        float meshAspect = (float) meshWidth / meshHeight;

        // 计算在目标网格中的有效区域
        int effectiveMeshWidth, effectiveMeshHeight;
        int offsetX = 0, offsetZ = 0;
        boolean isWidthFilled;

        if (srcAspect > meshAspect) {
            effectiveMeshWidth = meshWidth;
            effectiveMeshHeight = (int) (meshWidth / srcAspect);
            offsetZ = (meshHeight - effectiveMeshHeight) / 2;
            isWidthFilled = true;
        } else {
            effectiveMeshHeight = meshHeight;
            effectiveMeshWidth = (int) (meshHeight * srcAspect);
            offsetX = (meshWidth - effectiveMeshWidth) / 2;
            isWidthFilled = false;
        }

        Log.i(TAG, String.format("Effective mesh area: %dx%d, offset: (%d, %d), widthFilled: %b",
                effectiveMeshWidth, effectiveMeshHeight, offsetX, offsetZ, isWidthFilled));

        // 计算高度缩放比例
        float heightScale = isWidthFilled ?
                (float) effectiveMeshWidth / srcWidth :
                (float) effectiveMeshHeight / srcHeight;

        Log.i(TAG, String.format("Height scale: %.3f", heightScale));

        // ================== 关键修改：创建共享顶点网格 ==================
        // 创建顶点网格数组
        Vertex[][] vertexGrid = new Vertex[meshWidth][meshHeight];
        List<Vertex> vertexList = new ArrayList<>();
        List<Integer> indexList = new ArrayList<>();

        // 第一步：创建所有顶点
        for (int i = 0; i < meshWidth; i++) {
            for (int j = 0; j < meshHeight; j++) {
                // 检查是否在有效区域内
                boolean inEffectiveArea = (i >= offsetX && i < offsetX + effectiveMeshWidth &&
                        j >= offsetZ && j < offsetZ + effectiveMeshHeight);

                float x = (i / (float) (meshWidth - 1) - 0.5f) * TERRAIN_SIZE;
                float z = (j / (float) (meshHeight - 1) - 0.5f) * TERRAIN_SIZE;
                float y;
                float u, v;

                if (inEffectiveArea) {
                    // 在有效区域内
                    float srcX = ((float)(i - offsetX) / (effectiveMeshWidth - 1)) * (srcWidth - 1);
                    float srcY = ((float)(j - offsetZ) / (effectiveMeshHeight - 1)) * (srcHeight - 1);
                    y = bilinearInterpolate(heightData, srcX, srcY, heightScale);
                    u = (float) (i - offsetX) / (effectiveMeshWidth - 1);
                    v = (float) (j - offsetZ) / (effectiveMeshHeight - 1);
                } else {
                    // 在留白区域
                    y = 0.0f;
                    u = 0.0f;
                    v = 0.0f;
                }

                // 创建顶点
                float[] color = {1.0f, 1.0f, 1.0f};
                float[] normal = {0.0f, 1.0f, 0.0f};
                Vertex vertex = new Vertex(x, y, z, color[0], color[1], color[2], u, v);
                vertex.nx = normal[0];
                vertex.ny = normal[1];
                vertex.nz = normal[2];

                vertexGrid[i][j] = vertex;
                vertexList.add(vertex);
            }
        }

        // 第二步：创建三角形索引（共享顶点）
        for (int i = 0; i < meshWidth - 1; i++) {
            for (int j = 0; j < meshHeight - 1; j++) {
                // 计算四个顶点的索引
                int v00 = i * meshHeight + j;           // 当前行当前列
                int v10 = (i + 1) * meshHeight + j;     // 下一行当前列
                int v11 = (i + 1) * meshHeight + (j + 1); // 下一行下一列
                int v01 = i * meshHeight + (j + 1);     // 当前行下一列

                // 第一个三角形：v00 -> v10 -> v11
                indexList.add(v00);
                indexList.add(v10);
                indexList.add(v11);

                // 第二个三角形：v00 -> v11 -> v01
                indexList.add(v00);
                indexList.add(v11);
                indexList.add(v01);
            }
        }

        // 重新计算缩放后的高度范围
        float scaledMinHeight = minHeight * heightScale;
        float scaledMaxHeight = maxHeight * heightScale;
        Log.i(TAG, String.format("Scaled height range: %.3f to %.3f", scaledMinHeight, scaledMaxHeight));

        // 转换为FloatBuffer
        return createMeshData(vertexList, indexList, Math.min(scaledMinHeight, 0), scaledMaxHeight);
    }

    /**
     * 创建网格数据
     */
    private static MeshData createMeshData(List<Vertex> vertices, List<Integer> indices,
                                           float minHeight, float maxHeight) {
        MeshData meshData = new MeshData();
        meshData.vertexCount = vertices.size();
        meshData.indexCount = indices.size();

        // 创建顶点缓冲区
        float[] vertexArray = new float[vertices.size() * 3];
        float[] colorArray = new float[vertices.size() * 3];
        float[] normalArray = new float[vertices.size() * 3];
        float[] texCoordArray = new float[vertices.size() * 2];

        for (int i = 0; i < vertices.size(); i++) {
            Vertex v = vertices.get(i);
            vertexArray[i * 3] = v.x;
            vertexArray[i * 3 + 1] = v.y;
            vertexArray[i * 3 + 2] = v.z;

            colorArray[i * 3] = v.r;
            colorArray[i * 3 + 1] = v.g;
            colorArray[i * 3 + 2] = v.b;

            normalArray[i * 3] = v.nx;
            normalArray[i * 3 + 1] = v.ny;
            normalArray[i * 3 + 2] = v.nz;

            texCoordArray[i * 2] = v.u;
            texCoordArray[i * 2 + 1] = v.v;
        }

        meshData.vertices = createFloatBuffer(vertexArray);
        meshData.colors = createFloatBuffer(colorArray);
        meshData.normals = createFloatBuffer(normalArray);
        meshData.texCoords = createFloatBuffer(texCoordArray);

        // 创建索引缓冲区
        meshData.indices = createIntBuffer(indices);

        meshData.minHeight = minHeight;
        meshData.maxHeight = maxHeight;

        Log.i(TAG, String.format("Mesh created: %d vertices, %d indices, height range: %.3f to %.3f",
                meshData.vertexCount, meshData.indexCount, minHeight, maxHeight));

        // 生成线框索引
        meshData.generateLineIndices();

        return meshData;
    }

    private static FloatBuffer createFloatBuffer(float[] array) {
        ByteBuffer bb = ByteBuffer.allocateDirect(array.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer buffer = bb.asFloatBuffer();
        buffer.put(array);
        buffer.position(0);
        return buffer;
    }

    private static IntBuffer createIntBuffer(List<Integer> list) {
        ByteBuffer bb = ByteBuffer.allocateDirect(list.size() * 4);
        bb.order(ByteOrder.nativeOrder());
        IntBuffer buffer = bb.asIntBuffer();
        for (int value : list) {
            buffer.put(value);
        }
        buffer.position(0);
        return buffer;
    }
}