#version 300 es
precision mediump float;

in vec3 vColor;
in vec3 vNormal;
in vec3 vPosition;
in vec3 vWorldPosition;
in float vHeight;
flat in int vType;
in vec2 vTexCoord;

uniform vec3 uLightPosition;
uniform vec3 uCameraPosition;
uniform sampler2D uTerrainTexture;  // 添加纹理uniform
uniform int uUseTexture;
uniform float minHeight;
uniform float maxHeight;

out vec4 fragColor;

// 平滑的高度到颜色转换函数 - 浅绿到深绿渐变
vec3 smoothHeightToColor(float height, float minHeight, float maxHeight) {
    if (height <= 0.0) {
        return vec3(1.0, 1.0, 1.0);
    }
    // 归一化高度到 [0, 1] 范围
    float normalizedHeight = clamp((height - minHeight) / (maxHeight - minHeight), 0.0, 1.0);

    // 浅绿色 (低处)
    vec3 lightGreen = vec3(0.6, 0.9, 0.6);
    // 深绿色 (高处)
    vec3 darkGreen = vec3(0.1, 0.4, 0.1);

    // 线性插值
    return mix(lightGreen, darkGreen, normalizedHeight);
}

void main() {
    // 采样纹理
    vec4 texColor = texture(uTerrainTexture, vTexCoord);

    // 归一化向量
    vec3 normal = normalize(vNormal);
    vec3 lightDir = normalize(uLightPosition - vWorldPosition);
    vec3 viewDir = normalize(uCameraPosition - vWorldPosition);
    vec3 reflectDir = reflect(-lightDir, normal);

    // 环境光
    float ambient = 0.25;

    // 漫反射 - 使用半兰伯特光照模型
    float diff = max(dot(normal, lightDir), 0.0);
    diff = diff * 0.6 + 0.4;

    // 镜面反射
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);

    // 根据材质类型调整光照参数
    float materialShininess = 8.0;
    float materialSpecular = 0.2;

    // 使用纹理颜色作为基础颜色，或者与高度颜色混合
    vec3 baseColor;
    if (uUseTexture == 1) {
        baseColor= texColor.rgb;// 直接使用纹理颜色
    } else {
        baseColor = smoothHeightToColor(vHeight, minHeight, maxHeight);
    }
    if (texColor.a < 0.1) {
        // 这里边缘效果不好
        baseColor = smoothHeightToColor(vHeight, minHeight, maxHeight);
    }
    // 或者与高度颜色混合：vec3 baseColor = mix(texColor.rgb, smoothHeightToColor(vHeight, minHeight, maxHeight), 0.5);

    // 最终光照计算
    vec3 finalColor = baseColor * (ambient + diff) +
    vec3(1.0) * spec * materialSpecular;

    // 修正雾效：只在远距离应用，且强度降低
    float fogDistance = length(vWorldPosition - uCameraPosition);
    float fogFactor = 1.0 - exp(-fogDistance * 0.002);
    vec3 fogColor = vec3(0.5, 0.7, 1.0);

    // 只在较远距离应用雾效
    if (fogDistance > 50.0) {
        finalColor = mix(finalColor, fogColor, fogFactor * 0.5);
    }

    // 色调增强（适度）
    vec3 saturatedColor = mix(finalColor, vec3(dot(finalColor, vec3(0.299, 0.587, 0.114))), -0.05);

    fragColor = vec4(saturatedColor, 1.0);
}