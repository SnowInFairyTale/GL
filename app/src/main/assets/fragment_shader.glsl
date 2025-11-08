#version 300 es
precision mediump float;

in vec3 vColor;
in vec3 vNormal;
in vec3 vPosition;

out vec4 fragColor;

void main() {
    // 简化光照计算
    vec3 lightDir = normalize(vec3(1.0, 1.0, 1.0));
    vec3 normal = normalize(vNormal);

    // 漫反射
    float diff = max(dot(normal, lightDir), 0.3);

    // 最终颜色（不使用环境光，直接使用原始颜色）
    vec3 finalColor = vColor * diff;
    fragColor = vec4(finalColor, 1.0);
}