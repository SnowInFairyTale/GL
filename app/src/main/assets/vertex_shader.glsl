#version 300 es
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aColor;

uniform mat4 uMVPMatrix;
uniform mat4 uModelMatrix;

out vec3 vColor;
out vec3 vPosition;

void main() {
    vColor = aColor;
    vPosition = vec3(uModelMatrix * vec4(aPosition, 1.0));
    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
    gl_PointSize = 8.0;
}