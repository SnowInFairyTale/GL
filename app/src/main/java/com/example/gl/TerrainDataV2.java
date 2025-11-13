package com.example.gl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TerrainDataV2 {
    // 基础网格配置
    private static final int BASE_GRID_SIZE = 50;
    private static final int FINAL_GRID_SIZE = 200; // 提高基础网格密度
    public static final float TERRAIN_SIZE = 100.0f;
    private static final float MAX_HEIGHT = 10.0f;

    // 高级特性配置
    private static boolean useInterpolation = true;
    private static boolean enableTessellation = false;
    private static boolean enableNormalMapping = false;
    private static int tessellationLevel = 4;

    // 高度图数据（用于曲面细分）
    public static float[][] heightMapData;
    public static int heightMapTextureId = -1;

    public static class Vertex {
        public float x, y, z;
        public float r, g, b;
        public float nx, ny, nz;
        public int type;
        public float u, v;

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
            this.ny = 1.0f;
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
        public float[][] heightMap; // 新增：存储高度图数据
    }

    // 配置方法
    public static void setUseInterpolation(boolean use) {
        useInterpolation = use;
    }

    public static void setEnableTessellation(boolean enable) {
        enableTessellation = enable;
    }

    public static void setEnableNormalMapping(boolean enable) {
        enableNormalMapping = enable;
    }

    public static void setTessellationLevel(int level) {
        tessellationLevel = Math.max(1, Math.min(16, level));
    }

    public static boolean isTessellationEnabled() {
        return enableTessellation;
    }

    public static int getTessellationLevel() {
        return tessellationLevel;
    }

    // 生成基础高度图（传统方法）
    private static float[][] generateBaseHeightMap(Random random) {
        float[][] baseHeightMap = new float[BASE_GRID_SIZE][BASE_GRID_SIZE];

        for (int i = 0; i < BASE_GRID_SIZE; i++) {
            for (int j = 0; j < BASE_GRID_SIZE; j++) {
                float x = (i / (float) BASE_GRID_SIZE - 0.5f) * TERRAIN_SIZE;
                float z = (j / (float) BASE_GRID_SIZE - 0.5f) * TERRAIN_SIZE;

                // 多频率噪声创建更自然的地形
                float height = (float) (
                        Math.sin(x * 0.05) * Math.cos(z * 0.05) * 4.0f +      // 低频 - 主要地形
                                Math.sin(x * 0.1) * Math.cos(z * 0.08) * 2.0f +       // 中频 - 丘陵
                                Math.sin(x * 0.2) * Math.cos(z * 0.15) * 1.0f +       // 高频 - 细节
                                Math.sin(x * 0.4) * Math.cos(z * 0.3) * 0.5f          // 超高频 - 微细节
                );

                // 添加随机噪声
                height += random.nextFloat() * 1.5f - 0.75f;
                height = Math.max(-2.0f, Math.min(MAX_HEIGHT, height));

                baseHeightMap[i][j] = height;
            }
        }

        return baseHeightMap;
    }

    // 双线性插值生成平滑高度图
    private static float[][] interpolateHeightMap(float[][] baseHeightMap, Random random) {
        float[][] finalHeightMap = new float[FINAL_GRID_SIZE][FINAL_GRID_SIZE];

        for (int i = 0; i < FINAL_GRID_SIZE; i++) {
            for (int j = 0; j < FINAL_GRID_SIZE; j++) {
                float baseX = i / (float)FINAL_GRID_SIZE * (BASE_GRID_SIZE - 1);
                float baseZ = j / (float)FINAL_GRID_SIZE * (BASE_GRID_SIZE - 1);

                finalHeightMap[i][j] = bilinearInterpolate(baseHeightMap, baseX, baseZ);

                // 添加细微的高频噪声
                finalHeightMap[i][j] += (random.nextFloat() * 0.1f - 0.05f);
            }
        }

        return finalHeightMap;
    }

    // 双线性插值
    private static float bilinearInterpolate(float[][] values, float x, float z) {
        int x1 = (int) Math.floor(x);
        int x2 = x1 + 1;
        int z1 = (int) Math.floor(z);
        int z2 = z1 + 1;

        x1 = Math.max(0, Math.min(values.length - 1, x1));
        x2 = Math.max(0, Math.min(values.length - 1, x2));
        z1 = Math.max(0, Math.min(values[0].length - 1, z1));
        z2 = Math.max(0, Math.min(values[0].length - 1, z2));

        float q11 = values[x1][z1];
        float q12 = values[x1][z2];
        float q21 = values[x2][z1];
        float q22 = values[x2][z2];

        float dx = x - x1;
        float dz = z - z1;

        float top = q11 * (1 - dx) + q21 * dx;
        float bottom = q12 * (1 - dx) + q22 * dx;
        return top * (1 - dz) + bottom * dz;
    }

    // 类型图插值（最近邻）
    private static int[][] interpolateTypeMap(int[][] baseTypeMap) {
        int[][] finalTypeMap = new int[FINAL_GRID_SIZE][FINAL_GRID_SIZE];

        for (int i = 0; i < FINAL_GRID_SIZE; i++) {
            for (int j = 0; j < FINAL_GRID_SIZE; j++) {
                int baseX = Math.round(i / (float)FINAL_GRID_SIZE * (BASE_GRID_SIZE - 1));
                int baseZ = Math.round(j / (float)FINAL_GRID_SIZE * (BASE_GRID_SIZE - 1));

                baseX = Math.max(0, Math.min(BASE_GRID_SIZE - 1, baseX));
                baseZ = Math.max(0, Math.min(BASE_GRID_SIZE - 1, baseZ));

                finalTypeMap[i][j] = baseTypeMap[baseX][baseZ];
            }
        }

        return finalTypeMap;
    }

    // 使用Sobel算子计算精确法线
    private static float[] calculateDetailedNormal(float[][] heightMap, int x, int y, int gridSize) {
        float dx = 0, dz = 0;

        if (x > 0 && x < gridSize - 1 && y > 0 && y < gridSize - 1) {
            // Sobel算子计算梯度
            dx = (heightMap[x+1][y-1] + 2 * heightMap[x+1][y] + heightMap[x+1][y+1] -
                    heightMap[x-1][y-1] - 2 * heightMap[x-1][y] - heightMap[x-1][y+1]) / 8.0f;

            dz = (heightMap[x-1][y+1] + 2 * heightMap[x][y+1] + heightMap[x+1][y+1] -
                    heightMap[x-1][y-1] - 2 * heightMap[x][y-1] - heightMap[x+1][y-1]) / 8.0f;
        }

        float nx = -dx;
        float ny = 1.0f;
        float nz = -dz;

        float length = (float)Math.sqrt(nx*nx + ny*ny + nz*nz);
        if (length > 0) {
            nx /= length;
            ny /= length;
            nz /= length;
        }

        return new float[]{nx, ny, nz};
    }

    // 平滑法线计算
    private static void calculateSmoothNormals(List<Vertex> vertices) {
        Map<String, List<Integer>> positionMap = new HashMap<>();

        for (int i = 0; i < vertices.size(); i++) {
            Vertex vertex = vertices.get(i);
            String key = String.format("%.4f,%.4f,%.4f", vertex.x, vertex.y, vertex.z);

            if (!positionMap.containsKey(key)) {
                positionMap.put(key, new ArrayList<>());
            }
            positionMap.get(key).add(i);
        }

        Map<String, float[]> normalAccumulator = new HashMap<>();
        Map<String, Integer> normalCount = new HashMap<>();

        for (int i = 0; i < vertices.size(); i += 3) {
            if (i + 2 >= vertices.size()) break;

            Vertex v1 = vertices.get(i);
            Vertex v2 = vertices.get(i + 1);
            Vertex v3 = vertices.get(i + 2);

            float[] faceNormal = calculateFaceNormal(v1, v2, v3);

            accumulateVertexNormal(normalAccumulator, normalCount, v1, faceNormal);
            accumulateVertexNormal(normalAccumulator, normalCount, v2, faceNormal);
            accumulateVertexNormal(normalAccumulator, normalCount, v3, faceNormal);
        }

        for (Map.Entry<String, List<Integer>> entry : positionMap.entrySet()) {
            String key = entry.getKey();
            List<Integer> indices = entry.getValue();

            float[] accumulatedNormal = normalAccumulator.get(key);
            Integer count = normalCount.get(key);

            if (accumulatedNormal != null && count != null && count > 0) {
                float nx = accumulatedNormal[0] / count;
                float ny = accumulatedNormal[1] / count;
                float nz = accumulatedNormal[2] / count;

                float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (length > 0.0001f) {
                    nx /= length;
                    ny /= length;
                    nz /= length;
                }

                for (int index : indices) {
                    Vertex vertex = vertices.get(index);
                    vertex.nx = nx;
                    vertex.ny = ny;
                    vertex.nz = nz;
                }
            }
        }
    }

    private static float[] calculateFaceNormal(Vertex v1, Vertex v2, Vertex v3) {
        float ux = v2.x - v1.x, uy = v2.y - v1.y, uz = v2.z - v1.z;
        float vx = v3.x - v1.x, vy = v3.y - v1.y, vz = v3.z - v1.z;

        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;

        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0.0001f) {
            nx /= length;
            ny /= length;
            nz /= length;
        }

        return new float[]{nx, ny, nz};
    }

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

    // 基础法线计算方法
    private static float[] calculateNormal(float x1, float y1, float z1,
                                           float x2, float y2, float z2,
                                           float x3, float y3, float z3) {
        float ux = x2 - x1, uy = y2 - y1, uz = z2 - z1;
        float vx = x3 - x1, vy = y3 - y1, vz = z3 - z1;

        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;

        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0) {
            nx /= length;
            ny /= length;
            nz /= length;
        }

        return new float[]{nx, ny, nz};
    }

    // 主地形生成方法
    public static MeshData generateTerrainMesh() {
        List<Vertex> vertexList = new ArrayList<>();
        float[][] heightMap;
        int[][] typeMap;

        Random random = new Random(42);
        float minHeight = 0;
        float maxHeight = 0;

        // 步骤1：生成基础高度图
        float[][] baseHeightMap = generateBaseHeightMap(random);
        int[][] baseTypeMap = new int[BASE_GRID_SIZE][BASE_GRID_SIZE];

        // 初始化基础类型图
        for (int i = 0; i < BASE_GRID_SIZE; i++) {
            for (int j = 0; j < BASE_GRID_SIZE; j++) {
                baseTypeMap[i][j] = ElementType.Land;
                float height = baseHeightMap[i][j];
                minHeight = Math.min(minHeight, height);
                maxHeight = Math.max(maxHeight, height);
            }
        }

        // 在基础网格上添加特征
        addRoad(baseHeightMap, baseTypeMap, BASE_GRID_SIZE / 2, 0, BASE_GRID_SIZE, 8, minHeight, maxHeight);
        addWaterPool(baseHeightMap, baseTypeMap, BASE_GRID_SIZE / 4, BASE_GRID_SIZE / 4, 6, minHeight, maxHeight);
        addLawn(baseHeightMap, baseTypeMap, BASE_GRID_SIZE * 3 / 4, BASE_GRID_SIZE * 3 / 4, 10, minHeight, maxHeight);
        addBuilding(baseHeightMap, baseTypeMap, BASE_GRID_SIZE / 4, BASE_GRID_SIZE * 3 / 4, 6, 6, 10.0f, minHeight, maxHeight);

        // 步骤2：选择是否进行插值
        final int finalGridSize;
        if (useInterpolation) {
            heightMap = interpolateHeightMap(baseHeightMap, random);
            typeMap = interpolateTypeMap(baseTypeMap);
            finalGridSize = FINAL_GRID_SIZE;
        } else {
            heightMap = baseHeightMap;
            typeMap = baseTypeMap;
            finalGridSize = BASE_GRID_SIZE;
        }

        // 保存高度图数据用于曲面细分
        heightMapData = heightMap;

        // 步骤3：更新高度范围
        minHeight = 0;
        maxHeight = 0;
        for (int i = 0; i < finalGridSize; i++) {
            for (int j = 0; j < finalGridSize; j++) {
                float height = heightMap[i][j];
                minHeight = Math.min(minHeight, height);
                maxHeight = Math.max(maxHeight, height);
            }
        }

        // 步骤4：生成网格顶点
        for (int i = 0; i < finalGridSize - 1; i++) {
            for (int j = 0; j < finalGridSize - 1; j++) {
                addQuad(vertexList, heightMap, typeMap, i, j, i + 1, j, i, j + 1, finalGridSize);
                addQuad(vertexList, heightMap, typeMap, i + 1, j, i + 1, j + 1, i, j + 1, finalGridSize);
            }
        }

        // 步骤5：添加树木和建筑物
        addTrees(vertexList, heightMap, typeMap, finalGridSize);
        addDetailedBuildings(vertexList, heightMap, typeMap, finalGridSize);

        // 步骤6：计算平滑法线
        calculateSmoothNormals(vertexList);

        // 步骤7：创建网格数据
        MeshData meshData = createMeshData(vertexList, minHeight, maxHeight);
        meshData.heightMap = heightMap; // 保存高度图数据

        return meshData;
    }

    // 生成高度图纹理（用于曲面细分）
    public static int generateHeightMapTexture() {
        if (heightMapData == null) return -1;

        int width = heightMapData.length;
        int height = heightMapData[0].length;

        // 找到高度范围
        float minHeight = Float.MAX_VALUE;
        float maxHeight = Float.MIN_VALUE;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                minHeight = Math.min(minHeight, heightMapData[i][j]);
                maxHeight = Math.max(maxHeight, heightMapData[i][j]);
            }
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
        buffer.order(ByteOrder.nativeOrder());

        // 归一化高度到[0,1]范围并转换为RGBA
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                float normalized = (heightMapData[i][j] - minHeight) / (maxHeight - minHeight);
                byte value = (byte)(normalized * 255);
                buffer.put(value); // R
                buffer.put(value); // G
                buffer.put(value); // B
                buffer.put((byte)255); // A
            }
        }
        buffer.position(0);

        int[] textureId = new int[1];
        android.opengl.GLES32.glGenTextures(1, textureId, 0);
        android.opengl.GLES32.glBindTexture(android.opengl.GLES32.GL_TEXTURE_2D, textureId[0]);

        android.opengl.GLES32.glTexImage2D(
                android.opengl.GLES32.GL_TEXTURE_2D, 0, android.opengl.GLES32.GL_RGBA,
                width, height, 0, android.opengl.GLES32.GL_RGBA,
                android.opengl.GLES32.GL_UNSIGNED_BYTE, buffer
        );

        android.opengl.GLES32.glTexParameteri(android.opengl.GLES32.GL_TEXTURE_2D,
                android.opengl.GLES32.GL_TEXTURE_MIN_FILTER, android.opengl.GLES32.GL_LINEAR);
        android.opengl.GLES32.glTexParameteri(android.opengl.GLES32.GL_TEXTURE_2D,
                android.opengl.GLES32.GL_TEXTURE_MAG_FILTER, android.opengl.GLES32.GL_LINEAR);
        android.opengl.GLES32.glTexParameteri(android.opengl.GLES32.GL_TEXTURE_2D,
                android.opengl.GLES32.GL_TEXTURE_WRAP_S, android.opengl.GLES32.GL_CLAMP_TO_EDGE);
        android.opengl.GLES32.glTexParameteri(android.opengl.GLES32.GL_TEXTURE_2D,
                android.opengl.GLES32.GL_TEXTURE_WRAP_T, android.opengl.GLES32.GL_CLAMP_TO_EDGE);

        heightMapTextureId = textureId[0];
        return textureId[0];
    }

    // 以下为辅助方法（与TerrainData相同）
    private static void addQuad(List<Vertex> vertices, float[][] heightMap, int[][] typeMap,
                                int i1, int j1, int i2, int j2, int i3, int j3, int gridSize) {
        float x1 = (i1 / (float) gridSize - 0.5f) * TERRAIN_SIZE;
        float z1 = (j1 / (float) gridSize - 0.5f) * TERRAIN_SIZE;
        float y1 = heightMap[i1][j1];

        float x2 = (i2 / (float) gridSize - 0.5f) * TERRAIN_SIZE;
        float z2 = (j2 / (float) gridSize - 0.5f) * TERRAIN_SIZE;
        float y2 = heightMap[i2][j2];

        float x3 = (i3 / (float) gridSize - 0.5f) * TERRAIN_SIZE;
        float z3 = (j3 / (float) gridSize - 0.5f) * TERRAIN_SIZE;
        float y3 = heightMap[i3][j3];

        float[] defaultNormal = {0.0f, 1.0f, 0.0f};

        addVertex(vertices, x1, y1, z1, typeMap[i1][j1], defaultNormal);
        addVertex(vertices, x2, y2, z2, typeMap[i2][j2], defaultNormal);
        addVertex(vertices, x3, y3, z3, typeMap[i3][j3], defaultNormal);
    }

    private static void addVertex(List<Vertex> vertices, float x, float y, float z, int type, float[] normal) {
        float[] color = getColorForType(type);
        Vertex vertex = new Vertex(x, y, z, color[0], color[1], color[2], type);
        vertex.nx = normal[0];
        vertex.ny = normal[1];
        vertex.nz = normal[2];
        vertices.add(vertex);
    }

    private static void addVertex(List<Vertex> vertices, float x, float y, float z, int type, float[] color, float[] normal) {
        Vertex vertex = new Vertex(x, y, z, color[0], color[1], color[2], type);
        vertex.nx = normal[0];
        vertex.ny = normal[1];
        vertex.nz = normal[2];
        vertices.add(vertex);
    }

    private static void addVertexWithTexCoord(List<Vertex> vertices, float x, float y, float z,
                                              int type, float[] color, float[] normal, float u, float v) {
        Vertex vertex = new Vertex(x, y, z, color[0], color[1], color[2], type, u, v);
        vertex.nx = normal[0];
        vertex.ny = normal[1];
        vertex.nz = normal[2];
        vertices.add(vertex);
    }

    private static float[] getColorForType(int type) {
        Random random = new Random(42);
        switch (type) {
            case ElementType.Road:
                float gray = 0.25f + random.nextFloat() * 0.1f;
                return new float[]{gray, gray, gray};
            case ElementType.WaterPool:
                float blueDepth = 0.6f + random.nextFloat() * 0.2f;
                return new float[]{0.1f, 0.4f, blueDepth};
            case ElementType.Lawn:
                float greenVar = random.nextFloat() * 0.2f;
                return new float[]{0.1f + greenVar * 0.2f, 0.5f + greenVar, 0.1f + greenVar * 0.1f};
            case ElementType.Building:
            case ElementType.HouseWall:
                float brownVar = random.nextFloat() * 0.15f;
                return new float[]{0.5f + brownVar, 0.3f + brownVar, 0.1f + brownVar};
            case ElementType.Roof:
                float roofVar = random.nextFloat() * 0.15f;
                return new float[]{0.3f + roofVar, 0.2f + roofVar, 0.1f + roofVar};
            case ElementType.Trunk:
                return new float[]{0.4f, 0.2f, 0.1f};
            case ElementType.Canopy:
                return new float[]{0.1f, 0.5f, 0.1f};
            default:
                float groundVar = random.nextFloat() * 0.15f;
                return new float[]{0.7f + groundVar, 0.6f + groundVar, 0.45f + groundVar};
        }
    }

    private static void addRoad(float[][] heightMap, int[][] typeMap, int centerX, int centerZ, int length, int width, float minHeight, float maxHeight) {
        int halfWidth = width / 2;
        for (int i = centerX - length / 2; i < centerX + length / 2; i++) {
            for (int j = centerZ - halfWidth; j < centerZ + halfWidth; j++) {
                if (i >= 0 && i < heightMap.length && j >= 0 && j < heightMap[0].length) {
                    heightMap[i][j] = (maxHeight - minHeight) / 2;
                    typeMap[i][j] = ElementType.Road;
                }
            }
        }
    }

    private static void addWaterPool(float[][] heightMap, int[][] typeMap, int centerX, int centerZ, int radius, float minHeight, float maxHeight) {
        float maxRange = lineDistance(radius, radius);
        for (int i = centerX - radius; i <= centerX + radius; i++) {
            for (int j = centerZ - radius; j <= centerZ + radius; j++) {
                if (i >= 0 && i < heightMap.length && j >= 0 && j < heightMap[0].length) {
                    float dist = (float) Math.sqrt(Math.pow(i - centerX, 2) + Math.pow(j - centerZ, 2));
                    if (dist <= radius) {
                        float currRange = lineDistance(Math.abs(centerX - i), Math.abs(centerZ - j));
                        float rate = currRange / Math.max(currRange, maxRange);
                        heightMap[i][j] = minHeight - (1 - rate) * 1.5f;
                        typeMap[i][j] = ElementType.WaterPool;
                    }
                }
            }
        }
    }

    private static void addLawn(float[][] heightMap, int[][] typeMap, int centerX, int centerZ, int radius, float minHeight, float maxHeight) {
        for (int i = centerX - radius; i <= centerX + radius; i++) {
            for (int j = centerZ - radius; j <= centerZ + radius; j++) {
                if (i >= 0 && i < heightMap.length && j >= 0 && j < heightMap[0].length) {
                    float dist = (float) Math.sqrt(Math.pow(i - centerX, 2) + Math.pow(j - centerZ, 2));
                    if (dist <= radius && typeMap[i][j] == ElementType.Land) {
                        typeMap[i][j] = ElementType.Lawn;
                    }
                }
            }
        }
    }

    private static void addBuilding(float[][] heightMap, int[][] typeMap, int startX, int startZ, int width, int depth, float height, float minHeight, float maxHeight) {
        for (int i = startX; i < startX + width && i < heightMap.length; i++) {
            for (int j = startZ; j < startZ + depth && j < heightMap[0].length; j++) {
                boolean isTop = i > startX + width / 4 && i <= startX + width / 4 * 3 && j > startZ + depth / 4 && j <= startZ + depth / 4 * 3;
                heightMap[i][j] = isTop ? height + 2 : height;
                typeMap[i][j] = ElementType.Building;
            }
        }
    }

    private static void addTrees(List<Vertex> vertices, float[][] heightMap, int[][] typeMap, int gridSize) {
        Random random = new Random(42);
        int treeCount = gridSize;

        for (int t = 0; t < treeCount; t++) {
            int i = random.nextInt(gridSize - 4) + 2;
            int j = random.nextInt(gridSize - 4) + 2;

            if ((typeMap[i][j] == ElementType.Land || typeMap[i][j] == ElementType.Lawn) &&
                    heightMap[i][j] > -1.0f && heightMap[i][j] < 5.0f) {

                float x = (i / (float) gridSize - 0.5f) * TERRAIN_SIZE;
                float z = (j / (float) gridSize - 0.5f) * TERRAIN_SIZE;
                float y = heightMap[i][j];

                addTree(vertices, x, y, z);
            }
        }
    }

    private static void addTree(List<Vertex> vertices, float x, float baseY, float z) {
        float trunkHeight = 2.0f;
        float trunkWidth = 0.3f;
        addCube(vertices, x, baseY + trunkHeight / 2, z, trunkWidth, trunkHeight, ElementType.Trunk, trunkWidth,
                new float[]{0.4f, 0.2f, 0.1f});

        float crownRadius = 1.2f;
        addSphere(vertices, x, baseY + trunkHeight + crownRadius / 2, z, crownRadius,
                new float[]{0.1f, 0.5f, 0.1f});
    }

    private static void addDetailedBuildings(List<Vertex> vertices, float[][] heightMap, int[][] typeMap, int gridSize) {
        Random random = new Random(42);
        int buildingCount = gridSize / 20;

        for (int b = 0; b < buildingCount; b++) {
            int startX = random.nextInt(gridSize - 8) + 4;
            int startZ = random.nextInt(gridSize - 8) + 4;
            int width = random.nextInt(4) + 4;
            int depth = random.nextInt(4) + 4;
            float height = random.nextFloat() * 4.0f + 3.0f;

            boolean validLocation = true;
            for (int i = startX; i < startX + width && i < gridSize; i++) {
                for (int j = startZ; j < startZ + depth && j < gridSize; j++) {
                    if (typeMap[i][j] == ElementType.Road || typeMap[i][j] == ElementType.WaterPool) {
                        validLocation = false;
                        break;
                    }
                }
                if (!validLocation) break;
            }

            if (validLocation) {
                addBuildingWithCube(vertices, startX, startZ, width, depth, height, gridSize);
            }
        }
    }

    private static void addBuildingWithCube(List<Vertex> vertices, int startX, int startZ, int width, int depth, float height, int gridSize) {
        float centerX = (startX + width / 2.0f) / gridSize * TERRAIN_SIZE - TERRAIN_SIZE / 2;
        float centerZ = (startZ + depth / 2.0f) / gridSize * TERRAIN_SIZE - TERRAIN_SIZE / 2;
        float baseY = 0f;

        addCube(vertices, centerX, baseY + height / 2, centerZ,
                width * TERRAIN_SIZE / gridSize, height, ElementType.HouseWall, depth * TERRAIN_SIZE / gridSize,
                new float[]{0.6f, 0.4f, 0.2f});

        addCube(vertices, centerX, baseY + height + 0.5f, centerZ,
                (width + 0.5f) * TERRAIN_SIZE / gridSize, 1.0f, ElementType.Roof, (depth + 0.5f) * TERRAIN_SIZE / gridSize,
                new float[]{0.3f, 0.2f, 0.1f});
    }

    private static void addCube(List<Vertex> vertices, float centerX, float centerY, float centerZ,
                                float width, float height, int type, float depth, float[] color) {
        float halfWidth = width / 2;
        float halfHeight = height / 2;
        float halfDepth = depth / 2;

        // 立方体的8个顶点
        float[][] cubeVertices = {
                // 前面
                {centerX - halfWidth, centerY - halfHeight, centerZ + halfDepth},
                {centerX + halfWidth, centerY - halfHeight, centerZ + halfDepth},
                {centerX + halfWidth, centerY + halfHeight, centerZ + halfDepth},
                {centerX - halfWidth, centerY + halfHeight, centerZ + halfDepth},
                // 后面
                {centerX - halfWidth, centerY - halfHeight, centerZ - halfDepth},
                {centerX + halfWidth, centerY - halfHeight, centerZ - halfDepth},
                {centerX + halfWidth, centerY + halfHeight, centerZ - halfDepth},
                {centerX - halfWidth, centerY + halfHeight, centerZ - halfDepth}
        };

        // 定义每个面的三角形和对应的纹理坐标
        Object[][] faceData = {
                // 前面 - 两个三角形和纹理坐标
                {new int[]{0, 1, 2, 0, 2, 3}, new float[]{0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1}},
                // 后面
                {new int[]{5, 4, 7, 5, 7, 6}, new float[]{0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1}},
                // 左面
                {new int[]{4, 0, 3, 4, 3, 7}, new float[]{0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1}},
                // 右面
                {new int[]{1, 5, 6, 1, 6, 2}, new float[]{0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1}},
                // 上面
                {new int[]{3, 2, 6, 3, 6, 7}, new float[]{0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1}},
                // 下面
                {new int[]{4, 5, 1, 4, 1, 0}, new float[]{0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1}}
        };

        for (int faceIndex = 0; faceIndex < faceData.length; faceIndex++) {
            int[] triangleIndices = (int[]) faceData[faceIndex][0];
            float[] texCoords = (float[]) faceData[faceIndex][1];

            // 计算当前面的法线
            int v1Idx = triangleIndices[0];
            int v2Idx = triangleIndices[1];
            int v3Idx = triangleIndices[2];
            float[] normal = calculateNormal(
                    cubeVertices[v1Idx][0], cubeVertices[v1Idx][1], cubeVertices[v1Idx][2],
                    cubeVertices[v2Idx][0], cubeVertices[v2Idx][1], cubeVertices[v2Idx][2],
                    cubeVertices[v3Idx][0], cubeVertices[v3Idx][1], cubeVertices[v3Idx][2]
            );

            // 添加6个顶点（2个三角形）
            for (int i = 0; i < 6; i++) {
                int vertexIndex = triangleIndices[i];
                float[] vertex = cubeVertices[vertexIndex];
                float u = texCoords[i * 2];
                float v = texCoords[i * 2 + 1];

                addVertexWithTexCoord(vertices, vertex[0], vertex[1], vertex[2],
                        type, color, normal, u, v);
            }
        }
    }

    private static void addSphere(List<Vertex> vertices, float centerX, float centerY, float centerZ,
                                  float radius, float[] color) {
        int stacks = 8;  // 经线分段数
        int sectors = 8; // 纬线分段数

        for (int i = 0; i < stacks; ++i) {
            float phi = (float) (Math.PI * i / stacks);
            float nextPhi = (float) (Math.PI * (i + 1) / stacks);

            for (int j = 0; j < sectors; ++j) {
                float theta = (float) (2 * Math.PI * j / sectors);
                float nextTheta = (float) (2 * Math.PI * (j + 1) / sectors);

                // 当前面的四个顶点
                float[][] points = new float[4][3];
                points[0] = getSpherePoint(centerX, centerY, centerZ, radius, phi, theta);
                points[1] = getSpherePoint(centerX, centerY, centerZ, radius, phi, nextTheta);
                points[2] = getSpherePoint(centerX, centerY, centerZ, radius, nextPhi, nextTheta);
                points[3] = getSpherePoint(centerX, centerY, centerZ, radius, nextPhi, theta);

                // 将四边形分成两个三角形 - 使用逆时针顺序
                float[] normal1 = calculateNormal(points[0][0], points[0][1], points[0][2],
                        points[1][0], points[1][1], points[1][2],
                        points[2][0], points[2][1], points[2][2]);

                float[] normal2 = calculateNormal(points[0][0], points[0][1], points[0][2],
                        points[2][0], points[2][1], points[2][2],
                        points[3][0], points[3][1], points[3][2]);

                // 第一个三角形 - 逆时针
                addVertex(vertices, points[0][0], points[0][1], points[0][2], ElementType.Canopy, color, normal1);
                addVertex(vertices, points[1][0], points[1][1], points[1][2], ElementType.Canopy, color, normal1);
                addVertex(vertices, points[2][0], points[2][1], points[2][2], ElementType.Canopy, color, normal1);

                // 第二个三角形 - 逆时针
                addVertex(vertices, points[0][0], points[0][1], points[0][2], ElementType.Canopy, color, normal2);
                addVertex(vertices, points[2][0], points[2][1], points[2][2], ElementType.Canopy, color, normal2);
                addVertex(vertices, points[3][0], points[3][1], points[3][2], ElementType.Canopy, color, normal2);
            }
        }
    }

    private static float[] getSpherePoint(float centerX, float centerY, float centerZ,
                                          float radius, float phi, float theta) {
        float x = centerX + radius * (float) (Math.sin(phi) * Math.cos(theta));
        float y = centerY + radius * (float) (Math.cos(phi));
        float z = centerZ + radius * (float) (Math.sin(phi) * Math.sin(theta));
        return new float[]{x, y, z};
    }

    private static MeshData createMeshData(List<Vertex> vertices, float minHeight, float maxHeight) {
        MeshData meshData = new MeshData();
        meshData.vertexCount = vertices.size();

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

        return meshData;
    }

    private static float lineDistance(float dx, float dy) {
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static FloatBuffer createFloatBuffer(float[] array) {
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocateDirect(array.length * 4);
        bb.order(java.nio.ByteOrder.nativeOrder());
        FloatBuffer buffer = bb.asFloatBuffer();
        buffer.put(array);
        buffer.position(0);
        return buffer;
    }

    private static IntBuffer createIntBuffer(int[] array) {
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocateDirect(array.length * 4);
        bb.order(java.nio.ByteOrder.nativeOrder());
        IntBuffer buffer = bb.asIntBuffer();
        buffer.put(array);
        buffer.position(0);
        return buffer;
    }
}
