uniform mat4 uMVPMatrix;
attribute vec4 aPosition;

varying float vHeight;

void main() {
    gl_Position = uMVPMatrix * aPosition;
    vHeight = aPosition.y;
}