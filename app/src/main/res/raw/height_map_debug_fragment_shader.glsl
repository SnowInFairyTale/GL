#version 300 es
precision mediump float;

in vec2 vTexCoord;
uniform sampler2D uHeightMap;
out vec4 fragColor;

void main() {
    // 直接显示彩色纹理，不进行灰度转换
    fragColor = texture(uHeightMap, vTexCoord);
}