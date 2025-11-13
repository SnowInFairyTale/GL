#version 320 es
#extension GL_EXT_tessellation_shader : enable

layout(triangles, equal_spacing, ccw) in;

uniform mat4 uMVPMatrix;
uniform mat4 uModelMatrix;
uniform sampler2D uHeightMap;
uniform float uTerrainSize;
uniform vec3 uCameraPosition;
uniform vec3 uLightPosition;

in vec3 tcPosition[];

out vec3 vWorldPosition;
out vec3 vNormal;
out vec3 vColor;
out vec2 vTexCoord;
out float vHeight;

// 双线性插值
float sampleHeightMap(vec2 uv) {
    // 从高度图采样高度
    float height = texture(uHeightMap, uv).r;
    // 将[0,1]范围映射到实际高度范围
    return height * 20.0 - 10.0; // -10 到 +10 的范围
}

// 计算法线（使用高度图梯度）
vec3 calculateNormal(vec2 uv) {
    float texelSize = 1.0 / 1024.0; // 假设高度图是1024x1024

    // 采样周围点的高度
    float hL = sampleHeightMap(uv + vec2(-texelSize, 0.0));
    float hR = sampleHeightMap(uv + vec2(texelSize, 0.0));
    float hB = sampleHeightMap(uv + vec2(0.0, -texelSize));
    float hT = sampleHeightMap(uv + vec2(0.0, texelSize));

    // 计算梯度
    float dx = (hR - hL) / (2.0 * texelSize);
    float dz = (hT - hB) / (2.0 * texelSize);

    // 法线 = (-dx, 1, -dz) 然后归一化
    return normalize(vec3(-dx, 1.0, -dz));
}

// 根据高度和法线计算颜色
vec3 calculateTerrainColor(float height, vec3 normal) {
    // 定义颜色关键点
    vec3 deepWater = vec3(0.0, 0.2, 0.6);
    vec3 shallowWater = vec3(0.0, 0.4, 0.8);
    vec3 sand = vec3(0.76, 0.7, 0.5);
    vec3 grass = vec3(0.2, 0.6, 0.2);
    vec3 forest = vec3(0.1, 0.4, 0.1);
    vec3 rock = vec3(0.5, 0.5, 0.5);
    vec3 snow = vec3(0.9, 0.9, 0.9);

    // 基于高度的颜色混合
    if (height < -2.0) return deepWater;
    else if (height < 0.0) return mix(deepWater, shallowWater, (height + 2.0) / 2.0);
    else if (height < 1.0) return mix(shallowWater, sand, height);
    else if (height < 3.0) return mix(sand, grass, (height - 1.0) / 2.0);
    else if (height < 6.0) return mix(grass, forest, (height - 3.0) / 3.0);
    else if (height < 8.0) return mix(forest, rock, (height - 6.0) / 2.0);
    else return mix(rock, snow, (height - 8.0) / 2.0);
}

void main() {
    // 使用重心坐标插值原始三角形顶点
    vec3 position =
    gl_TessCoord.x * tcPosition[0] +
    gl_TessCoord.y * tcPosition[1] +
    gl_TessCoord.z * tcPosition[2];

    // 计算纹理坐标
    vec2 terrainUV = (position.xz + uTerrainSize * 0.5) / uTerrainSize;
    terrainUV = clamp(terrainUV, 0.01, 0.99); // 避免纹理边缘问题

    // 从高度图采样高度
    float height = sampleHeightMap(terrainUV);
    position.y = height;

    // 计算世界坐标
    vWorldPosition = vec3(uModelMatrix * vec4(position, 1.0));

    // 计算法线
    vNormal = calculateNormal(terrainUV);

    // 计算颜色
    vColor = calculateTerrainColor(height, vNormal);

    // 传递其他属性
    vTexCoord = terrainUV;
    vHeight = height;

    // 计算最终位置
    gl_Position = uMVPMatrix * vec4(position, 1.0);
}