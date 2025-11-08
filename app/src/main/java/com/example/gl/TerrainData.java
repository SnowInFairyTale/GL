package com.example.gl;

// TerrainData.java

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TerrainData {
    private static final int GRID_SIZE = 50; // 50x50的网格
    private static final float TERRAIN_SIZE = 100.0f;
    private static final float MAX_HEIGHT = 10.0f;

    public static class Vertex {
        public float x, y, z;
        public float r, g, b;
        public float nx, ny, nz; // 法线
        public float type; // 新增：地形类型

        public Vertex(float x, float y, float z, float r, float g, float b, float type) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.r = r;
            this.g = g;
            this.b = b;
            this.type = type;
        }
    }

    public static class MeshData {
        public FloatBuffer vertices;
        public FloatBuffer colors;
        public FloatBuffer normals;
        public FloatBuffer types; // 新增：类型缓冲区
        public int vertexCount;
    }

    public static MeshData generateTerrainMesh() {
        List<Vertex> vertexList = new ArrayList<>();
        float[][] heightMap = new float[GRID_SIZE][GRID_SIZE];
        int[][] typeMap = new int[GRID_SIZE][GRID_SIZE];

        Random random = new Random(42);
        float minHeight = 0;
        float maxHeight = 0;

        // 生成高度图
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                float x = (i / (float) GRID_SIZE - 0.5f) * TERRAIN_SIZE;
                float z = (j / (float) GRID_SIZE - 0.5f) * TERRAIN_SIZE;

                // 基础地形高度
                float height = (float) (Math.sin(x * 0.1) * Math.cos(z * 0.1) * 3.0f +
                        Math.sin(x * 0.05) * Math.cos(z * 0.03) * 2.0f);

                // 添加随机噪声
                height += random.nextFloat() * 2.0f - 1.0f;
                height = Math.max(-2.0f, Math.min(MAX_HEIGHT, height));

                heightMap[i][j] = height;
                typeMap[i][j] = 0; // 默认地面
                minHeight = Math.min(minHeight, height);
                maxHeight = Math.max(maxHeight, height);
            }
        }

        // 添加道路
        addRoad(heightMap, typeMap, GRID_SIZE / 2, 0, GRID_SIZE, 8, minHeight, maxHeight);

        // 添加水坑
        addWaterPool(heightMap, typeMap, GRID_SIZE / 4, GRID_SIZE / 4, 6, minHeight, maxHeight);

        // 添加草坪
        addLawn(heightMap, typeMap, GRID_SIZE * 3 / 4, GRID_SIZE * 3 / 4, 10, minHeight, maxHeight);

        // 添加建筑物区域
        addBuilding(heightMap, typeMap, GRID_SIZE / 4, GRID_SIZE * 3 / 4, 4, 4, 5.0f, minHeight, maxHeight);

        // 生成网格顶点 - 修复顶点顺序为逆时针
        for (int i = 0; i < GRID_SIZE - 1; i++) {
            for (int j = 0; j < GRID_SIZE - 1; j++) {
                // 两个三角形组成一个四边形
                addQuad(vertexList, heightMap, typeMap, i, j, i + 1, j, i, j + 1);
                addQuad(vertexList, heightMap, typeMap, i + 1, j, i + 1, j + 1, i, j + 1);
            }
        }

        // 添加树木
        addTrees(vertexList, heightMap, typeMap);

        // 添加详细建筑物
        addDetailedBuildings(vertexList, heightMap, typeMap);

        // 转换为FloatBuffer
        return createMeshData(vertexList);
    }

    private static void addRoad(float[][] heightMap, int[][] typeMap, int centerX, int centerZ, int length, int width, float minHeight, float maxHeight) {
        int halfWidth = width / 2;
        for (int i = centerX - length / 2; i < centerX + length / 2; i++) {
            for (int j = centerZ - halfWidth; j < centerZ + halfWidth; j++) {
                if (i >= 0 && i < GRID_SIZE && j >= 0 && j < GRID_SIZE) {
                    heightMap[i][j] = (maxHeight - minHeight) / 2; // 平坦道路
                    typeMap[i][j] = ElementType.Road; // 道路类型
                }
            }
        }
    }

    private static void addWaterPool(float[][] heightMap, int[][] typeMap, int centerX, int centerZ, int radius, float minHeight, float maxHeight) {
        float maxRange = MathUtils.lineDistance(radius, radius);
        for (int i = centerX - radius; i <= centerX + radius; i++) {
            for (int j = centerZ - radius; j <= centerZ + radius; j++) {
                if (i >= 0 && i < GRID_SIZE && j >= 0 && j < GRID_SIZE) {
                    float dist = (float) Math.sqrt(Math.pow(i - centerX, 2) + Math.pow(j - centerZ, 2));
                    if (dist <= radius) {
                        float currRange = MathUtils.lineDistance(Math.abs(centerX - i), Math.abs(centerZ - j));
                        float rate = currRange / Math.max(currRange, maxRange);
                        heightMap[i][j] = minHeight - rate * 1.5f; // 水面高度
                        typeMap[i][j] = ElementType.WaterPool; // 水坑类型
                    }
                }
            }
        }
    }

    private static void addLawn(float[][] heightMap, int[][] typeMap, int centerX, int centerZ, int radius, float minHeight, float maxHeight) {
        for (int i = centerX - radius; i <= centerX + radius; i++) {
            for (int j = centerZ - radius; j <= centerZ + radius; j++) {
                if (i >= 0 && i < GRID_SIZE && j >= 0 && j < GRID_SIZE) {
                    float dist = (float) Math.sqrt(Math.pow(i - centerX, 2) + Math.pow(j - centerZ, 2));
                    if (dist <= radius && typeMap[i][j] == 0) {
                        typeMap[i][j] = ElementType.Lawn; // 草坪类型
                    }
                }
            }
        }
    }

    private static void addBuilding(float[][] heightMap, int[][] typeMap, int startX, int startZ, int width, int depth, float height, float minHeight, float maxHeight) {
        for (int i = startX; i < startX + width && i < GRID_SIZE; i++) {
            for (int j = startZ; j < startZ + depth && j < GRID_SIZE; j++) {
                heightMap[i][j] = height;
                typeMap[i][j] = ElementType.Building; // 建筑物类型
            }
        }
    }

    // 在 addQuad 方法中，修改法线计算
    private static void addQuad(List<Vertex> vertices, float[][] heightMap, int[][] typeMap,
                                int i1, int j1, int i2, int j2, int i3, int j3) {
        float x1 = (i1 / (float) GRID_SIZE - 0.5f) * TERRAIN_SIZE;
        float z1 = (j1 / (float) GRID_SIZE - 0.5f) * TERRAIN_SIZE;
        float y1 = heightMap[i1][j1];

        float x2 = (i2 / (float) GRID_SIZE - 0.5f) * TERRAIN_SIZE;
        float z2 = (j2 / (float) GRID_SIZE - 0.5f) * TERRAIN_SIZE;
        float y2 = heightMap[i2][j2];

        float x3 = (i3 / (float) GRID_SIZE - 0.5f) * TERRAIN_SIZE;
        float z3 = (j3 / (float) GRID_SIZE - 0.5f) * TERRAIN_SIZE;
        float y3 = heightMap[i3][j3];

        // 为每个顶点计算独立的法线（简化版本）
        float[] normal = {0.0f, 1.0f, 0.0f}; // 默认朝上的法线

        // 添加三个顶点（一个三角形）- 逆时针顺序
        addVertex(vertices, x1, y1, z1, typeMap[i1][j1], normal);
        addVertex(vertices, x2, y2, z2, typeMap[i2][j2], normal);
        addVertex(vertices, x3, y3, z3, typeMap[i3][j3], normal);
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

    private static float[] getColorForType(int type) {
        Random random = new Random(42);
        switch (type) {
            case 1: // 道路 - 更真实的灰色，带一些变化
                float gray = 0.3f + random.nextFloat() * 0.1f;
                return new float[]{gray, gray, gray};

            case 2: // 水坑 - 更生动的蓝色，带一些深度变化
                float blueDepth = 0.6f + random.nextFloat() * 0.2f;
                return new float[]{0.1f, 0.4f, blueDepth};

            case 3: // 草坪 - 更丰富的绿色
                float greenVar = random.nextFloat() * 0.2f;
                return new float[]{0.1f + greenVar * 0.2f, 0.5f + greenVar, 0.1f + greenVar * 0.1f};

            case 4: // 建筑物 - 更真实的材质颜色
                float brownVar = random.nextFloat() * 0.15f;
                return new float[]{0.5f + brownVar, 0.3f + brownVar, 0.1f + brownVar};

            default: // 地面 - 更自然的土黄色
                float groundVar = random.nextFloat() * 0.15f;
                return new float[]{0.7f + groundVar, 0.6f + groundVar, 0.45f + groundVar};
        }
    }

    private static void addTrees(List<Vertex> vertices, float[][] heightMap, int[][] typeMap) {
        Random random = new Random(42);
        int treeCount = 15;

        for (int t = 0; t < treeCount; t++) {
            int i = random.nextInt(GRID_SIZE - 4) + 2;
            int j = random.nextInt(GRID_SIZE - 4) + 2;

            // 确保在草坪或普通地面上，且不在道路、水坑或建筑物上
            if ((typeMap[i][j] == 0 || typeMap[i][j] == 3) &&
                    heightMap[i][j] > -1.0f && heightMap[i][j] < 5.0f) {

                float x = (i / (float) GRID_SIZE - 0.5f) * TERRAIN_SIZE;
                float z = (j / (float) GRID_SIZE - 0.5f) * TERRAIN_SIZE;
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

    private static void addDetailedBuildings(List<Vertex> vertices, float[][] heightMap, int[][] typeMap) {
        Random random = new Random(42);
        int buildingCount = 3;

        for (int b = 0; b < buildingCount; b++) {
            int startX = random.nextInt(GRID_SIZE - 6) + 3;
            int startZ = random.nextInt(GRID_SIZE - 6) + 3;
            int width = random.nextInt(3) + 3;
            int depth = random.nextInt(3) + 3;
            float height = random.nextFloat() * 4.0f + 3.0f;

            // 确保不在道路或水坑上
            boolean validLocation = true;
            for (int i = startX; i < startX + width && i < GRID_SIZE; i++) {
                for (int j = startZ; j < startZ + depth && j < GRID_SIZE; j++) {
                    if (typeMap[i][j] == 1 || typeMap[i][j] == 2) {
                        validLocation = false;
                        break;
                    }
                }
                if (!validLocation) break;
            }

            if (validLocation) {
                addBuildingWithCube(vertices, startX, startZ, width, depth, height);
            }
        }
    }

    private static void addBuildingWithCube(List<Vertex> vertices, int startX, int startZ, int width, int depth, float height) {
        float centerX = (startX + width / 2.0f) / GRID_SIZE * TERRAIN_SIZE - TERRAIN_SIZE / 2;
        float centerZ = (startZ + depth / 2.0f) / GRID_SIZE * TERRAIN_SIZE - TERRAIN_SIZE / 2;
        float baseY = 0f; // 假设地面高度为0

        // 建筑物主体
        addCube(vertices, centerX, baseY + height / 2, centerZ,
                width * TERRAIN_SIZE / GRID_SIZE, height, ElementType.HouseWall, depth * TERRAIN_SIZE / GRID_SIZE,
                new float[]{0.6f, 0.4f, 0.2f});

        // 屋顶
        addCube(vertices, centerX, baseY + height + 0.5f, centerZ,
                (width + 0.5f) * TERRAIN_SIZE / GRID_SIZE, 1.0f, ElementType.Roof, (depth + 0.5f) * TERRAIN_SIZE / GRID_SIZE,
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

        // 立方体的6个面（每个面2个三角形）- 全部使用逆时针顺序
        int[][] cubeFaces = {
                // 前面 (从外面看是逆时针)
                {0, 1, 2}, {0, 2, 3},
                // 后面
                {5, 4, 7}, {5, 7, 6},
                // 左面
                {4, 0, 3}, {4, 3, 7},
                // 右面 (从外面看是逆时针)
                {1, 5, 6}, {1, 6, 2},
                // 上面 (从外面看是逆时针)
                {3, 2, 6}, {3, 6, 7},
                // 下面 (从外面看是逆时针)
                {4, 5, 1}, {4, 1, 0}
        };

        // 为每个面计算法线并添加顶点
        for (int[] face : cubeFaces) {
            float[] v1 = cubeVertices[face[0]];
            float[] v2 = cubeVertices[face[1]];
            float[] v3 = cubeVertices[face[2]];

            float[] normal = calculateNormal(v1[0], v1[1], v1[2],
                    v2[0], v2[1], v2[2],
                    v3[0], v3[1], v3[2]);

            addVertex(vertices, v1[0], v1[1], v1[2], type, color, normal);
            addVertex(vertices, v2[0], v2[1], v2[2], type, color, normal);
            addVertex(vertices, v3[0], v3[1], v3[2], type, color, normal);
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

    private static MeshData createMeshData(List<Vertex> vertices) {
        MeshData meshData = new MeshData();
        meshData.vertexCount = vertices.size();

        // 创建顶点缓冲区
        float[] vertexArray = new float[vertices.size() * 3];
        float[] colorArray = new float[vertices.size() * 3];
        float[] normalArray = new float[vertices.size() * 3];
        float[] typeArray = new float[vertices.size()]; // 新增：类型数组

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

            typeArray[i] = v.type; // 存储类型
        }

        meshData.vertices = createFloatBuffer(vertexArray);
        meshData.colors = createFloatBuffer(colorArray);
        meshData.normals = createFloatBuffer(normalArray);
        meshData.types = createFloatBuffer(typeArray); // 创建类型缓冲区

        return meshData;
    }

    private static FloatBuffer createFloatBuffer(float[] array) {
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocateDirect(array.length * 4);
        bb.order(java.nio.ByteOrder.nativeOrder());
        FloatBuffer buffer = bb.asFloatBuffer();
        buffer.put(array);
        buffer.position(0);
        return buffer;
    }
}