#version 300 es
precision mediump float;

in vec3 vColor;
in vec3 vNormal;
in vec3 vPosition;

out vec4 fragColor;

void main() {
    // 直接使用颜色，不计算光照
    fragColor = vec4(vColor, 1.0);
}