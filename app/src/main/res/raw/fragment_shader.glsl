#version 300 es
precision mediump float;

in vec3 vColor;
in vec3 vNormal;
in vec3 vPosition;
in vec3 vWorldPosition;
in float vHeight;// 新增：接收高度信息
flat in int vType;
in vec2 vTexCoord;

uniform sampler2D uTexture;
uniform float minHeight;  // 第一个float值
uniform float maxHeight;  // 第二个float值

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
const vec3 deepGrass = vec3(0.1, 0.4, 0.1);// 深绿色 - 深草滩
const vec3 grass = vec3(0.2, 0.6, 0.2);// 绿色 - 草地
const vec3 sand = vec3(0.7, 0.6, 0.4);// 沙色 - 沙滩
const vec3 land = vec3(0.3, 0.2, 0.1);// 土色 - 土地
const vec3 rock = vec3(0.5, 0.5, 0.5);// 灰色 - 岩石
const vec3 snow = vec3(0.9, 0.9, 0.9);// 白色 - 雪地

// 判断是否为特殊区域
bool isSpecialArea(vec3 color) {
    return vType == Road || vType == Canopy || vType == Trunk;
}


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
    return mix(grass, deepGrass, normalizedHeight);
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
vec3 smoothHeightToColor(float height, float minHeight, float maxHeight) {
    // 基于动态minHeight和maxHeight的平滑颜色过渡
    float totalRange = maxHeight - minHeight;

    // 水域过渡（minHeight以下）
    if (height < minHeight - 1.0) {
        // 深水区域
        return deepWater;
    } else if (height < minHeight - 0.5) {
        // 深水到浅水过渡
        float rangeStart = minHeight - 1.0;
        float t = (height - rangeStart) / 0.5;
        t = clamp(t, 0.0, 1.0);
        return mix(deepWater, shallowWater, t);
    } else if (height < minHeight) {
        // 浅水到深草滩过渡
        float rangeStart = minHeight - 0.5;
        float t = (height - rangeStart) / 0.5;
        t = clamp(t, 0.0, 1.0);
        return mix(shallowWater, deepGrass, t);
    }
    // 陆地过渡（minHeight以上）
    else if (height < minHeight + totalRange * 0.15) {
        // 深草滩到浅草滩过渡
        float rangeStart = minHeight;
        float rangeEnd = minHeight + totalRange * 0.15;
        float t = (height - rangeStart) / (rangeEnd - rangeStart);
        t = clamp(t, 0.0, 1.0);
        return mix(deepGrass, grass, t);
    } else if (height < minHeight + totalRange * 0.3) {
        // 浅草滩到沙滩过渡
        float rangeStart = minHeight + totalRange * 0.15;
        float rangeEnd = minHeight + totalRange * 0.3;
        float t = (height - rangeStart) / (rangeEnd - rangeStart);
        t = clamp(t, 0.0, 1.0);
        return mix(grass, sand, t);
    } else if (height < minHeight + totalRange * 0.45) {
        // 沙滩到土地过渡
        float rangeStart = minHeight + totalRange * 0.3;
        float rangeEnd = minHeight + totalRange * 0.45;
        float t = (height - rangeStart) / (rangeEnd - rangeStart);
        t = clamp(t, 0.0, 1.0);
        return mix(sand, land, t);
    } else if (height < minHeight + totalRange * 0.6) {
        // 土地到岩石过渡
        float rangeStart = minHeight + totalRange * 0.45;
        float rangeEnd = minHeight + totalRange * 0.6;
        float t = (height - rangeStart) / (rangeEnd - rangeStart);
        t = clamp(t, 0.0, 1.0);
        return mix(land, rock, t);
    } else if (height < maxHeight) {
        // 岩石到雪地过渡
        float rangeStart = minHeight + totalRange * 0.75;
        float rangeEnd = maxHeight;
        float t = (height - rangeStart) / (rangeEnd - rangeStart);
        t = clamp(t, 0.0, 1.0);
        return mix(rock, snow, t);
    } else {
        // 最高区域
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

    // 判断是否为特殊区域
    bool isSpecial = isSpecialArea(vColor);
    vec3 baseColor;
    if (isSpecial) {
        baseColor = vColor;
    } else if (vType == HouseWall || vType == Roof) {
        // 对建筑物使用纹理
        vec4 texColor = texture(uTexture, vTexCoord);
        baseColor = texColor.rgb;
    } else {
        baseColor = smoothHeightToColor(vHeight, minHeight, maxHeight);
    }
//    if (vType == WaterPool) {
//        // 特殊区域保持原有颜色
//        baseColor = smoothHeightWaterPool(vHeight);
//
//        // 水面的特殊处理
//        materialShininess = 64.0;
//        materialSpecular = 0.6;
//        spec = pow(max(dot(viewDir, reflectDir), 0.0), 128.0);
//
//        // 水面增加波动效果
//        float wave = sin(vWorldPosition.x * 3.0 + vWorldPosition.z * 2.0) * 0.05;
//        diff += wave * 0.1;
//    } else if (vType == Land || vType == Building) {
//        // 普通地形使用高度颜色映射
//        baseColor = getLandColorByHeight(vHeight);// 线性变化
//    } else if (vType == Lawn) {
//        baseColor = smoothHeightLawn(vHeight);
//    } else {
//        baseColor = vColor;
//    }

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