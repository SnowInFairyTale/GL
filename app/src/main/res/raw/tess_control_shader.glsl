#version 320 es
#extension GL_EXT_tessellation_shader : require

layout(vertices = 3) out;

uniform float uTessLevel;
uniform vec3 uCameraPosition;
uniform float uTerrainSize;

out vec3 tcPosition[];

// 计算点到相机的距离
float getDistanceToCamera(vec3 worldPos) {
    return length(worldPos - uCameraPosition);
}

// 基于距离和曲率的自适应细分
float calculateAdaptiveTessLevel(vec3 p0, vec3 p1, vec3 p2) {
    // 计算三角形中心
    vec3 center = (p0 + p1 + p2) / 3.0;

    // 基于距离的细分
    float distance = getDistanceToCamera(center);
    float distanceFactor = clamp(50.0 / (distance + 1.0), 0.5, 4.0);

    // 基于曲率的细分（三角形面积越大，曲率可能越小）
    vec3 edge1 = p1 - p0;
    vec3 edge2 = p2 - p0;
    float area = length(cross(edge1, edge2));
    float curvatureFactor = clamp(area * 10.0, 0.5, 3.0);

    // 基于高度的细分（高处需要更多细节）
    float heightFactor = 1.0 + abs(center.y) * 0.1;

    return uTessLevel * distanceFactor * curvatureFactor * heightFactor;
}

void main() {
    // 传递位置到评估着色器
    tcPosition[gl_InvocationID] = gl_in[gl_InvocationID].gl_Position.xyz;

    // 只在第一个调用中设置细分级别
    if (gl_InvocationID == 0) {
        vec3 p0 = gl_in[0].gl_Position.xyz;
        vec3 p1 = gl_in[1].gl_Position.xyz;
        vec3 p2 = gl_in[2].gl_Position.xyz;

        float adaptiveLevel = calculateAdaptiveTessLevel(p0, p1, p2);

        // 设置细分级别
        gl_TessLevelInner[0] = adaptiveLevel;
        gl_TessLevelOuter[0] = adaptiveLevel;
        gl_TessLevelOuter[1] = adaptiveLevel;
        gl_TessLevelOuter[2] = adaptiveLevel;
    }
}