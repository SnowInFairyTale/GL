#version 300 es
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aColor;
layout(location = 2) in vec3 aNormal;

uniform mat4 uMVPMatrix;
uniform mat4 uModelMatrix;
uniform vec3 uLightPosition;

out vec3 vColor;
out vec3 vNormal;
out vec3 vPosition;

void main() {
    vColor = aColor;
    vNormal = aNormal; // 直接使用法线，不转换
    vPosition = aPosition; // 直接使用位置
    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
}