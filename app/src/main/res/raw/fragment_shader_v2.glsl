#version 300 es
precision mediump float;

in vec3 vColor;
in vec3 vNormal;
in vec3 vPosition;
in vec3 vWorldPosition;
in float vHeight;
flat in int vType;
in vec2 vTexCoord;

uniform sampler2D uWallTexture;
uniform int uUseTexture;
uniform sampler2D uRoofTexture;

uniform float minHeight;
uniform float maxHeight;

uniform vec3 uLightPosition;
uniform vec3 uCameraPosition;

out vec4 fragColor;

// 地形类型常量
const int Road = 1;
const int WaterPool = 2;
const int Lawn = 3;
const int Canopy = 4;
const int Trunk = 5;
const int Building = 6;
const int HouseWall = 7;
const int Roof = 8;
const int Land = 0;

// 改进的光照计算
vec3 calculateAdvancedLighting(vec3 normal, vec3 lightDir, vec3 viewDir, vec3 baseColor) {
    // 环境光遮蔽模拟
    float ao = 1.0 - (1.0 - dot(normal, vec3(0, 1, 0))) * 0.3;

    // 半兰伯特漫反射
    float ndotl = dot(normal, lightDir);
    float halfLambert = ndotl * 0.5 + 0.5;
    float diffuse = clamp(halfLambert, 0.0, 1.0);

    // 边缘光
    float rim = 1.0 - max(dot(normal, viewDir), 0.0);
    rim = smoothstep(0.5, 1.0, rim);

    return baseColor * (0.3 + diffuse * 0.7) * ao + baseColor * rim * 0.1;
}

void main() {
    vec3 normal = normalize(vNormal);
    vec3 lightDir = normalize(uLightPosition - vWorldPosition);
    vec3 viewDir = normalize(uCameraPosition - vWorldPosition);

    vec3 baseColor = vColor;

    // 纹理应用
    if (uUseTexture == 1) {
        if (vType == HouseWall) {
            baseColor = texture(uWallTexture, vTexCoord).rgb;
        } else if (vType == Roof) {
            baseColor = texture(uRoofTexture, vTexCoord).rgb;
        }
    }

    // 应用改进的光照
    vec3 finalColor = calculateAdvancedLighting(normal, lightDir, viewDir, baseColor);

    // 水面特殊效果
    if (vType == WaterPool) {
        // 添加水面光泽
        vec3 reflectDir = reflect(-lightDir, normal);
        float specular = pow(max(dot(viewDir, reflectDir), 0.0), 64.0);
        finalColor += vec3(0.3, 0.3, 0.5) * specular;
    }

    fragColor = vec4(finalColor, 1.0);
}