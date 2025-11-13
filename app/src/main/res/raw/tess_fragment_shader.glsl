#version 320 es
precision mediump float;

in highp vec3 vWorldPosition;
in highp vec3 vNormal;
in mediump vec3 vColor;
in mediump vec2 vTexCoord;
in highp float vHeight;

uniform highp vec3 uLightPosition;
uniform highp vec3 uCameraPosition;

out mediump vec4 fragColor;

// 改进的光照计算
mediump vec3 calculateLighting(highp vec3 normal, highp vec3 lightDir, highp vec3 viewDir, mediump vec3 baseColor) {
    // 环境光
    mediump float ambient = 0.3;

    // 半兰伯特漫反射
    mediump float ndotl = dot(normal, lightDir);
    mediump float halfLambert = ndotl * 0.5 + 0.5;
    mediump float diffuse = clamp(halfLambert, 0.0, 1.0);

    // 镜面反射
    highp vec3 halfDir = normalize(lightDir + viewDir);
    mediump float specular = pow(max(dot(normal, halfDir), 0.0), 32.0);

    // 边缘光
    mediump float rim = 1.0 - max(dot(normal, viewDir), 0.0);
    rim = smoothstep(0.4, 1.0, rim);

    // 组合所有光照成分
    mediump vec3 finalColor = baseColor * (ambient + diffuse) +
    vec3(1.0) * specular * 0.3 +
    baseColor * rim * 0.2;

    return finalColor;
}

// 简单的雾效
mediump vec3 applyFog(mediump vec3 color, highp vec3 worldPos, highp vec3 cameraPos) {
    highp float distance = length(worldPos - cameraPos);
    mediump float fogFactor = 1.0 - exp(-distance * 0.005);
    mediump vec3 fogColor = vec3(0.5, 0.7, 1.0);

    return mix(color, fogColor, fogFactor * 0.7);
}

void main() {
    // 归一化向量
    highp vec3 normal = normalize(vNormal);
    highp vec3 lightDir = normalize(uLightPosition - vWorldPosition);
    highp vec3 viewDir = normalize(uCameraPosition - vWorldPosition);

    // 计算光照
    mediump vec3 litColor = calculateLighting(normal, lightDir, viewDir, vColor);

    // 应用雾效
    mediump vec3 finalColor = applyFog(litColor, vWorldPosition, uCameraPosition);

    // 水面特殊处理
    if (vHeight < 0.0) {
        // 添加水面波动效果
        mediump float wave = sin(vWorldPosition.x * 3.0 + vWorldPosition.z * 2.0) * 0.02;
        finalColor += vec3(0.1, 0.1, 0.2) * wave;

        // 水面更亮
        finalColor *= 1.2;
    }

    // 输出最终颜色
    fragColor = vec4(finalColor, 1.0);
}