#version 300 es
precision mediump float;

in vec3 vColor;
in vec3 vNormal;
in vec3 vPosition;
in vec3 vWorldPosition;
in float vHeight; // 新增：接收高度信息

uniform vec3 uLightPosition;
uniform vec3 uCameraPosition;

out vec4 fragColor;

// 平滑的高度颜色映射函数
vec3 smoothHeightToColor(float height) {
    // 定义颜色关键点
    vec3 deepWater = vec3(0.0, 0.2, 0.6);    // 深蓝色 - 深水
    vec3 shallowWater = vec3(0.0, 0.4, 0.8); // 浅蓝色 - 浅水
    vec3 sand = vec3(0.9, 0.8, 0.5);         // 沙色 - 沙滩
    vec3 grass = vec3(0.2, 0.6, 0.2);        // 绿色 - 草地
    vec3 forest = vec3(0.1, 0.4, 0.1);       // 深绿色 - 森林
    vec3 rock = vec3(0.5, 0.5, 0.5);         // 灰色 - 岩石
    vec3 snow = vec3(0.9, 0.9, 0.9);         // 白色 - 雪地

    // 平滑的颜色过渡
    if (height < -1.5) {
        return deepWater;
    } else if (height < -1.0) {
        float t = (height + 1.5) / 0.5;
        return mix(deepWater, shallowWater, t);
    } else if (height < 0.0) {
        float t = (height + 1.0) / 1.0;
        return mix(shallowWater, sand, t);
    } else if (height < 2.0) {
        float t = height / 2.0;
        return mix(sand, grass, t);
    } else if (height < 4.0) {
        float t = (height - 2.0) / 2.0;
        return mix(grass, forest, t);
    } else if (height < 7.0) {
        float t = (height - 4.0) / 3.0;
        return mix(forest, rock, t);
    } else if (height < 10.0) {
        float t = (height - 7.0) / 3.0;
        return mix(rock, snow, t);
    } else {
        return snow;
    }
}

// 判断是否为特殊区域
bool isSpecialArea(vec3 color) {
    // 水面检测：蓝色分量高，红色分量低
    bool isWater = color.b > 0.5 && color.r < 0.3;

    // 道路检测：接近灰色
    bool isRoad = length(color - vec3(0.4, 0.4, 0.4)) < 0.2;

    // 建筑物检测：棕色系
    bool isBuilding = color.r > 0.5 && color.g < 0.4 && color.b < 0.3;

    // 草坪检测：绿色系
    bool isLawn = color.g > 0.5 && color.r < 0.4 && color.b < 0.4;

    return isWater || isRoad || isBuilding || isLawn;
}

void main() {
    // 归一化向量
    vec3 normal = normalize(vNormal);
    vec3 lightDir = normalize(uLightPosition - vWorldPosition);
    vec3 viewDir = normalize(uCameraPosition - vWorldPosition);
    vec3 reflectDir = reflect(-lightDir, normal);

    // 环境光
    float ambient = 0.25;

    // 漫反射 - 使用半兰伯特光照模型
    float diff = max(dot(normal, lightDir), 0.0);
    diff = diff * 0.6 + 0.4;

    // 镜面反射
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);

    // 根据材质类型调整光照参数
    float materialShininess = 8.0;
    float materialSpecular = 0.2;

    // 判断是否为特殊区域
    bool isSpecial = isSpecialArea(vColor);

    vec3 baseColor;
    if (isSpecial) {
        // 特殊区域保持原有颜色
        baseColor = vColor;

        // 水面的特殊处理
        if (vColor.b > 0.5 && vColor.r < 0.3) {
            materialShininess = 64.0;
            materialSpecular = 0.6;
            spec = pow(max(dot(viewDir, reflectDir), 0.0), 128.0);

            // 水面增加波动效果
            float wave = sin(vWorldPosition.x * 3.0 + vWorldPosition.z * 2.0) * 0.05;
            diff += wave * 0.1;
        }
    } else {
        // 普通地形使用高度颜色映射
        baseColor = smoothHeightToColor(vHeight);
    }

    // 最终光照计算
    vec3 finalColor = baseColor * (ambient + diff) +
    vec3(1.0) * spec * materialSpecular;

    // 修正雾效：只在远距离应用，且强度降低
    float fogDistance = length(vWorldPosition - uCameraPosition);
    float fogFactor = 1.0 - exp(-fogDistance * 0.002);
    vec3 fogColor = vec3(0.5, 0.7, 1.0);

    // 只在较远距离应用雾效
    if (fogDistance > 50.0) {
        finalColor = mix(finalColor, fogColor, fogFactor * 0.5);
    }

    // 色调增强（适度）
    vec3 saturatedColor = mix(finalColor, vec3(dot(finalColor, vec3(0.299, 0.587, 0.114))), -0.05);

    fragColor = vec4(saturatedColor, 1.0);
}