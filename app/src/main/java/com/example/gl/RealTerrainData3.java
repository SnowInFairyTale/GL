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
    private static final String TAG = "RealTerrainData3";

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


        public void recalculateNormals() {
            if (vertices == null || indices == null || vertexCount == 0 || indexCount == 0) {
                Log.e(TAG, "无法计算法线：缺少顶点或索引数据");
                return;
            }

            Log.d(TAG, "=== 开始计算法线 ===");

            try {
                // 1. 准备数据数组 - 使用正确的缓冲区位置
                vertices.position(0);
                float[] vertexArray = new float[vertices.remaining()];
                vertices.get(vertexArray);
                vertices.position(0);

                indices.position(0);
                int[] indexArray = new int[indices.remaining()];
                indices.get(indexArray);
                indices.position(0);

                Log.d(TAG, String.format("顶点数组长度: %d, 需要: %d",
                        vertexArray.length, vertexCount * 3));
                Log.d(TAG, String.format("索引数组长度: %d, 需要: %d",
                        indexArray.length, indexCount));

                // 2. 检查顶点数组长度是否足够
                if (vertexArray.length < vertexCount * 3) {
                    Log.e(TAG, String.format("顶点数组长度不足: 需要%d, 实际%d",
                            vertexCount * 3, vertexArray.length));
                    return;
                }

                // 3. 检查索引的有效性
                int invalidIndices = 0;
                int maxIndex = 0;
                for (int i = 0; i < Math.min(20, indexArray.length); i++) {
                    maxIndex = Math.max(maxIndex, indexArray[i]);
                    if (indexArray[i] >= vertexCount) {
                        invalidIndices++;
                        Log.w(TAG, String.format("索引[%d] = %d 超出范围 (vertexCount=%d)",
                                i, indexArray[i], vertexCount));
                    }
                }

                // 检查全部索引
                for (int i = 0; i < indexArray.length; i++) {
                    maxIndex = Math.max(maxIndex, indexArray[i]);
                    if (indexArray[i] >= vertexCount) {
                        invalidIndices++;
                    }
                }

                Log.d(TAG, String.format("最大索引: %d, 顶点数: %d, 无效索引数: %d",
                        maxIndex, vertexCount, invalidIndices));

                if (maxIndex >= vertexCount) {
                    Log.e(TAG, String.format("错误: 最大索引 %d >= 顶点数 %d", maxIndex, vertexCount));
                }

                // 4. 法线累加数组
                float[] normalArray = new float[vertexCount * 3];

                // 5. 遍历三角形累加法线
                int triangleCount = 0;
                int processedTriangles = 0;
                for (int i = 0; i < indexArray.length; i += 3) {
                    triangleCount++;

                    // 确保有足够的索引
                    if (i + 2 >= indexArray.length) {
                        Log.w(TAG, String.format("索引不足: i=%d, arrayLength=%d", i, indexArray.length));
                        break;
                    }

                    int i0 = indexArray[i];
                    int i1 = indexArray[i + 1];
                    int i2 = indexArray[i + 2];

                    // 检查索引是否有效
                    if (i0 >= vertexCount || i1 >= vertexCount || i2 >= vertexCount) {
                        if (processedTriangles < 10) {
                            Log.w(TAG, String.format("跳过无效三角形 %d: i0=%d, i1=%d, i2=%d",
                                    triangleCount, i0, i1, i2));
                        }
                        continue;
                    }

                    // 获取顶点
                    int base0 = i0 * 3;
                    int base1 = i1 * 3;
                    int base2 = i2 * 3;

                    // 检查数组边界
                    if (base0 + 2 >= vertexArray.length ||
                            base1 + 2 >= vertexArray.length ||
                            base2 + 2 >= vertexArray.length) {
                        Log.w(TAG, String.format("数组越界: base0=%d, base1=%d, base2=%d",
                                base0, base1, base2));
                        continue;
                    }

                    float x0 = vertexArray[base0];
                    float y0 = vertexArray[base0 + 1];
                    float z0 = vertexArray[base0 + 2];

                    float x1 = vertexArray[base1];
                    float y1 = vertexArray[base1 + 1];
                    float z1 = vertexArray[base1 + 2];

                    float x2 = vertexArray[base2];
                    float y2 = vertexArray[base2 + 1];
                    float z2 = vertexArray[base2 + 2];

                    // 计算三角形法线
                    float[] normal = calculateTriangleNormal(x0, y0, z0, x1, y1, z1, x2, y2, z2);

                    // 累加到顶点
                    normalArray[base0] += normal[0];
                    normalArray[base0 + 1] += normal[1];
                    normalArray[base0 + 2] += normal[2];

                    normalArray[base1] += normal[0];
                    normalArray[base1 + 1] += normal[1];
                    normalArray[base1 + 2] += normal[2];

                    normalArray[base2] += normal[0];
                    normalArray[base2 + 1] += normal[1];
                    normalArray[base2 + 2] += normal[2];

                    processedTriangles++;
                }

                Log.d(TAG, String.format("处理完成: 总三角形数=%d, 有效三角形数=%d",
                        triangleCount, processedTriangles));

                // 6. 归一化并更新缓冲区
                normalizeAndUpdateBuffer(normalArray);

            } catch (Exception e) {
                Log.e(TAG, "计算法线时发生错误: " + e.getMessage(), e);
                e.printStackTrace();
            }

            Log.d(TAG, "=== 法线计算完成 ===");
        }

        private float[] calculateTriangleNormal(float x0, float y0, float z0,
                                                float x1, float y1, float z1,
                                                float x2, float y2, float z2) {
            float edge1x = x1 - x0;
            float edge1y = y1 - y0;
            float edge1z = z1 - z0;

            float edge2x = x2 - x0;
            float edge2y = y2 - y0;
            float edge2z = z2 - z0;

            // 叉积
            float nx = edge1y * edge2z - edge1z * edge2y;
            float ny = edge1z * edge2x - edge1x * edge2z;
            float nz = edge1x * edge2y - edge1y * edge2x;

            // 归一化
            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length > 0) {
                return new float[]{nx / length, ny / length, nz / length};
            }
            return new float[]{0.0f, 1.0f, 0.0f}; // 默认向上
        }

        private void normalizeAndUpdateBuffer(float[] normalArray) {
            // 确保数组长度正确
            if (normalArray.length != vertexCount * 3) {
                Log.e(TAG, String.format("法线数组长度不正确: expected=%d, actual=%d",
                        vertexCount * 3, normalArray.length));
                return;
            }

            // 归一化每个顶点的法线
            for (int i = 0; i < vertexCount; i++) {
                float nx = normalArray[i * 3];
                float ny = normalArray[i * 3 + 1];
                float nz = normalArray[i * 3 + 2];

                float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (length > 0) {
                    normalArray[i * 3] = nx / length;
                    normalArray[i * 3 + 1] = ny / length;
                    normalArray[i * 3 + 2] = nz / length;
                } else {
                    // 零长度法线，使用默认向上
                    normalArray[i * 3] = 0.0f;
                    normalArray[i * 3 + 1] = 1.0f;
                    normalArray[i * 3 + 2] = 0.0f;
                }
            }

            // 更新缓冲区 - 确保创建正确大小的缓冲区
            ByteBuffer bb = ByteBuffer.allocateDirect(vertexCount * 3 * 4);
            bb.order(ByteOrder.nativeOrder());
            normals = bb.asFloatBuffer();
            normals.put(normalArray);
            normals.position(0);

            Log.d(TAG, "法线缓冲区更新完成，容量: " + normals.capacity());
        }

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