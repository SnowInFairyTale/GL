#version 320 es
#extension GL_EXT_geometry_shader : require

layout(triangles) in;
layout(triangle_strip, max_vertices = 3) out;

out vec3 gNormal;
out vec3 gWorldPosition;

void main() {
    // 计算面法线
    vec3 a = gl_in[1].gl_Position.xyz - gl_in[0].gl_Position.xyz;
    vec3 b = gl_in[2].gl_Position.xyz - gl_in[0].gl_Position.xyz;
    vec3 faceNormal = normalize(cross(a, b));

    for (int i = 0; i < 3; i++) {
        gl_Position = gl_in[i].gl_Position;
        gNormal = faceNormal;
        gWorldPosition = gl_in[i].gl_Position.xyz;
        EmitVertex();
    }
    EndPrimitive();
}