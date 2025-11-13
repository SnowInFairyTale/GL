#version 300 es
precision mediump float;

in vec2 vTexCoord;
uniform sampler2D uRoofTexture;
out vec4 fragColor;

void main() {
    // 高度图通常是单通道的，取r分量作为高度值
    float height = texture(uRoofTexture, vTexCoord).r;

    // 将高度值映射到灰度颜色
    fragColor = vec4(vec3(height), 1.0);
}