#version 320 es
precision mediump float;

layout(location = 0) in vec3 aPosition;

void main() {
    // 曲面细分只需要传递顶点位置
    // 其他属性会在细分阶段计算
    gl_Position = vec4(aPosition, 1.0);
}