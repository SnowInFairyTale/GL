#version 300 es
precision mediump float;

in vec2 vTexCoord;

uniform sampler2D uRoofTexture;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(uRoofTexture, vTexCoord);
    fragColor = texColor;
}