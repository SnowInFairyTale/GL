#version 320 es
precision mediump float;

in vec3 vWorldPosition;
in vec3 vNormal;
in vec3 vColor;
in vec2 vTexCoord;
in float vHeight;

uniform vec3 uLightPosition;
uniform vec3 uCameraPosition;

out vec4 fragColor;

// 改进的光照计算
vec3 calculateLighting(vec3 normal, vec3 lightDir, vec3 viewDir, vec3 baseColor) {
    // 环境光
    float ambient = 0.3;

    // 半兰伯特漫反射（更平滑的光照）
    float ndotl = dot(normal, lightDir);
    float halfLambert = ndotl * 0.5 + 0.5;
    float diffuse = clamp(halfLambert, 0.0, 1.0);

    // 镜面反射（Blinn-Phong）
    vec3 halfDir = normalize(lightDir + viewDir);
    float specular = pow(max(dot(normal, halfDir), 0.0), 32.0);

    // 边缘光（Rim Lighting）
    float rim = 1.0 - max(dot(normal, viewDir), 0.0);
    rim = smoothstep(0.4, 1.0, rim);

    // 组合所有光照成分
    vec3 finalColor = baseColor * (ambient + diffuse) +
    vec3(1.0) * specular * 0.3 +
    baseColor * rim * 0.2;

    return finalColor;
}

// 简单的雾效
vec3 applyFog(vec3 color, vec3 worldPos, vec3 cameraPos) {
    float distance = length(worldPos - cameraPos);
    float fogFactor = 1.0 - exp(-distance * 0.005);
    vec3 fogColor = vec3(0.5, 0.7, 1.0);

    return mix(color, fogColor, fogFactor * 0.7);
}

void main() {
    // 归一化向量
    vec3 normal = normalize(vNormal);
    vec3 lightDir = normalize(uLightPosition - vWorldPosition);
    vec3 viewDir = normalize(uCameraPosition - vWorldPosition);

    // 计算光照
    vec3 litColor = calculateLighting(normal, lightDir, viewDir, vColor);

    // 应用雾效
    vec3 finalColor = applyFog(litColor, vWorldPosition, uCameraPosition);

    // 水面特殊处理
    if (vHeight < 0.0) {
        // 添加水面波动效果
        float wave = sin(vWorldPosition.x * 3.0 + vWorldPosition.z * 2.0) * 0.02;
        finalColor += vec3(0.1, 0.1, 0.2) * wave;

        // 水面更亮
        finalColor *= 1.2;
    }

    // 输出最终颜色
    fragColor = vec4(finalColor, 1.0);

    // 调试：显示细分级别
    // fragColor = vec4(gl_TessCoord, 1.0); // 取消注释以可视化细分坐标
}