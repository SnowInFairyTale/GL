#version 320 es
#extension GL_EXT_tessellation_shader : require

layout(vertices = 3) out;

uniform float uTessLevel;
uniform vec3 uCameraPosition;

void main() {
    gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;

    if (gl_InvocationID == 0) {
        // 基于距离和曲率的自适应细分
        vec3 center = (gl_in[0].gl_Position.xyz +
                      gl_in[1].gl_Position.xyz +
                      gl_in[2].gl_Position.xyz) / 3.0;

        float distance = length(center - uCameraPosition);
        float adaptiveLevel = max(2.0, uTessLevel * (50.0 / (distance + 1.0)));

        gl_TessLevelInner[0] = adaptiveLevel;
        gl_TessLevelOuter[0] = adaptiveLevel;
        gl_TessLevelOuter[1] = adaptiveLevel;
        gl_TessLevelOuter[2] = adaptiveLevel;
    }
}