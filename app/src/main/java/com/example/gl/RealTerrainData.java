package com.example.gl;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RealTerrainData {
    private static final String TAG = "RealTerrainData";

    // 地形配置
    public static final float TERRAIN_SIZE = 100.0f;

    // 顶点类
    public static class Vertex {
        public float x, y, z;
        public float r, g, b;
        public float nx, ny, nz; // 法线
        public int type; // 地形类型
        public float u, v; // 纹理坐标

        public Vertex(float x, float y, float z, float r, float g, float b, int type) {
            this(x, y, z, r, g, b, type, 0, 0);
        }

        public Vertex(float x, float y, float z, float r, float g, float b, int type, float u, float v) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.r = r;
            this.g = g;
            this.b = b;
            this.type = type;
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
        public IntBuffer types;
        public int vertexCount;
        public float minHeight;
        public float maxHeight;
    }

    /**
     * 从真实的32位浮点数据生成地形网格
     */
    public static MeshData generateTerrainMeshFromHeightData(float[][] heightData) {
        if (heightData == null || heightData.length == 0) {
            Log.e(TAG, "Invalid height data");
            return null;
        }

        int width = heightData.length;
        int height = heightData[0].length;

        Log.i(TAG, String.format("Generating terrain from real data: %dx%d", width, height));

        List<Vertex> vertexList = new ArrayList<>();

        // 计算真实的高度范围
        float minHeight = Float.MAX_VALUE;
        float maxHeight = Float.MIN_VALUE;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                float h = heightData[i][j];
                minHeight = Math.min(minHeight, h);
                maxHeight = Math.max(maxHeight, h);
            }
        }

        Log.i(TAG, String.format("Real height range: %.3f to %.3f", minHeight, maxHeight));

        // 生成网格顶点 - 顶点顺序为逆时针
        for (int i = 0; i < width - 1; i++) {
            for (int j = 0; j < height - 1; j++) {
                // 两个三角形组成一个四边形
                addQuad(vertexList, heightData, i, j, i + 1, j, i, j + 1, width, height);
                addQuad(vertexList, heightData, i + 1, j, i + 1, j + 1, i, j + 1, width, height);
            }
        }

        // 计算平滑法线
        calculateSmoothNormals(vertexList);

        // 转换为FloatBuffer
        return createMeshData(vertexList, minHeight, maxHeight);
    }

    /**
     * 添加四边形（两个三角形）
     */
    private static void addQuad(List<Vertex> vertices, float[][] heightMap,
                                int i1, int j1, int i2, int j2, int i3, int j3, int width, int height) {
        float x1 = (i1 / (float) (width - 1) - 0.5f) * TERRAIN_SIZE;
        float z1 = (j1 / (float) (height - 1) - 0.5f) * TERRAIN_SIZE;
        float y1 = heightMap[i1][j1];

        float x2 = (i2 / (float) (width - 1) - 0.5f) * TERRAIN_SIZE;
        float z2 = (j2 / (float) (height - 1) - 0.5f) * TERRAIN_SIZE;
        float y2 = heightMap[i2][j2];

        float x3 = (i3 / (float) (width - 1) - 0.5f) * TERRAIN_SIZE;
        float z3 = (j3 / (float) (height - 1) - 0.5f) * TERRAIN_SIZE;
        float y3 = heightMap[i3][j3];

        // 使用默认法线，平滑法线会在后续统一计算
        float[] defaultNormal = {0.0f, 1.0f, 0.0f};

        // 添加三个顶点（一个三角形）- 逆时针顺序
        addVertex(vertices, x1, y1, z1, ElementType.Land, defaultNormal);
        addVertex(vertices, x2, y2, z2, ElementType.Land, defaultNormal);
        addVertex(vertices, x3, y3, z3, ElementType.Land, defaultNormal);
    }

    /**
     * 平滑法线计算的核心方法
     */
    private static void calculateSmoothNormals(List<Vertex> vertices) {
        Map<String, List<Integer>> positionMap = new HashMap<>();
        Map<String, float[]> normalAccumulator = new HashMap<>();
        Map<String, Integer> normalCount = new HashMap<>();

        // 第一遍：收集所有相同位置的顶点索引
        for (int i = 0; i < vertices.size(); i++) {
            Vertex vertex = vertices.get(i);
            String key = String.format("%.4f,%.4f,%.4f", vertex.x, vertex.y, vertex.z);

            if (!positionMap.containsKey(key)) {
                positionMap.put(key, new ArrayList<>());
            }
            positionMap.get(key).add(i);
        }

        // 第二遍：为每个三角形计算面法线并累加到共享顶点
        for (int i = 0; i < vertices.size(); i += 3) {
            if (i + 2 >= vertices.size()) break;

            Vertex v1 = vertices.get(i);
            Vertex v2 = vertices.get(i + 1);
            Vertex v3 = vertices.get(i + 2);

            // 计算三角形面法线
            float[] faceNormal = calculateFaceNormal(v1, v2, v3);

            // 为这个三角形的三个顶点累加法线
            accumulateVertexNormal(normalAccumulator, normalCount, v1, faceNormal);
            accumulateVertexNormal(normalAccumulator, normalCount, v2, faceNormal);
            accumulateVertexNormal(normalAccumulator, normalCount, v3, faceNormal);
        }

        // 第三遍：应用平均法线到所有共享顶点
        for (Map.Entry<String, List<Integer>> entry : positionMap.entrySet()) {
            String key = entry.getKey();
            List<Integer> indices = entry.getValue();

            float[] accumulatedNormal = normalAccumulator.get(key);
            Integer count = normalCount.get(key);

            if (accumulatedNormal != null && count != null && count > 0) {
                // 计算平均法线
                float nx = accumulatedNormal[0] / count;
                float ny = accumulatedNormal[1] / count;
                float nz = accumulatedNormal[2] / count;

                // 归一化
                float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (length > 0.0001f) {
                    nx /= length;
                    ny /= length;
                    nz /= length;
                }

                // 应用到所有共享这个位置的顶点
                for (int index : indices) {
                    Vertex vertex = vertices.get(index);
                    vertex.nx = nx;
                    vertex.ny = ny;
                    vertex.nz = nz;
                }
            }
        }
    }

    /**
     * 计算三角形面法线
     */
    private static float[] calculateFaceNormal(Vertex v1, Vertex v2, Vertex v3) {
        // 计算两个边向量
        float ux = v2.x - v1.x;
        float uy = v2.y - v1.y;
        float uz = v2.z - v1.z;

        float vx = v3.x - v1.x;
        float vy = v3.y - v1.y;
        float vz = v3.z - v1.z;

        // 计算叉积得到法线
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;

        // 归一化
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0.0001f) {
            nx /= length;
            ny /= length;
            nz /= length;
        }

        return new float[]{nx, ny, nz};
    }

    /**
     * 累加顶点法线
     */
    private static void accumulateVertexNormal(Map<String, float[]> accumulator,
                                               Map<String, Integer> count,
                                               Vertex vertex, float[] normal) {
        String key = String.format("%.4f,%.4f,%.4f", vertex.x, vertex.y, vertex.z);

        if (!accumulator.containsKey(key)) {
            accumulator.put(key, new float[]{0.0f, 0.0f, 0.0f});
            count.put(key, 0);
        }

        float[] acc = accumulator.get(key);
        acc[0] += normal[0];
        acc[1] += normal[1];
        acc[2] += normal[2];
        count.put(key, count.get(key) + 1);
    }

    /**
     * 添加顶点
     */
    private static void addVertex(List<Vertex> vertices, float x, float y, float z, int type, float[] normal) {
        // 使用统一的颜色（白色），在shader中着色
        float[] color = new float[]{1.0f, 1.0f, 1.0f};

        Vertex vertex = new Vertex(x, y, z, color[0], color[1], color[2], type);
        vertex.nx = normal[0];
        vertex.ny = normal[1];
        vertex.nz = normal[2];

        // 计算纹理坐标（基于世界位置）
        vertex.u = (x / TERRAIN_SIZE) + 0.5f;
        vertex.v = (z / TERRAIN_SIZE) + 0.5f;

        vertices.add(vertex);
    }

    /**
     * 创建网格数据
     */
    private static MeshData createMeshData(List<Vertex> vertices, float minHeight, float maxHeight) {
        MeshData meshData = new MeshData();
        meshData.vertexCount = vertices.size();

        // 创建顶点缓冲区
        float[] vertexArray = new float[vertices.size() * 3];
        float[] colorArray = new float[vertices.size() * 3];
        float[] normalArray = new float[vertices.size() * 3];
        float[] texCoordArray = new float[vertices.size() * 2];
        int[] typeArray = new int[vertices.size()];

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

            typeArray[i] = v.type;
        }

        meshData.vertices = createFloatBuffer(vertexArray);
        meshData.colors = createFloatBuffer(colorArray);
        meshData.normals = createFloatBuffer(normalArray);
        meshData.texCoords = createFloatBuffer(texCoordArray);
        meshData.minHeight = minHeight;
        meshData.maxHeight = maxHeight;
        meshData.types = createIntBuffer(typeArray);

        Log.i(TAG, String.format("Mesh created: %d vertices, height range: %.3f to %.3f",
                meshData.vertexCount, minHeight, maxHeight));

        return meshData;
    }

    /**
     * 创建 FloatBuffer
     */
    private static FloatBuffer createFloatBuffer(float[] array) {
        ByteBuffer bb = ByteBuffer.allocateDirect(array.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer buffer = bb.asFloatBuffer();
        buffer.put(array);
        buffer.position(0);
        return buffer;
    }

    /**
     * 创建 IntBuffer
     */
    private static IntBuffer createIntBuffer(int[] array) {
        ByteBuffer bb = ByteBuffer.allocateDirect(array.length * 4);
        bb.order(ByteOrder.nativeOrder());
        IntBuffer buffer = bb.asIntBuffer();
        buffer.put(array);
        buffer.position(0);
        return buffer;
    }
}
