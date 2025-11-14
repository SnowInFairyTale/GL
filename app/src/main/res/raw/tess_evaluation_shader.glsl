#version 320 es
#extension GL_EXT_tessellation_shader : enable

layout(triangles, equal_spacing, ccw) in;

uniform highp mat4 uMVPMatrix;
uniform highp mat4 uModelMatrix;
uniform highp sampler2D uHeightMap;
uniform highp float uTerrainSize;
uniform highp vec3 uCameraPosition;
uniform highp vec3 uLightPosition;

in highp vec3 tcPosition[];

out highp vec3 vWorldPosition;
out highp vec3 vNormal;
out mediump vec3 vColor;
out mediump vec2 vTexCoord;
out highp float vHeight;

// 双线性插值
highp float sampleHeightMap(mediump vec2 uv) {
    // 从高度图采样高度
    highp float height = texture(uHeightMap, uv).r;
    // 将[0,1]范围映射到实际高度范围
    return (1.0 - height) * 10.0;
}

// 计算法线
highp vec3 calculateNormal(mediump vec2 uv) {
    highp float texelSize = 1.0 / 1024.0;

    // 采样周围点的高度
    highp float hL = sampleHeightMap(uv + vec2(-texelSize, 0.0));
    highp float hR = sampleHeightMap(uv + vec2(texelSize, 0.0));
    highp float hB = sampleHeightMap(uv + vec2(0.0, -texelSize));
    highp float hT = sampleHeightMap(uv + vec2(0.0, texelSize));

    // 计算梯度
    highp float dx = (hR - hL) / (2.0 * texelSize);
    highp float dz = (hT - hB) / (2.0 * texelSize);

    return normalize(vec3(-dx, 1.0, -dz));
}

// 根据高度计算颜色
mediump vec3 calculateTerrainColor(highp float height, highp vec3 normal, mediump vec2 uv) {
//    // 定义颜色关键点
//    mediump vec3 deepWater = vec3(0.0, 0.2, 0.6);
//    mediump vec3 shallowWater = vec3(0.0, 0.4, 0.8);
//    mediump vec3 sand = vec3(0.76, 0.7, 0.5);
//    mediump vec3 grass = vec3(0.2, 0.6, 0.2);
//    mediump vec3 forest = vec3(0.1, 0.4, 0.1);
//    mediump vec3 rock = vec3(0.5, 0.5, 0.5);
//    mediump vec3 snow = vec3(0.9, 0.9, 0.9);
//
//    // 基于高度的颜色混合
//    if (height < -2.0) return deepWater;
//    else if (height < 0.0) return mix(deepWater, shallowWater, (height + 2.0) / 2.0);
//    else if (height < 1.0) return mix(shallowWater, sand, height);
//    else if (height < 3.0) return mix(sand, grass, (height - 1.0) / 2.0);
//    else if (height < 6.0) return mix(grass, forest, (height - 3.0) / 3.0);
//    else if (height < 8.0) return mix(forest, rock, (height - 6.0) / 2.0);
//    else return mix(rock, snow, (height - 8.0) / 2.0);
    vec4 texColor = texture(uHeightMap, uv);
    return texColor.rgb;
}

void main() {
    // 使用重心坐标插值
    highp vec3 position =
    gl_TessCoord.x * tcPosition[0] +
    gl_TessCoord.y * tcPosition[1] +
    gl_TessCoord.z * tcPosition[2];

    // 计算纹理坐标
    mediump vec2 terrainUV = (position.xz + uTerrainSize * 0.5) / uTerrainSize;
    terrainUV = clamp(terrainUV, 0.01, 0.99);

    // 从高度图采样高度
    highp float height = sampleHeightMap(terrainUV);
    position.y = height;

    // 计算世界坐标
    vWorldPosition = vec3(uModelMatrix * vec4(position, 1.0));

    // 计算法线
    vNormal = calculateNormal(terrainUV);

    // 计算颜色
    vColor = calculateTerrainColor(height, vNormal, terrainUV);

    // 传递其他属性
    vTexCoord = terrainUV;
    vHeight = height;

    // 计算最终位置
    gl_Position = uMVPMatrix * vec4(position, 1.0);
}