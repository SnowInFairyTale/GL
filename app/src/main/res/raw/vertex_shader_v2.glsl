#version 300 es
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aColor;
layout(location = 2) in vec3 aNormal;
layout(location = 3) in int aType;
layout(location = 4) in vec2 aTexCoord;

uniform mat4 uMVPMatrix;
uniform mat4 uModelMatrix;
uniform vec3 uLightPosition;
uniform vec3 uCameraPosition;

out vec3 vColor;
out vec3 vNormal;
out vec3 vPosition;
out vec3 vWorldPosition;
out float vHeight;
flat out int vType;
out vec2 vTexCoord;

void main() {
    vColor = aColor;
    vNormal = aNormal;
    vPosition = aPosition;
    vTexCoord = aTexCoord;
    vType = aType;

    // 计算世界空间位置
    vWorldPosition = vec3(uModelMatrix * vec4(aPosition, 1.0));
    vHeight = aPosition.y;

    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
}