#version 300 es
precision mediump float;

in vec3 vColor;
in vec3 vPosition;

out vec4 fragColor;

void main() {
    // 为不同元素添加一些简单的光照效果
    vec3 lightDir = normalize(vec3(1.0, 1.0, 1.0));
    float diff = max(dot(normalize(vPosition), lightDir), 0.2);

    vec3 finalColor = vColor * diff;
    fragColor = vec4(finalColor, 1.0);
}