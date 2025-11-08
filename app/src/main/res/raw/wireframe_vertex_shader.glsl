#version 300 es
layout(location = 0) in vec3 aPosition;

uniform mat4 uMVPMatrix;

out float vPointSize;

void main() {
    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
    gl_PointSize = 6.0; // 在着色器中设置点大小
    vPointSize = 6.0;
}