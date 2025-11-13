package com.example.gl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TerrainData {
    // 基础网格配置
    private static final int BASE_GRID_SIZE = 50; // 传统方法生成的网格大小
    private static final int FINAL_GRID_SIZE = 100; // 插值后的最终网格大小
    private static final float TERRAIN_SIZE = 100.0f;
    private static final float MAX_HEIGHT = 10.0f;

    // 插值配置
    private static final boolean USE_INTERPOLATION = false; // 是否启用插值

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
        public FloatBuffer texCoords; // 纹理坐标缓冲区
        public IntBuffer types;
        public int vertexCount;
        public float minHeight;
        public float maxHeight;
    }

    // 生成传统高度图（50x50）
    private static float[][] generateBaseHeightMap(Random random) {
        float[][] baseHeightMap = new float[BASE_GRID_SIZE][BASE_GRID_SIZE];

        for (int i = 0; i < BASE_GRID_SIZE; i++) {
            for (int j = 0; j < BASE_GRID_SIZE; j++) {
                float x = (i / (float) BASE_GRID_SIZE - 0.5f) * TERRAIN_SIZE;
                float z = (j / (float) BASE_GRID_SIZE - 0.5f) * TERRAIN_SIZE;

                // 传统的地形高度计算
                float height = (float) (Math.sin(x * 0.1) * Math.cos(z * 0.1) * 3.0f +
                        Math.sin(x * 0.05) * Math.cos(z * 0.03) * 2.0f);

                // 添加随机噪声
                height += random.nextFloat() * 2.0f - 1.0f;
                height = Math.max(-2.0f, Math.min(MAX_HEIGHT, height));

                baseHeightMap[i][j] = height;
            }
        }

        return baseHeightMap;
    }

    // 在基础高度图上进行双线性插值生成最终高度图
    private static float[][] interpolateHeightMap(float[][] baseHeightMap, Random random) {
        float[][] finalHeightMap = new float[FINAL_GRID_SIZE][FINAL_GRID_SIZE];

        for (int i = 0; i < FINAL_GRID_SIZE; i++) {
            for (int j = 0; j < FINAL_GRID_SIZE; j++) {
                // 计算在基础网格中的对应位置
                float baseX = i / (float)FINAL_GRID_SIZE * (BASE_GRID_SIZE - 1);
                float baseZ = j / (float)FINAL_GRID_SIZE * (BASE_GRID_SIZE - 1);

                // 双线性插值
                finalHeightMap[i][j] = bilinearInterpolate(baseHeightMap, baseX, baseZ);

                // 添加细微的高频噪声以增加真实感，但幅度较小
                finalHeightMap[i][j] += (random.nextFloat() * 0.2f - 0.1f);
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

        // 边界检查
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

        // 双线性插值公式
        float top = q11 * (1 - dx) + q21 * dx;
        float bottom = q12 * (1 - dx) + q22 * dx;
        return top * (1 - dz) + bottom * dz;
    }

    // 平滑法线计算的核心方法
    private static void calculateSmoothNormals(List<Vertex> vertices) {
        // 使用更精确的键来识别相同位置的顶点
        Map<String, List<Integer>> positionMap = new HashMap<>();

        // 第一遍：收集所有相同位置的顶点索引
        for (int i = 0; i < vertices.size(); i++) {
            Vertex vertex = vertices.get(i);
            // 使用更精确的键，考虑浮点精度问题
            String key = String.format("%.4f,%.4f,%.4f", vertex.x, vertex.y, vertex.z);

            if (!positionMap.containsKey(key)) {
                positionMap.put(key, new ArrayList<>());
            }
            positionMap.get(key).add(i);
        }

        // 第二遍：为每个三角形计算面法线并累加到共享顶点
        Map<String, float[]> normalAccumulator = new HashMap<>();
        Map<String, Integer> normalCount = new HashMap<>();

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

    // 计算三角形面法线
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

    // 累加顶点法线
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

    public static MeshData generateTerrainMesh() {
        List<Vertex> vertexList = new ArrayList<>();
        float[][] heightMap;
        int[][] typeMap;

        Random random = new Random(42);
        float minHeight = 0;
        float maxHeight = 0;

        // 生成基础高度图（50x50）
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
        // 添加道路
        addRoad(baseHeightMap, baseTypeMap, BASE_GRID_SIZE / 2, 0, BASE_GRID_SIZE, 8, minHeight, maxHeight);

        // 添加水坑
        addWaterPool(baseHeightMap, baseTypeMap, BASE_GRID_SIZE / 4, BASE_GRID_SIZE / 4, 6, minHeight, maxHeight);

        // 添加草坪
        addLawn(baseHeightMap, baseTypeMap, BASE_GRID_SIZE * 3 / 4, BASE_GRID_SIZE * 3 / 4, 10, minHeight, maxHeight);

        // 添加建筑物区域
        addBuilding(baseHeightMap, baseTypeMap, BASE_GRID_SIZE / 4, BASE_GRID_SIZE * 3 / 4, 6, 6, 10.0f, minHeight, maxHeight);

        // 选择是否进行插值
        if (USE_INTERPOLATION) {
            // 使用插值生成最终高度图和类型图
            heightMap = interpolateHeightMap(baseHeightMap, random);
            typeMap = interpolateTypeMap(baseTypeMap);
        } else {
            // 直接使用基础网格
            heightMap = baseHeightMap;
            typeMap = baseTypeMap;
        }

        // 更新最终的高度范围
        final int finalGridSize = USE_INTERPOLATION ? FINAL_GRID_SIZE : BASE_GRID_SIZE;
        maxHeight = 0;
        for (int i = 0; i < finalGridSize; i++) {
            for (int j = 0; j < finalGridSize; j++) {
                float height = heightMap[i][j];
                maxHeight = Math.max(maxHeight, height);
            }
        }

        // 生成最终网格顶点 - 顶点顺序为逆时针
        for (int i = 0; i < finalGridSize - 1; i++) {
            for (int j = 0; j < finalGridSize - 1; j++) {
                // 两个三角形组成一个四边形
                addQuad(vertexList, heightMap, typeMap, i, j, i + 1, j, i, j + 1, finalGridSize);
                addQuad(vertexList, heightMap, typeMap, i + 1, j, i + 1, j + 1, i, j + 1, finalGridSize);
            }
        }

        // 添加树木
        addTrees(vertexList, heightMap, typeMap, finalGridSize);

        // 添加详细建筑物
        addDetailedBuildings(vertexList, heightMap, typeMap, finalGridSize);

        // 在创建网格数据前计算平滑法线
//        calculateSmoothNormals(vertexList);

        // 转换为FloatBuffer
        return createMeshData(vertexList, minHeight, maxHeight);
    }

    private static void addRoad(float[][] heightMap, int[][] typeMap, int centerX, int centerZ, int length, int width, float minHeight, float maxHeight) {
        int halfWidth = width / 2;
        for (int i = centerX - length / 2; i < centerX + length / 2; i++) {
            for (int j = centerZ - halfWidth; j < centerZ + halfWidth; j++) {
                if (i >= 0 && i < heightMap.length && j >= 0 && j < heightMap[0].length) {
                    heightMap[i][j] = (maxHeight - minHeight) / 2; // 平坦道路
                    typeMap[i][j] = ElementType.Road; // 道路类型
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
                        heightMap[i][j] = minHeight - (1 - rate) * 1.5f; // 水面高度
                        typeMap[i][j] = ElementType.WaterPool; // 水坑类型
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
                        typeMap[i][j] = ElementType.Lawn; // 草坪类型
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
                typeMap[i][j] = ElementType.Building; // 建筑物类型
            }
        }
    }

    // 在 addQuad 方法中，修改法线计算
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

        // 计算法线
//        float[] normal = calculateNormal(x1, y1, z1, x2, y2, z2, x3, y3, z3);

        // 使用默认法线，平滑法线会在后续统一计算
        float[] defaultNormal = {0.0f, 1.0f, 0.0f};

        // 添加三个顶点（一个三角形）- 逆时针顺序
        addVertex(vertices, x1, y1, z1, typeMap[i1][j1], defaultNormal);
        addVertex(vertices, x2, y2, z2, typeMap[i2][j2], defaultNormal);
        addVertex(vertices, x3, y3, z3, typeMap[i3][j3], defaultNormal);
    }

    private static float[] calculateNormal(float x1, float y1, float z1,
                                           float x2, float y2, float z2,
                                           float x3, float y3, float z3) {
        // 计算两个边向量（基于逆时针顺序）
        float ux = x2 - x1, uy = y2 - y1, uz = z2 - z1;
        float vx = x3 - x1, vy = y3 - y1, vz = z3 - z1;

        // 叉积得到法线 (u x v)
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;

        // 归一化
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0) {
            nx /= length;
            ny /= length;
            nz /= length;
        }

        return new float[]{nx, ny, nz};
    }

    private static void addVertex(List<Vertex> vertices, float x, float y, float z, int type, float[] normal) {
        float[] color = getColorForType(type);
        Vertex vertex = new Vertex(x, y, z, color[0], color[1], color[2], type);
        vertex.nx = normal[0];
        vertex.ny = normal[1];
        vertex.nz = normal[2];
        vertices.add(vertex);
    }

    // 重载addVertex方法以接受颜色数组
    private static void addVertex(List<Vertex> vertices, float x, float y, float z, int type, float[] color, float[] normal) {
        Vertex vertex = new Vertex(x, y, z, color[0], color[1], color[2], type);
        vertex.nx = normal[0];
        vertex.ny = normal[1];
        vertex.nz = normal[2];
        vertices.add(vertex);
    }

    // 新增：带纹理坐标的顶点添加方法
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
            case ElementType.Road: // 道路 - 更真实的灰色，带一些变化
                float gray = 0.25f + random.nextFloat() * 0.1f;
                return new float[]{gray, gray, gray};

            case ElementType.WaterPool: // 水坑 - 更生动的蓝色，带一些深度变化
                float blueDepth = 0.6f + random.nextFloat() * 0.2f;
                return new float[]{0.1f, 0.4f, blueDepth};

            case ElementType.Lawn: // 草坪 - 更丰富的绿色
                float greenVar = random.nextFloat() * 0.2f;
                return new float[]{0.1f + greenVar * 0.2f, 0.5f + greenVar, 0.1f + greenVar * 0.1f};

            case ElementType.Building: // 建筑物 - 更真实的材质颜色
            case ElementType.HouseWall:
                float brownVar = random.nextFloat() * 0.15f;
                return new float[]{0.5f + brownVar, 0.3f + brownVar, 0.1f + brownVar};

            case ElementType.Roof: // 屋顶 - 深棕色
                float roofVar = random.nextFloat() * 0.15f;
                return new float[]{0.3f + roofVar, 0.2f + roofVar, 0.1f + roofVar};

            case ElementType.Trunk: // 树干 - 棕色
                return new float[]{0.4f, 0.2f, 0.1f};

            case ElementType.Canopy: // 树冠 - 绿色
                return new float[]{0.1f, 0.5f, 0.1f};

            default: // 地面 - 更自然的土黄色
                float groundVar = random.nextFloat() * 0.15f;
                return new float[]{0.7f + groundVar, 0.6f + groundVar, 0.45f + groundVar};
        }
    }

    private static void addTrees(List<Vertex> vertices, float[][] heightMap, int[][] typeMap, int gridSize) {
        Random random = new Random(42);
        int treeCount = 15;

        for (int t = 0; t < treeCount; t++) {
            int i = random.nextInt(gridSize - 4) + 2;
            int j = random.nextInt(gridSize - 4) + 2;

            // 确保在草坪或普通地面上，且不在道路、水坑或建筑物上
            if ((typeMap[i][j] == ElementType.Land || typeMap[i][j] == ElementType.Lawn) &&
                    heightMap[i][j] > -1.0f && heightMap[i][j] < 5.0f) {

                float x = (i / (float) gridSize - 0.5f) * TERRAIN_SIZE;
                float z = (j / (float) gridSize - 0.5f) * TERRAIN_SIZE;
                float y = heightMap[i][j];

                // 创建简单的树（树干和树冠）
                addTree(vertices, x, y, z);
            }
        }
    }

    private static void addTree(List<Vertex> vertices, float x, float baseY, float z) {
        // 树干（棕色立方体）- 使用逆时针顶点顺序
        float trunkHeight = 2.0f;
        float trunkWidth = 0.3f;
        addCube(vertices, x, baseY + trunkHeight / 2, z, trunkWidth, trunkHeight, ElementType.Trunk, trunkWidth,
                new float[]{0.4f, 0.2f, 0.1f});

        // 树冠（绿色球体）
        float crownRadius = 1.2f;
        addSphere(vertices, x, baseY + trunkHeight + crownRadius / 2, z, crownRadius,
                new float[]{0.1f, 0.5f, 0.1f});
    }

    private static void addDetailedBuildings(List<Vertex> vertices, float[][] heightMap, int[][] typeMap, int gridSize) {
        Random random = new Random(42);
        int buildingCount = 3;

        for (int b = 0; b < buildingCount; b++) {
            int startX = random.nextInt(gridSize - 6) + 3;
            int startZ = random.nextInt(gridSize - 6) + 3;
            int width = random.nextInt(3) + 3;
            int depth = random.nextInt(3) + 3;
            float height = random.nextFloat() * 4.0f + 3.0f;

            // 确保不在道路或水坑上
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
        float baseY = 0f; // 假设地面高度为0

        // 建筑物主体 - 使用墙体纹理类型
        addCube(vertices, centerX, baseY + height / 2, centerZ,
                width * TERRAIN_SIZE / gridSize, height, ElementType.HouseWall, depth * TERRAIN_SIZE / gridSize,
                new float[]{0.6f, 0.4f, 0.2f});

        // 屋顶 - 使用屋顶纹理类型
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

    // 类型图插值（最近邻插值，因为类型是离散值）
    private static int[][] interpolateTypeMap(int[][] baseTypeMap) {
        int[][] finalTypeMap = new int[FINAL_GRID_SIZE][FINAL_GRID_SIZE];

        for (int i = 0; i < FINAL_GRID_SIZE; i++) {
            for (int j = 0; j < FINAL_GRID_SIZE; j++) {
                // 计算在基础网格中的对应位置（使用最近邻插值）
                int baseX = Math.round(i / (float)FINAL_GRID_SIZE * (BASE_GRID_SIZE - 1));
                int baseZ = Math.round(j / (float)FINAL_GRID_SIZE * (BASE_GRID_SIZE - 1));

                baseX = Math.max(0, Math.min(BASE_GRID_SIZE - 1, baseX));
                baseZ = Math.max(0, Math.min(BASE_GRID_SIZE - 1, baseZ));

                finalTypeMap[i][j] = baseTypeMap[baseX][baseZ];
            }
        }

        return finalTypeMap;
    }

    private static MeshData createMeshData(List<Vertex> vertices, float minHeight, float maxHeight) {
        MeshData meshData = new MeshData();
        meshData.vertexCount = vertices.size();

        // 创建顶点缓冲区
        float[] vertexArray = new float[vertices.size() * 3];
        float[] colorArray = new float[vertices.size() * 3];
        float[] normalArray = new float[vertices.size() * 3];
        float[] texCoordArray = new float[vertices.size() * 2]; // 纹理坐标数组
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

            texCoordArray[i * 2] = v.u;     // 纹理U坐标
            texCoordArray[i * 2 + 1] = v.v; // 纹理V坐标

            typeArray[i] = v.type;
        }

        meshData.vertices = createFloatBuffer(vertexArray);
        meshData.colors = createFloatBuffer(colorArray);
        meshData.normals = createFloatBuffer(normalArray);
        meshData.texCoords = createFloatBuffer(texCoordArray); // 创建纹理坐标缓冲区
        meshData.minHeight = minHeight;
        meshData.maxHeight = maxHeight;
        meshData.types = createIntBuffer(typeArray);

        return meshData;
    }

    // 工具方法：计算直线距离
    private static float lineDistance(float dx, float dy) {
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    // 创建 FloatBuffer
    private static FloatBuffer createFloatBuffer(float[] array) {
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocateDirect(array.length * 4);
        bb.order(java.nio.ByteOrder.nativeOrder());
        FloatBuffer buffer = bb.asFloatBuffer();
        buffer.put(array);
        buffer.position(0);
        return buffer;
    }

    // 创建 IntBuffer
    private static IntBuffer createIntBuffer(int[] array) {
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocateDirect(array.length * 4);
        bb.order(java.nio.ByteOrder.nativeOrder());
        IntBuffer buffer = bb.asIntBuffer();
        buffer.put(array);
        buffer.position(0);
        return buffer;
    }
}