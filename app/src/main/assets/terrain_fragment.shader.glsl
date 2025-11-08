precision mediump float;
varying float vHeight;

void main() {
    // 根据高度值着色
    vec3 color;
    if (vHeight > 0.3) {
        color = vec3(0.6, 0.6, 0.6); // 雪地
    } else if (vHeight > 0.1) {
        color = vec3(0.2, 0.6, 0.2); // 森林
    } else if (vHeight > -0.1) {
        color = vec3(0.8, 0.7, 0.4); // 土地
    } else {
        color = vec3(0.0, 0.2, 0.8); // 水域
    }

    gl_FragColor = vec4(color, 1.0);
}