#version 300 es
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aColor;
layout(location = 2) in vec3 aNormal;

uniform mat4 uMVPMatrix;
uniform mat4 uModelMatrix;
uniform vec3 uLightPosition;
uniform vec3 uCameraPosition; // 新增相机位置

out vec3 vColor;
out vec3 vNormal;
out vec3 vPosition;
out vec3 vWorldPosition; // 世界空间位置
out float vHeight; // 新增：传递高度信息

void main() {
    vColor = aColor;
    vNormal = aNormal;
    vPosition = aPosition;

    // 计算世界空间位置
    vWorldPosition = vec3(uModelMatrix * vec4(aPosition, 1.0));
    vHeight = aPosition.y; // 传递原始高度信息

    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
}