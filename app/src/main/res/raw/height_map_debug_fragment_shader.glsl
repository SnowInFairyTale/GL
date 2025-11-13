#version 300 es
precision mediump float;

in vec2 vTexCoord;
uniform sampler2D uHeightMap;
out vec4 fragColor;

void main() {
    // 直接采样纹理并显示
    vec4 textureColor = texture(uHeightMap, vTexCoord);

    // 方法1：直接显示原始纹理颜色
    fragColor = textureColor;

    // 方法2：如果纹理是单通道高度图，显示为灰度（取消注释使用）
    // float height = textureColor.r;
    // fragColor = vec4(height, height, height, 1.0);

    // 方法3：调试模式 - 显示UV坐标（取消注释使用）
    // fragColor = vec4(vTexCoord, 0.0, 1.0);

    // 方法4：如果还是黑色，强制显示红色来测试
    // if (textureColor.r + textureColor.g + textureColor.b < 0.1) {
    //     fragColor = vec4(1.0, 0.0, 0.0, 1.0); // 显示红色表示纹理采样失败
    // } else {
    //     fragColor = textureColor;
    // }
}