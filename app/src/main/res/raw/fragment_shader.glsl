#version 300 es
precision mediump float;

in vec3 vColor;
in vec3 vNormal;
in vec3 vPosition;
in vec3 vWorldPosition;
in float vHeight;// 新增：接收高度信息
flat in int vType;

uniform vec3 uLightPosition;
uniform vec3 uCameraPosition;

out vec4 fragColor;

// 地形类型常量
const int Road = 1;// 道路
const int WaterPool = 2;// 水池
const int Lawn = 3;// 草坪
const int Canopy = 4;// 树冠
const int Trunk = 5;// 树杆
const int Building = 6;// 建筑物
const int HouseWall = 7;// 屋墙
const int Roof = 8;// 屋顶
const int Land = 0;// 土地

// 定义颜色关键点
const vec3 deepWater = vec3(0.0, 0.2, 0.6);// 深蓝色 - 深水
const vec3 shallowWater = vec3(0.0, 0.4, 0.8);// 浅蓝色 - 浅水
const vec3 sand = vec3(0.9, 0.8, 0.5);// 沙色 - 沙滩
const vec3 grass = vec3(0.2, 0.6, 0.2);// 绿色 - 草地
const vec3 forest = vec3(0.1, 0.4, 0.1);// 深绿色 - 森林
const vec3 rock = vec3(0.5, 0.5, 0.5);// 灰色 - 岩石
const vec3 snow = vec3(0.9, 0.9, 0.9);// 白色 - 雪地


vec3 smoothHeightWaterPool(float height) {
    if (height <= 0.0) {
        // 将高度映射到0-1范围
        float minHeight = -5.0;
        float maxHeight = 0.0;
        float normalizedHeight = (height - minHeight) / (maxHeight - minHeight);
        normalizedHeight = clamp(normalizedHeight, 0.0, 1.0);
        // 使用mix函数在低处和高处颜色之间插值
        return mix(deepWater, shallowWater, normalizedHeight);
    } else {
        // 将高度映射到0-1范围
        float minHeight = -2.0;
        float maxHeight = 10.0;
        float normalizedHeight = (height - minHeight) / (maxHeight - minHeight);
        normalizedHeight = clamp(normalizedHeight, 0.0, 1.0);

        // 使用mix函数在低处和高处颜色之间插值
        return mix(vec3(0.7, 0.6, 0.4), vec3(0.3, 0.2, 0.1), normalizedHeight);
    }
}

vec3 smoothHeightLawn(float height) {
    // 将高度映射到0-1范围
    float minHeight = 0.0;
    float maxHeight = 5.0;
    float normalizedHeight = (height - minHeight) / (maxHeight - minHeight);
    normalizedHeight = clamp(normalizedHeight, 0.0, 1.0);

    // 使用mix函数在低处和高处颜色之间插值
    return mix(grass, forest, normalizedHeight);
}

// 根据高度获取地面颜色
vec3 getLandColorByHeight(float height) {
    // 将高度映射到0-1范围
    float minHeight = -2.0;
    float maxHeight = 10.0;
    float normalizedHeight = (height - minHeight) / (maxHeight - minHeight);
    normalizedHeight = clamp(normalizedHeight, 0.0, 1.0);

    // 使用mix函数在低处和高处颜色之间插值
    return mix(vec3(0.7, 0.6, 0.4), vec3(0.3, 0.2, 0.1), normalizedHeight);
}

// 平滑的高度颜色映射函数
vec3 smoothHeightToColor(float height) {
    // 平滑的颜色过渡
    if (height < -1.5) {
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

    vec3 baseColor;
    if (vType == WaterPool) {
        // 特殊区域保持原有颜色
        baseColor = smoothHeightWaterPool(vHeight);

        // 水面的特殊处理
        materialShininess = 64.0;
        materialSpecular = 0.6;
        spec = pow(max(dot(viewDir, reflectDir), 0.0), 128.0);

        // 水面增加波动效果
        float wave = sin(vWorldPosition.x * 3.0 + vWorldPosition.z * 2.0) * 0.05;
        diff += wave * 0.1;
    } else if (vType == Land || vType == Building) {
        // 普通地形使用高度颜色映射
        baseColor = getLandColorByHeight(vHeight);// 线性变化
    } else if (vType == Lawn) {
        baseColor = smoothHeightLawn(vHeight);
    } else {
        baseColor = vColor;
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