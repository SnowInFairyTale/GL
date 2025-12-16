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
     * 将网格坐标映射到源数据坐标（返回浮点数用于插值）
     */
    private static float mapToSourceCoordFloat(int meshCoord, int meshSize, int srcSize) {
        return ((float) meshCoord / (meshSize - 1)) * (srcSize - 1);
    }

    /**
     * 从真实的32位浮点数据生成地形网格
     * heightData 357 × 203像素，转换为500 × 500网格，保持比例不变形，使用双线性插值
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

        List<Vertex> vertexList = new ArrayList<>();
        List<Integer> indexList = new ArrayList<>();  // 新增：存储索引

        // 计算真实的高度范围（仅从有效数据区域）
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
        float srcAspect = (float) srcWidth / srcHeight;  // 357/203 ≈ 1.758
        float meshAspect = (float) meshWidth / meshHeight; // 500/500 = 1.0

        // 计算在目标网格中的有效区域（保持原始比例）
        int effectiveMeshWidth, effectiveMeshHeight;
        int offsetX = 0, offsetZ = 0;
        boolean isWidthFilled; // 标记哪个方向填满

        if (srcAspect > meshAspect) {
            // 源数据更宽，在宽度方向填满，高度方向留白
            effectiveMeshWidth = meshWidth;
            effectiveMeshHeight = (int) (meshWidth / srcAspect);
            offsetZ = (meshHeight - effectiveMeshHeight) / 2;
            isWidthFilled = true;
        } else {
            // 源数据更高，在高度方向填满，宽度方向留白
            effectiveMeshHeight = meshHeight;
            effectiveMeshWidth = (int) (meshHeight * srcAspect);
            offsetX = (meshWidth - effectiveMeshWidth) / 2;
            isWidthFilled = false;
        }

        Log.i(TAG, String.format("Effective mesh area: %dx%d, offset: (%d, %d), widthFilled: %b",
                effectiveMeshWidth, effectiveMeshHeight, offsetX, offsetZ, isWidthFilled));

        // 计算高度缩放比例
        float heightScale;
        if (isWidthFilled) {
            // 宽度方向填满：使用宽度方向的缩放比例
            heightScale = (float) effectiveMeshWidth / srcWidth;
        } else {
            // 高度方向填满：使用高度方向的缩放比例
            heightScale = (float) effectiveMeshHeight / srcHeight;
        }

        Log.i(TAG, String.format("Height scale: %.3f", heightScale));

        // 生成网格顶点 - 使用插值和高度缩放
        for (int i = 0; i < meshWidth - 1; i++) {
            for (int j = 0; j < meshHeight - 1; j++) {
                // 检查当前网格点是否在有效区域内
                boolean inEffectiveArea = (i >= offsetX && i < offsetX + effectiveMeshWidth - 1 &&
                        j >= offsetZ && j < offsetZ + effectiveMeshHeight - 1);

                if (inEffectiveArea) {
                    // 在有效区域内，使用浮点坐标进行双线性插值
                    addQuadWithInterpolation(vertexList, indexList, heightData,
                            meshWidth, meshHeight, i, j, offsetX, offsetZ,
                            effectiveMeshWidth, effectiveMeshHeight, srcWidth, srcHeight, heightScale);
                } else {
                    // 在留白区域，高度为0
                    addQuadFixed(vertexList, indexList, heightData,
                            meshWidth, meshHeight, i, j, offsetX, offsetZ,
                            effectiveMeshWidth, effectiveMeshHeight, heightScale);
                }
            }
        }

        // 计算法线
        // calculateNormals(vertexList);

        // 重新计算缩放后的高度范围
        float scaledMinHeight = minHeight * heightScale;
        float scaledMaxHeight = maxHeight * heightScale;
        Log.i(TAG, String.format("Scaled height range: %.3f to %.3f", scaledMinHeight, scaledMaxHeight));

        // 转换为FloatBuffer
        return createMeshData(vertexList, indexList, Math.min(scaledMinHeight, 0), scaledMaxHeight);
    }

    /**
     * 使用双线性插值的四边形添加方法（有效区域）- 增加索引参数
     */
    private static void addQuadWithInterpolation(List<Vertex> vertices, List<Integer> indices,
                                                 float[][] heightMap,
                                                 int meshWidth, int meshHeight, int meshI, int meshJ,
                                                 int offsetX, int offsetZ, int effectiveMeshWidth,
                                                 int effectiveMeshHeight, int srcWidth, int srcHeight,
                                                 float heightScale) {

        // 计算四个顶点在源数据中的浮点坐标
        float srcX0 = mapToSourceCoordFloat(meshI - offsetX, effectiveMeshWidth, srcWidth);
        float srcY0 = mapToSourceCoordFloat(meshJ - offsetZ, effectiveMeshHeight, srcHeight);
        float srcX1 = mapToSourceCoordFloat(meshI + 1 - offsetX, effectiveMeshWidth, srcWidth);
        float srcY1 = mapToSourceCoordFloat(meshJ - offsetZ, effectiveMeshHeight, srcHeight);
        float srcX2 = mapToSourceCoordFloat(meshI + 1 - offsetX, effectiveMeshWidth, srcWidth);
        float srcY2 = mapToSourceCoordFloat(meshJ + 1 - offsetZ, effectiveMeshHeight, srcHeight);
        float srcX3 = mapToSourceCoordFloat(meshI - offsetX, effectiveMeshWidth, srcWidth);
        float srcY3 = mapToSourceCoordFloat(meshJ + 1 - offsetZ, effectiveMeshHeight, srcHeight);

        // 计算四个顶点的世界坐标
        float x0 = (meshI / (float) (meshWidth - 1) - 0.5f) * TERRAIN_SIZE;
        float z0 = (meshJ / (float) (meshHeight - 1) - 0.5f) * TERRAIN_SIZE;
        float y0 = bilinearInterpolate(heightMap, srcX0, srcY0, heightScale);

        float x1 = ((meshI + 1) / (float) (meshWidth - 1) - 0.5f) * TERRAIN_SIZE;
        float z1 = (meshJ / (float) (meshHeight - 1) - 0.5f) * TERRAIN_SIZE;
        float y1 = bilinearInterpolate(heightMap, srcX1, srcY1, heightScale);

        float x2 = ((meshI + 1) / (float) (meshWidth - 1) - 0.5f) * TERRAIN_SIZE;
        float z2 = ((meshJ + 1) / (float) (meshHeight - 1) - 0.5f) * TERRAIN_SIZE;
        float y2 = bilinearInterpolate(heightMap, srcX2, srcY2, heightScale);

        float x3 = (meshI / (float) (meshWidth - 1) - 0.5f) * TERRAIN_SIZE;
        float z3 = ((meshJ + 1) / (float) (meshHeight - 1) - 0.5f) * TERRAIN_SIZE;
        float y3 = bilinearInterpolate(heightMap, srcX3, srcY3, heightScale);

        // 计算纹理坐标 - 在有效区域内映射到[0,1]
        float u0 = (float) (meshI - offsetX) / (effectiveMeshWidth - 1);
        float v0 = (float) (meshJ - offsetZ) / (effectiveMeshHeight - 1);
        float u1 = (float) (meshI + 1 - offsetX) / (effectiveMeshWidth - 1);
        float v1 = (float) (meshJ - offsetZ) / (effectiveMeshHeight - 1);
        float u2 = (float) (meshI + 1 - offsetX) / (effectiveMeshWidth - 1);
        float v2 = (float) (meshJ + 1 - offsetZ) / (effectiveMeshWidth - 1);
        float u3 = (float) (meshI - offsetX) / (effectiveMeshWidth - 1);
        float v3 = (float) (meshJ + 1 - offsetZ) / (effectiveMeshHeight - 1);

        // 使用默认法线，后续会统一计算
        float[] defaultNormal = {0.0f, 1.0f, 0.0f};

        // 获取当前顶点的起始索引
        int baseIndex = vertices.size();

        // 添加四个顶点
        addVertex(vertices, x0, y0, z0, defaultNormal, u0, v0);
        addVertex(vertices, x1, y1, z1, defaultNormal, u1, v1);
        addVertex(vertices, x2, y2, z2, defaultNormal, u2, v2);
        addVertex(vertices, x3, y3, z3, defaultNormal, u3, v3);

        // 添加两个三角形的索引（逆时针顺序）
        // 第一个三角形：左下->右下->右上
        indices.add(baseIndex);
        indices.add(baseIndex + 1);
        indices.add(baseIndex + 2);

        // 第二个三角形：左下->右上->左上
        indices.add(baseIndex);
        indices.add(baseIndex + 2);
        indices.add(baseIndex + 3);
    }

    /**
     * 四边形添加方法（留白区域）- 增加索引参数
     */
    private static void addQuadFixed(List<Vertex> vertices, List<Integer> indices,
                                     float[][] heightMap,
                                     int meshWidth, int meshHeight, int meshI, int meshJ,
                                     int offsetX, int offsetZ, int effectiveMeshWidth,
                                     int effectiveMeshHeight, float heightScale) {

        // 计算四个顶点的世界坐标（留白区域高度为0）
        float x0 = (meshI / (float) (meshWidth - 1) - 0.5f) * TERRAIN_SIZE;
        float z0 = (meshJ / (float) (meshHeight - 1) - 0.5f) * TERRAIN_SIZE;
        float y0 = 0.0f;

        float x1 = ((meshI + 1) / (float) (meshWidth - 1) - 0.5f) * TERRAIN_SIZE;
        float z1 = (meshJ / (float) (meshHeight - 1) - 0.5f) * TERRAIN_SIZE;
        float y1 = 0.0f;

        float x2 = ((meshI + 1) / (float) (meshWidth - 1) - 0.5f) * TERRAIN_SIZE;
        float z2 = ((meshJ + 1) / (float) (meshHeight - 1) - 0.5f) * TERRAIN_SIZE;
        float y2 = 0.0f;

        float x3 = (meshI / (float) (meshWidth - 1) - 0.5f) * TERRAIN_SIZE;
        float z3 = ((meshJ + 1) / (float) (meshHeight - 1) - 0.5f) * TERRAIN_SIZE;
        float y3 = 0.0f;

        // 留白区域的纹理坐标设为0（使用纹理左下角颜色）
        float u = 0.0f, v = 0.0f;

        // 使用默认法线，后续会统一计算
        float[] defaultNormal = {0.0f, 1.0f, 0.0f};

        // 获取当前顶点的起始索引
        int baseIndex = vertices.size();

        // 添加四个顶点
        addVertex(vertices, x0, y0, z0, defaultNormal, u, v);
        addVertex(vertices, x1, y1, z1, defaultNormal, u, v);
        addVertex(vertices, x2, y2, z2, defaultNormal, u, v);
        addVertex(vertices, x3, y3, z3, defaultNormal, u, v);

        // 添加两个三角形的索引（逆时针顺序）
        // 第一个三角形：左下->右下->右上
        indices.add(baseIndex);
        indices.add(baseIndex + 1);
        indices.add(baseIndex + 2);

        // 第二个三角形：左下->右上->左上
        indices.add(baseIndex);
        indices.add(baseIndex + 2);
        indices.add(baseIndex + 3);
    }

    /**
     * 添加顶点
     */
    private static void addVertex(List<Vertex> vertices, float x, float y, float z, float[] normal, float u, float v) {
        float[] color = new float[]{1.0f, 1.0f, 1.0f};

        Vertex vertex = new Vertex(x, y, z, color[0], color[1], color[2], u, v);
        vertex.nx = normal[0];
        vertex.ny = normal[1];
        vertex.nz = normal[2];

        vertices.add(vertex);
    }

    /**
     * 创建网格数据 - 增加索引参数
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