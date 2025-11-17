#version 300 es
precision mediump float;

in vec3 vColor;
in vec3 vNormal;
in vec3 vPosition;
in vec3 vWorldPosition;
in float vHeight;
flat in int vType;
in vec2 vTexCoord;

uniform vec3 uLightPosition;
uniform vec3 uCameraPosition;
uniform sampler2D uTerrainTexture;
uniform int uUseTexture;
uniform float minHeight;
uniform float maxHeight;

out vec4 fragColor;

// 平滑的高度到颜色转换函数 - 浅绿到深绿渐变
vec3 smoothHeightToColor(float height, float minHeight, float maxHeight) {
    if (height <= 0.0) {
        return vec3(1.0, 1.0, 1.0);
    }
    // 归一化高度到 [0, 1] 范围
    float normalizedHeight = clamp((height - minHeight) / (maxHeight - minHeight), 0.0, 1.0);

    // 浅绿色 (低处)
    vec3 lightGreen = vec3(0.6, 0.9, 0.6);
    // 深绿色 (高处)
    vec3 darkGreen = vec3(0.1, 0.4, 0.1);

    // 线性插值
    return mix(lightGreen, darkGreen, normalizedHeight);
}

// 边缘优化函数 - 只处理透明像素的边缘过渡
vec3 optimizeEdgeColor1(vec2 texCoord) {
    vec4 texColor = texture(uTerrainTexture, texCoord);
    vec3 stColor = smoothHeightToColor(vHeight, minHeight, maxHeight);

    // 如果是透明像素，使用最近的边缘颜色
    if (texColor.a < 0.1) {


//        return vec3(1.0, 1.0, 1.0);
        if (stColor.r < 1.0) {
//            vec2 edgeCoord = clamp(texCoord, 0.0, 1.0);
//            return texture(uTerrainTexture, edgeCoord).rgb;

            vec2 innerCoord;
            innerCoord.x = clamp(texCoord.x, 0.5, 0.5);
            innerCoord.y = clamp(texCoord.y, 0.5, 0.5);

            return texture(uTerrainTexture, innerCoord).rgb;
        } else {
            return stColor.rgb;
        }
//        // 计算到纹理边缘的距离
//        vec2 edgeDist = abs(texCoord - 0.5) * 2.0;
//        float maxEdgeDist = max(edgeDist.x, edgeDist.y);
//
//        // 如果完全在纹理外，使用最近的边缘颜色
//        if (maxEdgeDist > 4.0) {
//            vec2 edgeCoord = clamp(texCoord, 0.0, 1.0);
//            return texture(uTerrainTexture, edgeCoord).rgb;
//        }
//
//        // 在纹理内但透明，使用高度颜色
//        return smoothHeightToColor(vHeight, minHeight, maxHeight);
    }

    return texColor.rgb;
}

// 更激进的膨胀算法，修复严重腐蚀
vec3 optimizeEdgeColor4(vec2 texCoord) {
    vec4 texColor = texture(uTerrainTexture, texCoord);
    vec3 stColor = smoothHeightToColor(vHeight, minHeight, maxHeight);

    // 完全不透明
    if (texColor.a >= 0.98) {
        return texColor.rgb;
    }

    if (stColor.r < 1.0) {
        // 寻找周围不透明像素来直接替换
        vec2 texelSize = 1.0 / vec2(textureSize(uTerrainTexture, 0));

        // 采样5x5区域寻找不透明像素
        for (int x = -50; x <= 50; x=x+10) {
            for (int y = -50; y <= 50; y=y+10) {
                vec2 sampleCoord = texCoord + vec2(x, y) * texelSize;
                vec4 sampleColor = texture(uTerrainTexture, sampleCoord);

                // 找到第一个不透明像素就直接使用它的颜色
                if (sampleColor.a >= 0.98) {
                    return sampleColor.rgb;
                }
            }
        }
        return stColor.rgb;
    } else {
        return stColor.rgb;
    }
}

// 简化的边缘优化函数
vec3 optimizeEdgeColor2(vec2 texCoord) {
    vec4 texColor = texture(uTerrainTexture, texCoord);

    // 直接混合：纹理不透明时用纹理颜色，透明时用高度颜色
    float alpha = texColor.a;
    vec3 heightColor = smoothHeightToColor(vHeight, minHeight, maxHeight);

    return mix(heightColor, texColor.rgb, alpha);
}

// 边缘优化函数 - 处理半透明边缘颜色
vec3 optimizeEdgeColor(vec2 texCoord) {
    vec4 texColor = texture(uTerrainTexture, texCoord);

    // 如果像素完全不透明，直接返回颜色
    if (texColor.a >= 0.95) {
        return texColor.rgb;
    }

    // 如果像素完全透明，使用高度颜色
    if (texColor.a <= 0.05) {
        return smoothHeightToColor(vHeight, minHeight, maxHeight);
    }
    return vec3(1.0, 0.0, 0.0);

//    // 对于半透明像素（边缘区域），进行颜色修正
//    // 采样周围像素来获取正确的颜色
//    vec2 texelSize = 1.0 / vec2(textureSize(uTerrainTexture, 0));
//
//    // 采样周围4个方向的像素
//    vec4 colorRight = texture(uTerrainTexture, texCoord + vec2(texelSize.x, 0.0));
//    vec4 colorLeft = texture(uTerrainTexture, texCoord + vec2(-texelSize.x, 0.0));
//    vec4 colorUp = texture(uTerrainTexture, texCoord + vec2(0.0, texelSize.y));
//    vec4 colorDown = texture(uTerrainTexture, texCoord + vec2(0.0, -texelSize.y));
//
//    // 寻找最近的不透明像素颜色
//    vec3 opaqueColor = texColor.rgb;
//    float maxAlpha = texColor.a;
//
//    if (colorRight.a > maxAlpha) {
//        opaqueColor = colorRight.rgb;
//        maxAlpha = colorRight.a;
//    }
//    if (colorLeft.a > maxAlpha) {
//        opaqueColor = colorLeft.rgb;
//        maxAlpha = colorLeft.a;
//    }
//    if (colorUp.a > maxAlpha) {
//        opaqueColor = colorUp.rgb;
//        maxAlpha = colorUp.a;
//    }
//    if (colorDown.a > maxAlpha) {
//        opaqueColor = colorDown.rgb;
//        maxAlpha = colorDown.a;
//    }
//
//    // 如果找到了足够不透明的颜色，使用它来修正当前像素
//    if (maxAlpha > 0.8) {
//        // 根据当前像素的透明度来混合修正颜色
//        float blendFactor = clamp(texColor.a * 2.0, 0.0, 1.0); // 增强混合效果
//        return mix(opaqueColor, texColor.rgb, blendFactor);
//    }
//
//    // 如果没有找到合适的不透明颜色，使用当前颜色
//    return texColor.rgb;
}

vec3 optimizeEdgeColor5(vec2 texCoord) {
    vec4 texColor = texture(uTerrainTexture, texCoord);
    vec3 stColor = smoothHeightToColor(vHeight, minHeight, maxHeight);

    // 完全不透明
    if (texColor.a >= 0.98 && stColor.r < 1.0) {
        return texColor.rgb;
    }

    if (stColor.r < 1.0) {
        // 寻找周围不透明像素来直接替换
        vec2 texelSize = 1.0 / vec2(textureSize(uTerrainTexture, 0));

        // 采样附近区域寻找不透明像素
        for (int x = -200; x <= 200; x = x + 20) {
            for (int y = -200; y <= 200; y = y + 20) {
                vec2 sampleCoord = texCoord + vec2(x, y) * texelSize;
                sampleCoord = clamp(sampleCoord, 0.0, 1.0); // 边界保护
                vec4 sampleColor = texture(uTerrainTexture, sampleCoord);

                // 找到第一个不透明像素就直接使用它的颜色
                if (sampleColor.a >= 0.9) {
                    return sampleColor.rgb;
                }
            }
        }
        return stColor.rgb;
    } else {
        return stColor.rgb;
    }
}

vec3 optimizeEdgeColor6(vec2 texCoord) {
    vec4 texColor = texture(uTerrainTexture, texCoord);
    vec3 stColor = smoothHeightToColor(vHeight, minHeight, maxHeight);

    // 完全不透明
    if (texColor.a >= 0.98 && stColor.r < 1.0) {
        return texColor.rgb;
    } else {
        return vec3(1.0, 1.0, 1.0);
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

    // 使用纹理颜色作为基础颜色，或者与高度颜色混合
    vec3 baseColor;
    if (uUseTexture == 1) {
        baseColor = optimizeEdgeColor5(vTexCoord); // 使用优化后的边缘颜色
    } else {
        baseColor = smoothHeightToColor(vHeight, minHeight, maxHeight);
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