#version 300 es
precision mediump float;

in vec3 vColor;
in vec3 vNormal;
in vec3 vPosition;
in vec3 vWorldPosition;

uniform vec3 uLightPosition;
uniform vec3 uCameraPosition;

out vec4 fragColor;

void main() {
    // 归一化向量
    vec3 normal = normalize(vNormal);
    vec3 lightDir = normalize(uLightPosition - vWorldPosition);
    vec3 viewDir = normalize(uCameraPosition - vWorldPosition);
    vec3 reflectDir = reflect(-lightDir, normal);

    // 环境光
    float ambient = 0.2;

    // 漫反射 - 使用半兰伯特光照模型，效果更好
    float diff = max(dot(normal, lightDir), 0.0);
    diff = diff * 0.5 + 0.5; // 半兰伯特

    // 镜面反射
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);

    // 根据材质类型调整光照
    float materialShininess = 8.0;
    float materialSpecular = 0.3;

    // 水面的特殊处理
    if (vColor.b > 0.5 && vColor.r < 0.2) { // 检测蓝色水面
        materialShininess = 64.0;
        materialSpecular = 0.8;
        spec = pow(max(dot(viewDir, reflectDir), 0.0), 128.0);
    }

    // 最终光照计算
    vec3 finalColor = vColor * (ambient + diff) +
    vec3(1.0) * spec * materialSpecular;

    // 增加一些雾效
    float fogDistance = length(vWorldPosition - uCameraPosition);
    float fogFactor = 1.0 - exp(-fogDistance * 0.005);
    vec3 fogColor = vec3(0.5, 0.7, 1.0); // 天空蓝雾色
    finalColor = mix(finalColor, fogColor, fogFactor * 0.3);

    // 色调增强和饱和度提升
    vec3 saturatedColor = mix(finalColor, vec3(dot(finalColor, vec3(0.299, 0.587, 0.114))), -0.1);

    fragColor = vec4(saturatedColor, 1.0);
}