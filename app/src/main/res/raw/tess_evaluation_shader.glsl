#version 320 es
#extension GL_EXT_tessellation_shader : require

layout(triangles, equal_spacing, ccw) in;

uniform mat4 uMVPMatrix;
uniform mat4 uModelMatrix;
uniform sampler2D uHeightMap;
uniform float uTerrainSize;

out vec3 vWorldPosition;
out vec3 vNormal;
out vec2 vTexCoord;

// PN三角形曲面插值
vec3 interpolate3D(vec3 v0, vec3 v1, vec3 v2) {
    return gl_TessCoord.x * v0 + gl_TessCoord.y * v1 + gl_TessCoord.z * v2;
}

void main() {
    // 插值位置
    vec3 position = interpolate3D(gl_in[0].gl_Position.xyz,
                                 gl_in[1].gl_Position.xyz,
                                 gl_in[2].gl_Position.xyz);

    // 从高度图采样进行置换映射
    vec2 terrainUV = (position.xz + uTerrainSize * 0.5) / uTerrainSize;
    float height = texture(uHeightMap, terrainUV).r * 20.0 - 10.0;

    // 应用高度
    position.y = height;

    // 计算世界坐标
    vWorldPosition = vec3(uModelMatrix * vec4(position, 1.0));

    // 计算法线（使用高度图梯度）
    float texelSize = 1.0 / 1024.0; // 假设高度图是1024x1024
    float hL = texture(uHeightMap, terrainUV + vec2(-texelSize, 0.0)).r;
    float hR = texture(uHeightMap, terrainUV + vec2(texelSize, 0.0)).r;
    float hB = texture(uHeightMap, terrainUV + vec2(0.0, -texelSize)).r;
    float hT = texture(uHeightMap, terrainUV + vec2(0.0, texelSize)).r;

    vNormal = normalize(vec3(hL - hR, 2.0 * texelSize * 20.0, hB - hT));

    vTexCoord = terrainUV;
    gl_Position = uMVPMatrix * vec4(position, 1.0);
}